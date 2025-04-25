package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render2d.DrawList2D;
import com.github.rmheuer.voxel.block.Block;
import com.github.rmheuer.voxel.render.AtlasSprite;

public final class UIDrawList {
    private final int width, height;
    private final DrawList2D draw;
    private final Texture2D blockAtlas;

    public UIDrawList(int width, int height, Texture2D blockAtlas) {
        this.width = width;
        this.height = height;
        this.blockAtlas = blockAtlas;

        draw = new DrawList2D();
    }

    public void drawSprite(int x, int y, UISprite sprite) {
        draw.drawImage(x, y, sprite.getWidth(), sprite.getHeight(), sprite.getTexture());
    }

    public void drawBlockAsItem(int x, int y, Block block) {
        // TODO: Render the block's 3D model instead
        AtlasSprite sprite = block.getShape().getParticleSprite();
        draw.drawImage(
                x, y, 16, 16,
                blockAtlas,
                sprite.u1, sprite.v1, sprite.u2, sprite.v2
        );
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
}
