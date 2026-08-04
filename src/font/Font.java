package font;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.json.JSONException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Font {

    public static class Glyph {
        public float u0, v0, u1, v1;
        public int x, y, w, h;
    }

    private Glyph[] glyphs = new Glyph[128];
    private int textureId;
    private int atlasWidth;
    private int atlasHeight;

    public Font(String pngPath, String jsonPath) {
        loadTexture(pngPath);
        loadMetadata(jsonPath);
    }

    private void loadTexture(String path) {
        try {
            BufferedImage img = ImageIO.read(Files.newInputStream(Paths.get(path)));
            atlasWidth = img.getWidth();
            atlasHeight = img.getHeight();

            int[] pixels = new int[atlasWidth * atlasHeight];
            img.getRGB(0, 0, atlasWidth, atlasHeight, pixels, 0, atlasWidth);

            ByteBuffer buffer = ByteBuffer.allocateDirect(atlasWidth * atlasHeight * 4);

            for (int y = 0; y < atlasHeight; y++) {
                for (int x = 0; x < atlasWidth; x++) {
                    int pixel = pixels[y * atlasWidth + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                    buffer.put((byte) (pixel & 0xFF));         // B
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
                }
            }

            buffer.flip();

            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    atlasWidth, atlasHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load font texture: " + path, e);
        }
    }

    private void loadMetadata(String path) {
        try {
            String jsonText = Files.readString(Paths.get(path));
            JSONObject root = new JSONObject(jsonText);

            for (int i = 32; i < 127; i++) {
                char ch = (char) i;
                if (!root.has(String.valueOf(ch))) continue;

                JSONObject g = root.getJSONObject(String.valueOf(ch));

                Glyph glyph = new Glyph();
                glyph.x = g.getInt("x");
                glyph.y = g.getInt("y");
                glyph.w = g.getInt("w");
                glyph.h = g.getInt("h");
                glyph.u0 = (float) g.getDouble("u0");
                glyph.v0 = (float) g.getDouble("v0");
                glyph.u1 = (float) g.getDouble("u1");
                glyph.v1 = (float) g.getDouble("v1");

                glyphs[i] = glyph;
            }

        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to load font metadata: " + path, e);
        }
    }

    public Glyph getGlyph(char c) {
        if (c < 0 || c >= 128) return null;
        return glyphs[c];
    }

    public int getTextureId() {
        return textureId;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }
}
