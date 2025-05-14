package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.math.CubeFace;
import com.github.rmheuer.azalea.utils.SafeCloseable;

import java.util.Collections;
import java.util.List;

/**
 * Holds the data to render one section of the level.
 */
public final class SectionRenderData implements SafeCloseable {
    private final SectionRenderLayer opaque;
    private final SectionRenderLayer translucent;
    private List<BlockFace> translucentFaces;
    private SectionVisibility visibility;

    private boolean meshOutdated;

    public SectionRenderData() {
        opaque = new SectionRenderLayer();
        translucent = new SectionRenderLayer();
        translucentFaces = Collections.emptyList();
        visibility = null;

        meshOutdated = true;
    }

    /**
     * Marks this section to have its mesh regenerated.
     */
    public void markOutdated() {
        meshOutdated = true;
    }

    /**
     * Marks this section as no longer outdated.
     */
    public void clearOutdated() {
        meshOutdated = false;
    }

    /**
     * Gets whether the mesh is outdated and needs to be regenerated.
     *
     * @return whether the mesh is outdated
     */
    public boolean isMeshOutdated() {
        return meshOutdated;
    }
    
    public SectionRenderLayer getOpaqueLayer() {
        return opaque;
    }

    public SectionRenderLayer getTranslucentLayer() {
        return translucent;
    }

    public List<BlockFace> getTranslucentFaces() {
        return translucentFaces;
    }

    public void setTranslucentFaces(List<BlockFace> translucentFaces) {
        this.translucentFaces = translucentFaces;
    }

    /**
     * Marks this section to have its visibility recalculated.
     */
    public void markVisibilityOutdated() {
        visibility = null;
    }

    /**
     * @return whether the visibility needs to be recalculated
     */
    public boolean isVisibilityOutdated() {
        return visibility == null;
    }

    /**
     * Updates the visibility info for this section. This also marks it as no
     * longer outdated.
     *
     * @param visibility new visiblity info
     */
    public void updateVisibility(SectionVisibility visibility) {
        this.visibility = visibility;
    }

    public SectionVisibility getVisibility() {
        return visibility;
    }

    /**
     * Gets whether it is possible to see through this section through a pair of
     * faces.
     *
     * @param from face looking into
     * @param to face looking out of
     * @return whether it is possible to see between the faces
     */
    public boolean canSeeThrough(CubeFace from, CubeFace to) {
        // If visibility is not yet calculated, assume everything is visible
        if (visibility == null || from == to)
            return true;

        return visibility.isVisible(from, to);
    }

    @Override
    public void close() {
        opaque.close();
        translucent.close();
    }
}
