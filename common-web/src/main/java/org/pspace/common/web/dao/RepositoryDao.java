package org.pspace.common.web.dao;

import org.pspace.common.api.FileInfo;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.api.ObjectWithID;
import org.pspace.common.web.dao.jackrabbit.SearchResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;

/**
 * @author peach
 */
public interface RepositoryDao {

    List<SearchResult> search(String q) throws Exception;

    String suggestQuery(String q) throws Exception;

    void saveAttachment(ObjectWithID objectWithID, MultipartFile file) throws RepositoryException, IOException;

    FileInfo getImage(ObjectWithAttachments objectWithAttachments);

    void removeRelatedFiles(ObjectWithID objectWithID);

    void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments);

    @Transactional
    void importDirectory(String fileName) throws Exception;
}
