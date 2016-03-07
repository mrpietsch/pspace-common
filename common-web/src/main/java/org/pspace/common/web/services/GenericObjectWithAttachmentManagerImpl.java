package org.pspace.common.web.services;

import org.pspace.common.api.GenericObjectsWithAttachmentsManager;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.web.dao.RepositoryDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author mrpietsch
 */
public abstract class GenericObjectWithAttachmentManagerImpl<T extends ObjectWithAttachments<PK>, PK extends Number>
        extends GenericManagerImpl<T, PK>
        implements GenericObjectsWithAttachmentsManager<T, PK> {

    protected final RepositoryDao repositoryDao;

    public GenericObjectWithAttachmentManagerImpl(Class<T> objectClass,
                                                  PagingAndSortingRepository<T, PK> dao,
                                                  RepositoryDao repositoryDao) {
        super(objectClass, dao);
        this.repositoryDao = repositoryDao;
    }

    public List<T> getAllIncludingAttachmentAndImage() throws Exception {
        List<T> list = getAll();
        for (final T t : list) {
            repositoryDao.populateObjectWithFileInfos(t);
        }
        return list;
    }

    public T getIncludingAttachmentAndImage(final PK id) throws Exception {
        T obj = get(id);
        repositoryDao.populateObjectWithFileInfos(obj);
        return obj;
    }

    @Override
    @Transactional
    public void remove(final PK id) throws Exception {
        T obj = get(id);
        repositoryDao.removeRelatedFiles(obj);
        GenericObjectWithAttachmentManagerImpl.super.remove(id);
    }

}
