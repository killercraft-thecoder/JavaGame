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

    private int vao = 0;
    private int vbo = 0;
    private int ebo = 0;
    private int indexCount = 0;

    public Chunk(int cx, int cy, int cz) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }

    // -----------------------------------------
    // Block indexing
    // -----------------------------------------
    private int idx(int x, int y, int z) {
        return (y * SX * SZ) + (z * SX) + x;
    }

    public int getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SX || y >= SY || z >= SZ) {
            return Block.AIR;
        }
        return blocks[idx(x, y, z)];
    }

    public void setBlock(int x, int y, int z, int id) {
        if (x < 0 || y < 0 || z < 0 || x >= SX || y >= SY || z >= SZ) {
            return;
        }
        blocks[idx(x, y, z)] = id;
    }

    // -----------------------------------------
    // Mesh building (positions + UVs)
    // -----------------------------------------
    public void buildMesh() {

        // Delete old mesh
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
        }

        final float SCALE = 2.0f;

        float[] verts = new float[SX * SY * SZ * 6 * 4 * 5];
        int[] inds = new int[SX * SY * SZ * 6 * 6];

        int vpos = 0;
        int ipos = 0;
        int base = 0;

        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {

                    int id = getBlock(x, y, z);
                    if (!Block.isSolid(id)) continue;

                    float wx = (cx * SX + x) * SCALE;
                    float wy = (cy * SY + y) * SCALE;
                    float wz = (cz * SZ + z) * SCALE;

                    float[][][] uv = Block.getUVs(id);

                    float sx = SCALE;

                    // +X
                    if (x == SX - 1 || !Block.isSolid(getBlock(x + 1, y, z))) {
                        vpos = addFace(verts, vpos,
                                wx + sx, wy,      wz,      uv[0][0][0], uv[0][0][1],
                                wx + sx, wy + sx, wz,      uv[0][1][0], uv[0][1][1],
                                wx + sx, wy + sx, wz + sx, uv[0][2][0], uv[0][2][1],
                                wx + sx, wy,      wz + sx, uv[0][3][0], uv[0][3][1]);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -X
                    if (x == 0 || !Block.isSolid(getBlock(x - 1, y, z))) {
                        vpos = addFace(verts, vpos,
                                wx,      wy,      wz,      uv[1][0][0], uv[1][0][1],
                                wx,      wy + sx, wz,      uv[1][1][0], uv[1][1][1],
                                wx,      wy + sx, wz + sx, uv[1][2][0], uv[1][2][1],
                                wx,      wy,      wz + sx, uv[1][3][0], uv[1][3][1]);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // +Y
                    if (y == SY - 1 || !Block.isSolid(getBlock(x, y + 1, z))) {
                        vpos = addFace(verts, vpos,
                                wx,      wy + sx, wz,      uv[2][0][0], uv[2][0][1],
                                wx + sx, wy + sx, wz,      uv[2][1][0], uv[2][1][1],
                                wx + sx, wy + sx, wz + sx, uv[2][2][0], uv[2][2][1],
                                wx,      wy + sx, wz + sx, uv[2][3][0], uv[2][3][1]);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -Y
                    if (y == 0 || !Block.isSolid(getBlock(x, y - 1, z))) {
                        vpos = addFace(verts, vpos,
                                wx,      wy,      wz,      uv[3][0][0], uv[3][0][1],
                                wx + sx, wy,      wz,      uv[3][1][0], uv[3][1][1],
                                wx + sx, wy,      wz + sx, uv[3][2][0], uv[3][2][1],
                                wx,      wy,      wz + sx, uv[3][3][0], uv[3][3][1]);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // +Z
                    if (z == SZ - 1 || !Block.isSolid(getBlock(x, y, z + 1))) {
                        vpos = addFace(verts, vpos,
                                wx,      wy,      wz + sx, uv[4][0][0], uv[4][0][1],
                                wx + sx, wy,      wz + sx, uv[4][1][0], uv[4][1][1],
                                wx + sx, wy + sx, wz + sx, uv[4][2][0], uv[4][2][1],
                                wx,      wy + sx, wz + sx, uv[4][3][0], uv[4][3][1]);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -Z
                    if (z == 0 || !Block.isSolid(getBlock(x, y, z - 1))) {
                        vpos = addFace(verts, vpos,
                                wx,      wy,      wz,      uv[5][0][0], uv[5][0][1],
                                wx + sx, wy,      wz,      uv[5][1][0], uv[5][1][1],
                                wx + sx, wy + sx, wz,      uv[5][2][0], uv[5][2][1],
                                wx,      wy + sx, wz,      uv[5][3][0], uv[5][3][1]);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }
                }
            }
        }

        indexCount = ipos;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, inds, GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    // -----------------------------------------
    // Face builder helpers (positions + UVs)
    // -----------------------------------------
    private int addFace(float[] v, int p,
                        float x0, float y0, float z0, float u0, float v0,
                        float x1, float y1, float z1, float u1, float v1,
                        float x2, float y2, float z2, float u2, float v2,
                        float x3, float y3, float z3, float u3, float v3) {

        p = addVertex(v, p, x0, y0, z0, u0, v0);
        p = addVertex(v, p, x1, y1, z1, u1, v1);
        p = addVertex(v, p, x2, y2, z2, u2, v2);
        p = addVertex(v, p, x3, y3, z3, u3, v3);

        return p;
    }

    private int addVertex(float[] v, int p, float x, float y, float z, float u, float vCoord) {
        v[p++] = x;
        v[p++] = y;
        v[p++] = z;
        v[p++] = u;
        v[p++] = vCoord;
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

    // -----------------------------------------
    // Renderer access
    // -----------------------------------------
    public boolean hasMesh() {
        return vao != 0;
    }

    public int getVAO() {
        return vao;
    }

    public int getIndexCount() {
        return indexCount;
    }
}
