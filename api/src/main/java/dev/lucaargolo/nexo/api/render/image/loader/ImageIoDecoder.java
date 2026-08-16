package dev.lucaargolo.nexo.api.render.image.loader;

import dev.lucaargolo.nexo.api.render.image.Image;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class ImageIoDecoder {

    private ImageIoDecoder() {
    }

    static @NotNull BufferedImage read(byte @NotNull [] data) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            throw new IOException("No ImageIO reader accepted the image");
        }
        return image;
    }

    static @NotNull Image convertToPng(byte @NotNull [] data) throws IOException {
        BufferedImage source = read(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream(data.length);
        if (!ImageIO.write(source, "PNG", output)) {
            throw new IOException("No PNG ImageIO writer is available");
        }
        return new Image(output.toByteArray());
    }

}
