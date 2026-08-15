package engine;

import org.lwjgl.glfw.GLFW;

import gui.ConfigGUI;
import world.World;
import world.Block;
import engine.AABB;
import font.TextRenderer;
import font.Font;
import org.lwjgl.opengl.GL11;

public class Game {

    private static final float PLAYER_HEIGHT = 2.0f;
    private static final float PLAYER_HALF_WIDTH = 0.45f;

    private World world;
    private Renderer renderer;
    private Camera camera;
    private Input input;
    private ConfigGUI gui;
    private long window;

    private AABB aabb;

    private boolean debugVisible = false;
    private boolean lastF3 = false;

    private long lastFrameTime = System.nanoTime();
    private float fps = 0f;

    public long lightingTimeNs = 0;
    public static long renderTimeNs = 0;

    private TextRenderer textRenderer;

    private float px = 8.0f, py = 9.5f, pz = 8.0f;   // player position
    private float yaw = 45.0f, pitch = -18f;         // mouse look
    private boolean lastF11 = false;

    private final float speed = 0.45f;
    private final float mouseSensitivity = 0.12f;
    public static final long startTimeNs = System.nanoTime();

    private static long msSinceStart() {
        return (System.nanoTime() - startTimeNs) / 1_000_000;
    }
    


    public Game(World world, Renderer renderer, Input input, long window, ConfigGUI gui) {
        System.out.println("[INIT " + msSinceStart() + "ms] Game class initialized");
        this.world = world;
        this.renderer = renderer;
        this.input = input;
        this.window = window;
        this.gui = gui;
        this.textRenderer = new TextRenderer(new Font("font.png", "font.json"));
        System.out.println("[INIT " + msSinceStart() + "ms] TextRenderer initialized");

        camera = new Camera();
        System.out.println("[INIT " + msSinceStart() + "ms] Camera initialized");
        aabb = new AABB(world);   // NEW
        System.out.println("[INIT " + msSinceStart() + "ms] AABB system initialized");

    }

    public float[] getCameraPosition() {
        return new float[]{ px, py, pz };
    }
    
    public float[] getCameraForward() {
        // forward vector from yaw/pitch (same math Camera.look uses)
        float cy = (float)Math.cos(Math.toRadians(yaw));
        float sy = (float)Math.sin(Math.toRadians(yaw));
        float cp = (float)Math.cos(Math.toRadians(pitch));
        float sp = (float)Math.sin(Math.toRadians(pitch));
    
        float fx = sy * cp;
        float fy = sp;
        float fz = -cy * cp;
    
        return new float[]{ fx, fy, fz };
    }
    
    

    // -----------------------------------------
    // Update loop
    // -----------------------------------------
    public void update() {
        this.lightingTimeNs = 0;
        if (gui.getScreen() == ConfigGUI.Screen.PLAYING) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            handleMouseLook();
            handleMovement();
            handleBlockActions();
        }

        boolean f11Pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F11) == GLFW.GLFW_PRESS;
        if (f11Pressed && !lastF11) {
            Main.toggleFullscreen();
        }
        lastF11 = f11Pressed;

        // ESC opens config GUI
        // ESC opens config GUI
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            gui.setScreen(ConfigGUI.Screen.MAIN_MENU);

            // RELEASE MOUSE
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }

