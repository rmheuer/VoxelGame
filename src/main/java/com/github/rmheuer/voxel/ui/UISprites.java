package com.github.rmheuer.voxel.ui;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.texture.Texture2DRegion;
import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.io.IOException;

public final class UISprites implements SafeCloseable {
    private static final int ATLAS_SIZE = 256;

    private final Texture2D atlasTexture;

    private final UISprite crosshair;
    private final UISprite hotbar;
    private final UISprite hotbarHighlight;

    public UISprites(Renderer renderer) throws IOException {
        atlasTexture = renderer.createTexture2D(ResourceUtil.readAsStream("gui.png"));

        crosshair = getSprite(ATLAS_SIZE - 16, 0, 16, 16);
        hotbar = getSprite(0, 0, 182, 22);
        hotbarHighlight = getSprite(0, 22, 24, 24);
    }

    private UISprite getSprite(int x, int y, int w, int h) {
        Texture2DRegion texture = atlasTexture.getSubRegion(
                (float) x / ATLAS_SIZE,
                (float) y / ATLAS_SIZE,
                (float) (x + w) / ATLAS_SIZE,
                (float) (y + h) / ATLAS_SIZE
        );

        return new UISprite(w, h, texture);
    }

    public UISprite getCrosshair() {
        return crosshair;
    }

    public UISprite getHotbar() {
        return hotbar;
    }

    public UISprite getHotbarHighlight() {
        return hotbarHighlight;
    }

    @Override
    public void close() {
        atlasTexture.close();
    }
}
