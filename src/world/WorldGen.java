package worldgen;

import world.Block;
import world.Chunk;
import world.World;

import java.util.Random;

public class WorldGen {

    private static final int MEGA_POINTS = 4;
    private static final int MINI_POINTS = 10;

    private static final float MEGA_RADIUS = 350f;
    private static final float MINI_RADIUS = 140f;

    private static final float MEGA_WEIGHT = 20f;
    private static final float MINI_WEIGHT = 7f;

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    private static void generatePoints(int cx, int cz, float[][] mega, float[][] mini) {
        long seed = (((long)cx) << 32) ^ (cz * 0x9E3779B97F4A7C15L);
        Random rng = new Random(seed);

        // Mega points: wide world offsets
        for (int i = 0; i < MEGA_POINTS; i++) {
            mega[i][0] = rng.nextFloat() * 3000f - 1500f; // world X offset
            mega[i][1] = rng.nextFloat() * 3000f - 1500f; // world Z offset
        }

        // Mini points: closer offsets
        for (int i = 0; i < MINI_POINTS; i++) {
            mini[i][0] = rng.nextFloat() * 800f - 400f;
            mini[i][1] = rng.nextFloat() * 800f - 400f;
        }
    }

    private static float computeHeight(float wx, float wz, float[][] mega, float[][] mini) {
        float height = 0f;

        // Mega influence
        for (int i = 0; i < MEGA_POINTS; i++) {
            float dx = wx - mega[i][0];
            float dz = wz - mega[i][1];
            float d2 = dx*dx + dz*dz;

            float t = Math.max(0f, 1f - (d2 / (MEGA_RADIUS * MEGA_RADIUS)));
            height += smooth(t) * MEGA_WEIGHT;
        }

        // Mini influence
        for (int i = 0; i < MINI_POINTS; i++) {
            float dx = wx - mini[i][0];
            float dz = wz - mini[i][1];
            float d2 = dx*dx + dz*dz;

            float t = Math.max(0f, 1f - (d2 / (MINI_RADIUS * MINI_RADIUS)));
            height += smooth(t) * MINI_WEIGHT;
        }

        return height;
    }

    public void generate(Chunk chunk) {

        float[][] mega = new float[MEGA_POINTS][2];
        float[][] mini = new float[MINI_POINTS][2];

        generatePoints(chunk.cx, chunk.cz, mega, mini);

        for (int y = 0; y < World.CHUNK_SIZE_Y; y++) {
            for (int z = 0; z < World.CHUNK_SIZE_Z; z++) {
                for (int x = 0; x < World.CHUNK_SIZE_X; x++) {

                    float wx = chunk.cx * World.CHUNK_SIZE_X + x;
                    float wz = chunk.cz * World.CHUNK_SIZE_Z + z;

                    float terrainHeight = computeHeight(wx, wz, mega, mini);
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
