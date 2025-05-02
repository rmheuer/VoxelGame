package com.github.rmheuer.voxel.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.ActivePipeline;
import com.github.rmheuer.azalea.render.pipeline.PipelineInfo;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.LiquidShape;
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
    private final Mesh bedrockMesh, waterMesh;

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
        waterMesh = renderer.createMesh();
        try (MeshData data = createWaterMesh(blocksX, blocksZ)) {
            waterMesh.setData(data, DataUsage.STATIC);
        }
    }

    private void putQuad(MeshData data, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int width, int height, float shade) {
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(x1, y1, z1); data.putVec2(0, 0); data.putFloat(shade);
        data.putVec3(x2, y2, z2); data.putVec2(width, 0); data.putFloat(shade);
        data.putVec3(x3, y3, z3); data.putVec2(width, height); data.putFloat(shade);
        data.putVec3(x4, y4, z4); data.putVec2(0, height); data.putFloat(shade);
    }

    // Variable names here are short so that the code isn't significantly long

    // w: blocksX, d: blocksZ
    private void createTopSurface(MeshData data, int w, int d, float y) {
        int s = DISTANCE;

        putQuad(data, w, y, -s, w + s, y, -s, w + s, y, d, w, y, d, s, d + s, LightingConstants.SHADE_UP);
        putQuad(data, -s, y, 0, 0, y, 0, 0, y, d + s, -s, y, d + s, s, d + s, LightingConstants.SHADE_UP);
        putQuad(data, 0, y, d, w + s, y, d, w + s, y, d + s, 0, y, d + s, w + s, s, LightingConstants.SHADE_UP);
        putQuad(data, -s, y, -s, w, y, -s, w, y, 0, -s, y, 0, w + s, s, LightingConstants.SHADE_UP);
    }

    private MeshData createBedrockMesh(int w, int d) {
        MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES);

        // Bottom
        putQuad(data, 0, 0, 0, w, 0, 0, w, 0, d, 0, 0, d, w, d, LightingConstants.SHADE_UP);

        int h = WATER_LEVEL - WATER_DEPTH;

        // Sides
        putQuad(data, w, h, 0, w, h, d, w, 0, d, w, 0, 0, d, h, LightingConstants.SHADE_LEFT_RIGHT);
        putQuad(data, 0, h, d, 0, h, 0, 0, 0, 0, 0, 0, d, d, h, LightingConstants.SHADE_LEFT_RIGHT);
        putQuad(data, w, h, d, 0, h, d, 0, 0, d, w, 0, d, w, h, LightingConstants.SHADE_FRONT_BACK);
        putQuad(data, 0, h, 0, w, h, 0, w, 0, 0, 0, 0, 0, w, h, LightingConstants.SHADE_FRONT_BACK);

        createTopSurface(data, w, d, h);

        return data;
    }

    private MeshData createWaterMesh(int blocksX, int blocksZ) {
        MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES);
        createTopSurface(data, blocksX, blocksZ, WATER_LEVEL - 1 + LiquidShape.LIQUID_SURFACE_HEIGHT);
        return data;
    }

    public void renderOpaqueLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
        renderLayer(bedrockTex, bedrockMesh, renderer, view, proj, fogInfo);
    }

    public void renderTranslucentLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
        renderLayer(waterTex, waterMesh, renderer, view, proj, fogInfo);
    }

    private void renderLayer(Texture2D tex, Mesh mesh, Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo) {
        try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
            pipe.getUniform("u_View").setMat4(view);
            pipe.getUniform("u_Proj").setMat4(proj);
            pipe.getUniform("u_FogStart").setFloat(fogInfo.minDistance);
            pipe.getUniform("u_FogEnd").setFloat(fogInfo.maxDistance);
            pipe.getUniform("u_FogColor").setVec4(fogInfo.color);

            pipe.bindTexture(0, tex);
            pipe.draw(mesh);
        }
    }

    @Override
    public void close() {
        bedrockMesh.close();
        waterMesh.close();
        bedrockTex.close();
        waterTex.close();
        shader.close();
    }
}
