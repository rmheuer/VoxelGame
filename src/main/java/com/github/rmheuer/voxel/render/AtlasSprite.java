package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.math.MathUtil;

public final class AtlasSprite {
    public static final float TILE_SZ = 1 / 16.0f;

    public final float u1, v1, u2, v2;

    public AtlasSprite(int x, int y) {
        u1 = x * TILE_SZ;
        u2 = u1 + TILE_SZ;
        v1 = y * TILE_SZ;
        v2 = v1 + TILE_SZ;
    }

    private AtlasSprite(float u1, float v1, float u2, float v2) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
    }

    public AtlasSprite getSection(float leftX, float topY, float rightX, float bottomY) {
        return new AtlasSprite(
                MathUtil.lerp(u1, u2, leftX),
                MathUtil.lerp(v1, v2, topY),
                MathUtil.lerp(u1, u2, rightX),
                MathUtil.lerp(v1, v2, bottomY)
        );
    }

    public AtlasSprite flipHorizontally() {
        return new AtlasSprite(u2, v1, u1, v2);
    }
}
