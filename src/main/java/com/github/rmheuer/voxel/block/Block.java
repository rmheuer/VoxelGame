package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.OcclusionType;

public final class Block {
    private final byte id;
    private final BlockShape shape;

    private byte itemId;
    private AABB boundingBox;
    private boolean solid;
    private boolean interactable;
    private boolean lightBlocking;
    private Liquid liquid;
    private float particleGravityScale;

    public Block(byte id, BlockShape shape) {
        this.id = id;
        this.shape = shape;

        itemId = id;
        boundingBox = shape.getDefaultBoundingBox();
        solid = true;
        interactable = true;
        lightBlocking = true;
        liquid = null;
        particleGravityScale = 1;
    }

    public Block setItemId(byte item) {
        this.itemId = item;
        return this;
    }

    public Block setBoundingBox(AABB boundingBox) {
        this.boundingBox = boundingBox;
        return this;
    }

    public Block setSolid(boolean solid) {
        this.solid = solid;
        return this;
    }

    public Block setInteractable(boolean interactable) {
        this.interactable = interactable;
        return this;
    }

    public Block setLightBlocking(boolean lightBlocking) {
        this.lightBlocking = lightBlocking;
        return this;
    }

    public Block setLiquid(Liquid liquid) {
        this.liquid = liquid;
        return this;
    }

    public Block setParticleGravityScale(float particleGravityScale) {
        this.particleGravityScale = particleGravityScale;
        return this;
    }

    public byte getId() {
        return id;
    }

    public BlockShape getShape() {
        return shape;
    }

    public byte getItemId() {
        return itemId;
    }

    public AABB getBoundingBox() {
        return boundingBox;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isInteractable() {
        return interactable;
    }

    public boolean isLightBlocking() {
        return lightBlocking;
    }

    public Liquid getLiquid() {
        return liquid;
    }

    public float getParticleGravityScale() {
        return particleGravityScale;
    }

    public OcclusionType getOcclusion(CubeFace face) {
        return shape.getOcclusion(face);
    }
}
