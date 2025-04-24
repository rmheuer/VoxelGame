package com.github.rmheuer.voxel.block;

import com.github.rmheuer.voxel.render.SectionContext;
import com.github.rmheuer.voxel.render.SectionGeometry;

public interface BlockShape {
    void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom);
}
