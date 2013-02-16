package org.pspace.common.web.dao.ldap;

import org.junit.Test;

public class LDAPDaoApacheDSTest {

    @Test
    public void testSetupService() throws Exception {

        LDAPDaoApacheDS ldapDaoApacheDS = new LDAPDaoApacheDS();
        ldapDaoApacheDS.setPort(10123);
        ldapDaoApacheDS.setRootDomain("example.de");
        ldapDaoApacheDS.afterPropertiesSet();


    }
}
