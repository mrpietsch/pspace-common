package org.pspace.common.api;

/**
 * @author peach
 */
public class ImageFileInfo extends FileInfo {

    public ImageFileInfo() {
    }

    public ImageFileInfo(String name, String path) {
        super(name, path);
    }

    public ImageFileInfo(String name, String path, String mimeType) {
        super(name, path, mimeType);
    }

    private String thumbnailPath;

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    @Override
    public boolean isImage() {
        return true;
    }
}
