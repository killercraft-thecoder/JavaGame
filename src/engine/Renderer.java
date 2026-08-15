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
 * Renderer with two temporary performance hacks: 1) Skip chunks that are behind
 * the camera (cheap dot test). 2) Render far chunks (>= 75% of render distance)
 * with a flat average color shader to avoid texture sampling and heavy fragment
 * work.
 *
 * Camera position and forward vector are derived from the view matrix so this
 * file does not depend on any Game camera accessors.
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

        float[] camPos = game.getCameraPosition();
        float[] camForward = game.getCameraForward();

        // Render distance in chunks (assumed)
        int renderDistanceChunks = gui.getRenderDistance();
        float farThresholdChunks = renderDistanceChunks * 0.75f;

        final float chunkSizeX = World.CHUNK_SIZE_X;
        final float chunkSizeZ = World.CHUNK_SIZE_Z;

        for (Chunk chunk : world.getVisibleChunks(game)) {
            if (!chunk.hasMesh()) {
                continue;
            }

            // Compute chunk center in world (block) coordinates
            float chunkCenterX = chunk.cx * chunkSizeX + chunkSizeX * 0.5f;
            float chunkCenterZ = chunk.cz * chunkSizeZ + chunkSizeZ * 0.5f;

            float dx = chunkCenterX - camPos[0];
            float dz = chunkCenterZ - camPos[2];

            // world-space distance in blocks (XZ plane)
            float distBlocks = (float) Math.sqrt(dx * dx + dz * dz);

// convert to chunk units
            float distChunks = distBlocks / chunkSizeX;

// use threshold in chunks
            //boolean useFlat = distChunks >= farThresholdChunks;
            boolean useFlat = false;
            // 1) Skip chunks that are behind the camera (cheap dot test)
            float vx = dx;
            float vz = dz;
            float fwx = camForward[0];
            float fwz = camForward[2];
            float dot = vx * fwx + vz * fwz;
            //if (dot < -0.1f) {
            //    // chunk is behind camera; skip drawing it
            //     continue;
            //}

            if (useFlat) {
                // Flat LOD: cheap color shader, no texture bound
                glUseProgram(flatProgram);

                int viewLoc = glGetUniformLocation(flatProgram, "view");
                int projLoc = glGetUniformLocation(flatProgram, "projection");
                if (viewLoc >= 0) {
                    glUniformMatrix4fv(viewLoc, false, view);
                }
                if (projLoc >= 0) {
                    glUniformMatrix4fv(projLoc, false, projection);
                }

                float[] avg = computeChunkAverageColor(chunk);
                int colorLoc = glGetUniformLocation(flatProgram, "uColor");
                if (colorLoc >= 0) {
                    glUniform3f(colorLoc, avg[0], avg[1], avg[2]);
                }

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
        int[] sampleX = {World.CHUNK_SIZE_X / 2, 2, World.CHUNK_SIZE_X - 3, 2, World.CHUNK_SIZE_X - 3};
        int[] sampleZ = {World.CHUNK_SIZE_Z / 2, 2, 2, World.CHUNK_SIZE_Z - 3, World.CHUNK_SIZE_Z - 3};

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

        return new float[]{r / count, g / count, b / count};
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

}
