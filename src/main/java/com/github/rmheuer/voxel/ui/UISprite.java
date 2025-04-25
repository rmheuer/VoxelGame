package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.render.texture.Texture2DRegion;

public final class UISprite {
    private final int width;
    private final int height;
    private final Texture2DRegion texture;

    public UISprite(int width, int height, Texture2DRegion texture) {
        this.width = width;
        this.height = height;
        this.texture = texture;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Texture2DRegion getTexture() {
        return texture;
    }
}
