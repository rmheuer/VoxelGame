package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;
import com.github.rmheuer.voxel.render.*;
import org.joml.Vector3f;

public final class CrossShape implements BlockShape {
    private static final float SIZE = (float) Math.sqrt(2) / 4;
    private static final float MIN_POS = 0.5f - SIZE;
    private static final float MAX_POS = 0.5f + SIZE;

    private final AtlasSprite sprite;

    public CrossShape(AtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        float lightShade = ctx.isLit(x, y, z) ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

        geom.addDoubleSidedFace(true, new BlockFace(
                new Vector3f(x + MIN_POS, y + 1, z + MIN_POS),
                new Vector3f(x + MIN_POS, y, z + MIN_POS),
                new Vector3f(x + MAX_POS, y, z + MAX_POS),
                new Vector3f(x + MAX_POS, y + 1, z + MAX_POS),
                sprite,
                lightShade
        ));
        geom.addDoubleSidedFace(true, new BlockFace(
                new Vector3f(x + MAX_POS, y + 1, z + MIN_POS),
                new Vector3f(x + MAX_POS, y, z + MIN_POS),
                new Vector3f(x + MIN_POS, y, z + MAX_POS),
                new Vector3f(x + MIN_POS, y + 1, z + MAX_POS),
                sprite,
                lightShade
        ));
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        return OcclusionType.NONE;
    }

    @Override
    public AABB getDefaultBoundingBox() {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    @Override
    public AtlasSprite getParticleSprite() {
        return sprite;
    }
}
