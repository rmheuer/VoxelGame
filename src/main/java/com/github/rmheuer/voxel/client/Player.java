package com.github.rmheuer.voxel.client;

import com.github.rmheuer.azalea.math.MathUtil;
import org.joml.Vector3f;

/**
 * Handles the state and physics of the player.
 */
public abstract class Player {
    protected final Vector3f position;
    protected final Vector3f prevPosition;
    protected final Vector3f velocity;

    protected float pitch, yaw;

    /**
     * @param x X coordinate of feet position
     * @param y Y coordinate of feet position
     * @param z Z coordinate of feet position
     */
    public Player(float x, float y, float z, float pitch, float yaw) {
        position = new Vector3f(x, y, z);
        prevPosition = new Vector3f(x, y, z);
        velocity = new Vector3f(0, 0, 0);

        this.pitch = pitch;
        this.yaw = yaw;
    }

    public void teleport(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void move(float dx, float dy, float dz) {
        position.add(dx, dy, dz);
    }

    public void setDirection(float pitch, float yaw) {
        this.pitch = MathUtil.wrap(pitch, -(float) Math.PI, (float) Math.PI);;
        this.yaw = yaw;
    }

    /**
     * Rotates the view direction by the specified inputs.
     *
     * @param deltaPitch angle to rotate by around X axis
     * @param deltaYaw angle to rotate by around Y axis
     */
    public void turn(float deltaPitch, float deltaYaw) {
        pitch += deltaPitch;
        pitch = MathUtil.clamp(pitch, -(float) Math.PI / 2, (float) Math.PI / 2);

        yaw += deltaYaw;
    }

    public Vector3f getPosition() {
        return position;
    }

    /**
     * Gets the smoothed feet position.
     *
     * @param subtick percentage elapsed within the current tick
     * @return feet position
     */
    public Vector3f getSmoothedPosition(float subtick) {
        return new Vector3f(prevPosition).lerp(position, subtick);
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }
}
