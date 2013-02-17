package org.pspace.common.web.dao.ldap;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
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
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
            String trim = person.getFullName().trim();
            Dn memberDn = new Dn("cn=" + trim + "," + rootDn);

            Entry memberEntry = directoryService.newEntry(memberDn);
            memberEntry.add("objectClass", "top", "person", "inetOrgPerson", "organizationalPerson");
            memberEntry.add("employeeNumber", "" + person.getId());
            memberEntry.add("cn", trim);
            memberEntry.add("displayName", trim);
            memberEntry.add("givenName", person.getPrename());
            memberEntry.add("sn", person.getLastname());
            memberEntry.add("o", person.getOrganizationName());
//            memberEntry.add("countryName", "Germany");

            if (person.getOrganizationalUnitName() != null && !person.getOrganizationalUnitName().isEmpty()) memberEntry.add("ou", person.getOrganizationalUnitName());
            if (person.getUsername() != null && !person.getUsername().isEmpty()) memberEntry.add("uid", person.getUsername());
            if (person.getPassword() != null && !person.getPassword().isEmpty()) memberEntry.add("userpassword", encodePass(person.getPassword()));
            if (person.getPrefix() != null && !person.getPrefix().isEmpty()) memberEntry.add("title", person.getPrefix());
            if (person.getEmail() != null && !person.getEmail().isEmpty()) memberEntry.add("mail", person.getEmail());
            if (person.getPhone() != null && !person.getPhone().isEmpty()) {
                PHONE_EDITOR.setAsText(person.getPhone());
                memberEntry.add("telephoneNumber", PHONE_EDITOR.getValue().toString());
            }
            if (person.getMobile() != null && !person.getMobile().isEmpty()) {
                PHONE_EDITOR.setAsText(person.getMobile());
                memberEntry.add("mobile", PHONE_EDITOR.getValue().toString());
            }
            if (person.getStreet() != null && !person.getStreet().isEmpty()) memberEntry.add("street", person.getStreet());
            if (person.getCity() != null && !person.getCity().isEmpty()) memberEntry.add("l", person.getCity());
            if (person.getPostalCode() != null && !person.getPostalCode().isEmpty()) memberEntry.add("postalCode", person.getPostalCode());
            directoryService.getAdminSession().add(memberEntry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodePass(String password) {

        try {
            MessageDigest md = MessageDigest.getInstance(PW_ENCODING_ALGORITHM);
            md.update(password.getBytes());
            byte[] bytes = md.digest();
            BASE64Encoder base64encoder = new BASE64Encoder();
            return String.format("{%s}%s", PW_ENCODING_ALGORITHM, base64encoder.encode(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }
}
