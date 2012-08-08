package org.pspace.common.web.dao.jackrabbit;

import org.pspace.common.api.FileInfo;
import org.pspace.common.api.ImageFileInfo;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.api.ObjectWithID;
import org.pspace.common.web.dao.RepositoryDao;
import org.pspace.common.web.mvc.MimeTypeUtils;
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
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author peach
 */
@Service("repositoryDao")
public class RepositoryDaoJackrabbit implements RepositoryDao {

    private final Logger log = LoggerFactory.getLogger(RepositoryDaoJackrabbit.class);

    private static final String THUMBNAIL_PREFIX = "th_";
    private static final String IGNORES_FILES_REGEX = "(\\.DS_Store|\\._\\.DS_Store|\\._.*)";

    @Autowired
    private JcrTemplate jcrTemplate;

//    private final static String ATTACHMENT_FOLDER = "attachments";
//    private final static String GALLERY_FOLDER = "gallery";

    @Override
    @Transactional
    public void importDirectory(String fileName) throws IOException, RepositoryException {
        traverse(new File(fileName), jcrTemplate.getRootNode(), true);

    }

    private void traverse(final File f, Node parentNode, boolean isRoot) throws IOException, RepositoryException {
        if (f.isDirectory()) {
            // process directory
            final Node newParentNode;
            if (!isRoot) {
                newParentNode = getOrCreateFolder(true, parentNode, f.getName());
            } else {
                newParentNode = parentNode;
            }
            final File[] childs = f.listFiles();
            if (childs != null) {
                for (File child : childs) {
                    // descend in recursion
                    if (!child.getName().matches(IGNORES_FILES_REGEX)) {
                        traverse(child, newParentNode, false);
                    }
                }
            }
        } else {
            // process file
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(f));
                String name = f.getName();
                log.info(String.format("Saving file %s in folder %s", f.getPath(), parentNode.getPath()));
                saveInputStream(parentNode, is, name);
            } finally {
                if (is != null) is.close();
            }
        }
    }

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
        final Node entityFolder = getOrCreateFolder(createIfNotExists, rootNode, entityFolderName);

        if (entityFolder == null) return null;

        String objectFolderName = objectWithID.getId().toString();

        return getOrCreateFolder(createIfNotExists, entityFolder, objectFolderName);
    }

    private Node getOrCreateFolder(boolean createIfNotExists, Node node, String folderName) throws RepositoryException {
        if (node.hasNode(folderName)) {
            return node.getNode(folderName);
        } else {
            if (createIfNotExists) {
                log.debug("Creating new folder " + folderName + " below node " + node.getPath());
                return node.addNode(folderName, "nt:folder");
            } else {
                return null;
            }
        }
    }

    private void saveInputStream(Node parentFolder, InputStream inputStream, String fileName) throws RepositoryException {
        String mimeType = MimeTypeUtils.getMimeTypeFromFileName(fileName);
        saveInputStream(parentFolder, inputStream, fileName, mimeType);
    }

    private void saveInputStream(Node parentFolder, InputStream inputStream, String fileName, String mimeType) throws RepositoryException {
        assert parentFolder != null;

        Node file = parentFolder.addNode(fileName, "nt:file");
        file.addMixin("mix:lockable");

        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:data", jcrTemplate.getValueFactory().createBinary(inputStream));
        if (mimeType != null) resource.setProperty("jcr:mimeType", "", PropertyType.STRING);
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        log.info("Saving file {}", file.getPath());

        jcrTemplate.save();
    }


    @Transactional
    @Override
    public void saveAttachment(ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {

        Node objectFolder = getFolderForObject(objectWithID, true);
        String originalFilename = multipartFile.getOriginalFilename();
        String mimeType = multipartFile.getContentType();
        InputStream inputStream = multipartFile.getInputStream();

        saveInputStream(objectFolder, inputStream, originalFilename, mimeType);

        jcrTemplate.save();
    }

    @Override
    public void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments) {
        try {
            Node folder = getFolderForObject(objectWithAttachments, false);

            if (folder == null) return;

            boolean changes = false;

            NodeIterator nodeIt = folder.getNodes();
            List<ImageFileInfo> imageFileNames = new ArrayList<ImageFileInfo>();
            List<FileInfo> regularFileNames = new ArrayList<FileInfo>();
            while (nodeIt.hasNext()) {
                Node fileNode = nodeIt.nextNode();

                if (fileNode.getName().startsWith(THUMBNAIL_PREFIX)) continue;

                if (fileNode.hasNode("jcr:content")) {
                    Node resourceNode = fileNode.getNode("jcr:content");
                    if (resourceNode.hasProperty("jcr:mimeType")) {
                        String mimeType = resourceNode.getProperty("jcr:mimeType").getString();

                        if (mimeType.startsWith("image/")) {

                            ImageFileInfo fileInfo = new ImageFileInfo();
                            fileInfo.setName(fileNode.getName());
                            fileInfo.setPath(fileNode.getPath());
                            fileInfo.setMimeType(mimeType);

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

                            imageFileNames.add(fileInfo);
                        } else {
                            // regular file
                            FileInfo fileInfo = new FileInfo();
                            fileInfo.setName(fileNode.getName());
                            fileInfo.setPath(fileNode.getPath());
                            fileInfo.setMimeType(mimeType);

                            regularFileNames.add(fileInfo);
                        }
                    }
                }
            }

            objectWithAttachments.setImages(imageFileNames);
            objectWithAttachments.setAttachments(regularFileNames);

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
