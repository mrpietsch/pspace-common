package org.pspace.common.web.dao.ldap;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.pspace.common.api.Person;
import org.pspace.common.web.dao.LDAPDao;
import org.pspace.common.web.mvc.PhoneEditor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import sun.misc.BASE64Encoder;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created at 08.12.11 - 07:30
 *
 * @author peach
 */
public class LDAPDaoApacheDS implements LDAPDao, InitializingBean, DisposableBean {

    private final static String PW_ENCODING_ALGORITHM = "SHA";

    private static final PhoneEditor PHONE_EDITOR = new PhoneEditor();

    private String rootDomain;
    private String rootDn;

    private DefaultDirectoryService directoryService;
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

        // example.com
        String[] rootDnParts = rootDomain.split("\\.");
        assert rootDnParts.length >= 1;
        // dc=example,dc=com
        rootDn = StringUtils.collectionToDelimitedString(Arrays.asList(rootDnParts),",","dc=","");
        // example
        String rootDc = rootDnParts[0];

        directoryService = new DefaultDirectoryService();
        directoryService.setShutdownHookEnabled(true);

        ldapServer = new LdapServer();
        ldapServer.setDirectoryService(directoryService);
        ldapServer.setAllowAnonymousAccess(true);

        // Set LDAP port to 10389
        TcpTransport ldapTransport = new TcpTransport(port);
        ldapServer.setTransports(ldapTransport);

        // Determine an appropriate working directory
        directoryService.setWorkingDirectory(getTempDir());

        // Create a new partition named 'apache'.
        Partition partition = new JdbmPartition();
        partition.setId(rootDc);
        partition.setSuffix(rootDn);
        directoryService.addPartition(partition);

        // Index some attributes on the apache partition
        HashSet<Index<?, ServerEntry>> indexedAttributes = new HashSet<Index<?, ServerEntry>>();
        for (String attribute : new String[]{"objectClass", "ou", "uid"}) {
            indexedAttributes.add(new JdbmIndex<String, ServerEntry>(attribute));
        }
        ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);

        // start the service
        directoryService.startup();
        ldapServer.start();

        // create the root entry
        if (!directoryService.getAdminSession().exists(partition.getSuffixDn())) {
            LdapDN dnApache = new LdapDN(rootDn);
            ServerEntry entryApache = directoryService.newEntry(dnApache);
            entryApache.add("objectClass", "top", "domain", "extensibleObject");
            entryApache.add("dc", rootDc);
            directoryService.getAdminSession().add(entryApache);
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
            LdapDN memberDn = new LdapDN("cn=" + trim + "," + rootDn);

            ServerEntry memberEntry = directoryService.newEntry(memberDn);
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
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        } catch (NamingException e) {
            throw new RuntimeException(e);
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
