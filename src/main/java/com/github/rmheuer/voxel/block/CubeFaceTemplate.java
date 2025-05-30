package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.client.render.AtlasSprite;
import com.github.rmheuer.voxel.client.render.BlockFace;
import com.github.rmheuer.voxel.client.render.LightingConstants;
import org.joml.Vector3f;

/**
 * Template for one face of a cuboid.
 */
public final class CubeFaceTemplate {
    public final CubeFace face;
    private final Vector3f v1, v2, v3, v4;
    private final float faceShade;

    /**
     * @param face which face of the cuboid this is
     * @param faceShade brightness of the face. Should be one of the SHADE
     *                  values from {@link LightingConstants}
     */
    public CubeFaceTemplate(CubeFace face, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float faceShade) {
        this.face = face;
        this.v1 = new Vector3f(x1, y1, z1);
        this.v2 = new Vector3f(x2, y2, z2);
        this.v3 = new Vector3f(x3, y3, z3);
        this.v4 = new Vector3f(x4, y4, z4);
        this.faceShade = faceShade;
    }

    /**
     * Creates the face at the given position.
     *
     * @param x x position to offset by
     * @param y y position to offset by
     * @param z z position to offset by
     * @param sprite sprite that should be drawn on the face
     * @param lightShade shade multiplier from lighting
     */
    public BlockFace makeFace(int x, int y, int z, AtlasSprite sprite, float lightShade) {
        return new BlockFace(
                new Vector3f(v1).add(x, y, z),
                new Vector3f(v2).add(x, y, z),
                new Vector3f(v3).add(x, y, z),
                new Vector3f(v4).add(x, y, z),
                sprite,
                faceShade * lightShade
        );
    }
}
