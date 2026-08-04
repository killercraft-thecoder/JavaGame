package engine;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import world.World;
import world.Chunk;
import world.Block;
import font.TextRenderer;
import gui.ConfigGUI;

public class Renderer {

    private int shaderProgram;
    private int textureId;

    private float[] projection;
    private float[] view;

    private World world;
    private ConfigGUI gui;
    private TextRenderer text;

    public Renderer(World world, ConfigGUI gui, TextRenderer text, int textureId) {
        this.world = world;
        this.gui = gui;
        this.text = text;
        this.textureId = textureId;

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

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
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    // -----------------------------
    // Draw world (with atlas + block uniforms)
    // -----------------------------
    public void drawWorld() {
        glUseProgram(shaderProgram);

        // camera
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "view"), false, view);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, projection);

        // texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(glGetUniformLocation(shaderProgram, "tex"), 0);

        // atlas info (from Block)
        glUniform1i(glGetUniformLocation(shaderProgram, "atlasWidth"), Block.atlasWidth);
        glUniform1i(glGetUniformLocation(shaderProgram, "atlasHeight"), Block.atlasHeight);

        // for now: render as grass (blockId = 0) – you can change per-chunk later
        int blockIdLocation = glGetUniformLocation(shaderProgram, "blockId");
        glUniform1i(blockIdLocation, Block.GRASS);

        for (Chunk chunk : world.getVisibleChunks()) {
            if (!chunk.hasMesh()) continue;

            glBindVertexArray(chunk.getVAO());
            glDrawElements(GL_TRIANGLES, chunk.getIndexCount(), GL_UNSIGNED_INT, 0);
        }
    }

    // -----------------------------
    // Draw GUI
    // -----------------------------
    public void drawGUI() {
        glDisable(GL_DEPTH_TEST);

        gui.draw();
        text.drawText("Render Distance: " + gui.getRenderDistance(), 10, 10);

        glEnable(GL_DEPTH_TEST);
    }

    public void endFrame() {
        // nothing yet
    }
}
