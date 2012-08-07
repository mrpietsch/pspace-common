package org.pspace.common.web.mvc;

import org.apache.commons.io.FilenameUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.server.io.IOHandler;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * @author peach
 */
public class MimeTypeUtils {
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

    public static String getMimeTypeFromFileName(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        return getMimeTypeFromSuffix(ext);
    }

    public static String getMimeTypeFromSuffix(String suffix) {
        if (suffix == null) return null;
        String extLc = suffix.toLowerCase();
        return properties.containsKey(extLc) ? properties.getProperty(extLc) : null;
    }
}
