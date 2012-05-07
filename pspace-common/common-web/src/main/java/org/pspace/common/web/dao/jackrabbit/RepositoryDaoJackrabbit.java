package org.pspace.common.web.dao.jackrabbit;

import org.apache.jackrabbit.util.Text;
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
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author peach
 */
@Service("repositoryDao")
public class RepositoryDaoJackrabbit implements RepositoryDao {

    private static final String THUMBNAIL_PREFIX = "th_";
    private final Logger log = LoggerFactory.getLogger(RepositoryDaoJackrabbit.class);

    @Autowired
    private JcrTemplate jcrTemplate;

    private final static String IGNORES_FILES_REGEX = "(\\.DS_Store|\\._\\.DS_Store|\\._.*)";

//    private final static String ATTACHMENT_FOLDER = "attachments";
//    private final static String GALLERY_FOLDER = "gallery";

    @Override
    public List<SearchResult> search(String q) throws RepositoryException {
        String stmt;
        // String queryTerms = "";
        if (q.startsWith("related:")) {
            String path = q.substring("related:".length());
            path = path.replaceAll("'", "''");
            stmt = "//element(*, nt:file)[rep:similar(jcr:content, '" + path + "/jcr:content')]/rep:excerpt(.) order by @jcr:score descending";
            // queryTerms = "similar to <b>" + Text.encodeIllegalXMLCharacters(path) + "</b>";
        } else {
            // queryTerms = "for <b>" + Text.encodeIllegalXMLCharacters(q) + "</b>";
            q = q.replaceAll("'", "''");
            stmt = "//element(*, nt:file)[jcr:contains(jcr:content, '" + q + "')]/rep:excerpt(.) order by @jcr:score descending";
        }
        Session session = null;
        try {
            session = jcrTemplate.getSessionFactory().getSession();
            Query query = session.getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
            //long time = System.currentTimeMillis();
            RowIterator rows = query.execute().getRows();
            //time = System.currentTimeMillis() - time;

            List<SearchResult> results = new ArrayList<SearchResult>((int) rows.getSize());
            while (rows.hasNext()) {
                Row row = rows.nextRow();
                SearchResult r = new SearchResult();
                r.setScore(row.getScore());
                r.setPath(row.getPath());
                r.setExcerpt(row.getValue("rep:excerpt(jcr:content)").getString());
                r.setData(row.getNode());
                r.setTitle(row.getNode().getName());
                results.add(r);
            }


            return results;
        } finally {
            if (session != null) session.logout();
        }

    }

    @Override
    public String suggestQuery(String q) throws RepositoryException {
        Session session = jcrTemplate.getSessionFactory().getSession();
        Value v = session.getWorkspace().getQueryManager().createQuery(
                "/jcr:root[rep:spellcheck('" + q + "')]/(rep:spellcheck())",
                Query.XPATH).execute().getRows().nextRow().getValue("rep:spellcheck()");
        session.logout();
        return v != null ? v.getString() : null;
    }

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

    private void save(ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {

        Node objectFolder = getFolderForObject(objectWithID, true);

        assert objectFolder != null;

        Node file = objectFolder.addNode(multipartFile.getOriginalFilename(), "nt:file");
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
        save(objectWithID, multipartFile);
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

            NodeIterator nodeIt = folder.getNodes();
            List<ImageFileInfo> fileNames = new ArrayList<ImageFileInfo>();
            while (nodeIt.hasNext()) {
                Node fileNode = nodeIt.nextNode();

                if (fileNode.getName().startsWith(THUMBNAIL_PREFIX)) continue;

                if (fileNode.hasNode("jcr:content")) {
                    Node resourceNode = fileNode.getNode("jcr:content");
                    if (resourceNode.hasProperty("jcr:mimeType")) {
                        String mimeType = resourceNode.getProperty("jcr:mimeType").getString();

                        ImageFileInfo fileInfo = new ImageFileInfo();
                        fileInfo.setName(fileNode.getName());
                        fileInfo.setPath(fileNode.getPath());
                        fileInfo.setMimeType(mimeType);

                        if (mimeType.startsWith("image/")) {
                            // look if there is a thumbnail and create one if not
                            String thumbName = THUMBNAIL_PREFIX + fileNode.getName();
                            final Node thumbFile;

                            if (folder.hasNode(thumbName)) {
                                thumbFile = folder.getNode(thumbName);
                            } else {

                                thumbFile = folder.addNode(thumbName, "nt:file");
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

                            fileInfo.setThumbnailPath(thumbFile.getPath());
                        }

                        fileNames.add(fileInfo);

                    }
                }
            }
            objectWithAttachments.setImages(fileNames);

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
