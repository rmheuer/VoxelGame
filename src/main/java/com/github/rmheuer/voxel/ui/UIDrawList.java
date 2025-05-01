package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render2d.DrawList2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.render.AtlasSprite;

public final class UIDrawList implements SafeCloseable {
    private static final int TEXT_SHADOW_COLOR = Colors.RGBA.fromInts(64, 64, 64);

    private final int width, height;
    private final DrawList2D draw;
    private final Texture2D blockAtlas;
    private final TextRenderer textRenderer;

    public UIDrawList(int width, int height, Texture2D blockAtlas, TextRenderer textRenderer) {
        this.width = width;
        this.height = height;
        this.blockAtlas = blockAtlas;
        this.textRenderer = textRenderer;

        draw = new DrawList2D();
    }

    public void drawRect(int x, int y, int w, int h, int color) {
        draw.fillRect(x, y, w, h, color);
    }

    public void drawSprite(int x, int y, UISprite sprite) {
        draw.drawImage(x, y, sprite.getWidth(), sprite.getHeight(), sprite.getTexture());
    }

    public void drawBlockAsItem(int x, int y, int w, int h, Block block) {
        // TODO: Render the block's 3D model instead
        AtlasSprite sprite = block.getShape().getParticleSprite();
        draw.drawImage(
                x, y, w, h,
                blockAtlas,
                sprite.u1, sprite.v1, sprite.u2, sprite.v2
        );
    }

    public void drawText(int x, int y, String text) {
        textRenderer.drawText(draw, x + 1, y + 1, text, TEXT_SHADOW_COLOR);
        textRenderer.drawText(draw, x, y, text, Colors.RGBA.WHITE);
    }

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
