package org.pspace.common.web.services;

import org.apache.commons.collections.IteratorUtils;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.pspace.common.api.GenericManager;
import org.pspace.common.api.ObjectWithID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * This class serves as the Base class for all other Managers - namely to hold
 * common CRUD methods that they might all use. You should only need to extend
 * this class when your require custom CRUD logic.
 *
 * @param <T> a type variable
 */
@Transactional
public abstract class GenericManagerImpl<T extends ObjectWithID, REPO extends PagingAndSortingRepository<T, Long>>
        implements GenericManager<T> {

    @Autowired
    EntityManager entityManager;
    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * GenericDao instance, set by constructor of this class
     */
    protected final REPO dao;

    /**
     * Public constructor for creating a new GenericManagerImpl.
     *
     * @param dao
     */
    protected GenericManagerImpl(REPO dao) {
        this.dao = dao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> getAll() {
        return IteratorUtils.toList(dao.findAll().iterator());
    }

    @Override
    public long countAll() {
        return dao.count();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get(Long id) {
        return dao.findOne(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(Long id) {
        return dao.exists(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T save(T object) {
        T newObject = dao.save(object);
        if (log.isDebugEnabled())
            log.debug("Saved object of type " + newObject.getClass() + " with id " + newObject.getId());
        return newObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> saveAll(List<T> object) {
        return IteratorUtils.toList(dao.save(object).iterator());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> search(Class<T> entityClass, String queryString) {

        // get the full text entity manager
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        // create the query using Hibernate Search query DSL
        QueryBuilder queryBuilder = fullTextEntityManager
                .getSearchFactory()
                .buildQueryBuilder()
                .forEntity(entityClass)
                .get();

        Query query = queryBuilder.keyword().onField("").matching(queryString).createQuery();

        // wrap Lucene query in an Hibernate Query object
        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, entityClass);

        // execute search and return results (sorted by relevance as default)
        return jpaQuery.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        if (log.isDebugEnabled())
            log.debug("Deleting object of type from " + this.getClass().getSimpleName() + " with id " + id);
        dao.delete(id);
    }

    @Override
    public T convert(String source) {
        return dao.findOne(Long.parseLong(source));
    }


}
