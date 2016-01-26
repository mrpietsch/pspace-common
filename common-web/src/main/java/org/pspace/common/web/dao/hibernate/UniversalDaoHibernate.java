package org.pspace.common.web.dao.hibernate;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.util.Version;
import org.hibernate.*;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.pspace.common.web.dao.UniversalDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.*;

/**
 * This class serves as the a class that can CRUD any object witout any
 * Spring configuration. The only downside is it does require casting
 * from Object to the object class.
 *
 * @author Bryan Noll
 */
public class UniversalDaoHibernate implements UniversalDao {

    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Log log = LogFactory.getLog(getClass());
    private final Analyzer defaultAnalyzer = new StandardAnalyzer(Version.LUCENE_36);

    @Resource
    private SessionFactory sessionFactory;

    /**
     * {@inheritDoc}
     */
    public <T> T save(T o) {
        Session sess = getSession();
        return (T) sess.merge(o);
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> saveAll(List<T> objects) {
        Session sess = getSession();
        ArrayList<T> ret = new ArrayList<T>(objects.size());
        for (T object : objects) {
            ret.add((T) sess.merge(object));
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
    public <T> T get(Class<T> persistentClass, Serializable id) {

        Session sess = getSession();
        IdentifierLoadAccess byId = sess.byId(persistentClass);
        T entity = (T) byId.load(id);

        if (entity == null) {
            throw new ObjectRetrievalFailureException(persistentClass, id);
        }

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public boolean exists(Class persistentClass, Serializable id) {
        Session sess = getSession();
        IdentifierLoadAccess byId = sess.byId(persistentClass);
        return byId.load(id) != null;
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> getAll(Class<T> clazz) {
        return getSession()
                .createCriteria(clazz)
                // Hibernate Criteria returns children multiple times with FetchType.EAGER:pervent
                // This will work for most cases....except for when you try to use Criteria
                // to Fetch 2 collections/associations.
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void remove(Class clazz, Serializable id) {
        Session sess = getSession();
        IdentifierLoadAccess byId = sess.byId(clazz);
        sess.delete(byId.load(id));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public <T> void remove(T object) {
        Session sess = getSession();
        sess.delete(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findByNamedQuery(Class<T> clazz, String queryName, Map<String, Object> queryParams) {
        Session sess = getSession();

        Query namedQuery = sess.getNamedQuery(queryName);

        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            Object val = entry.getValue();
            String key = entry.getKey();
            if (val instanceof Collection) {
                namedQuery.setParameterList(key, (Collection) val);
            } else if (val instanceof Object[]) {
                namedQuery.setParameterList(key, (Object[]) val);
            } else {
                namedQuery.setParameter(key, val);
            }
        }
        return namedQuery.list();
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


        Query queryObject = getSession().getNamedQuery(queryName);
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T findUniqueByNamedQuery(Class<T> clazz, String queryName, Map<String, Object> queryParams) throws DataRetrievalFailureException {

        Session sess = getSession();

        Query namedQuery = sess.getNamedQuery(queryName);
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            Object val = entry.getValue();
            String key = entry.getKey();
            if (val instanceof Collection) {
                namedQuery.setParameterList(key, (Collection) val);
            } else if (val instanceof Object[]) {
                namedQuery.setParameterList(key, (Object[]) val);
            } else {
                namedQuery.setParameter(key, val);
            }
        }

        Iterator iterator = namedQuery.iterate();

        if (!iterator.hasNext()) {
            throw new DataRetrievalFailureException("Query returned no result.");
        }
        T ret = (T) iterator.next();
        if (iterator.hasNext()) {
            throw new DataRetrievalFailureException("Query returned more than one object.");
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> search(Class<T> clazz, String searchTerm) throws SearchException {
        Session sess = getSession();
        FullTextSession txtSession = Search.getFullTextSession(sess);

        org.apache.lucene.search.Query qry;
        try {
            qry = HibernateSearchTools.generateQuery(searchTerm, clazz, sess, defaultAnalyzer);
        } catch (ParseException ex) {
            throw new SearchException(ex);
        }
        org.hibernate.search.FullTextQuery hibQuery = txtSession.createFullTextQuery(qry, clazz);
        return hibQuery.list();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reindex(Class persistentClass) {
        HibernateSearchTools.reindex(persistentClass, getSessionFactory().getCurrentSession());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void reindexAll(boolean async) {
        HibernateSearchTools.reindexAll(async, getSessionFactory().getCurrentSession());
    }

    private Session getSession() throws HibernateException {
        Session sess = getSessionFactory().getCurrentSession();
        if (sess == null) {
            sess = getSessionFactory().openSession();
        }
        return sess;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
