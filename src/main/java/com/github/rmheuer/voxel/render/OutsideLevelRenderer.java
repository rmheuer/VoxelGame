package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.ActivePipeline;
import com.github.rmheuer.azalea.render.pipeline.PipelineInfo;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import org.joml.Matrix4f;

import java.io.IOException;

public final class OutsideLevelRenderer implements SafeCloseable {
    private static final int WATER_LEVEL = 32;
    private static final int WATER_DEPTH = 3;
    private static final int DISTANCE = 100;

    public static final VertexLayout VERTEX_LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.VEC2, // UV
            AttribType.FLOAT // Shade
    );

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final Texture2D bedrockTex, waterTex;
    private final Mesh bedrockMesh;

    public OutsideLevelRenderer(Renderer renderer, int blocksX, int blocksZ) throws IOException {
        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("shaders/outside_vertex.glsl"),
                ResourceUtil.readAsStream("shaders/fragment.glsl")
        );
        pipeline = new PipelineInfo(shader)
                .setDepthTest(true);

        bedrockTex = renderer.createTexture2D(ResourceUtil.readAsStream("rock.png"));
        waterTex = renderer.createTexture2D(ResourceUtil.readAsStream("water.png"));
        bedrockTex.setWrappingModes(Texture2D.WrappingMode.REPEAT);
        waterTex.setWrappingModes(Texture2D.WrappingMode.REPEAT);

        bedrockMesh = renderer.createMesh();
        try (MeshData data = createBedrockMesh(blocksX, blocksZ)) {
            bedrockMesh.setData(data, DataUsage.STATIC);
        }
    }

    private void createTopSurface(MeshData data, int blocksX, int blocksZ, int y) {
        // +X side
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(blocksX, y, -DISTANCE); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(blocksX + DISTANCE, y, -DISTANCE); data.putVec2(DISTANCE, 0); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(blocksX + DISTANCE, y, blocksZ); data.putVec2(DISTANCE, blocksZ + DISTANCE); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(blocksX, y, blocksZ); data.putVec2(0, blocksZ + DISTANCE); data.putFloat(LightingConstants.SHADE_UP);

        // -X side
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(-DISTANCE, y, 0); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(0, y, 0); data.putVec2(DISTANCE, 0); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(0, y, blocksZ + DISTANCE); data.putVec2(DISTANCE, blocksZ + DISTANCE); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(-DISTANCE, y, blocksZ + DISTANCE); data.putVec2(0, blocksZ + DISTANCE); data.putFloat(LightingConstants.SHADE_UP);
    }

    private MeshData createBedrockMesh(int blocksX, int blocksZ) {
        MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES);

        // Bottom
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(0, 0, 0); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(blocksX, 0, 0); data.putVec2(blocksX, 0); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(blocksX, 0, blocksZ); data.putVec2(blocksX, blocksZ); data.putFloat(LightingConstants.SHADE_UP);
        data.putVec3(0, 0, blocksZ); data.putVec2(0, blocksZ); data.putFloat(LightingConstants.SHADE_UP);

        int height = WATER_LEVEL - WATER_DEPTH;

        // +X
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(blocksX, height, 0); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);
        data.putVec3(blocksX, height, blocksZ); data.putVec2(blocksZ, 0); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);
        data.putVec3(blocksX, 0, blocksZ); data.putVec2(blocksZ, height); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);
        data.putVec3(blocksX, 0, 0); data.putVec2(0, height); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);

        // -X
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(0, height, blocksZ); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);
        data.putVec3(0, height, 0); data.putVec2(blocksZ, 0); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);
        data.putVec3(0, 0, 0); data.putVec2(blocksZ, height); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);
        data.putVec3(0, 0, blocksZ); data.putVec2(0, height); data.putFloat(LightingConstants.SHADE_LEFT_RIGHT);

        // +Z
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(blocksX, height, blocksZ); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_FRONT_BACK);
        data.putVec3(0, height, blocksZ); data.putVec2(blocksZ, 0); data.putFloat(LightingConstants.SHADE_FRONT_BACK);
        data.putVec3(0, 0, blocksZ); data.putVec2(blocksZ, height); data.putFloat(LightingConstants.SHADE_FRONT_BACK);
        data.putVec3(blocksX, 0, blocksZ); data.putVec2(0, height); data.putFloat(LightingConstants.SHADE_FRONT_BACK);

        // -Z
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(0, height, 0); data.putVec2(0, 0); data.putFloat(LightingConstants.SHADE_FRONT_BACK);
        data.putVec3(blocksX, height, 0); data.putVec2(blocksZ, 0); data.putFloat(LightingConstants.SHADE_FRONT_BACK);
        data.putVec3(blocksX, 0, 0); data.putVec2(blocksZ, height); data.putFloat(LightingConstants.SHADE_FRONT_BACK);
        data.putVec3(0, 0, 0); data.putVec2(0, height); data.putFloat(LightingConstants.SHADE_FRONT_BACK);

        createTopSurface(data, blocksX, blocksZ, height);

        return data;
    }

    public void render(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
        try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
            pipe.getUniform("u_View").setMat4(view);
            pipe.getUniform("u_Proj").setMat4(proj);
            pipe.getUniform("u_FogStart").setFloat(fogInfo.minDistance);
            pipe.getUniform("u_FogEnd").setFloat(fogInfo.maxDistance);
            pipe.getUniform("u_FogColor").setVec4(fogInfo.color);

            pipe.bindTexture(0, bedrockTex);
            pipe.draw(bedrockMesh);
        }
    }

    @Override
    public void close() {
        bedrockMesh.close();
        bedrockTex.close();
        waterTex.close();
        shader.close();
    }
}
