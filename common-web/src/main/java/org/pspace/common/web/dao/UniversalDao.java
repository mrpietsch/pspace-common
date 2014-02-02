package org.pspace.common.web.dao;

import org.hibernate.search.SearchException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) interface.
 *
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 *         <p/>
 *         Modifications and comments by <a href="mailto:bwnoll@gmail.com">Bryan Noll</a>
 *         This thing used to be named simply 'GenericDao' in versions of appfuse prior to 2.0.
 *         It was renamed in an attempt to distinguish and describe it as something
 *         different than GenericDao.  GenericDao is intended for subclassing, and was
 *         named Generic because 1) it has very general functionality and 2) is
 *         'generic' in the Java 5 sense of the word... aka... it uses Generics.
 *         <p/>
 *         Implementations of this class are not intended for subclassing. You most
 *         likely would want to subclass GenericDao.  The only real difference is that
 *         instances of java.lang.Class are passed into the methods in this class, and
 *         they are part of the constructor in the GenericDao, hence you'll have to do
 *         some casting if you use this one.
 * @see GenericDao
 */
public interface UniversalDao {

    /**
     * Generic method used to get all objects of a particular type. This
     * is the same as lookup up all rows in a table.
     *
     * @param clazz the type of objects (a.k.a. while table) to get data from
     * @return List of populated objects
     */
    <T> List<T> getAll(Class<T> clazz);

    /**
     * Generic method to get an object based on class and identifier. An
     * ObjectRetrievalFailureException Runtime Exception is thrown if
     * nothing is found.
     *
     * @param clazz model class to lookup
     * @param id    the identifier (primary key) of the class
     * @return a populated object
     */
    <T> T get(Class<T> clazz, Serializable id);

    boolean exists(Class clazz, Serializable id);

    long count(Class clazz);

    Object performNamedQueryWithSingleResult(String queryName, Map<String, Object> queryParams);

    /**
     * Generic method to save an object - handles both update and insert.
     *
     * @param o the object to save
     * @return a populated object
     */
    <T> T save(T o);

    <T> List<T> saveAll(List<T> o);

    /**
     * Generic method to delete an object based on class and id
     *
     * @param clazz model class to lookup
     * @param id    the identifier (primary key) of the class
     */
    void remove(Class clazz, Serializable id);

    <T> void remove(T object);

    <T> List<T> findByNamedQuery(Class<T> clazz, String s, Map<String, Object> objectMap);

    <T> T findUniqueByNamedQuery(Class<T> clazz, String s, Map<String, Object> objectMap);

    <T> List<T> findByNamedQueryPaginated(Class<T> clazz, final String queryName, Map<String, Object> queryParams, final int pageNumber, final int pageSize);

    <T> List<T> search(Class<T> clazz, String searchTerm) throws SearchException;

    void reindex(Class persistentClass);

    void reindexAll(boolean async);
}