package org.pspace.common.web.dao.ldap;

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.pspace.common.api.Person;
import org.pspace.common.web.dao.LDAPDao;
import org.pspace.common.web.mvc.PhoneEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created at 08.12.11 - 07:30
 *
 * @author peach
 */
public class LDAPDaoApacheDS implements LDAPDao, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LDAPDaoApacheDS.class);

    private final static String PW_ENCODING_ALGORITHM = "SHA";

    private static final PhoneEditor PHONE_EDITOR = new PhoneEditor();
    public static final int CACHE_SIZE = 100;

    private String rootDomain;
    private String rootDn;

    private DirectoryService directoryService;
    private LdapServer ldapServer;
    private int port = 10389;

    public void setPort(int port) {
        this.port = port;
    }

    public void setRootDomain(String rootDomain) {
        this.rootDomain = rootDomain;
    }

    @Override
    public void destroy() throws Exception {
        directoryService.shutdown();
        ldapServer.stop();
    }


    public void afterPropertiesSet() throws Exception {
        try {

            // example.com
            String[] rootDnParts = rootDomain.split("\\.");
            assert rootDnParts.length >= 1;
            // dc=example,dc=com
            rootDn = StringUtils.collectionToDelimitedString(Arrays.asList(rootDnParts), ",", "dc=", "");
            // example
            String rootDc = rootDnParts[0];

            DefaultDirectoryServiceFactory factory = new DefaultDirectoryServiceFactory();
            factory.init("Test");
            directoryService = factory.getDirectoryService();
            directoryService.setAllowAnonymousAccess(true);
            directoryService.setAccessControlEnabled(false);

            PartitionFactory partitionFactory = factory.getPartitionFactory();

            Partition partition = partitionFactory.createPartition(directoryService.getSchemaManager(), rootDc, rootDn, CACHE_SIZE, getTempDir());

//
//            Partition partition = new JdbmPartition(directoryService.getSchemaManager());
//            partition.setId(rootDc);
//            partition.setSuffixDn(new Dn(rootDn));
            directoryService.addPartition(partition);


            // start the service
            directoryService.startup();

            // create the root entry
            if (!directoryService.getAdminSession().exists(partition.getSuffixDn())) {
                Dn dnApache = new Dn(rootDn);
                Entry entryApache = directoryService.newEntry(dnApache);
                entryApache.add("objectClass", "top", "domain", "extensibleObject");
                entryApache.add("dc", rootDc);
                directoryService.getAdminSession().add(entryApache);
            }

            // start the actual server that listens on the port
            ldapServer = new LdapServer();
            ldapServer.setDirectoryService(directoryService);
            ldapServer.setTransports(new TcpTransport(port));
            ldapServer.start();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    private File getTempDir() {
        try {
            File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
            if (!(temp.delete())) {
                throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
            }

            if (!(temp.mkdir())) {
                throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
            }
            return temp;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updatePerson(Person person) {
        try {

            String trim = "" + person.getId();
            Dn memberDn = new Dn("cn=" + trim + "," + rootDn);

            // check if the entry already exists

            CoreSession adminSession = directoryService.getAdminSession();
            Entry memberEntry = null;
            try {
                memberEntry = adminSession.lookup(memberDn);
            } catch (LdapNoSuchObjectException e) {
                // nothing
            }
            Map<String, String> updates = getUpdateMapFromPerson(person, trim);

            if (memberEntry != null) {
                ArrayList<Modification> modifications = new ArrayList<Modification>();
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    if (entry.getValue() == null) {
                        if (memberEntry.containsAttribute(entry.getKey())) {
                            modifications.add(new DefaultModification(ModificationOperation.REMOVE_ATTRIBUTE, entry.getKey()));
                        }
                    } else {
                        modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, entry.getKey(), entry.getValue()));
                    }
                }

                adminSession.modify(memberDn, modifications);
            } else {
                memberEntry = directoryService.newEntry(memberDn);
                memberEntry.add("objectClass", "top", "person", "inetOrgPerson", "organizationalPerson");

                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        memberEntry.add(entry.getKey(), entry.getValue());
                    }
                }

                adminSession.add(memberEntry);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String nullify(String str) {
        return str == null || str.isEmpty() ? null : str;
    }


    private Map<String, String> getUpdateMapFromPerson(Person person, String fullName) {

        Map<String, String> updates = new HashMap<String, String>();

        updates.put("employeeNumber", "" + person.getId());
        updates.put("cn", person.getId().toString());
        updates.put("displayName", person.getFullName());
        updates.put("givenName", person.getPrename());
        updates.put("sn", person.getLastname());
        updates.put("o", person.getOrganizationName());
//                updates.put("countryName", "Germany");

        updates.put("ou", nullify(person.getOrganizationalUnitName()));
        updates.put("uid", nullify(person.getUsername()));
        updates.put("userpassword", encodePass(person.getPassword()));
        updates.put("title", nullify(person.getPrefix()));
        updates.put("mail", nullify(person.getEmail()));

        if (person.getPhone() != null && !person.getPhone().isEmpty()) {
            PHONE_EDITOR.setAsText(person.getPhone());
            updates.put("telephoneNumber", PHONE_EDITOR.getValue().toString());
        } else {
            updates.put("telephoneNumber", null);
        }
        if (person.getMobile() != null && !person.getMobile().isEmpty()) {
            PHONE_EDITOR.setAsText(person.getMobile());
            updates.put("mobile", PHONE_EDITOR.getValue().toString());
        } else {
            updates.put("mobile", null);
        }

        updates.put("street", nullify(person.getStreet()));
        updates.put("l", nullify(person.getCity()));
        updates.put("postalCode", nullify(person.getPostalCode()));

        return updates;
    }

    private static String encodePass(String password) {

        if (password == null || password.isEmpty()) return password;

        try {
            MessageDigest md = MessageDigest.getInstance(PW_ENCODING_ALGORITHM);
            md.update(password.getBytes());
            byte[] digest = md.digest();
            byte[] encodedDigest = Base64.getEncoder().encode(digest);
            return String.format("{%s}%s", PW_ENCODING_ALGORITHM, new String(encodedDigest));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }
}
