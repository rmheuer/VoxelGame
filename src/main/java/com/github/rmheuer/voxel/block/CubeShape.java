package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.OcclusionType;
import com.github.rmheuer.voxel.render.AtlasSprite;
import com.github.rmheuer.voxel.render.LightingConstants;
import com.github.rmheuer.voxel.render.SectionContext;
import com.github.rmheuer.voxel.render.SectionGeometry;

public final class CubeShape implements BlockShape {
    private static final CubeFaceTemplate[] CUBE_TEMPLATES = {
            new CubeFaceTemplate(CubeFace.POS_X, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.NEG_X, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.POS_Y, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, LightingConstants.SHADE_UP),
            new CubeFaceTemplate(CubeFace.NEG_Y, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, LightingConstants.SHADE_DOWN),
            new CubeFaceTemplate(CubeFace.POS_Z, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, LightingConstants.SHADE_FRONT_BACK),
            new CubeFaceTemplate(CubeFace.NEG_Z, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, LightingConstants.SHADE_FRONT_BACK)
    };

    private final AtlasSprite sprite;

    public CubeShape(AtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        for (CubeFaceTemplate faceTemplate : CUBE_TEMPLATES) {
            int nx = x + faceTemplate.face.x;
            int ny = y + faceTemplate.face.y;
            int nz = z + faceTemplate.face.z;

            Block neighbor = ctx.getSurroundingBlock(nx, ny, nz);
            if (neighbor == null && faceTemplate.face != CubeFace.POS_Y)
                continue;
            if (neighbor != null && neighbor.getOcclusion(faceTemplate.face.getReverse()) == OcclusionType.FULL)
                continue;

            boolean lit = ctx.isLit(nx, ny, nz);
            float lightShade = lit ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

            geom.addFace(true, faceTemplate.makeFace(x, y, z, sprite, lightShade));
        }
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        return OcclusionType.FULL;
    }
}
