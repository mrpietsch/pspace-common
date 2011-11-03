package org.pspace.common.web.dao.jackrabbit;

import org.pspace.common.api.*;
import org.pspace.common.web.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springmodules.jcr.JcrTemplate;

import javax.jcr.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author peach
 */
@Service("repositoryDao")
public class RepositoryDaoJackrabbit implements RepositoryDao {

    private static final String THUMBNAIL_PREFIX = "thumb_";
    private final Logger log = LoggerFactory.getLogger(RepositoryDaoJackrabbit.class);

    @Autowired
    private JcrTemplate jcrTemplate;

    private final static String IGNORES_FILES_REGEX = "(\\.DS_Store|\\._\\.DS_Store|\\._.*)";

    private final static String ATTACHMENT_FOLDER = "attachments";
    private final static String GALLERY_FOLDER = "gallery";


    @Transactional
    private Node getFolderForObject(ObjectWithID objectWithID, boolean createIfNotExists) throws RepositoryException {
        Node rootNode = jcrTemplate.getRootNode();

        String entityFolderName = objectWithID.getClass().getSimpleName().toLowerCase();
        final Node entityFolder;
        if (rootNode.hasNode(entityFolderName)) {
            entityFolder = rootNode.getNode(entityFolderName);
        } else {
            if (createIfNotExists) {
                entityFolder = rootNode.addNode(entityFolderName, "nt:folder");
            } else {
                return null;
            }
        }

        String objectFolderName = objectWithID.getId().toString();

        if (entityFolder.hasNode(objectFolderName)) {
            return entityFolder.getNode(objectFolderName);
        } else {
            if (createIfNotExists) {
                return entityFolder.addNode(objectFolderName, "nt:folder");
            } else {
                return null;
            }

        }
    }

    private void save(ObjectWithID objectWithID, MultipartFile multipartFile, String subFolderName) throws RepositoryException, IOException {

        Node objectFolder = getFolderForObject(objectWithID, true);

        assert objectFolder != null;
        Node attachmentsFolder = getOrCreateFolder(objectFolder, subFolderName);

        Node file = attachmentsFolder.addNode(multipartFile.getOriginalFilename(), "nt:file");
        file.addMixin("mix:lockable");

        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:data", jcrTemplate.getValueFactory().createBinary(multipartFile.getInputStream()));
        resource.setProperty("jcr:mimeType", multipartFile.getContentType(), PropertyType.STRING);
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        log.info("Saving file {}" + file.getPath());

        jcrTemplate.save();
    }

    @Transactional
    @Override
    public void saveAttachment(ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {
        save(objectWithID, multipartFile, ATTACHMENT_FOLDER);
    }

    @Transactional
    @Override
    public void saveImage(ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {
        save(objectWithID, multipartFile, GALLERY_FOLDER);
    }

    private Node getOrCreateFolder(Node node, String folderName) throws RepositoryException {
        return node.hasNode(folderName) ?
                node.getNode(folderName) :
                node.addNode(folderName, "nt:folder");
    }

    @Override
    public void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments) {
        try {
            Node folder = getFolderForObject(objectWithAttachments, false);

            if (folder == null) return;

            boolean changes = false;

            if (folder.hasNode(ATTACHMENT_FOLDER)) {
                Node attachmentsNode = folder.getNode(ATTACHMENT_FOLDER);
                NodeIterator nodeIt = attachmentsNode.getNodes();
                List<FileInfo> fileNames = new ArrayList<FileInfo>();
                while (nodeIt.hasNext()) {
                    Node node = nodeIt.nextNode();

                    if (node.getName().matches(IGNORES_FILES_REGEX)) continue;

                    fileNames.add(new FileInfo(node.getName(), node.getPath()));
                }
                objectWithAttachments.setAttachments(fileNames);
            }
            if (folder.hasNode(GALLERY_FOLDER)) {
                Node attachmentsNode = folder.getNode(GALLERY_FOLDER);
                NodeIterator nodeIt = attachmentsNode.getNodes();
                List<ImageFileInfo> fileNames = new ArrayList<ImageFileInfo>();
                while (nodeIt.hasNext()) {
                    Node fileNode = nodeIt.nextNode();

                    if ( fileNode.getName().startsWith(THUMBNAIL_PREFIX) ) continue;

                    if (fileNode.hasNode("jcr:content")) {
                        Node resourceNode = fileNode.getNode("jcr:content");
                        if (resourceNode.hasProperty("jcr:mimeType")) {
                            String mimeType = resourceNode.getProperty("jcr:mimeType").getString();
                            // TODO check mime type if MacOS starts providing the correct ones instead of application/xml
                            // if (mimeType.startsWith("image/")) {

                                // look if there is a thumbnail and create one if not
                                String thumbName = THUMBNAIL_PREFIX + fileNode.getName();
                                final Node thumbFile;
                                if (attachmentsNode.hasNode(thumbName)) {
                                    thumbFile = attachmentsNode.getNode(thumbName);
                                } else {

                                    thumbFile = attachmentsNode.addNode(thumbName, "nt:file");
                                    thumbFile.addMixin("mix:lockable");

                                    Property contentProperty = resourceNode.getProperty("jcr:data");
                                    Binary binary = contentProperty.getBinary();

                                    // resize the image
                                    Binary thumbImage = resizeImage(binary);

                                    Node resource = thumbFile.addNode("jcr:content", "nt:resource");
                                    resource.setProperty("jcr:data", thumbImage);
                                    resource.setProperty("jcr:mimeType", mimeType, PropertyType.STRING);
                                    resource.setProperty("jcr:lastModified", Calendar.getInstance());

                                    log.info("Saving thumbnail {}" + thumbFile.getPath());

                                    changes = true;

                                }

                                ImageFileInfo fileInfo = new ImageFileInfo();
                                fileInfo.setName(fileNode.getName());
                                fileInfo.setPath(fileNode.getPath());
                                fileInfo.setMimeType(mimeType);
                                fileInfo.setThumbnailPath(thumbFile.getPath());

                                fileNames.add(fileInfo);
                            //}

                        }
                    }
                }
                objectWithAttachments.setImages(fileNames);
            }

            if (changes) {
                jcrTemplate.save();
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    private Binary resizeImage(Binary binary) throws RepositoryException {

        // InputStream imageInputStream = binary.getStream();

        // TODO implement resizeImage

        return binary;

    }

    @Transactional()
    @Override
    public FileInfo getImage(ObjectWithAttachments objectWithAttachments) {
        // find

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Transactional
    public void removeRelatedFiles(ObjectWithID objectWithID) {
        try {
            Node objectFolder = getFolderForObject(objectWithID, false);
            if (objectFolder == null) return;
            objectFolder.remove();
            jcrTemplate.save();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    public void setJcrTemplate(JcrTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    private void export() {
        log.debug(jcrTemplate.dump(jcrTemplate.getRootNode()));
    }
}
