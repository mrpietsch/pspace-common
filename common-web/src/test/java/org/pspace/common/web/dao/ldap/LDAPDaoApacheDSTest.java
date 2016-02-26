package org.pspace.common.web.dao.ldap;

import org.junit.Ignore;
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

    Person testPersonMod = new Person() {
        public String getFullName() {
            return testPerson.getFullName();
        }

        public Number getId() {
            return testPerson.getId();
        }

        public String getPrefix() {
            return null;
        }

        public String getPrename() {
            return testPerson.getPrename();
        }

        public String getLastname() {
            return testPerson.getLastname();
        }

        public String getPassword() {
            return testPerson.getPassword();
        }

        public String getOrganizationalUnitName() {
            return testPerson.getOrganizationalUnitName();
        }

        public String getOrganizationName() {
            return testPerson.getOrganizationName();
        }

        public String getUsername() {
            return testPerson.getUsername();
        }

        public String getEmail() {
            return testPerson.getEmail();
        }

        public String getPhone() {
            return testPerson.getPhone();
        }

        public String getMobile() {
            return testPerson.getMobile();
        }

        public String getStreet() {
            return testPerson.getStreet();
        }

        public String getPostalCode() {
            return testPerson.getPostalCode();
        }

        public String getCity() {
            return testPerson.getCity();
        }
    };

    @Ignore
    @Test
    public void testSetupService() throws Exception {

        LDAPDaoApacheDS ldapDaoApacheDS = new LDAPDaoApacheDS();
        ldapDaoApacheDS.setPort(10123);
        ldapDaoApacheDS.setRootDomain("example.de");
        ldapDaoApacheDS.afterPropertiesSet();

        ldapDaoApacheDS.updatePerson(testPerson);

        ldapDaoApacheDS.updatePerson(testPersonMod);




    }
}
