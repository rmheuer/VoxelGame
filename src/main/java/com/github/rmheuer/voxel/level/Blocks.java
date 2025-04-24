package com.github.rmheuer.voxel.level;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.voxel.block.*;
import com.github.rmheuer.voxel.render.AtlasSprite;

public final class Blocks {
    /*
    Block {
      shape, lightPassthrough, collision box, picking box
    }

    Shape {
      invisible, cube(TransparencyType), liquid(opaque, shaded), cross, slab
    }

    TransparencyType {
      opaque,
      transparent,
      transparent occlude self
    }
     */

    public static final byte ID_AIR = 0;
    public static final byte ID_SOLID = 1;
    public static final byte ID_WATER = 2;
    public static final byte ID_LAVA = 3;
    public static final byte ID_CROSS = 4;
    public static final byte ID_SLAB = 5;

    private static final Block[] BLOCKS = new Block[256];
    static {
        BLOCKS[ID_AIR] = new Block(new InvisibleShape(), Colors.RGBA.TRANSPARENT)
                .setLightBlocking(false);
        BLOCKS[ID_SOLID] = new Block(new CubeShape(new AtlasSprite(1, 0)), Colors.RGBA.WHITE);
        BLOCKS[ID_WATER] = new Block(new LiquidShape(new AtlasSprite(14, 0), false, true), Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f))
                .setLiquid(Liquid.WATER)
                .setLightBlocking(false);
        BLOCKS[ID_LAVA] = new Block(new LiquidShape(new AtlasSprite(14, 1), true, false), Colors.RGBA.fromFloats(1.0f, 0.5f, 0.0f))
                .setLiquid(Liquid.LAVA);
        BLOCKS[ID_CROSS] = new Block(new CrossShape(new AtlasSprite(15, 0)), Colors.RGBA.fromFloats(0.2f, 0.5f, 0.2f))
                .setLightBlocking(false);
        BLOCKS[ID_SLAB] = new Block(new SlabShape(new AtlasSprite(6, 0), new AtlasSprite(5, 0)), Colors.RGBA.fromFloats(0.8f, 0.6f, 0.6f));
    }

    public static Block getBlock(byte id) {
        return BLOCKS[id];
    }

//    public static boolean blocksLight(byte id) {
//        switch (id) {
//            case ID_SOLID:
//            case ID_SLAB:
//            case ID_LAVA:
//                return true;
//            default:
//                return false;
//        }
//    }
//
//    public static OcclusionType getOcclusion(byte id, CubeFace face) {
//        switch (id) {
//            case ID_AIR:
//            case ID_WATER:
//            case ID_LAVA:
//            case ID_CROSS:
//                return OcclusionType.NONE;
//
//            case ID_SOLID:
//                return OcclusionType.FULL;
//
//            case ID_SLAB:
//                if (face == CubeFace.POS_Y)
//                    return OcclusionType.NONE;
//                else if (face == CubeFace.NEG_Y)
//                    return OcclusionType.FULL;
//                else
//                    return OcclusionType.HALF;
//
//            default:
//                throw new AssertionError();
//        }
//    }
//
//    public static AtlasSprite getSprite(byte id) {
//        switch (id) {
//            case ID_SOLID: return new AtlasSprite(1, 0);
//            case ID_WATER: return new AtlasSprite(14, 0);
//            case ID_LAVA: return new AtlasSprite(14, 1);
//            case ID_CROSS: return new AtlasSprite(15, 0);
//            case ID_SLAB: return new AtlasSprite(6, 0);
//            default:
//                return new AtlasSprite(15, 15);
//        }
//    }
//
//    public static int getColor(byte id) {
//        switch (id) {
//            case ID_SOLID:
//                return Colors.RGBA.WHITE;
//            case ID_WATER:
//                return Colors.RGBA.fromFloats(0.3f, 0.3f, 1.0f, 0.6f);
//            case ID_LAVA:
//                return Colors.RGBA.fromFloats(1.0f, 0.5f, 0.0f);
//            case ID_CROSS:
//                return Colors.RGBA.fromFloats(0.2f, 0.5f, 0.2f);
//            case ID_SLAB:
//                return Colors.RGBA.fromFloats(0.8f, 0.6f, 0.6f);
//
//            default:
//                return Colors.RGBA.MAGENTA;
//        }
//    }
}
