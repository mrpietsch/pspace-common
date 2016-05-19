package org.pspace.common.api;

import org.springframework.core.convert.converter.Converter;

import java.io.Serializable;
import java.util.List;

/**
 * Generic Manager that talks to GenericDao to CRUD POJOs.
 * <p/>
 * <p>Extend this interface if you want typesafe (no casting necessary) managers
 * for your domain objects.
 *
 * @param <T> a type variable
 */
public interface GenericManager<T> extends Converter<String, T> {

    /**
     * Generic method used to get all objects of a particular type. This
     * is the same as lookup up all rows in a table.
     *
     * @return List of populated objects
     */
    List<T> getAll();

    long countAll();

    /**
     * Generic method to get an object based on class and identifier. An
     * ObjectRetrievalFailureException Runtime Exception is thrown if
     * nothing is found.
     *
     * @param id the identifier (primary key) of the object to get
     * @return a populated object
     */
    T get(Long id);

    /**
     * Checks for existence of an object of type T using the id arg.
     *
     * @param id the identifier (primary key) of the object to get
     * @return - true if it exists, false if it doesn't
     */
    boolean exists(Long id);

    /**
     * Generic method to save an object - handles both update and insert.
     *
     * @param object the object to save
     * @return the updated object
     */
    T save(T object);

    List<T> saveAll(List<T> object);
//
    List<T> search(Class<T> entityClass, String query, String ... fields);

    /**
     * Generic method to delete an object based on class and id
     *
     * @param id the identifier (primary key) of the object to remove
     */
    void remove(Long id);

//    void reindex();
//
//    void reindexAll(boolean async);
}
