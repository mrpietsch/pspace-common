package org.pspace.common.web.dao.hibernate;

import org.pspace.common.web.dao.UniversalDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class serves as the a class that can CRUD any object witout any
 * Spring configuration. The only downside is it does require casting
 * from Object to the object class.
 *
 * @author Bryan Noll
 */
public class UniversalDaoHibernate extends HibernateDaoSupport implements UniversalDao {

    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * {@inheritDoc}
     */
    public <T> T save(T o) {
        return getHibernateTemplate().merge(o);
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> saveAll(List<T> objects) {
        ArrayList<T> ret = new ArrayList<T>(objects.size());
        for (T object : objects) {
            ret.add(getHibernateTemplate().merge(object));
        }
        return ret;
    }

    @Override
    public long count(Class clazz) {
        String tableName = resolveTableName(clazz);
        return ((Number) getSession().createQuery("select count(*) from " + tableName).iterate().next()).longValue();
    }

    @Override
    public Object performNamedQueryWithSingleResult(String queryName, Map<String, Object> queryParams) {
        Query q = getSession().getNamedQuery(queryName);
        for (String s : queryParams.keySet()) {
            q.setParameter(s, queryParams.get(s));
        }
        return q.iterate().next();
    }

    private String resolveTableName(Class clazz) {
        ClassMetadata hibernateMetadata = getSessionFactory().getClassMetadata(clazz);
        if (hibernateMetadata == null) {
            return null;
        }
        if (hibernateMetadata instanceof AbstractEntityPersister) {
            AbstractEntityPersister persister = (AbstractEntityPersister) hibernateMetadata;
            return persister.getTableName();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(Class<T> clazz, Serializable id) {
        T o = getHibernateTemplate().get(clazz, id);

        if (o == null) {
            throw new ObjectRetrievalFailureException(clazz, id);
        }

        return o;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public boolean exists(Class clazz, Serializable id) {
        return getHibernateTemplate().get(clazz, id) != null;
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> getAll(Class<T> clazz) {
        return getHibernateTemplate().loadAll(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void remove(Class clazz, Serializable id) {
        getHibernateTemplate().delete(get(clazz, id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findByNamedQuery(Class<T> clazz, String queryName, Map<String, Object> queryParams) {
        String[] params = new String[queryParams.size()];
        Object[] values = new Object[queryParams.size()];
        int index = 0;
        for (String s : queryParams.keySet()) {
            params[index] = s;
            values[index++] = queryParams.get(s);
        }
        return getHibernateTemplate().findByNamedQueryAndNamedParam(queryName, params, values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findByNamedQueryPaginated(Class<T> clazz, final String queryName, Map<String, Object> queryParams, final int pageNumber, final int pageSize) {
        final String[] params = new String[queryParams.size()];
        final Object[] values = new Object[queryParams.size()];
        int index = 0;
        for (String s : queryParams.keySet()) {
            params[index] = s;
            values[index++] = queryParams.get(s);
        }

        HibernateTemplate hibernateTemplate = getHibernateTemplate();

        return hibernateTemplate.execute(new HibernateCallback<List>() {
            public List doInHibernate(Session session) throws HibernateException {
                Query queryObject = session.getNamedQuery(queryName);
                queryObject.setFirstResult((pageNumber - 1) * pageSize);
                queryObject.setMaxResults(pageSize);
                for (int i = 0; i < values.length; i++) {
                    if (values[i] instanceof Collection) {
                        queryObject.setParameterList(params[i], (Collection) values[i]);
                    } else if (values[i] instanceof Object[]) {
                        queryObject.setParameterList(params[i], (Object[]) values[i]);
                    } else {
                        queryObject.setParameter(params[i], values[i]);
                    }
                }
                return queryObject.list();
            }
        });

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T findUniqueByNamedQuery(Class<T> clazz, String queryName, Map<String, Object> queryParams) throws DataRetrievalFailureException {
        String[] params = new String[queryParams.size()];
        Object[] values = new Object[queryParams.size()];
        int index = 0;
        for (String s : queryParams.keySet()) {
            params[index] = s;
            values[index++] = queryParams.get(s);
        }
        List<T> x = getHibernateTemplate().findByNamedQueryAndNamedParam(queryName, params, values);
        if (x.isEmpty()) {
            throw new DataRetrievalFailureException("Query returned no result.");
        }
        if (x.size() > 1) {
            throw new DataRetrievalFailureException("Query returned more than one object.");
        }
        return x.get(0);
    }
}
