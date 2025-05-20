package com.github.rmheuer.voxel.block;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.LevelAccess;

/**
 * Properties of each block type
 */
public final class Block {
    private final byte id;
    private final BlockShape shape;

    private byte itemId;
    private AABB boundingBox;
    private boolean solid;
    private boolean interactable;
    private boolean replaceable;
    private boolean lightBlocking;
    private Liquid liquid;
    private float particleGravityScale;
    private BlockBehavior placementBehavior;
    private BlockBehavior neighborUpdateBehavior;

    /**
     * Creates a new block with the provided shape and default properties.
     *
     * @param id ID of this block. Should match the ID in {@link Blocks}
     * @param shape shape of the block
     */
    public Block(byte id, BlockShape shape) {
        this.id = id;
        this.shape = shape;

        itemId = id;
        boundingBox = shape.getDefaultBoundingBox();
        solid = true;
        interactable = true;
        replaceable = false;
        lightBlocking = true;
        liquid = null;
        particleGravityScale = 1;

        placementBehavior = LevelAccess::setBlockId;
        neighborUpdateBehavior = null;
    }

    /**
     * Sets the item associated with this block. This is the item given to the
     * player when pick-block is used on the block.
     *
     * @param item ID of the item to associate with this block
     * @return this
     */
    public Block setItemId(byte item) {
        this.itemId = item;
        return this;
    }

    /**
     * Sets the bounding box for the block. This is used for both collision and
     * picking.
     *
     * @param boundingBox new bounding box
     * @return this
     */
    public Block setBoundingBox(AABB boundingBox) {
        this.boundingBox = boundingBox;
        return this;
    }

    /**
     * Sets whether this block has solid collision.
     *
     * @param solid whether the block should be collidable
     * @return this
     */
    public Block setSolid(boolean solid) {
        this.solid = solid;
        return this;
    }

    /**
     * Sets whether this block can be targeted by the crosshair.
     *
     * @param interactable whether the block should be interactable
     * @return this
     */
    public Block setInteractable(boolean interactable) {
        this.interactable = interactable;
        return this;
    }

    /**
     * Sets whether this block can be replaced by a new block when placing.
     *
     * @param replaceable whether the block can be replaced by the player
     * @return this
     */
    public Block setReplaceable(boolean replaceable) {
        this.replaceable = replaceable;
        return this;
    }

    /**
     * Sets whether the block should block light from above, casting a shadow.
     *
     * @param lightBlocking whether the block should block light
     * @return this
     */
    public Block setLightBlocking(boolean lightBlocking) {
        this.lightBlocking = lightBlocking;
        return this;
    }

    /**
     * Sets the liquid associated with this block.
     *
     * @param liquid the liquid that should be associated with the block
     * @return this
     */
    public Block setLiquid(Liquid liquid) {
        this.liquid = liquid;
        return this;
    }

    /**
     * Scales the strength of gravity on the particles from breaking this block.
     *
     * @param particleGravityScale scalar for gravity, 1.0 is normal gravity
     * @return this
     */
    public Block setParticleGravityScale(float particleGravityScale) {
        this.particleGravityScale = particleGravityScale;
        return this;
    }

    public Block setPlacementBehavior(BlockBehavior placementBehavior) {
        this.placementBehavior = placementBehavior;
        return this;
    }

    public Block setNeighborUpdateBehavior(BlockBehavior neighborUpdateBehavior) {
        this.neighborUpdateBehavior = neighborUpdateBehavior;
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

    public boolean isReplaceable() {
        return replaceable;
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

    public BlockBehavior getPlacementBehavior() {
        return placementBehavior;
    }

    public BlockBehavior getNeighborUpdateBehavior() {
        return neighborUpdateBehavior;
    }

    /**
     * Helper to get the occlusion from the block shape.
     *
     * @param face face to get occlusion for
     * @return the type of occlusion of that face
     * @see BlockShape#getOcclusion
     */
    public OcclusionType getOcclusion(CubeFace face) {
        return shape.getOcclusion(face);
    }
}
