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

    private final Texture2DRegion hotbar;
    private final Texture2DRegion hotbarHighlight;

    public UISprites(Renderer renderer) throws IOException {
        atlasTexture = renderer.createTexture2D(ResourceUtil.readAsStream("gui.png"));

        // TODO: Figure out what the coordinates of each one are
        hotbar = getSprite(0, 0, 0, 0);
        hotbarHighlight = getSprite(0, 0, 0, 0);
    }

    private Texture2DRegion getSprite(int x, int y, int w, int h) {
        return atlasTexture.getSubRegion(
                (float) x / ATLAS_SIZE,
                (float) y / ATLAS_SIZE,
                (float) (x + w) / ATLAS_SIZE,
                (float) (y + h) / ATLAS_SIZE
        );
    }

    public Texture2DRegion getHotbar() {
        return hotbar;
    }

    public Texture2DRegion getHotbarHighlight() {
        return hotbarHighlight;
    }

    @Override
    public void close() {
        atlasTexture.close();
    }
}
