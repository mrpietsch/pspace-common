package org.pspace.common.web.dao;

import org.pspace.common.api.FileInfo;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.api.ObjectWithID;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.RepositoryException;
import java.io.IOException;

public interface RepositoryDao {

    void saveAttachment(ObjectWithID objectWithID, MultipartFile file) throws RepositoryException, IOException;

    FileInfo getImage(ObjectWithAttachments objectWithAttachments);

    void removeRelatedFiles(ObjectWithID objectWithID);

    void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments);

}
