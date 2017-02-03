package org.pspace.common.web.dao.jackrabbit;

import org.pspace.common.web.dao.RepositoryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class AbstractRepositoryDao implements RepositoryDao {

    protected static final String THUMBNAIL_PREFIX    = "th_";
    protected static final String IGNORES_FILES_REGEX = "(\\.DS_Store|\\._\\.DS_Store|\\._.*)";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${maxThumbnailHeight:50}")
    protected int maxThumbnailHeight = 50;

    @Value("${maxThumbnailWidth:100}")
    protected int maxThumbnailWidth = 100;

    protected InputStream resizeImage(InputStream imageInputStream) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageInputStream);
        int           type          = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

        double maxSquarePixel = maxThumbnailHeight * maxThumbnailWidth;
        double originalRatio  = (double) originalImage.getHeight() / (double) originalImage.getWidth();

        // We want the thumbnail to have a maximum area and to have the same ratio as the original
        // (1) thumbSquarePixel = maxSquarePixel
        // (2) thumbRatio = originalRatio

        // Definitions:
        // (3) thumbWidth * thumbHeight = maxSquarePixel
        // (4) thumbHeight / thumbWidth = originalRatio

        // Derived from the system of equations above we have to calculate:
        int thumbWidth  = (int) Math.round(Math.sqrt(maxSquarePixel / originalRatio));
        int thumbHeight = (int) Math.round(Math.sqrt(maxSquarePixel * originalRatio));

        // actually resizing the image
        BufferedImage resizedImage = new BufferedImage(thumbWidth, thumbHeight, type);
        Graphics2D    g            = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // providing the image in an InputStream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public void setMaxThumbnailHeight(int maxThumbnailHeight) {
        this.maxThumbnailHeight = maxThumbnailHeight;
    }

    public void setMaxThumbnailWidth(int maxThumbnailWidth) {
        this.maxThumbnailWidth = maxThumbnailWidth;
    }
}
