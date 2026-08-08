package world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import worldgen.WorldGen;

public class World {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 32;   // vertical
    public static final int CHUNK_SIZE_Z = 16;

    private final HashMap<Long, Chunk> chunks = new HashMap<>();
    private WorldGen generator;

    private int renderDistance = 4;

    public World() {
        this(new WorldGen());
    }

    public World(WorldGen generator) {
        this.generator = generator;
    }

    public void generateAll(WorldGen generator) {
        this.generator = generator;
        for (Chunk chunk : chunks.values()) {
            generator.generate(chunk);
            chunk.buildMesh();
        }
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
        int cx = Math.floorDiv(x, CHUNK_SIZE_X);
        int cy = Math.floorDiv(y, CHUNK_SIZE_Y);
        int cz = Math.floorDiv(z, CHUNK_SIZE_Z);

        Chunk c = getChunk(cx, cy, cz);
        int lx = Math.floorMod(x, CHUNK_SIZE_X);
        int ly = Math.floorMod(y, CHUNK_SIZE_Y);
        int lz = Math.floorMod(z, CHUNK_SIZE_Z);
        return c.getBlock(lx, ly, lz);
    }

    public void setBlock(int x, int y, int z, int id) {
        int cx = Math.floorDiv(x, CHUNK_SIZE_X);
        int cy = Math.floorDiv(y, CHUNK_SIZE_Y);
        int cz = Math.floorDiv(z, CHUNK_SIZE_Z);

        Chunk c = getChunk(cx, cy, cz);
        int lx = Math.floorMod(x, CHUNK_SIZE_X);
        int ly = Math.floorMod(y, CHUNK_SIZE_Y);
        int lz = Math.floorMod(z, CHUNK_SIZE_Z);
        c.setBlock(lx, ly, lz, id);
        c.buildMesh();
    }

    // -----------------------------------------
    // Visible chunks for renderer
    // -----------------------------------------
    public List<Chunk> getVisibleChunks() {
        return getVisibleChunks(0f, 0f, 0f);
    }

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
