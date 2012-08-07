package org.pspace.common.web.mvc;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.server.io.DefaultHandler;
import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.VersionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;

public class MimeTypeForceHandler extends DefaultHandler implements IOHandler {

    private static Logger log = LoggerFactory.getLogger(VersionHandler.class);

    @Override
    protected void exportProperties(ExportContext context, boolean isCollection, Node contentNode) throws IOException {
        super.exportProperties(context, isCollection, contentNode);

        try {
            Node exportNode = (Node) context.getExportRoot();
            String name = exportNode.getName();

            String mimeType = MimeTypeUtils.getMimeTypeFromFileName(name);

            if (mimeType != null) {
                String encoding = null;
                if (contentNode.hasProperty(JcrConstants.JCR_ENCODING)) {
                    encoding = contentNode.getProperty(JcrConstants.JCR_ENCODING).getString();
                    // ignore "" encoding (although this is avoided during import)
                    if ("".equals(encoding)) encoding = null;
                }
                context.setContentType(mimeType, encoding);
                context.setContentLength(-1); // problems with sitemesh if the content length is already set
            }
        } catch (RepositoryException e) {
            // should never occur
            log.error("Unexpected error {0} while exporting properties: {1}", e.getClass().getName(), e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

}
