package engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import font.Font;
import font.TextRenderer;
import gui.ConfigGUI;
import world.Block;
import world.World;
import worldgen.WorldGen;

public class Main {

    private static long window;
    private static int windowWidth = 1280;
    private static int windowHeight = 720;
    private static int lastWindowX = 100;
    private static int lastWindowY = 100;
    private static boolean fullscreen = false;

    public static void main(String[] args) {

        // -----------------------------------------
        // GLFW Init
        // -----------------------------------------
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW failed to initialize");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(windowWidth, windowHeight, "Voxel Engine", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                windowWidth = w;
                windowHeight = h;
                GL11.glViewport(0, 0, w, h);
            }
        });

        GLFW.glfwSetWindowPosCallback(window, (win, x, y) -> {
            if (!fullscreen) {
                lastWindowX = x;
                lastWindowY = y;
            }
        });

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // vsync

        GL.createCapabilities();
        GL11.glViewport(0, 0, windowWidth, windowHeight);

        // -----------------------------------------
        // Load texture atlas
        // -----------------------------------------
        int textureId = loadTexture("textures.png");

        // -----------------------------------------
        // Create world + generate terrain
        // -----------------------------------------
        World world = new World(new WorldGen());

        // -----------------------------------------
        // Input + Renderer + Game
        // -----------------------------------------
        Input input = new Input(window);
        ConfigGUI gui = new ConfigGUI();
        Font font = new Font("font.png", "font.json");
        TextRenderer text = new TextRenderer(font);

        Renderer renderer = new Renderer(world, gui, text, textureId, window);
        Game game = new Game(world, renderer, input, window, gui);

        // -----------------------------------------
        // Main Loop
        // -----------------------------------------
        while (!GLFW.glfwWindowShouldClose(window)) {

            GLFW.glfwPollEvents();
            gui.handleInput(window);

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

    public static void toggleFullscreen() {
        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (monitor == 0L) {
            return;
        }

        if (!fullscreen) {
            int[] x = new int[1];
            int[] y = new int[1];
            GLFW.glfwGetWindowPos(window, x, y);
            lastWindowX = x[0];
            lastWindowY = y[0];

            int[] w = new int[1];
            int[] h = new int[1];
            GLFW.glfwGetWindowSize(window, w, h);
            windowWidth = w[0];
            windowHeight = h[0];

            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            GLFW.glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
            fullscreen = true;
        } else {
            GLFW.glfwSetWindowMonitor(window, 0L, lastWindowX, lastWindowY, windowWidth, windowHeight, GLFW.GLFW_DONT_CARE);
            fullscreen = false;
        }
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
