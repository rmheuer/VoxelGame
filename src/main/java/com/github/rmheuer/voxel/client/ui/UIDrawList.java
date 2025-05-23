package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render2d.DrawList2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.client.render.AtlasSprite;

/**
 * Collects UI elements to render.
 */
public final class UIDrawList implements SafeCloseable {
    private static final int TEXT_SHADOW_COLOR = Colors.RGBA.fromInts(64, 64, 64);

    private final int width, height;
    private final DrawList2D draw;
    private final Texture2D blockAtlas;
    private final TextRenderer textRenderer;

    /**
     * @param width width of the UI canvas in pixels
     * @param height height of the UI canvas in pixels
     * @param blockAtlas block atlas texture
     * @param textRenderer text renderer for drawing text
     */
    public UIDrawList(int width, int height, Texture2D blockAtlas, TextRenderer textRenderer) {
        this.width = width;
        this.height = height;
        this.blockAtlas = blockAtlas;
        this.textRenderer = textRenderer;

        draw = new DrawList2D();
    }

    /**
     * Draws a solid colored rectangle with the specified position and size.
     *
     * @param x X coordinate of top-left corner
     * @param y Y coordinate of top-left corner
     * @param w width of rectangle in pixels
     * @param h height of rectangle in pixels
     * @param color color to fill the rectangle with
     */
    public void drawRect(int x, int y, int w, int h, int color) {
        draw.fillRect(x, y, w, h, color);
    }

    /**
     * Draws a rectangle containing a vertical linear gradient with the
     * specified position and size.
     *
     * @param x X coordinate of top-left corner
     * @param y Y coordinate of top-left corner
     * @param w width of rectangle in pixels
     * @param h height of rectangle in pixels
     * @param topColor color at the top edge of the rectangle
     * @param bottomColor color at the bottom edge of the rectangle
     */
    public void drawRectVGradient(int x, int y, int w, int h, int topColor, int bottomColor) {
        draw.fillRectVGradient(x, y, w, h, topColor, bottomColor);
    }

    /**
     * Renders a UI sprite at the specified position.
     *
     * @param x X coordinate of top-left corner
     * @param y Y coordinate of top-left corner
     * @param sprite UI sprite to draw
     */
    public void drawSprite(int x, int y, UISprite sprite) {
        draw.drawImage(x, y, sprite.getWidth(), sprite.getHeight(), sprite.getTexture());
    }

    /**
     * Draws a block as an item with the specified size.
     *
     * @param x X coordinate of top-left corner
     * @param y Y coordinate of top-left corner
     * @param w width of the item sprite
     * @param h height of the item sprite
     * @param block block to render as item
     */
    public void drawBlockAsItem(int x, int y, int w, int h, Block block) {
        // TODO: Render the block's 3D model instead
        AtlasSprite sprite = block.getShape().getParticleSprite();
        draw.drawImage(
                x, y, w, h,
                blockAtlas,
                sprite.u1, sprite.v1, sprite.u2, sprite.v2
        );
    }

    /**
     * Draws a string of text at the specified position.
     *
     * @param x X coordinate of left baseline position
     * @param y Y coordinate of left baseline position
     * @param text text to draw
     */
    public void drawText(int x, int y, String text) {
        textRenderer.drawText(draw, x + 1, y + 1, text, TEXT_SHADOW_COLOR);
        textRenderer.drawText(draw, x, y, text, Colors.RGBA.WHITE);
    }

    /**
     * Draws a string of text centered horizontally at the specified position.
     *
     * @param x X coordinate of center baseline position
     * @param y Y coordinate of center baseline position
     * @param text text to draw
     */
    public void drawTextCentered(int x, int y, String text) {
        int w = textRenderer.textWidth(text);
        drawText(x - w / 2, y, text);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public DrawList2D getDrawList() {
        return draw;
    }

    @Override
    public void close() {
        draw.close();
    }
}
