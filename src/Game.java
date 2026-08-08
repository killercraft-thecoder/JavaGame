package engine;

import org.lwjgl.glfw.GLFW;

import gui.ConfigGUI;
import world.World;
import world.Block;

public class Game {

    private static final float PLAYER_HEIGHT = 2.0f;
    private static final float PLAYER_HALF_WIDTH = 0.45f;

    private World world;
    private Renderer renderer;
    private Camera camera;
    private Input input;
    private ConfigGUI gui;
    private long window;

    private float px = 8.0f, py = 9.5f, pz = 8.0f;   // player position
    private float yaw = 45.0f, pitch = -18f;         // mouse look
    private boolean lastF11 = false;

    private final float speed = 0.45f;
    private final float mouseSensitivity = 0.12f;

    public Game(World world, Renderer renderer, Input input, long window, ConfigGUI gui) {
        this.world = world;
        this.renderer = renderer;
        this.input = input;
        this.window = window;
        this.gui = gui;

        camera = new Camera();
    }

    // -----------------------------------------
    // Update loop
    // -----------------------------------------
    public void update() {

        if (gui.getScreen() == ConfigGUI.Screen.PLAYING) {
            handleMouseLook();
            handleMovement();
            handleBlockActions();
        }

        boolean f11Pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F11) == GLFW.GLFW_PRESS;
        if (f11Pressed && !lastF11) {
            Main.toggleFullscreen();
        }
        lastF11 = f11Pressed;

        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetFramebufferSize(window, w, h);

        float aspect = (w[0] > 0 && h[0] > 0) ? (float) w[0] / (float) h[0] : 1280f / 720f;
        float[] view = camera.look(px, py + PLAYER_HEIGHT * 0.5f, pz, yaw, pitch);
        float[] proj = camera.perspective(78f, aspect, 0.1f, 500f);

        renderer.setCamera(view, proj);
    }

    // -----------------------------------------
    // Mouse look
    // -----------------------------------------
    private void handleMouseLook() {
        float dx = input.getMouseDX();
        float dy = input.getMouseDY();

        yaw += dx * mouseSensitivity;
        pitch -= dy * mouseSensitivity;

        if (pitch > 89) pitch = 89;
        if (pitch < -89) pitch = -89;
    }

    // -----------------------------------------
    // Movement (WASD)
    // -----------------------------------------
    private void handleMovement() {

        float sinY = (float)Math.sin(Math.toRadians(yaw));
        float cosY = (float)Math.cos(Math.toRadians(yaw));

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

        if (input.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            movePlayer(0.0f, speed * 0.7f, 0.0f);
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            movePlayer(0.0f, -speed * 0.7f, 0.0f);
        }

        movePlayer(moveX, 0.0f, moveZ);
    }

    private void movePlayer(float dx, float dy, float dz) {
        float oldX = px;
        float oldY = py;
        float oldZ = pz;

        px += dx;
        if (isPlayerInsideBlock()) {
            px = oldX;
        }

        pz += dz;
        if (isPlayerInsideBlock()) {
            pz = oldZ;
        }

        py += dy;
        if (isPlayerInsideBlock()) {
            py = oldY;
            if (dy > 0.0f) {
                py += 0.05f;
            }
        }
    }

    private boolean isPlayerInsideBlock() {
        float minX = px - PLAYER_HALF_WIDTH;
        float maxX = px + PLAYER_HALF_WIDTH;
        float minY = py;
        float maxY = py + PLAYER_HEIGHT;
        float minZ = pz - PLAYER_HALF_WIDTH;
        float maxZ = pz + PLAYER_HALF_WIDTH;

        for (int x = (int)Math.floor(minX); x <= (int)Math.floor(maxX); x++) {
            for (int y = (int)Math.floor(minY); y <= (int)Math.floor(maxY); y++) {
                for (int z = (int)Math.floor(minZ); z <= (int)Math.floor(maxZ); z++) {
                    if (world.getBlock(x, y, z) != Block.AIR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // -----------------------------------------
    // Block interaction (raycast)
    // -----------------------------------------
    private void handleBlockActions() {

        float dirX = (float)Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
        float dirY = (float)Math.sin(Math.toRadians(pitch));
        float dirZ = -(float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));

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

            int bx = (int)Math.floor(rx);
            int by = (int)Math.floor(ry);
            int bz = (int)Math.floor(rz);

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
                placeX += (int)Math.signum(dirX);
            } else if (Math.abs(dirY) >= Math.abs(dirX) && Math.abs(dirY) >= Math.abs(dirZ)) {
                placeY += (int)Math.signum(dirY);
            } else {
                placeZ += (int)Math.signum(dirZ);
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
        renderer.beginFrame();
        renderer.drawWorld();
        renderer.drawGUI();
        renderer.endFrame();
    }
}
