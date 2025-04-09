package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.DataUsage;
import com.github.rmheuer.azalea.render.mesh.VertexBuffer;
import com.github.rmheuer.azalea.render.mesh.VertexData;
import com.github.rmheuer.azalea.utils.SafeCloseable;

public final class SectionRenderLayer implements SafeCloseable {
    private VertexBuffer vertexBuffer;
    private int elementCount;

    public SectionRenderLayer() {
        vertexBuffer = null;
    }

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

    public VertexBuffer getVertexBuffer() {
        return vertexBuffer;
    }

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
