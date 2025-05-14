package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.client.render.AtlasSprite;
import com.github.rmheuer.voxel.client.render.SectionContext;
import com.github.rmheuer.voxel.client.render.SectionGeometry;

/**
 * Invisible block
 */
public final class InvisibleShape implements BlockShape {
    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        // No geometry
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        return OcclusionType.NONE;
    }

    @Override
    public AABB getDefaultBoundingBox() {
        return null;
    }

    @Override
    public AtlasSprite getParticleSprite() {
        throw new UnsupportedOperationException("Should not be making particles for invisible block");
    }
}
