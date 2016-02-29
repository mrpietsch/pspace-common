package org.pspace.common.api;

import java.util.List;

/**
 * @author mrpietsch
 */
public interface GenericObjectsWithAttachmentsManager<T extends ObjectWithAttachments<PK>, PK extends Number> extends GenericManager<T, PK> {

    List<T> getAllIncludingAttachmentAndImage() throws Exception;

    T getIncludingAttachmentAndImage(PK id) throws Exception;
}
