package engine;

import org.lwjgl.glfw.GLFW;

import world.World;
import world.Block;

public class Game {

    private World world;
    private Renderer renderer;
    private Camera camera;
    private Input input;

    private float px = 0, py = 20, pz = 0;   // player position
    private float yaw = 0, pitch = 0;        // mouse look

    private final float speed = 0.1f;
    private final float mouseSensitivity = 0.15f;

    public Game(World world, Renderer renderer, Input input) {
        this.world = world;
        this.renderer = renderer;
        this.input = input;

        camera = new Camera();
    }

    // -----------------------------------------
    // Update loop
    // -----------------------------------------
    public void update() {

        handleMouseLook();
        handleMovement();
        handleBlockActions();

        float[] view = camera.look(px, py, pz, yaw, pitch);
        float[] proj = camera.perspective(70f, 1280f/720f, 0.1f, 500f);

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

        // forward/back
        if (input.isKeyDown(GLFW.GLFW_KEY_W)) {
            px += sinY * speed;
            pz -= cosY * speed;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_S)) {
            px -= sinY * speed;
            pz += cosY * speed;
        }

        // strafe
        if (input.isKeyDown(GLFW.GLFW_KEY_A)) {
            px -= cosY * speed;
            pz -= sinY * speed;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_D)) {
            px += cosY * speed;
            pz += sinY * speed;
        }
    }

    // -----------------------------------------
    // Block interaction (raycast)
    // -----------------------------------------
    private void handleBlockActions() {

        // Raycast 6 blocks ahead
        float dirX = (float)Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
        float dirY = (float)Math.sin(Math.toRadians(pitch));
        float dirZ = -(float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));

        float rx = px;
        float ry = py;
        float rz = pz;

        int hitX = -1, hitY = -1, hitZ = -1;

        for (int i = 0; i < 6; i++) {
            rx += dirX;
            ry += dirY;
            rz += dirZ;

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

        // destroy block
        if (hitX != -1 && input.isMousePressed(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
            world.setBlock(hitX, hitY, hitZ, Block.AIR);
        }

        // place block (grass or index 0)
        if (hitX != -1 && input.isMousePressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {

            // place on the block face in front of hit
            int px = hitX + (int)Math.signum(dirX);
            int py = hitY + (int)Math.signum(dirY);
            int pz = hitZ + (int)Math.signum(dirZ);

            world.setBlock(px, py, pz, Block.GRASS);
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
