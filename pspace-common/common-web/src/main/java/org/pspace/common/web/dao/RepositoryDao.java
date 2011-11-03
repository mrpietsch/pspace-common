package org.pspace.common.web.dao;

import org.pspace.common.api.*;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * @author peach
 */
public interface RepositoryDao {

    void saveAttachment(ObjectWithID objectWithID, MultipartFile file) throws RepositoryException, IOException;

    void saveImage(ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException;

    FileInfo getImage(ObjectWithAttachments objectWithAttachments);

    void removeRelatedFiles(ObjectWithID objectWithID);

    void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments);

}
