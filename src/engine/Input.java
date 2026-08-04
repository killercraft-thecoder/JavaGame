package engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

public class Input {

    private long window;

    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private double mouseDX = 0;
    private double mouseDY = 0;

    public Input(long window) {
        this.window = window;

        GLFW.glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long win, double xpos, double ypos) {
                mouseDX = xpos - lastMouseX;
                mouseDY = ypos - lastMouseY;
                lastMouseX = xpos;
                lastMouseY = ypos;
            }
        });

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    // -----------------------------------------
    // Keyboard
    // -----------------------------------------
    public boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    // -----------------------------------------
    // Mouse buttons
    // -----------------------------------------
    public boolean isMousePressed(int button) {
        return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    // -----------------------------------------
    // Mouse movement
    // -----------------------------------------
    public float getMouseDX() {
        float dx = (float) mouseDX;
        mouseDX = 0;
        return dx;
    }

    public float getMouseDY() {
        float dy = (float) mouseDY;
        mouseDY = 0;
        return dy;
    }
}
