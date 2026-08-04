package engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import world.World;
import worldgen.WorldGen;
import world.Block;

public class Main {

    private static long window;

    public static void main(String[] args) {

        // -----------------------------------------
        // GLFW Init
        // -----------------------------------------
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW failed to initialize");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(1280, 720, "Voxel Engine", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // vsync

        GL.createCapabilities();

        // -----------------------------------------
        // Load texture atlas
        // -----------------------------------------
        int textureId = loadTexture("textures.png");

        // -----------------------------------------
        // Create world + generate terrain
        // -----------------------------------------
        World world = new World();
        WorldGen gen = new WorldGen();
        world.generateAll(gen);

        // -----------------------------------------
        // Input + Renderer + Game
        // -----------------------------------------
        Input input = new Input(window);
        ConfigGUI gui = new ConfigGUI();   // if you have one
        TextRenderer text = new TextRenderer(); // if you have one

        Renderer renderer = new Renderer(world, gui, text, textureId);
        Game game = new Game(world, renderer, input);

        // -----------------------------------------
        // Main Loop
        // -----------------------------------------
        while (!GLFW.glfwWindowShouldClose(window)) {

            GLFW.glfwPollEvents();

            game.update();
            game.render();

            GLFW.glfwSwapBuffers(window);
        }

        // -----------------------------------------
        // Cleanup
        // -----------------------------------------
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    // -----------------------------------------
    // Texture loader (simple PNG loader)
    // -----------------------------------------
    private static int loadTexture(String path) {
        try {
            java.awt.image.BufferedImage img =
                javax.imageio.ImageIO.read(new java.io.File(path));

            int width = img.getWidth();
            int height = img.getHeight();

            Block.setAtlasSize(width, height);

            int[] pixels = new int[width * height];
            img.getRGB(0, 0, width, height, pixels, 0, width);

            int tex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

            java.nio.ByteBuffer buffer =
                java.nio.ByteBuffer.allocateDirect(width * height * 4);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p = pixels[y * width + x];
                    buffer.put((byte)((p >> 16) & 0xFF)); // R
                    buffer.put((byte)((p >> 8) & 0xFF));  // G
                    buffer.put((byte)(p & 0xFF));         // B
                    buffer.put((byte)((p >> 24) & 0xFF)); // A
                }
            }

            buffer.flip();

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            return tex;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }
}
