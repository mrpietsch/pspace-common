package org.pspace.common.web.dao.ldap;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.pspace.common.api.Person;
import org.pspace.common.web.dao.LDAPDao;
import org.pspace.common.web.mvc.PhoneEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

/**
 * Created at 08.12.11 - 07:30
 *
 * @author peach
 */
public class LDAPDaoApacheDS implements LDAPDao, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LDAPDaoApacheDS.class);

    private final static String PW_ENCODING_ALGORITHM = "SHA";

    private static final PhoneEditor PHONE_EDITOR = new PhoneEditor();

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

    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private JdbmPartition addPartition(String partitionId, String partitionDn) throws Exception {
        // Create a new partition named 'foo'.
        JdbmPartition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setPartitionDir(new File(directoryService.getWorkingDirectory(), partitionId));
        partition.setSuffix(partitionDn);
        directoryService.addPartition(partition);

        return partition;
    }


    /**
     * Add a new set of index on the given attributes
     *
     * @param partition The partition on which we want to add index
     * @param attrs     The list of attributes to index
     */
    private void addIndex(JdbmPartition partition, String... attrs) {
        // Index some attributes on the apache partition
        HashSet<Index<?, ServerEntry, Long>> indexedAttributes = new HashSet<Index<?, ServerEntry, Long>>();

        for (String attribute : attrs) {
            indexedAttributes.add(new JdbmIndex<String, ServerEntry>(attribute));
        }

        partition.setIndexedAttributes(indexedAttributes);
    }

    /**
     * initialize the schema manager and add the schema partition to diectory service
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception {
        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = directoryService.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workingDirectory + "/schema");

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File(workingDirectory, "schema");
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(new File(workingDirectory));
        extractor.extractOrCopy(true);

        schemaPartition.setWrappedPartition(ldifPartition);

        SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        directoryService.setSchemaManager(schemaManager);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager(schemaManager);

        List<Throwable> errors = schemaManager.getErrors();

        if (!errors.isEmpty()) {
            throw new Exception("Schema load failed : " + errors);
        }
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

            directoryService = new DefaultDirectoryService();
            directoryService.setShutdownHookEnabled(true);
            directoryService.setWorkingDirectory(getTempDir());

            // first load the schema
            initSchemaPartition();

            // then the system partition -- this is a MANDATORY partition
            Partition systemPartition = addPartition("system", ServerDNConstants.SYSTEM_DN);
            directoryService.setSystemPartition(systemPartition);

            // Disable the ChangeLog system
            directoryService.getChangeLog().setEnabled(false);
            directoryService.setDenormalizeOpAttrsEnabled(true);

            // Now we can create as many partitions as we need
            JdbmPartition partition = addPartition(rootDc, rootDn);

            // Index some attributes on the apache partition
            addIndex(partition, "objectClass", "ou", "uid");

            // start the service
            directoryService.startup();

            // create the root entry
            if (!directoryService.getAdminSession().exists(partition.getSuffixDn())) {
                DN dnApache = new DN(rootDn);
                ServerEntry entryApache = directoryService.newEntry(dnApache);
                entryApache.add("objectClass", "top", "domain", "extensibleObject");
                entryApache.add("dc", rootDc);
                directoryService.getAdminSession().add(entryApache);
            }

            // start the actual server that listens on the port
            ldapServer = new LdapServer();
            ldapServer.setDirectoryService(directoryService);
            ldapServer.setTransports(new TcpTransport(port));
            ldapServer.setAllowAnonymousAccess(true);
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
            DN memberDn = new DN("cn=" + trim + "," + rootDn);

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
