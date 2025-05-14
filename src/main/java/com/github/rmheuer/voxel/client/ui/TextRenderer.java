package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.texture.Bitmap;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.texture.Texture2DRegion;
import com.github.rmheuer.azalea.render2d.DrawList2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper to render text using the bitmap font texture.
 */
public final class TextRenderer implements SafeCloseable {
    private static final class Glyph {
        private final Texture2DRegion region;
        private final int width;

        public Glyph(Texture2DRegion region, int width) {
            this.region = region;
            this.width = width;
        }
    }

    private final Texture2D atlas;
    private final Map<Character, Glyph> glyphs;

    public TextRenderer(Renderer renderer) throws IOException {
        Bitmap bitmap = Bitmap.decode(ResourceUtil.readAsStream("default.png"));

        atlas = renderer.createTexture2D();
        atlas.setData(bitmap);
        glyphs = new HashMap<>();

        // TODO: Figure out what char codes the non-ASCII characters are
        for (char c = ' '; c <= '~'; c++) {
            int tx = (c % 16) * 8;
            int ty = (c / 16) * 8;

            // Find rightmost edge of character
            int width = 8;
            outer: while (width > 0) {
                for (int y = 0; y < 8; y++) {
                    if (Colors.RGBA.getAlpha(bitmap.getPixel(tx + width - 1, ty + y)) != 0) {
                        break outer;
                    }
                }
                width--;
            }

            if (c == ' ')
                width = 3;

            Texture2DRegion region = atlas.getSubRegion(tx / 128f, ty / 128f, (tx + width) / 128f, (ty + 8) / 128f);
            glyphs.put(c, new Glyph(region, width));
        }
    }

    /**
     * Gets the width in pixels that a String would be rendered at
     *
     * @param text text to get width
     * @return width of the text in pixels
     */
    public int textWidth(String text) {
        char[] chars = text.toCharArray();

        int width = 0;
        for (char c : chars) {
            Glyph glyph = glyphs.get(c);
            if (glyph == null)
                continue;

            width += glyph.width + 1;
        }

        if (width > 0)
            width--;

        return width;
    }

    /**
     * Draws a String of text into a draw list.
     *
     * @param draw draw list to draw into
     * @param x X coordinate of left edge
     * @param y Y coordinate of baseline
     * @param text text to draw
     * @param color color to draw the text
     */
    public void drawText(DrawList2D draw, int x, int y, String text, int color) {
        char[] chars = text.toCharArray();
        for (char c : chars) {
            Glyph glyph = glyphs.get(c);
            if (glyph == null)
                continue;

            draw.drawImage(x, y - 7, glyph.width, 8, glyph.region, color);
            x += glyph.width + 1;
        }
    }

    @Override
    public void close() {
        atlas.close();
    }
}
