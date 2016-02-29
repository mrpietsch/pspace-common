package org.pspace.common.web.dao;

import org.pspace.common.api.FileInfo;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.api.ObjectWithID;
import org.pspace.common.web.dao.jackrabbit.SearchResult;
import org.pspace.common.web.dao.jackrabbit.SessionAwareCallable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.List;

/**
 * @author peach
 */
public interface RepositoryDao {

    <T> T doInSession(SessionAwareCallable<T> callable) throws Exception;

    List<SearchResult> search(Session session, String q) throws Exception;

    String suggestQuery(Session session, String q) throws Exception;

    void saveAttachment(Session session, ObjectWithID objectWithID, MultipartFile file) throws RepositoryException, IOException;

    FileInfo getImage(Session session, ObjectWithAttachments objectWithAttachments);

    void removeRelatedFiles(Session session, ObjectWithID objectWithID);

    void populateObjectWithFileInfos(Session session, ObjectWithAttachments objectWithAttachments);

    @Transactional
    void importDirectory(Session session, String fileName) throws Exception;
}
