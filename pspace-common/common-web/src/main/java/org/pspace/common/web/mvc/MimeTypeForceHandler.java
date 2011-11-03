package org.pspace.common.web.mvc;

import org.apache.commons.io.FilenameUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.server.io.*;
import org.apache.jackrabbit.webdav.DavResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

public class MimeTypeForceHandler extends DefaultHandler implements IOHandler {
    private static Logger log = LoggerFactory.getLogger(VersionHandler.class);

    private static final Properties properties; // = ResourceBundle.getBundle();

    static {
        Properties props = new Properties();
        URL url = IOHandler.class.getResource("mimetypes.properties");
        try {
            props.load(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        properties = props;
    }

    @Override
    protected void exportProperties(ExportContext context, boolean isCollection, Node contentNode) throws IOException {
        super.exportProperties(context, isCollection, contentNode);    //To change body of overridden methods use File | Settings | File Templates.

        try {
            Node exportNode = (Node) context.getExportRoot();
            String name = exportNode.getName();
            String ext = FilenameUtils.getExtension(name);

            if (ext != null) {
                String extLc = ext.toLowerCase();

                if (properties.containsKey(extLc)) {
                    String mimeType = properties.getProperty(extLc);

                    String encoding = null;
                    if (contentNode.hasProperty(JcrConstants.JCR_ENCODING)) {
                        encoding = contentNode.getProperty(JcrConstants.JCR_ENCODING).getString();
                        // ignore "" encoding (although this is avoided during import)
                        if ("".equals(encoding)) encoding = null;
                    }
                    context.setContentType(mimeType, encoding);
                    context.setContentLength(-1); // problems with sitemesh if the content length is already set
                }


            }
        } catch (RepositoryException e) {
            // should never occur
            log.error("Unexpected error {0} while exporting properties: {1}", e.getClass().getName(), e.getMessage());
            throw new IOException(e.getMessage());
        }

    }

}
