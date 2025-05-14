package com.github.rmheuer.voxel.client.ui;

import com.github.rmheuer.azalea.render.texture.Texture2DRegion;

/**
 * Represents one sprite in the UI texture atlas.
 */
public final class UISprite {
    private final int width;
    private final int height;
    private final Texture2DRegion texture;

    /**
     * @param width width of the sprite in pixels
     * @param height height of the sprite in pixels
     * @param texture region of the UI atlas containing the sprite
     */
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
