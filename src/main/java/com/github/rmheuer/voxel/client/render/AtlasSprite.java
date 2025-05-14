package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.math.MathUtil;

/**
 * Represents a tile within the block atlas texture.
 */
public final class AtlasSprite {
    /** Size of one tile in the block atlas */
    public static final float TILE_SZ = 1 / 16.0f;

    public final float u1, v1, u2, v2;

    /**
     * @param x x coordinate of the tile in the atlas
     * @param y y coordinate of the tile in the atlas
     */
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

    /**
     * Gets a subsection of this sprite.
     *
     * @param leftX X coordinate of the left side of the section from 0-1
     * @param topY Y coordinate of the top side of the section from 0-1
     * @param rightX X coordinate of the right side of the section from 0-1
     * @param bottomY Y coordinate of the bottom side of the section from 0-1
     */
    public AtlasSprite getSection(float leftX, float topY, float rightX, float bottomY) {
        return new AtlasSprite(
                MathUtil.lerp(u1, u2, leftX),
                MathUtil.lerp(v1, v2, topY),
                MathUtil.lerp(u1, u2, rightX),
                MathUtil.lerp(v1, v2, bottomY)
        );
    }

    /**
     * Gets a view of this sprite that is flipped horizontally.
     *
     * @return sprite mirrored over the Y axis
     */
    public AtlasSprite flipHorizontally() {
        return new AtlasSprite(u2, v1, u1, v2);
    }
}