// F3 toggles debug overlay
        boolean f3Pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
        if (f3Pressed && !lastF3) {
            debugVisible = !debugVisible;
        }
        lastF3 = f3Pressed;

        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetFramebufferSize(window, w, h);

        float aspect = (w[0] > 0 && h[0] > 0) ? (float) w[0] / (float) h[0] : 1280f / 720f;
        float[] view = camera.look(px, py + PLAYER_HEIGHT * 0.5f, pz, yaw, pitch);
        float[] proj = camera.perspective(78f, aspect, 0.1f, 500f);

        renderer.setCamera(view, proj);

        long now = System.nanoTime();
        fps = 1_000_000_000f / (now - lastFrameTime);
        lastFrameTime = now;

    }

    // -----------------------------------------
    // Mouse look
    // -----------------------------------------
    private void handleMouseLook() {
        float dx = input.getMouseDX();
        float dy = input.getMouseDY();

        yaw += dx * mouseSensitivity;
        pitch -= dy * mouseSensitivity;

        if (pitch > 89) {
            pitch = 89;
        }
        if (pitch < -89) {
            pitch = -89;
        }
    }

    // -----------------------------------------
    // Movement (WASD)
    // -----------------------------------------
    private void handleMovement() {

        float sinY = (float) Math.sin(Math.toRadians(yaw));
        float cosY = (float) Math.cos(Math.toRadians(yaw));

        float moveX = 0.0f;
        float moveZ = 0.0f;

        if (input.isKeyDown(GLFW.GLFW_KEY_W)) {
            moveX += sinY * speed;
            moveZ -= cosY * speed;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_S)) {
            moveX -= sinY * speed;
            moveZ += cosY * speed;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_A)) {
            moveX -= cosY * speed;
            moveZ -= sinY * speed;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_D)) {
            moveX += cosY * speed;
            moveZ += sinY * speed;
        }

        float moveY = 0.0f;

        if (input.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            moveY += speed * 0.7f;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            moveY -= speed * 0.7f;
        }

        movePlayer(moveX, moveY, moveZ);
    }

    // -----------------------------------------
    // Movement + AABB collision
    // -----------------------------------------
    private void movePlayer(float dx, float dy, float dz) {

        float oldX = px;
        float oldY = py;
        float oldZ = pz;

        float newX = px + dx;
        float newY = py + dy;
        float newZ = pz + dz;

        float[] corrected = aabb.resolve(newX, newY, newZ, oldX, oldY, oldZ);

        px = corrected[0];
        py = corrected[1];
        pz = corrected[2];
    }

    // -----------------------------------------
    // Block interaction (raycast)
    // -----------------------------------------
    private void handleBlockActions() {

        float dirX = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        float dirY = (float) Math.sin(Math.toRadians(pitch));
        float dirZ = -(float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));

        float rx = px;
        float ry = py + PLAYER_HEIGHT * 0.5f;
        float rz = pz;

        int hitX = -1;
        int hitY = -1;
        int hitZ = -1;

        for (int i = 0; i < 12; i++) {
            rx += dirX * 0.2f;
            ry += dirY * 0.2f;
            rz += dirZ * 0.2f;

            int bx = (int) Math.floor(rx);
            int by = (int) Math.floor(ry);
            int bz = (int) Math.floor(rz);

            if (world.getBlock(bx, by, bz) != Block.AIR) {
                hitX = bx;
                hitY = by;
                hitZ = bz;
                break;
            }
        }

        if (hitX != -1 && input.isMousePressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
            world.setBlock(hitX, hitY, hitZ, Block.AIR);
        }

        if (hitX != -1 && input.isMousePressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            int placeX = hitX;
            int placeY = hitY;
            int placeZ = hitZ;

            if (Math.abs(dirX) >= Math.abs(dirY) && Math.abs(dirX) >= Math.abs(dirZ)) {
                placeX += (int) Math.signum(dirX);
            } else if (Math.abs(dirY) >= Math.abs(dirX) && Math.abs(dirY) >= Math.abs(dirZ)) {
                placeY += (int) Math.signum(dirY);
            } else {
                placeZ += (int) Math.signum(dirZ);
            }

            if (world.getBlock(placeX, placeY, placeZ) == Block.AIR) {
                world.setBlock(placeX, placeY, placeZ, Block.GRASS);
            }
        }
    }

    // -----------------------------------------
    // Render
    // -----------------------------------------
    public void render() {

        long start = System.nanoTime();

        renderer.beginFrame();
        renderer.drawWorld(this);
        renderer.drawGUI();
        renderer.endFrame();

        this.renderTimeNs = System.nanoTime() - start;

        if (debugVisible) {
            drawDebugOverlay();
        }
        

    }

    private void drawDebugOverlay() {

        // Switch to orthographic projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 1280, 720, 0, -1, 1);
    
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
    
        // Disable depth so text is visible
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    
        // Draw white background
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(300, 0);
        GL11.glVertex2f(300, 100);
        GL11.glVertex2f(0, 100);
        GL11.glEnd();
    
        // Draw black text
        textRenderer.drawText("FPS: " + (int)fps, 10, 10, 1.0f, 0f, 0f, 0f);
        textRenderer.drawText("Lighting: " + (this.lightingTimeNs / 1_000_000f) + " ms", 10, 30, 1.0f, 0f, 0f, 0f);
        textRenderer.drawText("Render: " + (this.renderTimeNs / 1_000_000f) + " ms", 10, 50, 1.0f, 0f, 0f, 0f);
        printDebugToTerminal();

        // Restore matrices
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
    
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    private void printDebugToTerminal() {
        System.out.print("\033[2J\033[H"); // clear + home
    
        System.out.println("=== DEBUG INFO ===");
        System.out.println("FPS: " + (int)fps);
        System.out.println("Lighting: " + (this.lightingTimeNs / 1_000_000f) + " ms");
        System.out.println("Render: " + (this.renderTimeNs / 1_000_000f) + " ms");
    }
    
    
}
