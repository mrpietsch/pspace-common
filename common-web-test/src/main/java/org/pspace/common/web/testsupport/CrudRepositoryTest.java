package org.pspace.common.web.testsupport;

import org.junit.Before;
import org.junit.Test;
import org.pspace.common.api.ObjectWithID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.EntityManager;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public abstract class CrudRepositoryTest<E extends ObjectWithID, R extends CrudRepository<E, Long>> extends VirginDatabaseTest {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected abstract R getRepository();
    protected abstract E exampleEntity();
    protected abstract E modifyExample(E entity);

    @Autowired private EntityManager entityManager;

    /**
     * Make sure we're not inside of a transaction already such that every JPA repository creates its own one.
     */
    @Before
    public void makeSureBeingOutsideOfTransaction() {
        assertThat(entityManager.isJoinedToTransaction(), is(false));
    }

    @Test
    public void itSupportsBasicCrudOperations() throws Exception {

        R repository = getRepository();
        E entity     = exampleEntity();

        // create
        logger.info("Saving " + entity);
        E savedEntity = repository.save(entity);
        assertThat(savedEntity.getId(), notNullValue());
        assertThat(savedEntity, equalTo(entity));

        // load by id
        E reloadedEntity = repository.findOne(savedEntity.getId());
        logger.debug("Reloaded " + reloadedEntity);
        assertThat(reloadedEntity, equalTo(savedEntity));

        // update
        E modifiedEntity      = modifyExample(reloadedEntity);
        E savedModifiedEntity = repository.save(modifiedEntity);
        assertThat(savedModifiedEntity.getId(), equalTo(reloadedEntity.getId()));
        assertThat(savedModifiedEntity, equalTo(modifiedEntity));

        // delete
        repository.delete(reloadedEntity.getId());
        E ghost = repository.findOne(reloadedEntity.getId());
        assertThat(ghost, nullValue());
    }

}
