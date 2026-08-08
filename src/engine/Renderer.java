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

public class Renderer {

    private int shaderProgram;
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
        glDisable(GL_CULL_FACE);

        shaderProgram = loadShaderProgram("shaders/block.vert", "shaders/block.frag");
    }

    // -----------------------------
    // Shader loading
    // -----------------------------
    private String loadFile(String path) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load shader: " + path);
        }
    }

    private int loadShaderProgram(String vertPath, String fragPath) {
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, loadFile(vertPath));
        glCompileShader(vs);

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, loadFile(fragPath));
        glCompileShader(fs);

        int program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);

        glDeleteShader(vs);
        glDeleteShader(fs);

        return program;
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
    // Draw world (with atlas + block uniforms)
    // -----------------------------
    public void drawWorld() {
        if (view == null || projection == null) {
            return; // avoid undefined shader state
        }
        glUseProgram(shaderProgram);

        int viewLoc = glGetUniformLocation(shaderProgram, "view");
        int projLoc = glGetUniformLocation(shaderProgram, "projection");

        if (viewLoc >= 0) {
            glUniformMatrix4fv(viewLoc, false, view);
        }
        if (projLoc >= 0) {
            glUniformMatrix4fv(projLoc, false, projection);
        }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(glGetUniformLocation(shaderProgram, "tex"), 0);

        for (Chunk chunk : world.getVisibleChunks()) {
            if (!chunk.hasMesh()) {
                continue;
            }
            glBindVertexArray(chunk.getVAO());
            glDrawElements(GL_TRIANGLES, chunk.getIndexCount(), GL_UNSIGNED_INT, 0);
        }
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
}
