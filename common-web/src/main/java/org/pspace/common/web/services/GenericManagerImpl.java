package org.pspace.common.web.services;

import org.apache.commons.collections.IteratorUtils;
import org.pspace.common.api.GenericManager;
import org.pspace.common.api.ObjectWithID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * This class serves as the Base class for all other Managers - namely to hold
 * common CRUD methods that they might all use. You should only need to extend
 * this class when your require custom CRUD logic.
 *
 * @param <T>  a type variable
 * @param <PK> the primary key for that type
 */
@Transactional
@Component
public abstract class GenericManagerImpl<T extends ObjectWithID, PK extends Serializable>
        implements GenericManager<T, PK> {

    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    final Class<T> objectClass;

    /**
     * GenericDao instance, set by constructor of this class
     */
    protected final PagingAndSortingRepository<T, PK> dao;

    /**
     * Public constructor for creating a new GenericManagerImpl.
     *
     * @param objectClass ...
     * @param dao
     */
    protected GenericManagerImpl(Class<T> objectClass, PagingAndSortingRepository<T, PK> dao) {
        this.objectClass = objectClass;
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
    public T get(PK id) {
        return dao.findOne(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(PK id) {
        return dao.exists(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T save(T object) {
        T newObject = dao.save(object);
        if (log.isDebugEnabled()) log.debug("Saved object of type " + newObject.getClass() + " with id " + newObject.getId());
        return newObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> saveAll(List<T> object) {
        return IteratorUtils.toList(dao.save(object).iterator());
    }

//    @Override
//    public List<T> search(String query) {
//        return dao.search(objectClass, query);
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(PK id) {
        if (log.isDebugEnabled()) log.debug("Deleting object of type " + objectClass + " with id " + id);
        dao.delete(id);
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public T convert(String source) {
//        if (source == null || source.isEmpty()) {
//            return null;
//        } else {
//            // parsing would not be necessary but I want to be sure I get a number here
//            Serializable l = Long.parseLong(source);
//            return dao.findOne(source);
//        }
//    }


//    @Override
//    public void reindex() {
//        dao.reindex(objectClass);
//    }
//
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void reindexAll(boolean async) {
//        dao.reindexAll(async);
//    }
}
