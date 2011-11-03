package org.pspace.common.web.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pspace.common.api.ObjectWithID;
import org.pspace.common.api.GenericManager;
import org.pspace.common.web.dao.UniversalDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * This class serves as the Base class for all other Managers - namely to hold
 * common CRUD methods that they might all use. You should only need to extend
 * this class when your require custom CRUD logic.
 * <p/>
 * <p>To register this class in your Spring context file, use the following XML.
 * <pre>
 *     &lt;bean id="userManager" class="org.cellnet.globalresearch.service.impl.GenericManagerImpl"&gt;
 *         &lt;constructor-arg&gt;
 *             &lt;bean class="org.cellnet.globalresearch.dao.hibernate.GenericDaoHibernate"&gt;
 *                 &lt;constructor-arg value="org.cellnet.globalresearch.model.User"/&gt;
 *                 &lt;property name="sessionFactory" ref="sessionFactory"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/constructor-arg&gt;
 *     &lt;/bean&gt;
 * </pre>
 * <p/>
 * <p>If you're using iBATIS instead of Hibernate, use:
 * <pre>
 *     &lt;bean id="userManager" class="org.cellnet.globalresearch.service.impl.GenericManagerImpl"&gt;
 *         &lt;constructor-arg&gt;
 *             &lt;bean class="org.cellnet.globalresearch.dao.ibatis.GenericDaoiBatis"&gt;
 *                 &lt;constructor-arg value="org.cellnet.globalresearch.model.User"/&gt;
 *                 &lt;property name="dataSource" ref="dataSource"/&gt;
 *                 &lt;property name="sqlMapClient" ref="sqlMapClient"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/constructor-arg&gt;
 *     &lt;/bean&gt;
 * </pre>
 *
 * @param <T>  a type variable
 * @param <PK> the primary key for that type
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 */
@Transactional
public abstract class GenericManagerImpl<T extends ObjectWithID, PK extends Serializable>
        implements GenericManager<T, PK> {

    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Log log = LogFactory.getLog(getClass());

    final Class<T> objectClass;

    /**
     * GenericDao instance, set by constructor of this class
     */
    @Autowired
    protected UniversalDao dao;

    /**
     * Public constructor for creating a new GenericManagerImpl.
     *
     * @param objectClass ...
     */
    protected GenericManagerImpl(Class<T> objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * {@inheritDoc}
     */
    public List<T> getAll() {
        return dao.getAll(objectClass);
    }

    @Override
    public long countAll() {
        return dao.count(objectClass);
    }

    /**
     * {@inheritDoc}
     */
    public T get(PK id) {
        return dao.get(objectClass, id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(PK id) {
        return dao.exists(objectClass, id);
    }

    /**
     * {@inheritDoc}
     */
    public T save(T object) {
        T newObject = dao.save(object);
        if (log.isDebugEnabled()) log.debug("Saved object of type " + newObject.getClass() + " with id " + newObject.getId());
        return newObject;
    }

    /**
     * {@inheritDoc}
     */
    public List<T> saveAll(List<T> object) {
        return dao.saveAll(object);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(PK id) {
        if (log.isDebugEnabled()) log.debug("Deleting object of type " + objectClass + " with id " + id);
        dao.remove(objectClass, id);
    }

    public void setDao(UniversalDao dao) {
        this.dao = dao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T convert(String source) {
        // parsing would not be necessary but I want to be sure I get a number here
        Serializable l = Long.parseLong(source);
        return dao.get(objectClass, l);
    }
}
