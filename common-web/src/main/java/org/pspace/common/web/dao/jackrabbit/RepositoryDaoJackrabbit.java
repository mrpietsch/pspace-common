package org.pspace.common.web.dao.jackrabbit;

import org.apache.jackrabbit.commons.JcrUtils;
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

import javax.imageio.ImageIO;
import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author peach
 */
@Service("repositoryDao")
public class RepositoryDaoJackrabbit implements RepositoryDao {

    private final Logger log = LoggerFactory.getLogger(RepositoryDaoJackrabbit.class);

    private static final String THUMBNAIL_PREFIX = "th_";
    private static final String IGNORES_FILES_REGEX = "(\\.DS_Store|\\._\\.DS_Store|\\._.*)";

    private int maxThumbnailHeight = 50;
    private int maxThumbnailWidth = 100;

    public void setMaxThumbnailHeight(int maxThumbnailHeight) {
        this.maxThumbnailHeight = maxThumbnailHeight;
    }

    public void setMaxThumbnailWidth(int maxThumbnailWidth) {
        this.maxThumbnailWidth = maxThumbnailWidth;
    }

    @Autowired
    private Repository repository;

//    private final static String ATTACHMENT_FOLDER = "attachments";
//    private final static String GALLERY_FOLDER = "gallery";

    @Override
    @Transactional
    public void importDirectory(Session session, final String fileName) throws Exception {
        traverse(session, new File(fileName), session.getRootNode(), true);
    }

    @Override
    public <T> T doInSession(SessionAwareCallable<T> callable) throws Exception {
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("username", "password".toCharArray()));
            return callable.call(session);
        } finally {
            if (session != null) session.logout();
        }
    }


    private void traverse(Session session, final File f, Node parentNode, boolean isRoot) throws IOException, RepositoryException {
        if (f.isDirectory()) {
            // process directory
            final Node newParentNode;
            if (!isRoot) {
                newParentNode = JcrUtils.getOrAddFolder(parentNode, f.getName());
            } else {
                newParentNode = parentNode;
            }
            final File[] childs = f.listFiles();
            if (childs != null) {
                for (File child : childs) {
                    // descend in recursion
                    if (!child.getName().matches(IGNORES_FILES_REGEX)) {
                        traverse(session, child, newParentNode, false);
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
                saveInputStream(session, parentNode, is, name);
            } finally {
                if (is != null) is.close();
            }
        }
    }

    @Override
    public List<SearchResult> search(Session session, String q) throws Exception {
        final String stmt;
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
    }

    @Override
    public String suggestQuery(Session session, String q) throws RepositoryException {
        Value v = session.getWorkspace().getQueryManager().createQuery(
                "/jcr:root[rep:spellcheck('" + q + "')]/(rep:spellcheck())",
                Query.XPATH).execute().getRows().nextRow().getValue("rep:spellcheck()");
        session.logout();
        return v != null ? v.getString() : null;
    }

    @Transactional
    private Node getFolderForObject(Session session, ObjectWithID objectWithID, boolean createIfNotExists) throws RepositoryException {
        Node rootNode = session.getRootNode();

        final String entityFolderName = objectWithID.getClass().getSimpleName().toLowerCase();
        final Node entityFolder = createIfNotExists ?
                JcrUtils.getOrAddFolder(rootNode, entityFolderName) :
                JcrUtils.getNodeIfExists(rootNode, entityFolderName);

        if (entityFolder == null) return null;

        final String objectFolderName = objectWithID.getId().toString();

        return createIfNotExists ?
                JcrUtils.getOrAddFolder(entityFolder, objectFolderName) :
                JcrUtils.getNodeIfExists(entityFolder, objectFolderName);
    }

    private void saveInputStream(Session session, Node parentFolder, InputStream inputStream, String fileName) throws RepositoryException {
        String mimeType = MimeTypeUtils.getMimeTypeFromFileName(fileName);
        saveInputStream(session, parentFolder, inputStream, fileName, mimeType);
    }

    private void saveInputStream(Session session, Node parentFolder, InputStream inputStream, String fileName, String mimeType) throws RepositoryException {
        assert parentFolder != null;
        Node file = JcrUtils.putFile(parentFolder, fileName, mimeType, inputStream);
        log.info("Saving file {}", file.getPath());
        session.save();
    }


    @Transactional
    @Override
    public void saveAttachment(Session session, ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {

        Node objectFolder = getFolderForObject(session, objectWithID, true);
        String originalFilename = multipartFile.getOriginalFilename();
        String mimeType = multipartFile.getContentType();
        InputStream inputStream = multipartFile.getInputStream();

        saveInputStream(session, objectFolder, inputStream, originalFilename, mimeType);
    }

    @Override
    public void populateObjectWithFileInfos(Session session, ObjectWithAttachments objectWithAttachments) {
        try {
            Node folder = getFolderForObject(session, objectWithAttachments, false);

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
                                thumbFile = JcrUtils.getNodeIfExists(folder, thumbName);
                            } else {
                                // resize the image
                                InputStream inputStream = JcrUtils.readFile(fileNode);
                                Binary thumbImage = resizeImage(session, inputStream);

                                thumbFile = JcrUtils.putFile(folder, thumbName, mimeType, thumbImage.getStream());

                                log.info("Saving thumbnail {}", thumbFile.getPath());

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

            Collections.sort(imageFileNames);
            Collections.sort(regularFileNames);

            objectWithAttachments.setImages(imageFileNames);
            objectWithAttachments.setAttachments(regularFileNames);

            if (changes) {
                session.save();
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    private Binary resizeImage(Session session, InputStream imageInputStream) throws RepositoryException, IOException {

        BufferedImage originalImage = ImageIO.read(imageInputStream);
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

        double maxSquarePixel = maxThumbnailHeight * maxThumbnailWidth;
        double originalRatio = (double) originalImage.getHeight() / (double) originalImage.getWidth();

        // We want the thumbnail to have a maximum area and to have the same ratio as the original
        // (1) thumbSquarePixel = maxSquarePixel
        // (2) thumbRatio = originalRatio

        // Definitions:
        // (3) thumbWidth * thumbHeight = maxSquarePixel
        // (4) thumbHeight / thumbWidth = originalRatio

        // Derived from the system of equations above we have to calculate:
        int thumbWidth = (int) Math.round(Math.sqrt(maxSquarePixel / originalRatio));
        int thumbHeight = (int) Math.round(Math.sqrt(maxSquarePixel * originalRatio));

        // actually resizing the image
        BufferedImage resizedImage = new BufferedImage(thumbWidth, thumbHeight, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // providing the image in an InputStream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        // create and return a binary from the InputStream
        return session.getValueFactory().createBinary(is);
    }

    @Transactional()
    @Override
    public FileInfo getImage(Session session, ObjectWithAttachments objectWithAttachments) {
        // find

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Transactional
    public void removeRelatedFiles(Session session, ObjectWithID objectWithID) {
        try {
            Node objectFolder = getFolderForObject(session, objectWithID, false);
            if (objectFolder == null) return;
            objectFolder.remove();
            session.save();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
