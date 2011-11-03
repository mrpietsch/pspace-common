package org.pspace.common.web.services;

import org.pspace.common.api.GenericObjectsWithAttachmentsManager;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.web.dao.RepositoryDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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

    public List<T> getAllIncludingAttachmentAndImage() {
        List<T> list = getAll();
        for (T t : list) {
            repositoryDao.populateObjectWithFileInfos(t);
        }
        return list;
    }

    public T getIncludingAttachmentAndImage(PK id) {
        T obj = get(id);
        repositoryDao.populateObjectWithFileInfos(obj);
        return obj;
    }

    @Override
    @Transactional
    public void remove(PK id) {
        T obj = get(id);
        repositoryDao.removeRelatedFiles(obj);
        super.remove(id);
    }

    public void setRepositoryDao(RepositoryDao repositoryDao) {
        this.repositoryDao = repositoryDao;
    }
}
