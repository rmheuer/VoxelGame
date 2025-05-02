package com.github.rmheuer.voxel;

import com.github.rmheuer.azalea.math.MathUtil;
import org.joml.Vector3f;

public final class Player {
    private final Vector3f position;
    private final Vector3f prevPosition;

    private float pitch, yaw;

    public Player(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        prevPosition = new Vector3f(x, y, z);

        pitch = 0;
        yaw = 0;
    }

    public void tickMovement(float inputForward, float inputRight, float inputUp) {
        float moveSpeed = 0.5f;
        Vector3f forward = new Vector3f(0, 0, -moveSpeed).rotateY(yaw);
        Vector3f right = new Vector3f(moveSpeed, 0, 0).rotateY(yaw);
        Vector3f up = new Vector3f(0, moveSpeed, 0);

        prevPosition.set(position);
        position.fma(inputForward, forward);
        position.fma(inputRight, right);
        position.fma(inputUp, up);
    }

    public void turn(float deltaPitch, float deltaYaw) {
        pitch += deltaPitch;
        pitch = MathUtil.clamp(pitch, -(float) Math.PI / 2, (float) Math.PI / 2);

        yaw += deltaYaw;
    }

    public Vector3f getPosition(float subtick) {
        return new Vector3f(prevPosition).lerp(position, subtick);
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }
}
