package org.pspace.common.web.dao.jackrabbit;

import org.apache.jackrabbit.commons.JcrUtils;
import org.pspace.common.api.FileInfo;
import org.pspace.common.api.ImageFileInfo;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.api.ObjectWithID;
import org.pspace.common.web.dao.SearchableRepositoryDao;
import org.pspace.common.web.mvc.MimeTypeUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author peach
 */
@Service("repositoryDao")
public class RepositoryDaoJackrabbit extends AbstractRepositoryDao implements SearchableRepositoryDao, InitializingBean, DisposableBean {

    @Value("${jackrabbit.username}")
    private String username;

    @Value("${jackrabbit.password}")
    private String password;

    @Autowired
    private Repository repository;

    private Session session;

    @PostConstruct
    @Override
    public void afterPropertiesSet() throws Exception {
        session = repository.login(new SimpleCredentials(username, password.toCharArray()));
    }

    @PreDestroy
    @Override
    public void destroy() throws Exception {
        if (session != null) session.logout();
    }

    //    private final static String ATTACHMENT_FOLDER = "attachments";
//    private final static String GALLERY_FOLDER = "gallery";

    @Transactional
    public void importDirectory(final String fileName) throws Exception {
        traverse(new File(fileName), session.getRootNode(), true);
    }

    private void traverse(final File f, Node parentNode, boolean isRoot) throws IOException, RepositoryException {
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
    public List<SearchResult> search(String q) throws Exception {
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
    public String suggestQuery( String q) throws RepositoryException {
        javax.jcr.Value v = session.getWorkspace().getQueryManager().createQuery(
                "/jcr:root[rep:spellcheck('" + q + "')]/(rep:spellcheck())",
                Query.XPATH).execute().getRows().nextRow().getValue("rep:spellcheck()");
        session.logout();
        return v != null ? v.getString() : null;
    }

    @Transactional
    private Node getFolderForObject( ObjectWithID objectWithID, boolean createIfNotExists) throws RepositoryException {
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

    private void saveInputStream( Node parentFolder, InputStream inputStream, String fileName) throws RepositoryException {
        String mimeType = MimeTypeUtils.getMimeTypeFromFileName(fileName);
        saveInputStream(parentFolder, inputStream, fileName, mimeType);
    }

    private void saveInputStream( Node parentFolder, InputStream inputStream, String fileName, String mimeType) throws RepositoryException {
        assert parentFolder != null;
        Node file = JcrUtils.putFile(parentFolder, fileName, mimeType, inputStream);
        log.info("Saving file {}", file.getPath());
        session.save();
    }


    @Transactional
    @Override
    public void saveAttachment( ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {

        Node objectFolder = getFolderForObject(objectWithID, true);
        String originalFilename = multipartFile.getOriginalFilename();
        String mimeType = multipartFile.getContentType();
        InputStream inputStream = multipartFile.getInputStream();

        saveInputStream(objectFolder, inputStream, originalFilename, mimeType);
    }

    @Override
    public void populateObjectWithFileInfos( ObjectWithAttachments objectWithAttachments) {
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
                                thumbFile = JcrUtils.getNodeIfExists(folder, thumbName);
                            } else {
                                // resize the image
                                InputStream inputStream = JcrUtils.readFile(fileNode);

                                InputStream is = resizeImage(inputStream);


                                Binary thumbImage = createBinaryFromInputStream(is);

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

    private Binary createBinaryFromInputStream(InputStream is) throws RepositoryException {
        // create and return a binary from the InputStream
        return session.getValueFactory().createBinary(is);
    }

    @Transactional()
    @Override
    public FileInfo getImage( ObjectWithAttachments objectWithAttachments) {
        // find

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Transactional
    public void removeRelatedFiles( ObjectWithID objectWithID) {
        try {
            Node objectFolder = getFolderForObject(objectWithID, false);
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
