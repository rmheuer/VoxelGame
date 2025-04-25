package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;
import com.github.rmheuer.voxel.render.SectionContext;
import com.github.rmheuer.voxel.render.SectionGeometry;

public final class InvisibleShape implements BlockShape {
    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        // No geometry
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        return OcclusionType.NONE;
    }
}
