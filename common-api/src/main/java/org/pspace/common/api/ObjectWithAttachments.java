package org.pspace.common.api;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peach
 */
public abstract class ObjectWithAttachments implements ObjectWithID {

    @Transient
    private List<FileInfo> attachments = new ArrayList<FileInfo>();

    @Transient
    private List<ImageFileInfo> images = new ArrayList<ImageFileInfo>();

    public List<FileInfo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<FileInfo> attachments) {
        this.attachments = attachments;
    }

    public List<ImageFileInfo> getImages() {
        return images;
    }

    public void setImages(List<ImageFileInfo> images) {
        this.images = images;
    }
}
