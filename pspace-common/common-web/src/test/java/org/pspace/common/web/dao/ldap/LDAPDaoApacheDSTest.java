package org.pspace.common.web.dao.ldap;

import org.junit.Test;
import org.pspace.common.api.Person;

public class LDAPDaoApacheDSTest {

    Person testPerson = new Person() {
        @Override
        public String getFullName() {
            return "Hans Mustermann";
        }

        @Override
        public Number getId() {
            return 4711;
        }

        @Override
        public String getPrefix() {
            return "Dr.";
        }

        @Override
        public String getPrename() {
            return "Hans";
        }

        @Override
        public String getLastname() {
            return "Mustermann";
        }

        @Override
        public String getPassword() {
            return "xxx";
        }

        @Override
        public String getOrganizationalUnitName() {
            return null;
        }

        @Override
        public String getOrganizationName() {
            return "YCD";
        }

        @Override
        public String getUsername() {
            return "hmustermann";
        }

        @Override
        public String getEmail() {
            return "hans@mustermann.de";
        }

        @Override
        public String getPhone() {
            return "+49 30 123456";
        }

        @Override
        public String getMobile() {
            return "+49 30 123457";
        }

        @Override
        public String getStreet() {
            return "Mustergasse 4";
        }

        @Override
        public String getPostalCode() {
            return "12345";
        }

        @Override
        public String getCity() {
            return "Berlin";
        }
    };

    @Test
    public void testSetupService() throws Exception {

        LDAPDaoApacheDS ldapDaoApacheDS = new LDAPDaoApacheDS();
        ldapDaoApacheDS.setPort(10123);
        ldapDaoApacheDS.setRootDomain("example.de");
        ldapDaoApacheDS.afterPropertiesSet();

        ldapDaoApacheDS.updatePerson(testPerson);

        ldapDaoApacheDS.updatePerson(testPerson);

    }
}
