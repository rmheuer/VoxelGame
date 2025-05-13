package com.github.rmheuer.voxel.level;

/**
 * Type of occlusion for one face of a block.
 */
public enum OcclusionType {
    /** The face does not occlude its neighbor at all. */
    NONE,
    /**
     * The face occludes only the bottom half of the neighboring face. This is
     * only valid for horizontal faces.
     */
    HALF,
    /** The face fully occludes its neighbor. */
    FULL
}
