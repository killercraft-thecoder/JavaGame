package worldgen;

import world.Block;
import world.Chunk;
import world.World;

public class WorldGen {

    private static final int GRASS_HEIGHT = 8;

    public void generate(Chunk chunk) {

        for (int y = 0; y < World.CHUNK_SIZE_Y; y++) {
            for (int z = 0; z < World.CHUNK_SIZE_Z; z++) {
                for (int x = 0; x < World.CHUNK_SIZE_X; x++) {

                    int worldY = chunk.cy * World.CHUNK_SIZE_Y + y;

                    if (worldY == GRASS_HEIGHT) {
                        chunk.setBlock(x, y, z, Block.GRASS);   // index 0
                    } else if (worldY < GRASS_HEIGHT) {
                        chunk.setBlock(x, y, z, Block.STONE);   // index 1
                    } else {
                        chunk.setBlock(x, y, z, Block.AIR);
                    }
                }
            }
        }
    }
}

