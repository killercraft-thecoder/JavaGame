package world;

public class Block {

    // -----------------------------------------
    // Block IDs
    // -----------------------------------------
    public static final int AIR = -1;
    public static final int GRASS = 0;
    public static final int STONE = 1;
    public static final int WOOD = 2;
    public static final int LEAVES = 3;

    // -----------------------------------------
    // Solid / transparent
    // -----------------------------------------
    public static boolean isSolid(int id) {
        return id >= 0;
    }

    // -----------------------------------------
    // Texture atlas info
    // -----------------------------------------
    public static final int TILE_SIZE = 16;
    public static int atlasWidth = 256;
    public static int atlasHeight = 256;

    public static void setAtlasSize(int w, int h) {
        atlasWidth = w;
        atlasHeight = h;
    }

    // -----------------------------------------
    // Compute UV for tile index = block ID
    // -----------------------------------------
    private static float[][] computeUV(int tileIndex) {

        int tilesX = atlasWidth / TILE_SIZE;
        int tilesY = atlasHeight / TILE_SIZE;

        int tx = tileIndex % tilesX;
        int ty = tileIndex / tilesX;

        float u0 = (float) tx / tilesX;
        float v0 = (float) ty / tilesY;
        float u1 = (float) (tx + 1) / tilesX;
        float v1 = (float) (ty + 1) / tilesY;

        return new float[][]{
            {u0, v0},
            {u1, v0},
            {u1, v1},
            {u0, v1}
        };
    }

    // -----------------------------------------
    // Per-face UVs (same texture on all faces)
    // -----------------------------------------
    public static float[][][] getUVs(int id) {

        if (id < 0) {
            float[][] empty = computeUV(0);
            return new float[][][]{empty, empty, empty, empty, empty, empty};
        }

        float[][] uv = computeUV(id);

        return new float[][][]{
            uv, uv, uv, uv, uv, uv
        };
    }
}
