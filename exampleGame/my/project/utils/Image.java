package my.project.utils; // Ensure this matches your folder structure

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Image {

    /**
     * Loads an image from the resources folder and converts it into a 2D array of Colors.
     */
    public static Color[][] loadPixelMap(String resourcePath) throws IOException {
        // FIX: Changed "ImageUtils.class" to "Image.class" to match your file name
        InputStream is = Image.class.getResourceAsStream(resourcePath);
        
        if (is == null) {
            throw new IOException("File not found in resources: " + resourcePath);
        }

        BufferedImage image = ImageIO.read(is);
        int width = image.getWidth();
        int height = image.getHeight();

        Color[][] pixelMap = new Color[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getRGB(x, y);
                // The 'true' argument tells Java to support transparency (Alpha channel)
                pixelMap[x][y] = new Color(pixel, true);
            }
        }

        return pixelMap;
    }
}