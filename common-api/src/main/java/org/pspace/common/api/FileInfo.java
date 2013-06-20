package org.pspace.common.api;

/**
 * @author mrpietsch
 */

public class FileInfo implements Comparable<FileInfo> {

    private String name;
    private String path;
    private String mimeType;

    public FileInfo() {
    }

    public FileInfo(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public FileInfo(String name, String path, String mimeType) {
        this.name = name;
        this.path = path;
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isImage() {
        return (mimeType != null && mimeType.startsWith("image/")) || name.matches(".*\\.(jpg|png|gif|JPG|PNG|GIF)");
    }

    @Override
    public int compareTo(FileInfo o) {
        return this.name.compareTo(o.name);
    }
}