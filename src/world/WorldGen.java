package worldgen;

import world.Block;
import world.Chunk;
import world.World;

import java.util.Random;

public class WorldGen {

    // Perlin permutation table
    private static final int[] PERM = new int[512];

    static {
        Random rng = new Random(1337); // global seed
        int[] p = new int[256];

        for (int i = 0; i < 256; i++) p[i] = i;

        // Shuffle
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        // Duplicate
        for (int i = 0; i < 512; i++) PERM[i] = p[i & 255];
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y) {
        int h = hash & 3;
        float u = (h < 2) ? x : y;
        float v = (h < 2) ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    // 2D Perlin noise
    private static float perlin(float x, float y) {
        int X = (int)Math.floor(x) & 255;
        int Y = (int)Math.floor(y) & 255;

        float xf = x - (int)Math.floor(x);
        float yf = y - (int)Math.floor(y);

        float u = fade(xf);
        float v = fade(yf);

        int aa = PERM[X     + PERM[Y]];
        int ab = PERM[X     + PERM[Y + 1]];
        int ba = PERM[X + 1 + PERM[Y]];
        int bb = PERM[X + 1 + PERM[Y + 1]];

        float x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf));
        float x2 = lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1));

        return lerp(v, x1, x2);
    }

    // Octave Perlin (fractal)
    private static float octavePerlin(float x, float y, int octaves, float persistence, float scale) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += perlin(x * frequency / scale, y * frequency / scale) * amplitude;
            maxValue += amplitude;

            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    // ---------------------------------------------------------
    // Main terrain generation
    // ---------------------------------------------------------
    public void generate(Chunk chunk) {

        for (int y = 0; y < World.CHUNK_SIZE_Y; y++) {
            for (int z = 0; z < World.CHUNK_SIZE_Z; z++) {
                for (int x = 0; x < World.CHUNK_SIZE_X; x++) {

                    float wx = chunk.cx * World.CHUNK_SIZE_X + x;
                    float wz = chunk.cz * World.CHUNK_SIZE_Z + z;

                    // Smooth Perlin height
                    float noise = octavePerlin(wx, wz, 5, 0.5f, 180f);

                    // Scale to world height
                    float terrainHeight = noise * 40f + 20f;
                    int roundedHeight = Math.round(terrainHeight);

                    int worldY = chunk.cy * World.CHUNK_SIZE_Y + y;

                    if (worldY == roundedHeight) {
                        chunk.setBlock(x, y, z, Block.GRASS);
                    } else if (worldY < roundedHeight) {
                        chunk.setBlock(x, y, z, Block.STONE);
                    } else {
                        chunk.setBlock(x, y, z, Block.AIR);
                    }
                }
            }
        }
    }
}
