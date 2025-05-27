package com.github.rmheuer.voxel.client;

import com.github.rmheuer.azalea.math.MathUtil;
import org.joml.Vector3f;

public final class RemotePlayer extends Player {
    private static final float SMOOTH_FACTOR = 0.8f;

    private Vector3f smoothedPosition;
    private float smoothedPitch;
    private float smoothedYaw;
    private float movementScale;

    public RemotePlayer(float x, float y, float z, float pitch, float yaw) {
        super(x, y, z, pitch, yaw);
        smoothedPosition = new Vector3f(x, y, z);
        smoothedPitch = pitch;
        smoothedYaw = yaw;
        movementScale = 0;
    }

    private Vector3f smoothPosition(float dt) {
        return new Vector3f(
                MathUtil.expDecay(smoothedPosition.x, position.x, SMOOTH_FACTOR, dt),
                MathUtil.expDecay(smoothedPosition.y, position.y, SMOOTH_FACTOR, dt),
                MathUtil.expDecay(smoothedPosition.z, position.z, SMOOTH_FACTOR, dt)
        );
    }

    private float smoothYawContinuous(float dt) {
        float error = MathUtil.wrap(yaw - smoothedYaw, -(float) Math.PI, (float) Math.PI);
        return smoothedYaw + MathUtil.expDecay(0, error, SMOOTH_FACTOR, dt);
    }

    public void tick() {
        smoothedPosition = smoothPosition(1.0f);
        smoothedPitch = MathUtil.expDecay(smoothedPitch, pitch, SMOOTH_FACTOR, 1.0f);
        smoothedYaw = smoothYawContinuous(1.0f);
        movementScale = MathUtil.expDecay(movementScale, isMovingHorizontally() ? 1 : 0, SMOOTH_FACTOR, 1.0f);
    }

    @Override
    public void teleportInstantly(float x, float y, float z) {
        super.teleportInstantly(x, y, z);
        smoothedPosition.set(position);
    }

    @Override
    public void moveInstantly(float dx, float dy, float dz) {
        super.moveInstantly(dx, dy, dz);
        smoothedPosition.set(position);
    }

    @Override
    public void setDirectionInstantly(float pitch, float yaw) {
        super.setDirectionInstantly(pitch, yaw);
        smoothedPitch = pitch;
        smoothedYaw = yaw;
    }

    @Override
    public Vector3f getSmoothedPosition(float subtick) {
        return smoothPosition(subtick);
    }

    public float getSmoothedPitch(float subtick) {
        return MathUtil.expDecay(smoothedPitch, pitch, SMOOTH_FACTOR, subtick);
    }

    public float getSmoothedYaw(float subtick) {
        return smoothYawContinuous(subtick);
    }

    public boolean isMovingHorizontally() {
        return smoothedPosition.distanceSquared(position.x, smoothedPosition.y, position.z) > 0.01f;
    }

    public float getMovementScale(float subtick) {
        return MathUtil.expDecay(movementScale, isMovingHorizontally() ? 1 : 0, SMOOTH_FACTOR, subtick);
    }
}
