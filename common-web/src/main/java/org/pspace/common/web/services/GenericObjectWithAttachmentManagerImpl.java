package org.pspace.common.web.services;

import org.pspace.common.api.GenericObjectsWithAttachmentsManager;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.web.dao.RepositoryDao;
import org.pspace.common.web.dao.jackrabbit.SessionAwareCallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.Session;
import java.util.List;

/**
 * @author mrpietsch
 */
public abstract class GenericObjectWithAttachmentManagerImpl<T extends ObjectWithAttachments<PK>, PK extends Number>
        extends GenericManagerImpl<T, PK>
        implements GenericObjectsWithAttachmentsManager<T, PK> {

    @Autowired
    private RepositoryDao repositoryDao;

    public GenericObjectWithAttachmentManagerImpl(Class<T> objectClass) {
        super(objectClass);
    }

    public List<T> getAllIncludingAttachmentAndImage() throws Exception {
        return repositoryDao.doInSession(new SessionAwareCallable<List<T>>() {
            @Override
            public List<T> call(Session session) throws Exception {
                List<T> list = getAll();
                for (final T t : list) {
                    repositoryDao.populateObjectWithFileInfos(session, t);
                }
                return list;
            }
        });
    }

    public T getIncludingAttachmentAndImage(final PK id) throws Exception {
        return repositoryDao.doInSession(new SessionAwareCallable<T>() {
            @Override
            public T call(Session session) throws Exception {
                T obj = get(id);
                repositoryDao.populateObjectWithFileInfos(session, obj);
                return obj;
            }
        });
    }

    @Override
    @Transactional
    public void remove(final PK id) throws Exception {
        repositoryDao.doInSession(new SessionAwareCallable<Void>() {
            @Override
            public Void call(Session session) throws Exception {
                T obj = get(id);
                repositoryDao.removeRelatedFiles(session, obj);
                GenericObjectWithAttachmentManagerImpl.super.remove(id);
                return null;
            }
        });
    }

    public void setRepositoryDao(RepositoryDao repositoryDao) {
        this.repositoryDao = repositoryDao;
    }
}
