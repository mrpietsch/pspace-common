package org.pspace.common.web.dao.hibernate;

import org.pspace.common.api.ObjectWithID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pspace.common.web.dao.GenericDao;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class serves as the Base class for all other DAOs - namely to hold
 * common CRUD methods that they might all use. You should only need to extend
 * this class when your require custom CRUD logic.
 * <p/>
 * <p>To register this class in your Spring context file, use the following XML.
 * <pre>
 *      &lt;bean id="fooDao" class="org.cellnet.globalresearch.dao.hibernate.GenericDaoHibernate"&gt;
 *          &lt;constructor-arg value="org.cellnet.globalresearch.model.Foo"/&gt;
 *          &lt;property name="sessionFactory" ref="sessionFactory"/&gt;
 *      &lt;/bean&gt;
 * </pre>
 *
 * @author <a href="mailto:bwnoll@gmail.com">Bryan Noll</a>
 * @param <T> a type variable
 * @param <PK> the primary key for that type
 */
public class GenericDaoHibernate<T, PK extends Serializable> extends HibernateDaoSupport implements GenericDao<T, PK> {

    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Log log = LogFactory.getLog(getClass());
    private Class<T> persistentClass;

    /**
     * Constructor that takes in a class to see which type of entity to persist
     *
     * @param persistentClass the class type you'd like to persist
     */
    public GenericDaoHibernate(final Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        return super.getHibernateTemplate().loadAll(this.persistentClass);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllDistinct() {
        Collection result = new LinkedHashSet(getAll());
        return new ArrayList(result);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T get(PK id) {
        T entity = (T) super.getHibernateTemplate().get(this.persistentClass, id);

        if (entity == null) {
            log.warn("Uh oh, '" + this.persistentClass + "' object with id '" + id + "' not found...");
            throw new ObjectRetrievalFailureException(this.persistentClass, id);
        }

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public boolean exists(PK id) {
        T entity = (T) super.getHibernateTemplate().get(this.persistentClass, id);
        return entity != null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T save(T object) {

        if (object instanceof ObjectWithID) {

            Number id = ((ObjectWithID) object).getId();

            // TODO due to a bug hibernate @PrePersist and other EventListeners do not work - this is a workaround
            for (Method m : this.persistentClass.getMethods()) {

                // in case there is a new object to persist / to be inserted
                if (id == null && m.isAnnotationPresent(PrePersist.class)) {
                    try {
                        m.invoke(object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // in case the object is to be updated
                if (id != null && m.isAnnotationPresent(PreUpdate.class)) {
                    try {
                        m.invoke(object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return (T) super.getHibernateTemplate().merge(object);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(PK id) {
        super.getHibernateTemplate().delete(this.get(id));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByNamedQuery(String queryName, Map<String, Object> queryParams) {
        String[] params = new String[queryParams.size()];
        Object[] values = new Object[queryParams.size()];
        int index = 0;
        Iterator<String> i = queryParams.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            params[index] = key;
            values[index++] = queryParams.get(key);
        }
        return getHibernateTemplate().findByNamedQueryAndNamedParam(queryName, params, values);
    }
}
