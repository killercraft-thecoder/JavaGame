package world;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Chunk {

    public static final int SX = World.CHUNK_SIZE_X;
    public static final int SY = World.CHUNK_SIZE_Y;
    public static final int SZ = World.CHUNK_SIZE_Z;

    public final int cx, cy, cz;

    private final int[] blocks = new int[SX * SY * SZ];

    // sunlight per block
    private final boolean[] sunlit = new boolean[SX * SY * SZ];

    // per-face light levels (0–15)
    private final byte[] faceLight = new byte[SX * SY * SZ * 6];

    public boolean lightComputed = false;

    private int vao = 0;
    private int vbo = 0;
    private int ebo = 0;
    private int indexCount = 0;

    public Chunk(int cx, int cy, int cz) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }

    public void computeLighting(World world) {
        if (lightComputed) {
            return;
        }

        computeSunlight(world);
        computeFaceLight(world);

        lightComputed = true;
    }

    // ---------------------------------------------------------
    // Optimized roof check: 5×5 area above block
    // ---------------------------------------------------------
    private boolean hasRoof(World world, int wx, int wy, int wz) {

        // offsets in order of priority: center first
        int[] offsets = {
            0, 0,
            -1, 0, 1, 0,
            0, -1, 0, 1,
            -2, 0, 2, 0,
            0, -2, 0, 2,
            -1, -1, 1, -1, -1, 1, 1, 1,
            -2, -2, 2, -2, -2, 2, 2, 2
        };

        for (int i = 0; i < offsets.length; i += 2) {
            int x = wx + offsets[i];
            int z = wz + offsets[i + 1];

            // scan upward
            for (int yy = wy + 1; yy < World.WORLD_HEIGHT; yy++) {
                int id = world.safeGetBlock(x, yy, z);
                if (id != -1) {
                    return true;
                }
            }
        }

        return false;
    }

    // ---------------------------------------------------------
    // Compute sunlight per block
    // ---------------------------------------------------------
    public void computeSunlight(World world) {

        for (int y = 0; y < SY; y++) {
            int wy = cy * SY + y;

            for (int z = 0; z < SZ; z++) {
                int wz = cz * SZ + z;

                for (int x = 0; x < SX; x++) {
                    int wx = cx * SX + x;

                    int id = getBlock(x, y, z);
                    if (id == -1) {
                        sunlit[idx(x, y, z)] = false;
                        continue;
                    }

                    boolean roof = hasRoof(world, wx, wy, wz);
                    sunlit[idx(x, y, z)] = !roof;
                }
            }
        }
    }

    public int getNeighbor(World world, int x, int y, int z, int face) {

        int nx = x;
        int ny = y;
        int nz = z;

        switch (face) {
            case 0:
                nx = x + 1;
                break; // +X
            case 1:
                nx = x - 1;
                break; // -X
            case 2:
                ny = y + 1;
                break; // +Y
            case 3:
                ny = y - 1;
                break; // -Y
            case 4:
                nz = z + 1;
                break; // +Z
            case 5:
                nz = z - 1;
                break; // -Z
        }

        // If inside this chunk, return directly
        if (nx >= 0 && nx < SX
                && ny >= 0 && ny < SY
                && nz >= 0 && nz < SZ) {

            return getBlock(nx, ny, nz);
        }

        // Outside chunk → world lookup
        int wx = this.cx * SX + nx;
        int wy = this.cy * SY + ny;
        int wz = this.cz * SZ + nz;

        return world.safeGetBlock(wx, wy, wz);
    }

    // ---------------------------------------------------------
    // Compute per-face light levels
    // ---------------------------------------------------------
    public void computeFaceLight(World world) {

        // Loop through all blocks in the chunk
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {

                    int id = getBlock(x, y, z);
                    //if (!sunlit[idx(x, y, z)]) continue;
                    if (id == -1) {
                        continue; // air → skip
                    }
                    int base = idx(x, y, z) * 6;

                    // For each face:
                    // 0 = +X
                    // 1 = -X
                    // 2 = +Y
                    // 3 = -Y
                    // 4 = +Z
                    // 5 = -Z
                    for (int face = 0; face < 6; face++) {

                        int neighbor = getNeighbor(world, x, y, z, face);

                        // If neighbor is solid → face is hidden → no light
                        if (neighbor != -1) {
                            faceLight[base + face] = 0;
                            continue;
                        }

                        // Visible face → compute light
                        // Simple sunlight model:
                        // Up face = brightest
                        // Side faces = medium
                        // Down face = dim
                        int light;

                        switch (face) {
                            case 2: // +Y (top)
                                light = 15;
                                break;

                            case 3: // -Y (bottom)
                                light = 2;
                                break;

                            default: // sides
                                light = 8;
                                break;
                        }

                        faceLight[base + face] = (byte) light;
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Block indexing
    // ---------------------------------------------------------
    private int idx(int x, int y, int z) {
        return (y * SX * SZ) + (z * SX) + x;
    }

    public int getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SX || y >= SY || z >= SZ) {
            return -1;
        }
        return blocks[idx(x, y, z)];
    }

    public void setBlock(int x, int y, int z, int id) {
        if (x < 0 || y < 0 || z < 0 || x >= SX || y >= SY || z >= SZ) {
            return;
        }
        blocks[idx(x, y, z)] = id;
    }

    // ---------------------------------------------------------
    // Mesh building (positions + UVs + light)
    // ---------------------------------------------------------
    public void buildMesh(World world) {

        // Delete old mesh
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            vao = vbo = ebo = 0;
            indexCount = 0;
        }

        final float SCALE = 2.0f;

        // ---------- PASS 1: count faces ----------
        int faceCount = 0;

        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {

                    int id = getBlock(x, y, z);
                    if (id == -1) {
                        continue;
                    }

                    if (x == SX - 1 || getBlock(x + 1, y, z) == -1) {
                        faceCount++;
                    }
                    if (x == 0 || getBlock(x - 1, y, z) == -1) {
                        faceCount++;
                    }
                    if (y == SY - 1 || getBlock(x, y + 1, z) == -1) {
                        faceCount++;
                    }
                    if (y == 0 || getBlock(x, y - 1, z) == -1) {
                        faceCount++;
                    }
                    if (z == SZ - 1 || getBlock(x, y, z + 1) == -1) {
                        faceCount++;
                    }
                    if (z == 0 || getBlock(x, y, z - 1) == -1) {
                        faceCount++;
                    }
                }
            }
        }

        if (faceCount == 0) {
            indexCount = 0;
            vao = 0;
            return;
        }

        // Each vertex = 6 floats (x,y,z,u,v,light)
        float[] verts = new float[faceCount * 4 * 6];
        int[] inds = new int[faceCount * 6];

        int vpos = 0;
        int ipos = 0;
        int base = 0;

        // ---------- PASS 2: fill buffers ----------
        for (int y = 0; y < SY; y++) {
            float wy = (cy * SY + y) * SCALE;

            for (int z = 0; z < SZ; z++) {
                float wz = (cz * SZ + z) * SCALE;

                for (int x = 0; x < SX; x++) {

                    int id = getBlock(x, y, z);
                    if (id == -1) {
                        continue;
                    }

                    float wx = (cx * SX + x) * SCALE;

                    float[][][] uv = Block.getUVs(id);
                    float sx = SCALE;

                    int off = idx(x, y, z) * 6;

                    // +X
                    if (x == SX - 1 || getBlock(x + 1, y, z) == -1) {
                        float light = faceLight[off + 0] / 15f;
                        vpos = addFace(verts, vpos,
                                wx + sx, wy, wz, uv[0][0][0], uv[0][0][1], light,
                                wx + sx, wy + sx, wz, uv[0][1][0], uv[0][1][1], light,
                                wx + sx, wy + sx, wz + sx, uv[0][2][0], uv[0][2][1], light,
                                wx + sx, wy, wz + sx, uv[0][3][0], uv[0][3][1], light);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -X
                    if (x == 0 || getBlock(x - 1, y, z) == -1) {
                        float light = faceLight[off + 1] / 15f;
                        vpos = addFace(verts, vpos,
                                wx, wy, wz, uv[1][0][0], uv[1][0][1], light,
                                wx, wy + sx, wz, uv[1][1][0], uv[1][1][1], light,
                                wx, wy + sx, wz + sx, uv[1][2][0], uv[1][2][1], light,
                                wx, wy, wz + sx, uv[1][3][0], uv[1][3][1], light);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // +Y
                    if (y == SY - 1 || getBlock(x, y + 1, z) == -1) {
                        float light = faceLight[off + 2] / 15f;
                        vpos = addFace(verts, vpos,
                                wx, wy + sx, wz, uv[2][0][0], uv[2][0][1], light,
                                wx + sx, wy + sx, wz, uv[2][1][0], uv[2][1][1], light,
                                wx + sx, wy + sx, wz + sx, uv[2][2][0], uv[2][2][1], light,
                                wx, wy + sx, wz + sx, uv[2][3][0], uv[2][3][1], light);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -Y
                    if (y == 0 || getBlock(x, y - 1, z) == -1) {
                        float light = faceLight[off + 3] / 15f;
                        vpos = addFace(verts, vpos,
                                wx, wy, wz, uv[3][0][0], uv[3][0][1], light,
                                wx + sx, wy, wz, uv[3][1][0], uv[3][1][1], light,
                                wx + sx, wy, wz + sx, uv[3][2][0], uv[3][2][1], light,
                                wx, wy, wz + sx, uv[3][3][0], uv[3][3][1], light);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // +Z
                    if (z == SZ - 1 || getBlock(x, y, z + 1) == -1) {
                        float light = faceLight[off + 4] / 15f;
                        vpos = addFace(verts, vpos,
                                wx, wy, wz + sx, uv[4][0][0], uv[4][0][1], light,
                                wx + sx, wy, wz + sx, uv[4][1][0], uv[4][1][1], light,
                                wx + sx, wy + sx, wz + sx, uv[4][2][0], uv[4][2][1], light,
                                wx, wy + sx, wz + sx, uv[4][3][0], uv[4][3][1], light);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -Z
                    if (z == 0 || getBlock(x, y, z - 1) == -1) {
                        float light = faceLight[off + 5] / 15f;
                        vpos = addFace(verts, vpos,
                                wx, wy, wz, uv[5][0][0], uv[5][0][1], light,
                                wx + sx, wy, wz, uv[5][1][0], uv[5][1][1], light,
                                wx + sx, wy + sx, wz, uv[5][2][0], uv[5][2][1], light,
                                wx, wy + sx, wz, uv[5][3][0], uv[5][3][1], light);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }
                }
            }
        }

        indexCount = ipos;

        // ---------- Upload to GL ----------
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, inds, GL_STATIC_DRAW);

        int stride = 6 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    // ---------------------------------------------------------
    // Face builder helpers
    // ---------------------------------------------------------
    private int addFace(float[] v, int p,
            float x0, float y0, float z0, float u0, float v0, float l0,
            float x1, float y1, float z1, float u1, float v1, float l1,
            float x2, float y2, float z2, float u2, float v2, float l2,
            float x3, float y3, float z3, float u3, float v3, float l3) {

        p = addVertex(v, p, x0, y0, z0, u0, v0, l0);
        p = addVertex(v, p, x1, y1, z1, u1, v1, l1);
        p = addVertex(v, p, x2, y2, z2, u2, v2, l2);
        p = addVertex(v, p, x3, y3, z3, u3, v3, l3);

        return p;
    }

    private int addVertex(float[] v, int p,
            float x, float y, float z,
            float u, float vCoord,
            float light) {

        v[p++] = x;
        v[p++] = y;
        v[p++] = z;
        v[p++] = u;
        v[p++] = vCoord;
        v[p++] = light;
        return p;
    }

    private int addIndices(int[] inds, int p, int base) {
        inds[p++] = base + 0;
        inds[p++] = base + 1;
        inds[p++] = base + 2;
        inds[p++] = base + 2;
        inds[p++] = base + 3;
        inds[p++] = base + 0;
        return p;
    }

    // ---------------------------------------------------------
    // Renderer access
    // ---------------------------------------------------------
    public boolean hasMesh() {
        return vao != 0 && indexCount > 0;
    }

    public int getIndexCount() {
        return Math.max(0, indexCount);
    }

    public int getVAO() {
        return vao;
    }
}
