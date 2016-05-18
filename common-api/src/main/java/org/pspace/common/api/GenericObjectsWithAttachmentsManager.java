package org.pspace.common.api;

import java.util.List;

/**
 * @author mrpietsch
 */
public interface GenericObjectsWithAttachmentsManager<T extends ObjectWithAttachments> extends GenericManager<T> {

    List<T> getAllIncludingAttachmentAndImage() throws Exception;

    T getIncludingAttachmentAndImage(Long id) throws Exception;
}
