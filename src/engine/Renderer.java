package engine;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.glfw.GLFW;

import world.World;
import world.Chunk;
import world.Block;
import font.TextRenderer;
import gui.ConfigGUI;
import engine.Game;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Renderer with two temporary performance hacks:
 *  1) Skip chunks that are behind the camera (cheap dot test).
 *  2) Render far chunks (>= 75% of render distance) with a flat average color shader
 *     to avoid texture sampling and heavy fragment work.
 *
 * Camera position and forward vector are derived from the view matrix so this file
 * does not depend on any Game camera accessors.
 */
public class Renderer {

    private int texturedProgram;
    private int flatProgram;
    private int textureId;
    private long window;

    private float[] projection;
    private float[] view;

    private World world;
    private ConfigGUI gui;
    private TextRenderer text;

    public Renderer(World world, ConfigGUI gui, TextRenderer text, int textureId, long window) {
        this.world = world;
        this.gui = gui;
        this.text = text;
        this.textureId = textureId;
        this.window = window;

        glEnable(GL_DEPTH_TEST);
        // Keep face culling disabled because your meshes rely on inconsistent winding.
        glDisable(GL_CULL_FACE);

        texturedProgram = loadShaderProgram("shaders/block.vert", "shaders/block.frag");
        flatProgram = loadShaderProgram("shaders/flat.vert", "shaders/flat.frag");
    }

