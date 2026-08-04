package font;

import org.lwjgl.opengl.GL11;

public class TextRenderer {

    private final Font font;

    public TextRenderer(Font font) {
        this.font = font;
    }

    public void drawText(String text, float x, float y, float scale, float r, float g, float b) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, font.getTextureId());
        GL11.glColor3f(r, g, b);

        float cursorX = x;
        float cursorY = y;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            Font.Glyph glyph = font.getGlyph(c);
            if (glyph == null) continue;

            float w = glyph.w * scale;
            float h = glyph.h * scale;

            float u0 = glyph.u0;
            float v0 = glyph.v0;
            float u1 = glyph.u1;
            float v1 = glyph.v1;

            GL11.glBegin(GL11.GL_QUADS);

            // top-left
            GL11.glTexCoord2f(u0, v0);
            GL11.glVertex2f(cursorX, cursorY);

            // top-right
            GL11.glTexCoord2f(u1, v0);
            GL11.glVertex2f(cursorX + w, cursorY);

            // bottom-right
            GL11.glTexCoord2f(u1, v1);
            GL11.glVertex2f(cursorX + w, cursorY + h);

            // bottom-left
            GL11.glTexCoord2f(u0, v1);
            GL11.glVertex2f(cursorX, cursorY + h);

            GL11.glEnd();

            cursorX += w; // advance horizontally
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void drawText(String text, float x, float y) {
        drawText(text, x, y, 1.0f, 1f, 1f, 1f);
    }
}
