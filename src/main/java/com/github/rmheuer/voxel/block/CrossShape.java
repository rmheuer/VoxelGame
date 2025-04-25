package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;
import com.github.rmheuer.voxel.render.*;
import org.joml.Vector3f;

public final class CrossShape implements BlockShape {
    private final AtlasSprite sprite;

    public CrossShape(AtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        float lightShade = ctx.isLit(x, y, z) ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

        geom.addDoubleSidedFace(true, new BlockFace(
                new Vector3f(x, y + 1, z),
                new Vector3f(x, y, z),
                new Vector3f(x + 1, y, z + 1),
                new Vector3f(x + 1, y + 1, z + 1),
                sprite,
                lightShade
        ));
        geom.addDoubleSidedFace(true, new BlockFace(
                new Vector3f(x + 1, y + 1, z),
                new Vector3f(x + 1, y, z),
                new Vector3f(x, y, z + 1),
                new Vector3f(x, y + 1, z + 1),
                sprite,
                lightShade
        ));
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        return OcclusionType.NONE;
    }
}
