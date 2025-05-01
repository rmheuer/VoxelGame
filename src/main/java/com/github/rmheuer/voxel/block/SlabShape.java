package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.Axis;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;
import com.github.rmheuer.voxel.render.AtlasSprite;
import com.github.rmheuer.voxel.render.LightingConstants;
import com.github.rmheuer.voxel.render.SectionContext;
import com.github.rmheuer.voxel.render.SectionGeometry;

public final class SlabShape implements BlockShape {
    private static final CubeFaceTemplate[] SLAB_TEMPLATES = {
            new CubeFaceTemplate(CubeFace.POS_X, 1, 0.5f, 1, 1, 0,    1, 1, 0,    0, 1, 0.5f, 0, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.NEG_X, 0, 0.5f, 0, 0, 0,    0, 0, 0,    1, 0, 0.5f, 1, LightingConstants.SHADE_LEFT_RIGHT),
            new CubeFaceTemplate(CubeFace.POS_Y, 0, 0.5f, 0, 0, 0.5f, 1, 1, 0.5f, 1, 1, 0.5f, 0, LightingConstants.SHADE_UP),
            new CubeFaceTemplate(CubeFace.NEG_Y, 1, 0,    0, 1, 0,    1, 0, 0,    1, 0, 0,    0, LightingConstants.SHADE_DOWN),
            new CubeFaceTemplate(CubeFace.POS_Z, 0, 0.5f, 1, 0, 0,    1, 1, 0,    1, 1, 0.5f, 1, LightingConstants.SHADE_FRONT_BACK),
            new CubeFaceTemplate(CubeFace.NEG_Z, 1, 0.5f, 0, 1, 0,    0, 0, 0,    0, 0, 0.5f, 0, LightingConstants.SHADE_FRONT_BACK)
    };

    private final AtlasSprite topSprite;
    private final AtlasSprite sideSprite;

    public SlabShape(AtlasSprite topSprite, AtlasSprite sideSprite) {
        this.topSprite = topSprite;
        this.sideSprite = sideSprite.getSection(0, 0, 1, 0.5f);
    }

    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        for (CubeFaceTemplate faceTemplate : SLAB_TEMPLATES) {
            int nx = x + faceTemplate.face.x;
            int ny = y + faceTemplate.face.y;
            int nz = z + faceTemplate.face.z;

            boolean lit;
            if (faceTemplate.face != CubeFace.POS_Y) {
                Block neighbor = ctx.getSurroundingBlock(nx, ny, nz);
                if (neighbor == null)
                    continue;

                OcclusionType occlusion = neighbor.getOcclusion(faceTemplate.face.getReverse());
                if (faceTemplate.face == CubeFace.NEG_Y && occlusion == OcclusionType.FULL)
                    continue;
                if (faceTemplate.face != CubeFace.NEG_Y && occlusion != OcclusionType.NONE)
                    continue;

                lit = ctx.isLit(nx, ny, nz);
            } else {
                lit = ctx.isLit(x, y, z);
            }

            float lightShade = lit ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

            AtlasSprite sprite = faceTemplate.face.axis == Axis.Y ? topSprite : sideSprite;
            geom.addFace(true, faceTemplate.makeFace(x, y, z, sprite, lightShade));
        }
    }

    @Override
    public OcclusionType getOcclusion(CubeFace face) {
        switch (face) {
            case POS_Y: return OcclusionType.NONE;
            case NEG_Y: return OcclusionType.FULL;
            default: return OcclusionType.HALF;
        }
    }

    @Override
    public AABB getDefaultBoundingBox() {
        return new AABB(0, 0, 0, 1, 0.5f, 1);
    }

    @Override
    public AtlasSprite getParticleSprite() {
        return topSprite;
    }
}
