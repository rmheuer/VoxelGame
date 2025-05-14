package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.DataUsage;
import com.github.rmheuer.azalea.render.mesh.VertexBuffer;
import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.utils.SafeCloseable;

/**
 * Holds the render data for one layer of one section of a level.
 */
public final class SectionRenderLayer implements SafeCloseable {
    private VertexBuffer vertexBuffer;
    private int elementCount;

    public SectionRenderLayer() {
        vertexBuffer = null;
    }

    /**
     * Updates the mesh for this layer.
     *
     * @param renderer renderer to use to update mesh
     * @param data new mesh data. If null, the mesh will be cleared.
     */
    public void updateMesh(Renderer renderer, VertexData data) {
        elementCount = data.getVertexCount() / 4 * 6;

        if (elementCount > 0) {
            if (vertexBuffer == null)
                vertexBuffer = renderer.createVertexBuffer();

            vertexBuffer.setData(data, DataUsage.DYNAMIC);
        } else {
            if (vertexBuffer != null) {
                vertexBuffer.close();
                vertexBuffer = null;
            }
        }
    }

    /**
     * Gets the vertex buffer containing the mesh data for this layer.
     *
     * @return vertex buffer
     */
    public VertexBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    /**
     * Gets the number of elements of the shared index buffer should be used to
     * render this layer. If elementCount is 0, the layer should not be
     * rendered.
     *
     * @return number of indices to use
     */
    public int getElementCount() {
        return elementCount;
    }

    @Override
    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
        }
    }
}
