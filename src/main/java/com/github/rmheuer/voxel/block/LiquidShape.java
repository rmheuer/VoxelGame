package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;
import com.github.rmheuer.voxel.render.*;
import org.joml.Vector3f;

public final class LiquidShape implements BlockShape {
    public static final float LIQUID_SURFACE_HEIGHT = 0.9f;
    private static final float LIQUID_INSET = 0.0015f; // To prevent Z-fighting on touching faces

    private static final class LiquidSideTemplate {
        public final CubeFace face;
        private final float x1, z1, x2, z2;
        private final float faceShade;

        public LiquidSideTemplate(CubeFace face, float x1, float z1, float x2, float z2, float faceShade) {
            this.face = face;
            this.x1 = x1;
            this.z1 = z1;
            this.x2 = x2;
            this.z2 = z2;
            this.faceShade = faceShade;
        }

        public BlockFace makeFace(int x, int y, int z, AtlasSprite tile, float lightShade, boolean applyShade, float bottomY, float topY) {
            return new BlockFace(
                    new Vector3f(x + x1, y + topY, z + z1),
                    new Vector3f(x + x1, y + bottomY, z + z1),
                    new Vector3f(x + x2, y + bottomY, z + z2),
                    new Vector3f(x + x2, y + topY, z + z2),
                    tile.getSection(0, 1 - topY, 1, 1 - bottomY),
                    applyShade ? faceShade * lightShade : 1.0f
            );
        }
    }

    private static final LiquidSideTemplate[] LIQUID_SIDE_TEMPLATES = {
            new LiquidSideTemplate(CubeFace.POS_X, 1 - LIQUID_INSET, 1, 1 - LIQUID_INSET, 0, LightingConstants.SHADE_LEFT_RIGHT),
            new LiquidSideTemplate(CubeFace.NEG_X, LIQUID_INSET, 0, LIQUID_INSET, 1, LightingConstants.SHADE_LEFT_RIGHT),
            new LiquidSideTemplate(CubeFace.POS_Z, 0, 1 - LIQUID_INSET, 1, 1 - LIQUID_INSET, LightingConstants.SHADE_FRONT_BACK),
            new LiquidSideTemplate(CubeFace.NEG_Z, 1, LIQUID_INSET, 0, LIQUID_INSET, LightingConstants.SHADE_FRONT_BACK)
    };

    private final AtlasSprite sprite;
    private final boolean opaque;
    private final boolean applyShade;

    public LiquidShape(AtlasSprite sprite, boolean opaque, boolean applyShade) {
        this.sprite = sprite;
        this.opaque = opaque;
        this.applyShade = applyShade;
    }

    @Override
    public void mesh(SectionContext ctx, Block block, int x, int y, int z, SectionGeometry geom) {
        Liquid thisLiquid = block.getLiquid();

        Block above = ctx.getSurroundingBlock(x, y + 1, z);
        boolean tall = above != null && above.getLiquid() == thisLiquid;

        float lightShade = ctx.isLit(x, y, z) ? LightingConstants.SHADE_LIT : LightingConstants.SHADE_SHADOW;

        if (!tall) {
            boolean surface = false;
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    Block aboveNeighbor = ctx.getSurroundingBlock(x + i, y + 1, z + j);
                    if (aboveNeighbor == null || (aboveNeighbor.getLiquid() != thisLiquid && aboveNeighbor.getOcclusion(CubeFace.NEG_Y) != OcclusionType.FULL)) {
                        surface = true;
                        break;
                    }
                }
            }

            if (surface) {
                float h = LIQUID_SURFACE_HEIGHT;
                geom.addDoubleSidedFace(opaque, new BlockFace(
                        new Vector3f(x, y + h, z),
                        new Vector3f(x, y + h, z + 1),
                        new Vector3f(x + 1, y + h, z + 1),
                        new Vector3f(x + 1, y + h, z),
                        sprite,
                        applyShade ? lightShade * LightingConstants.SHADE_UP : 1.0f
                ));
            }
        }

        for (LiquidSideTemplate sideTemplate : LIQUID_SIDE_TEMPLATES) {
            int nx = x + sideTemplate.face.x;
            int nz = z + sideTemplate.face.z;

            Block neighbor = ctx.getSurroundingBlock(nx, y, nz);
            if (neighbor == null)
                continue;

            if (neighbor.getLiquid() != thisLiquid && neighbor.getOcclusion(sideTemplate.face.getReverse()) != OcclusionType.FULL) {
                float h = tall ? 1 : LIQUID_SURFACE_HEIGHT;
                geom.addDoubleSidedFace(opaque, sideTemplate.makeFace(x, y, z, sprite, lightShade, applyShade, 0, h));
            } else if (tall && neighbor.getLiquid() == thisLiquid) {
                Block aboveNeighbor = ctx.getSurroundingBlock(nx, y + 1, nz);
                boolean neighborTall = aboveNeighbor != null && aboveNeighbor.getLiquid() == thisLiquid;

                if (!neighborTall)
                    geom.addDoubleSidedFace(opaque, sideTemplate.makeFace(x, y, z, sprite, lightShade, applyShade, LIQUID_SURFACE_HEIGHT - LIQUID_INSET, 1));
            }
        }

        Block below = ctx.getSurroundingBlock(x, y - 1, z);
        if (below != null && below.getLiquid() != thisLiquid && below.getOcclusion(CubeFace.POS_Y) != OcclusionType.FULL) {
            geom.addDoubleSidedFace(opaque, new BlockFace(
                    new Vector3f(x + 1, y + LIQUID_INSET, z),
                    new Vector3f(x + 1, y + LIQUID_INSET, z + 1),
                    new Vector3f(x, y + LIQUID_INSET, z + 1),
                    new Vector3f(x, y + LIQUID_INSET, z),
                    sprite,
                    applyShade ? lightShade * LightingConstants.SHADE_DOWN : 1.0f
            ));
        }
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
        return sprite;
    }
}
