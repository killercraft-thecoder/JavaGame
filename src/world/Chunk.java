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
        return blocks[idx(x, y, z)];
    }

    public void setBlock(int x, int y, int z, int id) {
        blocks[idx(x, y, z)] = id;
    }

    // -----------------------------------------
    // Mesh building (positions ONLY)
    // -----------------------------------------
    public void buildMesh() {

        // Delete old mesh
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
        }

        // Worst-case allocation
        float[] verts = new float[SX * SY * SZ * 6 * 4 * 3]; // 3 floats per vertex
        int[] inds = new int[SX * SY * SZ * 6 * 6];

        int vpos = 0;
        int ipos = 0;
        int base = 0;

        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {

                    int id = getBlock(x, y, z);
                    if (!Block.isSolid(id)) continue;

                    float wx = cx * SX + x;
                    float wy = cy * SY + y;
                    float wz = cz * SZ + z;

                    // +X
                    if (x == SX - 1 || !Block.isSolid(getBlock(x + 1, y, z))) {
                        vpos = addFace(verts, vpos,
                                wx+1, wy,   wz,
                                wx+1, wy+1, wz,
                                wx+1, wy+1, wz+1,
                                wx+1, wy,   wz+1);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -X
                    if (x == 0 || !Block.isSolid(getBlock(x - 1, y, z))) {
                        vpos = addFace(verts, vpos,
                                wx, wy,   wz,
                                wx, wy+1, wz,
                                wx, wy+1, wz+1,
                                wx, wy,   wz+1);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // +Y
                    if (y == SY - 1 || !Block.isSolid(getBlock(x, y + 1, z))) {
                        vpos = addFace(verts, vpos,
                                wx,   wy+1, wz,
                                wx+1, wy+1, wz,
                                wx+1, wy+1, wz+1,
                                wx,   wy+1, wz+1);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -Y
                    if (y == 0 || !Block.isSolid(getBlock(x, y - 1, z))) {
                        vpos = addFace(verts, vpos,
                                wx,   wy, wz,
                                wx+1, wy, wz,
                                wx+1, wy, wz+1,
                                wx,   wy, wz+1);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // +Z
                    if (z == SZ - 1 || !Block.isSolid(getBlock(x, y, z + 1))) {
                        vpos = addFace(verts, vpos,
                                wx,   wy,   wz+1,
                                wx+1, wy,   wz+1,
                                wx+1, wy+1, wz+1,
                                wx,   wy+1, wz+1);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }

                    // -Z
                    if (z == 0 || !Block.isSolid(getBlock(x, y, z - 1))) {
                        vpos = addFace(verts, vpos,
                                wx,   wy,   wz,
                                wx+1, wy,   wz,
                                wx+1, wy+1, wz,
                                wx,   wy+1, wz);
                        ipos = addIndices(inds, ipos, base);
                        base += 4;
                    }
                }
            }
        }

        indexCount = ipos;

        // Upload mesh
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, inds, GL_STATIC_DRAW);

        int stride = 3 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    // -----------------------------------------
    // Face builder helpers (positions only)
    // -----------------------------------------
    private int addFace(float[] v, int p,
                        float x0, float y0, float z0,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3) {

        p = addVertex(v, p, x0, y0, z0);
        p = addVertex(v, p, x1, y1, z1);
        p = addVertex(v, p, x2, y2, z2);
        p = addVertex(v, p, x3, y3, z3);

        return p;
    }

    private int addVertex(float[] v, int p, float x, float y, float z) {
        v[p++] = x;
        v[p++] = y;
        v[p++] = z;
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
