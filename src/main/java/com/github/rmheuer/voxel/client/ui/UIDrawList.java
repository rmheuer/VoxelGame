package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.texture.Texture2DRegion;
import com.github.rmheuer.azalea.render2d.DrawList2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.client.render.AtlasSprite;

/**
 * Collects UI elements to render.
 */
public final class UIDrawList implements SafeCloseable {
    public static final int TEXT_SHADOW_COLOR = Colors.RGBA.fromInts(64, 64, 64);

    private static final int BG_COLOR_1 = Colors.RGBA.fromInts(5, 5, 0, 96);
    private static final int BG_COLOR_2 = Colors.RGBA.fromInts(48, 48, 96, 160);

    private static final int DIRT_BG_TINT = Colors.RGBA.fromInts(64, 64, 64);

    private final int width, height;
    private final int screenHeight, scale;
    private final DrawList2D draw;
    private final Texture2D blockAtlas;
    private final UISprites sprites;
    private final TextRenderer textRenderer;

    /**
     * @param width width of the UI canvas in pixels
     * @param height height of the UI canvas in pixels
     * @param blockAtlas block atlas texture
     * @param textRenderer text renderer for drawing text
     */
    public UIDrawList(int width, int height, int screenHeight, int scale, Texture2D blockAtlas, UISprites sprites, TextRenderer textRenderer) {
        this.width = width;
        this.height = height;
        this.screenHeight = screenHeight;
        this.scale = scale;
        this.blockAtlas = blockAtlas;
        this.sprites = sprites;
        this.textRenderer = textRenderer;

        draw = new DrawList2D();
    }

    public void setClipRect(int x, int y, int w, int h) {
        draw.setClipRect(x * scale, screenHeight - (y + h) * scale, w * scale, h * scale);
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

    public void drawRectOutline(int x, int y, int w, int h, int color) {
        draw.fillRect(x, y, w, 1, color);
        draw.fillRect(x, y, 1, h, color);
        draw.fillRect(x, y + h - 1, w, 1, color);
        draw.fillRect(x + w - 1, y, 1, h, color);
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

    public void drawSpriteNineSlice(int x, int y, int w, int h, UISprite sprite, int border) {
        if (w == sprite.getWidth()) {
            drawNineSliceVerticalColumn(x, y, w, h, sprite, border, 0, 1);
        } else {
            float invW = 1.0f / sprite.getWidth();
            drawNineSliceVerticalColumn(x, y, border, h, sprite, border, 0, border * invW);
            drawNineSliceVerticalColumn(x + border, y, w - 2 * border, h, sprite, border, border * invW, (w - border) * invW);
            drawNineSliceVerticalColumn(x + w - border, y, border, h, sprite, border, 1 - border * invW, 1);
        }
    }

    private void drawNineSliceVerticalColumn(int x, int y, int w, int h, UISprite sprite, int border, float u1, float u2) {
        Texture2DRegion region = sprite.getTexture();

        if (h == sprite.getHeight()) {
            draw.drawImage(x, y, w, h, region, u1, 0, u2, 1);
        } else {
            float invH = 1.0f / sprite.getHeight();
            draw.drawImage(x, y, w, border, region, u1, 0, u2, border * invH);
            draw.drawImage(x, y + border, w, h - 2 * border, region, u1, border * invH, u2, (h - border) * invH);
            draw.drawImage(x, y + h - border, w, border, region, u1, 1 - border * invH, u2, 1);
        }
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

    public int textWidth(String text) {
        return textRenderer.textWidth(text);
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

    public void drawTextColored(int x, int y, String text, int color) {
        int shadowColor = Colors.RGBA.lerp(Colors.RGBA.BLACK, color, 0.25f);
        textRenderer.drawText(draw, x + 1, y + 1, text, shadowColor);
        textRenderer.drawText(draw, x, y, text, color);
    }

    public void drawTextAlpha(int x, int y, String text, float alpha) {
        textRenderer.drawText(draw, x + 1, y + 1, text, Colors.RGBA.setAlpha(TEXT_SHADOW_COLOR, (int) (alpha * 255)));
        textRenderer.drawText(draw, x, y, text, Colors.RGBA.fromFloats(1, 1, 1, alpha));
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

    public void drawTextCenteredColored(int x, int y, String text, int color) {
        int w = textRenderer.textWidth(text);
        drawTextColored(x - w / 2, y, text, color);
    }

    public void drawGradientBackground(int x, int y, int w, int h) {
        drawRectVGradient(x, y, w, h, BG_COLOR_1, BG_COLOR_2);
    }

    public void drawDirtBackground(int x, int y, int w, int h) {
        draw.drawImage(
                x, y, w, h,
                sprites.getDirtTexture(), DIRT_BG_TINT,
                0, 0, w / 32f, h / 32f
        );
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public UISprites getSprites() {
        return sprites;
    }

    public DrawList2D getDrawList() {
        return draw;
    }

    @Override
    public void close() {
        draw.close();
    }
}
