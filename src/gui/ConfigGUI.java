package gui;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class ConfigGUI {

    public enum Screen {
        MAIN_MENU,
        PAUSE_MENU,
        SETTINGS
    }

    private Screen currentScreen = Screen.MAIN_MENU;

    // Example setting
    private int renderDistance = 4; // default

    public void setScreen(Screen s) {
        currentScreen = s;
    }

    public Screen getScreen() {
        return currentScreen;
    }

    // Called every frame by your Renderer
    public void draw() {
        switch (currentScreen) {
            case MAIN_MENU:
                drawMainMenu();
                break;
            case PAUSE_MENU:
                drawPauseMenu();
                break;
            case SETTINGS:
                drawSettings();
                break;
        }
    }

    private void drawMainMenu() {
        drawBackground();
        drawText("MyVoxelGame", 50, 50);
        drawText("Press ENTER to Play", 50, 100);
        drawText("Press S for Settings", 50, 130);
    }

    private void drawPauseMenu() {
        drawBackground();
        drawText("Paused", 50, 50);
        drawText("Press R to Resume", 50, 100);
        drawText("Press S for Settings", 50, 130);
        drawText("Press ESC for Main Menu", 50, 160);
    }

    private void drawSettings() {
        drawBackground();
        drawText("Settings", 50, 50);

        drawText("Render Distance: " + renderDistance + " Chunks", 50, 100);
        drawText("Use LEFT/RIGHT arrows to adjust", 50, 130);
    }

    // Called by your input system
    public void handleInput(long window) {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS) {
            if (currentScreen == Screen.MAIN_MENU) {
                currentScreen = Screen.PAUSE_MENU; // game starts
            }
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            currentScreen = Screen.MAIN_MENU;
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            currentScreen = Screen.SETTINGS;
        }

        if (currentScreen == Screen.SETTINGS) {
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
                if (renderDistance > 1) renderDistance--;
            }
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
                if (renderDistance < 32) renderDistance++;
            }
        }
    }

    // --- Simple drawing helpers (OpenGL immediate mode) ---

    private void drawBackground() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(0.1f, 0.1f, 0.1f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(800, 0);
        GL11.glVertex2f(800, 600);
        GL11.glVertex2f(0, 600);
        GL11.glEnd();
    }

    private void drawText(String text, int x, int y) {
        // Placeholder — you will replace with bitmap font rendering later
        // For now, this is just a stub so your engine compiles.
    }

    public int getRenderDistance() {
        return renderDistance;
    }
}
