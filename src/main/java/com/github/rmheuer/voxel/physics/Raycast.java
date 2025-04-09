package com.github.rmheuer.voxel.physics;

import com.github.rmheuer.azalea.math.Axis;
import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.BlockMap;
import org.joml.Vector3f;
import org.joml.Vector3i;

public final class Raycast {
    public static final class Result {
        public final Vector3i blockPos;
        public final Vector3f hitPos;
        public final CubeFace hitFace;

        public Result(Vector3i blockPos, Vector3f hitPos, CubeFace hitFace) {
            this.blockPos = blockPos;
            this.hitPos = hitPos;
            this.hitFace = hitFace;
        }
    }

    // Direction must be normalized
    public static Result raycast(BlockMap map, Vector3f pos, Vector3f dir, float maxDistance) {
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);

        // FIXME: Should find point at which ray enters grid
        if (!map.isBlockInBounds(blockX, blockY, blockZ))
            return null;

        int stepX = dir.x > 0 ? 1 : -1;
        int stepY = dir.y > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;

        float tDeltaX = Math.abs(1.0f / dir.x);
        float tDeltaY = Math.abs(1.0f / dir.y);
        float tDeltaZ = Math.abs(1.0f / dir.z);

        float xDist = stepX > 0 ? (blockX + 1 - pos.x) : (pos.x - blockX);
        float yDist = stepY > 0 ? (blockY + 1 - pos.y) : (pos.y - blockY);
        float zDist = stepZ > 0 ? (blockZ + 1 - pos.z) : (pos.z - blockZ);

        float tMaxX = tDeltaX < Float.POSITIVE_INFINITY ? tDeltaX * xDist : Float.POSITIVE_INFINITY;
        float tMaxY = tDeltaY < Float.POSITIVE_INFINITY ? tDeltaY * yDist : Float.POSITIVE_INFINITY;
        float tMaxZ = tDeltaZ < Float.POSITIVE_INFINITY ? tDeltaZ * zDist : Float.POSITIVE_INFINITY;

        int escapeX = stepX > 0 ? map.getBlocksX() : -1;
        int escapeY = stepY > 0 ? map.getBlocksY() : -1;
        int escapeZ = stepZ > 0 ? map.getBlocksZ() : -1;

        Axis axis = null;
        float t = 0.0f;
        while (t < maxDistance) {
            byte blockId = map.getBlockId(blockX, blockY, blockZ);
            if (blockId == Blocks.ID_SOLID) {
                Vector3f hitPos = new Vector3f(pos).fma(t, dir);

                CubeFace hitFace = null;
                if (axis != null) {
                    switch (axis) {
                        case X:
                            hitFace = stepX > 0 ? CubeFace.NEG_X : CubeFace.POS_X;
                            break;
                        case Y:
                            hitFace = stepY > 0 ? CubeFace.NEG_Y : CubeFace.POS_Y;
                            break;
                        case Z:
                            hitFace = stepZ > 0 ? CubeFace.NEG_Z : CubeFace.POS_Z;
                            break;
                    }
                }

                return new Result(
                        new Vector3i(blockX, blockY, blockZ),
                        hitPos,
                        hitFace
                );
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                blockX += stepX;
                if (blockX == escapeX)
                    return null;
                t = tMaxX;
                tMaxX += tDeltaX;
                axis = Axis.X;
            } else if (tMaxY < tMaxX && tMaxY < tMaxZ) {
                blockY += stepY;
                if (blockY == escapeY)
                    return null;
                t = tMaxY;
                tMaxY += tDeltaY;
                axis = Axis.Y;
            } else {
                blockZ += stepZ;
                if (blockZ == escapeZ)
                    return null;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                axis = Axis.Z;
            }
        }

        // Too far away
        return null;
    }
}
