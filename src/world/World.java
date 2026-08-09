package world;

import engine.Game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import worldgen.WorldGen;

public class World {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 32;   // vertical
    public static final int CHUNK_SIZE_Z = 16;

    // You said you added this — good.
    public static final int WORLD_HEIGHT = 32;

    private final HashMap<Long, Chunk> chunks = new HashMap<>();
    private WorldGen generator;

    private int renderDistance = 4;

    public World() {
        this(new WorldGen());
    }

    public World(WorldGen generator) {
        this.generator = generator;
    }

    // ---------------------------------------------------------
    // Generate all existing chunks (safe)
    // ---------------------------------------------------------
    public void generateAll(WorldGen generator) {
        this.generator = generator;

        for (Chunk chunk : chunks.values()) {
            generator.generate(chunk);

            // compute lighting BEFORE mesh
            chunk.computeSunlight(this);
            chunk.computeFaceLight(this);

            chunk.buildMesh(this);
        }
    }

    // ---------------------------------------------------------
    // Chunk key
    // ---------------------------------------------------------
    private long key(int cx, int cy, int cz) {
        return (((long) cx & 0xFFFF) << 32)
                | (((long) cy & 0xFFFF) << 16)
                | ((long) cz & 0xFFFF);
    }

    // ---------------------------------------------------------
    // Get or create chunk (NO mesh building here)
    // ---------------------------------------------------------
    public Chunk getChunk(int cx, int cy, int cz) {
        long k = key(cx, cy, cz);

        Chunk c = chunks.get(k);
        if (c != null) {
            return c;
        }

        // Create new chunk
        c = new Chunk(cx, cy, cz);
        generator.generate(c);

        c.buildMesh(this);

        chunks.put(k, c);
        return c;
    }

    // ---------------------------------------------------------
    // Block access (NO mesh building here)
    // ---------------------------------------------------------
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

    public int safeGetBlock(int x, int y, int z) {
        int cx = Math.floorDiv(x, CHUNK_SIZE_X);
        int cy = Math.floorDiv(y, CHUNK_SIZE_Y);
        int cz = Math.floorDiv(z, CHUNK_SIZE_Z);

        long k = key(cx, cy, cz);
        Chunk c = chunks.get(k);
        if (c == null) {
            return -1; // treat missing chunk as air
        }
        int lx = Math.floorMod(x, CHUNK_SIZE_X);
        int ly = Math.floorMod(y, CHUNK_SIZE_Y);
        int lz = Math.floorMod(z, CHUNK_SIZE_Z);

        return c.getBlock(lx, ly, lz);
    }

    // ---------------------------------------------------------
    // Set block (safe lighting + mesh rebuild)
    // ---------------------------------------------------------
    public void setBlock(int x, int y, int z, int id) {
        int cx = Math.floorDiv(x, CHUNK_SIZE_X);
        int cy = Math.floorDiv(y, CHUNK_SIZE_Y);
        int cz = Math.floorDiv(z, CHUNK_SIZE_Z);

        Chunk c = getChunk(cx, cy, cz);

        int lx = Math.floorMod(x, CHUNK_SIZE_X);
        int ly = Math.floorMod(y, CHUNK_SIZE_Y);
        int lz = Math.floorMod(z, CHUNK_SIZE_Z);

        c.setBlock(lx, ly, lz, id);
        c.setBlock(lx, ly, lz, id);
        c.lightComputed = false;   // force recompute next time chunk is visible

        c.buildMesh(this);
    }

    // ---------------------------------------------------------
    // Visible chunks for renderer
    // ---------------------------------------------------------
    public List<Chunk> getVisibleChunks(Game game) {
        return getVisibleChunks(game,0f, 0f, 0f);
    }

    public List<Chunk> getVisibleChunks(Game game,float px, float py, float pz) {
        List<Chunk> list = new ArrayList<>();

        int pcx = (int) Math.floor(px / CHUNK_SIZE_X);
        int pcy = (int) Math.floor(py / CHUNK_SIZE_Y);
        int pcz = (int) Math.floor(pz / CHUNK_SIZE_Z);

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {

                    int cx = pcx + dx;
                    int cy = pcy + dy;
                    int cz = pcz + dz;

                    Chunk c = getChunk(cx, cy, cz);
                    // compute lighting ONLY when chunk becomes visible

                    long start = System.nanoTime();

                    c.computeLighting(this);

                    long end = System.nanoTime();

                    // ADD, not set
                    game.lightingTimeNs += (end - start);

// now build mesh AFTER lighting
                    c.buildMesh(this);
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
