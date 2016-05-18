package org.pspace.common.web.testsupport;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.NEVER;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional(NEVER)
@Rollback(value = false)
public abstract class VirginDatabaseTest {

    @Autowired DataSource dataSource;

    @Before
    public void clearDatabase() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.clean();
        flyway.migrate();
    }

}
