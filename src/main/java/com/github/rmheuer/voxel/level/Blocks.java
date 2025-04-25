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
        BLOCKS[ID_AIR] = new Block(new InvisibleShape())
                .setLightBlocking(false);
        BLOCKS[ID_SOLID] = new Block(new CubeShape(new AtlasSprite(1, 0)));
        BLOCKS[ID_WATER] = new Block(new LiquidShape(new AtlasSprite(14, 0), false, true))
                .setLiquid(Liquid.WATER)
                .setLightBlocking(false);
        BLOCKS[ID_LAVA] = new Block(new LiquidShape(new AtlasSprite(14, 1), true, false))
                .setLiquid(Liquid.LAVA);
        BLOCKS[ID_CROSS] = new Block(new CrossShape(new AtlasSprite(15, 0)))
                .setLightBlocking(false);
        BLOCKS[ID_SLAB] = new Block(new SlabShape(new AtlasSprite(6, 0), new AtlasSprite(5, 0)));
    }

    public static Block getBlock(byte id) {
        return BLOCKS[id];
    }
}
