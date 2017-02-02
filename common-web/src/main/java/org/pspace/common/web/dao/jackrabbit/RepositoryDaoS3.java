package org.pspace.common.web.dao.jackrabbit;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.pspace.common.api.FileInfo;
import org.pspace.common.api.ImageFileInfo;
import org.pspace.common.api.ObjectWithAttachments;
import org.pspace.common.api.ObjectWithID;
import org.pspace.common.web.dao.RepositoryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author peach
 */
@org.springframework.stereotype.Repository("repositoryDaoS3")
public class RepositoryDaoS3 extends AbstractRepositoryDao implements RepositoryDao {

    private final Logger log = LoggerFactory.getLogger(RepositoryDaoS3.class);

    private static final String THUMBNAIL_PREFIX    = "th_";
    private static final String IGNORES_FILES_REGEX = "(\\.DS_Store|\\._\\.DS_Store|\\._.*)";

    @Value("${maxThumbnailHeight:50}")
    private int maxThumbnailHeight = 50;

    @Value("${maxThumbnailWidth:100}")
    private int maxThumbnailWidth = 100;

    @Autowired
    AmazonS3Client amazonS3Client;

    String bucketName;

    public void setMaxThumbnailHeight(int maxThumbnailHeight) {
        this.maxThumbnailHeight = maxThumbnailHeight;
    }

    public void setMaxThumbnailWidth(int maxThumbnailWidth) {
        this.maxThumbnailWidth = maxThumbnailWidth;
    }

    private String getFolderForObject(ObjectWithID objectWithID) {
        String entityFolderName = objectWithID.getClass().getSimpleName().toLowerCase();
        String objectFolderName = objectWithID.getId().toString();
        return entityFolderName + "/" + objectFolderName;
    }

    private void saveInputStream(InputStream inputStream, String mimeType, String filePath) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(mimeType);
        amazonS3Client.putObject(bucketName, filePath, inputStream, metadata);

        log.info("Saving file {}", filePath);
    }

    @Override
    public void saveAttachment(ObjectWithID objectWithID, MultipartFile multipartFile) throws RepositoryException, IOException {
        String filePath = getFolderForObject(objectWithID) + "/" + multipartFile.getOriginalFilename();
        saveInputStream(
                multipartFile.getInputStream(),
                multipartFile.getContentType(),
                filePath);
    }

    @Override
    public void populateObjectWithFileInfos(ObjectWithAttachments objectWithAttachments) {
        try {
            String folder = getFolderForObject(objectWithAttachments);

            List<S3ObjectSummary> relatedFiles = getRelatedFiles(objectWithAttachments);

            if (relatedFiles.isEmpty()) return;

            List<ImageFileInfo> imageFileNames   = new ArrayList<>();
            List<FileInfo>      regularFileNames = new ArrayList<>();

            for (S3ObjectSummary fileNode : relatedFiles) {

                if (fileNode.getKey().startsWith(THUMBNAIL_PREFIX)) continue;

                ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(bucketName, fileNode.getKey());

                String   mimeType = objectMetadata.getContentType();
                String[] split    = fileNode.getKey().split("/");
                String   name     = split[split.length - 1];

                if (mimeType.startsWith("image/")) {

                    String thumbPath = makeSureThumbnailExists(folder, mimeType, name);

                    ImageFileInfo fileInfo = new ImageFileInfo();
                    fileInfo.setName(name);
                    fileInfo.setPath(fileNode.getKey());
                    fileInfo.setMimeType(mimeType);
                    fileInfo.setThumbnailPath(thumbPath);

                    imageFileNames.add(fileInfo);
                } else {
                    // regular file
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setName(name);
                    fileInfo.setPath(fileNode.getKey());
                    fileInfo.setMimeType(mimeType);

                    regularFileNames.add(fileInfo);
                }
            }

            Collections.sort(imageFileNames);
            Collections.sort(regularFileNames);

            objectWithAttachments.setImages(imageFileNames);
            objectWithAttachments.setAttachments(regularFileNames);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Look if there is a thumbnail and create one if not
     */
    private String makeSureThumbnailExists(String folder, String mimeType, String name) throws IOException {
        String thumbName = THUMBNAIL_PREFIX + name;
        String thumbPath = folder + "/" + thumbName;

        if (!amazonS3Client.doesObjectExist(bucketName, thumbPath)) {
            // resize the image
            InputStream originalImageData = amazonS3Client.getObject(bucketName, thumbName).getObjectContent();
            InputStream thumbImageData    = resizeImage(originalImageData);

            log.info("Saving thumbnail {}", thumbName);
            saveInputStream(thumbImageData, mimeType, thumbPath);
        }

        return thumbPath;
    }

    @Override
    public FileInfo getImage(ObjectWithAttachments objectWithAttachments) {
        // find

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override

    public void removeRelatedFiles(ObjectWithID objectWithID) {
        getRelatedFiles(objectWithID)
                .stream()
                .map(S3ObjectSummary::getKey)
                .forEach(
                        fileName -> amazonS3Client.deleteObject(bucketName, fileName)
                );
    }

    private List<S3ObjectSummary> getRelatedFiles(ObjectWithID objectWithID) {
        String objectFolder = getFolderForObject(objectWithID);

        return amazonS3Client
                .listObjects(bucketName, objectFolder)
                .getObjectSummaries();
    }

}
