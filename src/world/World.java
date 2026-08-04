package world;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import worldgen.WorldGen;

public class World {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 32;   // vertical
    public static final int CHUNK_SIZE_Z = 16;

    private final HashMap<Long, Chunk> chunks = new HashMap<>();
    private final WorldGen generator;

    private int renderDistance = 4;

    public World(WorldGen generator) {
        this.generator = generator;
    }

    // -----------------------------------------
    // Chunk key
    // -----------------------------------------
    private long key(int cx, int cy, int cz) {
        return (((long)cx & 0xFFFF) << 32) |
               (((long)cy & 0xFFFF) << 16) |
               ((long)cz & 0xFFFF);
    }

    // -----------------------------------------
    // Get or create chunk
    // -----------------------------------------
    public Chunk getChunk(int cx, int cy, int cz) {
        long k = key(cx, cy, cz);

        Chunk c = chunks.get(k);
        if (c != null) return c;

        // Create new chunk
        c = new Chunk(cx, cy, cz);
        generator.generate(c);
        c.buildMesh();

        chunks.put(k, c);
        return c;
    }

    // -----------------------------------------
    // Block access
    // -----------------------------------------
    public int getBlock(int x, int y, int z) {
        int cx = x / CHUNK_SIZE_X;
        int cy = y / CHUNK_SIZE_Y;
        int cz = z / CHUNK_SIZE_Z;

        Chunk c = getChunk(cx, cy, cz);
        return c.getBlock(x % CHUNK_SIZE_X, y % CHUNK_SIZE_Y, z % CHUNK_SIZE_Z);
    }

    public void setBlock(int x, int y, int z, int id) {
        int cx = x / CHUNK_SIZE_X;
        int cy = y / CHUNK_SIZE_Y;
        int cz = z / CHUNK_SIZE_Z;

        Chunk c = getChunk(cx, cy, cz);
        c.setBlock(x % CHUNK_SIZE_X, y % CHUNK_SIZE_Y, z % CHUNK_SIZE_Z, id);
        c.buildMesh();
    }

    // -----------------------------------------
    // Visible chunks for renderer
    // -----------------------------------------
    public List<Chunk> getVisibleChunks(float px, float py, float pz) {
        List<Chunk> list = new ArrayList<>();

        int pcx = (int)Math.floor(px / CHUNK_SIZE_X);
        int pcy = (int)Math.floor(py / CHUNK_SIZE_Y);
        int pcz = (int)Math.floor(pz / CHUNK_SIZE_Z);

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dy = -1; dy <= 1; dy++) { // vertical range small
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {

                    int cx = pcx + dx;
                    int cy = pcy + dy;
                    int cz = pcz + dz;

                    Chunk c = getChunk(cx, cy, cz);
                    list.add(c);
                }
            }
        }

        return list;
    }

    public void setRenderDistance(int rd) {
        this.renderDistance = rd;
    }

    public int getRenderDistance() {
        return renderDistance;
    }
}
