package world;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class TextureAtlas {

    private BufferedImage atlas;
    private int width;
    private int height;

    public static final int TILE_SIZE = 16;

    public TextureAtlas(String path) {
        try {
            atlas = ImageIO.read(Files.newInputStream(Paths.get(path)));
            width = atlas.getWidth();
            height = atlas.getHeight();

            // Tell Block.java the atlas size
            Block.setAtlasSize(width, height);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture atlas: " + path, e);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // ---------------------------------------------------------
    // Compute UVs for a tile index (REAL MAPPING)
    //
    // tileIndex = 0 → top-left 16×16 tile
    // tileIndex = 1 → next tile to the right
    // ...
    // tileIndex = tilesPerRow → first tile of next row
    //
    // This matches EXACTLY the rule you described.
    // ---------------------------------------------------------
    public float[][] computeUV(int tileIndex) {

        int tilesX = width / TILE_SIZE;
        int tilesY = height / TILE_SIZE;

        int tx = tileIndex % tilesX;
        int ty = tileIndex / tilesX;

        float u0 = (float) tx / tilesX;
        float v0 = (float) ty / tilesY;
        float u1 = (float) (tx + 1) / tilesX;
        float v1 = (float) (ty + 1) / tilesY;

        return new float[][] {
            {u0, v0},
            {u1, v0},
            {u1, v1},
            {u0, v1}
        };
    }
}
