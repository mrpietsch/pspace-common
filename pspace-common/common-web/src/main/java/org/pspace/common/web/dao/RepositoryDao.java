package org.pspace.common.web.dao;

import org.pspace.common.api.*;
import org.pspace.common.web.dao.jackrabbit.SearchResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import java.io.IOException;
import java.util.List;

/**
 * @author peach
 */
public interface RepositoryDao {

    List<SearchResult> search(String q) throws RepositoryException;

    String suggestQuery(String q) throws RepositoryException;

    void saveAttachment(ObjectWithID objectWithID, MultipartFile file) throws RepositoryException, IOException;

    FileInfo getImage(ObjectWithAttachments objectWithAttachments);

    void removeRelatedFiles(ObjectWithID objectWithID);

    void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments);

    @Transactional
    void importDirectory(String fileName) throws IOException, RepositoryException;
}
