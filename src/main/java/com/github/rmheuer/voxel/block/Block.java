package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;

public final class Block {
    private final BlockShape shape;
    private boolean lightBlocking;
    private Liquid liquid;

    public Block(BlockShape shape) {
        this.shape = shape;
        lightBlocking = true;
        liquid = null;
    }

    public Block setLightBlocking(boolean lightBlocking) {
        this.lightBlocking = lightBlocking;
        return this;
    }

    public Block setLiquid(Liquid liquid) {
        this.liquid = liquid;
        return this;
    }

    public BlockShape getShape() {
        return shape;
    }

    public boolean isLightBlocking() {
        return lightBlocking;
    }

    public Liquid getLiquid() {
        return liquid;
    }

    public OcclusionType getOcclusion(CubeFace face) {
        return shape.getOcclusion(face);
    }
}