    // -----------------------------
    // Shader loading
    // -----------------------------
    private String loadFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    private int loadShaderProgram(String vertPath, String fragPath) {
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, loadFile(vertPath));
        glCompileShader(vs);
        checkCompile(vs, vertPath);

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, loadFile(fragPath));
        glCompileShader(fs);
        checkCompile(fs, fragPath);

        int program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        checkLink(program);

        glDeleteShader(vs);
        glDeleteShader(fs);

        return program;
    }

    private void checkCompile(int shader, String path) {
        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            throw new RuntimeException("Shader compile failed (" + path + "): " + log);
        }
    }

    private void checkLink(int program) {
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            throw new RuntimeException("Program link failed: " + log);
        }
    }

    // -----------------------------
    // Camera setup
    // -----------------------------
    public void setCamera(float[] view, float[] projection) {
        this.view = view;
        this.projection = projection;
    }

    // -----------------------------
    // Frame control
    // -----------------------------
    public void beginFrame() {
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetFramebufferSize(window, w, h);
        glViewport(0, 0, w[0], h[0]);

        glClearColor(0.4f, 0.7f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    // -----------------------------
    // Draw world (textured + flat LOD)
    // -----------------------------
    public void drawWorld(Game game) {
        if (view == null || projection == null) {
            return; // avoid undefined shader state
        }

        // Derive camera position and forward vector from the view matrix
        // view[] is expected to be a 4x4 column-major matrix (as used with glUniformMatrix4fv)
        float[] camPos = extractCameraPositionFromView(view);
        float[] camForward = extractCameraForwardFromView(view);

        // Render distance in chunks (assumed)
        int renderDistanceChunks = gui.getRenderDistance();
        // threshold: 75% of render distance
        float farThresholdChunks = renderDistanceChunks * 0.75f;

        // Precompute chunk-size in world units (blocks)
        final float chunkSizeX = World.CHUNK_SIZE_X;
        final float chunkSizeZ = World.CHUNK_SIZE_Z;

        for (Chunk chunk : world.getVisibleChunks(game)) {
            if (!chunk.hasMesh()) continue;

            // Compute chunk center in world (block) coordinates
            float chunkCenterX = chunk.cx * chunkSizeX + chunkSizeX * 0.5f;
            float chunkCenterZ = chunk.cz * chunkSizeZ + chunkSizeZ * 0.5f;
            float dx = chunkCenterX - camPos[0];
            float dz = chunkCenterZ - camPos[2];

            // 1) Skip chunks that are behind the camera (cheap dot test)
            float vx = dx;
            float vz = dz;
            float fwx = camForward[0];
            float fwz = camForward[2];
            float dot = vx * fwx + vz * fwz;
            if (dot < 0f) {
                // chunk is behind camera; skip drawing it
                continue;
            }

            // 2) Decide LOD: near (textured) or far (flat)
            float distChunks = (float)Math.sqrt((dx*dx + dz*dz) / (chunkSizeX*chunkSizeX));
            boolean useFlat = distChunks >= farThresholdChunks;

            if (useFlat) {
                // Flat LOD: cheap color shader, no texture bound
                glUseProgram(flatProgram);

                int viewLoc = glGetUniformLocation(flatProgram, "view");
                int projLoc = glGetUniformLocation(flatProgram, "projection");
                if (viewLoc >= 0) glUniformMatrix4fv(viewLoc, false, view);
                if (projLoc >= 0) glUniformMatrix4fv(projLoc, false, projection);

                float[] avg = computeChunkAverageColor(chunk);
                int colorLoc = glGetUniformLocation(flatProgram, "uColor");
                if (colorLoc >= 0) glUniform3f(colorLoc, avg[0], avg[1], avg[2]);

                glBindVertexArray(chunk.getVAO());
                glDrawElements(GL_TRIANGLES, chunk.getIndexCount(), GL_UNSIGNED_INT, 0);

            } else {
                // Textured LOD: original shader and atlas
                glUseProgram(texturedProgram);

                int viewLoc = glGetUniformLocation(texturedProgram, "view");
                int projLoc = glGetUniformLocation(texturedProgram, "projection");

                if (viewLoc >= 0) {
                    glUniformMatrix4fv(viewLoc, false, view);
                }
                if (projLoc >= 0) {
                    glUniformMatrix4fv(projLoc, false, projection);
                }

                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, textureId);
                glUniform1i(glGetUniformLocation(texturedProgram, "tex"), 0);

                glBindVertexArray(chunk.getVAO());
                glDrawElements(GL_TRIANGLES, chunk.getIndexCount(), GL_UNSIGNED_INT, 0);
            }
        }

        // Unbind program/VAO for cleanliness
        glBindVertexArray(0);
        glUseProgram(0);
    }

    // -----------------------------
    // Draw GUI
    // -----------------------------
    public void drawGUI() {
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetFramebufferSize(window, w, h);

        glDisable(GL_DEPTH_TEST);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, w[0], h[0], 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        gui.draw(w[0], h[0]);
        text.drawText("Render Distance: " + gui.getRenderDistance(), 10, 10);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_DEPTH_TEST);
    }

    public void endFrame() {
        // nothing yet
    }

    // -----------------------------
    // Cheap chunk color estimation
    // -----------------------------
    private float[] computeChunkAverageColor(Chunk chunk) {
        int[] sampleX = { World.CHUNK_SIZE_X/2, 2, World.CHUNK_SIZE_X-3, 2, World.CHUNK_SIZE_X-3 };
        int[] sampleZ = { World.CHUNK_SIZE_Z/2, 2, 2, World.CHUNK_SIZE_Z-3, World.CHUNK_SIZE_Z-3 };

        float r = 0f, g = 0f, b = 0f;
        int count = 0;

        for (int i = 0; i < sampleX.length; i++) {
            int sx = sampleX[i];
            int sz = sampleZ[i];

            for (int y = World.CHUNK_SIZE_Y - 1; y >= 0; y--) {
                int id = chunk.getBlock(sx, y, sz);
                if (id != Block.AIR) {
                    float[] col = blockIdToColor(id);
                    r += col[0];
                    g += col[1];
                    b += col[2];
                    count++;
                    break;
                }
            }
        }

        if (count == 0) {
            return new float[]{0.4f, 0.7f, 1.0f};
        }

        return new float[]{ r / count, g / count, b / count };
    }

    private float[] blockIdToColor(int id) {
        switch (id) {
            case Block.GRASS:
                return new float[]{0.35f, 0.65f, 0.2f};
            case Block.STONE:
                return new float[]{0.5f, 0.5f, 0.5f};
            case Block.WOOD:
                return new float[]{0.45f, 0.3f, 0.15f};
            case Block.LEAVES:
                return new float[]{0.25f, 0.55f, 0.2f};
            default:
                return new float[]{0.6f, 0.6f, 0.6f};
        }
    }

    // -----------------------------
    // Camera helpers (derive from view matrix)
    // -----------------------------
    /**
     * Extract camera world position from the view matrix.
     * Assumes view is a 4x4 column-major matrix (as used with glUniformMatrix4fv).
     * For an affine view matrix M = [ R | T; 0 1 ] where M transforms world->camera:
     * cameraPos = -R^T * T
     */
    private float[] extractCameraPositionFromView(float[] viewMat) {
        // translation components (column-major indices 12,13,14)
        float tx = viewMat[12];
        float ty = viewMat[13];
        float tz = viewMat[14];

        // rotation matrix R (upper-left 3x3), column-major layout
        float r00 = viewMat[0], r01 = viewMat[4], r02 = viewMat[8];
        float r10 = viewMat[1], r11 = viewMat[5], r12 = viewMat[9];
        float r20 = viewMat[2], r21 = viewMat[6], r22 = viewMat[10];

        // cameraPos = -R^T * T
        float cx = -(r00 * tx + r10 * ty + r20 * tz);
        float cy = -(r01 * tx + r11 * ty + r21 * tz);
        float cz = -(r02 * tx + r12 * ty + r22 * tz);

        return new float[]{cx, cy, cz};
    }

    /**
     * Extract camera forward vector (normalized) from the view matrix.
     * We compute forward = - (R^T * (0,0,1)) = -third row of R^T = -third column of R.
     * For column-major R, third column is (r02, r12, r22).
     */
    private float[] extractCameraForwardFromView(float[] viewMat) {
        float r02 = viewMat[8];
        float r12 = viewMat[9];
        float r22 = viewMat[10];

        // forward = - (r02, r12, r22)
        float fx = -r02;
        float fy = -r12;
        float fz = -r22;

        // normalize
        float len = (float)Math.sqrt(fx*fx + fy*fy + fz*fz);
        if (len > 1e-6f) {
            fx /= len;
            fy /= len;
            fz /= len;
        } else {
            // fallback forward along -Z
            fx = 0f; fy = 0f; fz = -1f;
        }

        return new float[]{fx, fy, fz};
    }
}
