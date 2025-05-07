package com.github.rmheuer.voxel;

import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.Axis;
import com.github.rmheuer.azalea.math.MathUtil;
import com.github.rmheuer.voxel.block.Liquid;
import com.github.rmheuer.voxel.level.BlockMap;
import org.joml.Vector3f;

import java.util.List;

public final class Player {
    private static final float JUMP_VELOCITY = 0.42f;
    private static final float TRACTION_ON_GROUND = 0.1f;
    private static final float TRACTION_IN_AIR = 0.02f;
    private static final float AIR_FRICTION_Y = 0.98f;
    private static final float AIR_FRICTION_XZ = 0.91f;
    private static final float GROUND_FRICTION = 0.6f;
    private static final float GRAVITY = 0.08f;

    private static final float BB_SIZE = 0.6f;
    private static final float BB_HEIGHT = 1.8f;

    private final Vector3f position;
    private final Vector3f prevPosition;
    private final Vector3f velocity;

    private float pitch, yaw;
    private boolean onGround;

    public Player(float x, float y, float z) {
        position = new Vector3f(x, y, z);
        prevPosition = new Vector3f(x, y, z);
        velocity = new Vector3f(0, 0, 0);

        pitch = 0;
        yaw = 0;
        onGround = false;
    }

    public void tickMovement(BlockMap map, float inputForward, float inputRight, boolean jump) {
        prevPosition.set(position);

        inputForward *= 0.98f;
        inputRight *= 0.98f;

        AABB box = getBoundingBox();
        boolean inLiquid = map.containsAnyLiquid(box);
        boolean inWater = map.containsLiquid(box, Liquid.WATER);
        boolean inLava = map.containsLiquid(box, Liquid.LAVA);

        if (jump) {
            if (inLiquid) {
                velocity.y += 0.04f;
            } else if (onGround) {
                velocity.y = JUMP_VELOCITY;
            }
        }

        float magnitude = Math.min(1.0f, (float) Math.hypot(inputForward, inputRight));
        if (magnitude > 0) {
            float friction = onGround && !(inWater || inLava)
                    ? TRACTION_ON_GROUND : TRACTION_IN_AIR;
            float scalar = friction / magnitude;
            inputForward *= scalar;
            inputRight *= scalar;
            velocity.add(new Vector3f(inputRight, 0, -inputForward).rotateY(yaw));
        }

        AABB extended = box.expandTowards(velocity.x, velocity.y, velocity.z);
        List<AABB> colliders = map.getCollidersWithin(extended);

        float moveY = velocity.y;
        for (AABB collider : colliders) {
            moveY = box.collideAlongAxis(collider, Axis.Y, moveY);
        }
        box = box.translate(0, moveY, 0);
        float moveX = velocity.x;
        for (AABB collider : colliders) {
            moveX = box.collideAlongAxis(collider, Axis.X, moveX);
        }
        box = box.translate(moveX, 0, 0);
        float moveZ = velocity.z;
        for (AABB collider : colliders) {
            moveZ = box.collideAlongAxis(collider, Axis.Z, moveZ);
        }

        boolean horizontalCollision = moveX != velocity.x || moveZ != velocity.z;
        if (onGround && horizontalCollision) {
            AABB stepUp = getBoundingBox().translate(0, 0.6f, 0);
            AABB stepExtended = extended.expandTowards(0, 0.6f, 0).expandTowards(0, -0.6f, 0);
            colliders = map.getCollidersWithin(stepExtended);

            float stepMoveX = velocity.x;
            for (AABB collider : colliders) {
                stepMoveX = stepUp.collideAlongAxis(collider, Axis.X, stepMoveX);
            }
            stepUp = stepUp.translate(stepMoveX, 0, 0);
            float stepMoveZ = velocity.z;
            for (AABB collider : colliders) {
                stepMoveZ = stepUp.collideAlongAxis(collider, Axis.Z, stepMoveZ);
            }
            stepUp = stepUp.translate(0, 0, stepMoveZ);

            // Move down back onto floor
            float floorMoveY = -0.6f;
            for (AABB collider : colliders) {
                floorMoveY = stepUp.collideAlongAxis(collider, Axis.Y, floorMoveY);
            }
            stepUp = stepUp.translate(0, floorMoveY, 0);

            float stepMoveY = velocity.y;
            for (AABB collider : colliders) {
                stepMoveY = stepUp.collideAlongAxis(collider, Axis.Y, stepMoveY);
            }
            stepUp = stepUp.translate(0, stepMoveY, 0);

            if (stepMoveX * stepMoveX + stepMoveZ * stepMoveZ > moveX * moveX + moveZ * moveZ) {
                moveX = stepMoveX;
                moveY = 0.6f + floorMoveY + stepMoveY;
                moveZ = stepMoveZ;
            }
        }

        if (moveX != velocity.x)
            velocity.x = 0;
        if (moveZ != velocity.z)
            velocity.z = 0;

        onGround = false;
        if (moveY != velocity.y) {
            if (velocity.y < 0)
                onGround = true;
            velocity.y = 0;
        }

        position.add(moveX, moveY, moveZ);

        if (inLiquid) {
            float friction = inWater ? 0.8f : 0.5f;
            velocity.mul(friction);
            velocity.y -= 0.02f;

            if (horizontalCollision) {
                AABB jumpOut = getBoundingBox().translate(velocity.x, velocity.y + 0.6f - moveY, velocity.z);
                if (map.isFree(jumpOut))
                    velocity.y = 0.3f;
            }
        } else {
            velocity.mul(AIR_FRICTION_XZ, AIR_FRICTION_Y, AIR_FRICTION_XZ);
            velocity.y -= GRAVITY;
        }

        if (onGround) {
            velocity.mul(GROUND_FRICTION, 1.0f, GROUND_FRICTION);
        }
    }

    public void turn(float deltaPitch, float deltaYaw) {
        pitch += deltaPitch;
        pitch = MathUtil.clamp(pitch, -(float) Math.PI / 2, (float) Math.PI / 2);

        yaw += deltaYaw;
    }

    public Vector3f getPosition(float subtick) {
        return new Vector3f(prevPosition).lerp(position, subtick);
    }

    public AABB getBoundingBox() {
        return AABB.fromBaseCenterSize(position.x, position.y, position.z, BB_SIZE, BB_HEIGHT, BB_SIZE);
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }
}
