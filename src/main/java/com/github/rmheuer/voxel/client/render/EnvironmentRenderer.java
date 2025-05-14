package com.github.rmheuer.voxel.client.render;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.render.Colors;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.ActivePipeline;
import com.github.rmheuer.azalea.render.pipeline.PipelineInfo;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.texture.Bitmap;
import com.github.rmheuer.azalea.render.texture.ColorFormat;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.block.LiquidShape;
import org.joml.Matrix4f;

import java.io.IOException;

/**
 * Renders the environment outside the level (the sky and surrounding ocean).
 */
public final class EnvironmentRenderer implements SafeCloseable {
    private static final int WATER_LEVEL = 32;
    private static final int WATER_DEPTH = 3;
    private static final int DISTANCE = 2048;

    private static final int SKY_COLOR = Colors.RGBA.fromFloats(0.5f, 0.8f, 1.0f);

    public static final VertexLayout VERTEX_LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.VEC2, // UV
            AttribType.FLOAT // Shade
    );

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final Texture2D bedrockTex, waterTex;
    private final Texture2D skyTex, cloudsTex;
    private final Mesh bedrockMesh, waterMesh;
    private final Mesh skyMesh, cloudsMesh;

    private int tickCount;

    /**
     * @param renderer renderer to create resources with
     */
    public EnvironmentRenderer(Renderer renderer) throws IOException {
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
        try (Bitmap img = new Bitmap(1, 1, ColorFormat.RGBA, SKY_COLOR)) {
            skyTex = renderer.createTexture2D(img);
        }
        cloudsTex = renderer.createTexture2D(ResourceUtil.readAsStream("clouds.png"));
        cloudsTex.setWrappingModes(Texture2D.WrappingMode.REPEAT);

        bedrockMesh = renderer.createMesh();
        waterMesh = renderer.createMesh();
        skyMesh = renderer.createMesh();
        cloudsMesh = renderer.createMesh();

        tickCount = 0;
    }

    /**
     * Updates the environment to properly contain a level of the specified
     * size. This must be called at least once before the environment is
     * rendered.
     *
     * @param blocksX size of the level along the X axis
     * @param blocksZ size of the level along the Z axis
     */
    public void updateLevelSize(int blocksX, int blocksZ) {
        try (MeshData data = createBedrockMesh(blocksX, blocksZ)) {
            bedrockMesh.setData(data, DataUsage.STATIC);
        }
        try (MeshData data = createWaterMesh(blocksX, blocksZ)) {
            waterMesh.setData(data, DataUsage.STATIC);
        }
        try (MeshData data = createSkyMesh()) {
            skyMesh.setData(data, DataUsage.STATIC);
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
    private void createTopSurface(MeshData data, int w, int d, float y, float lightShade) {
        int s = DISTANCE;

        float shade = lightShade * LightingConstants.SHADE_UP;
        putQuad(data, w, y, -s, w + s, y, -s, w + s, y, d, w, y, d, s, d + s, shade);
        putQuad(data, -s, y, 0, 0, y, 0, 0, y, d + s, -s, y, d + s, s, d + s, shade);
        putQuad(data, 0, y, d, w + s, y, d, w + s, y, d + s, 0, y, d + s, w + s, s, shade);
        putQuad(data, -s, y, -s, w, y, -s, w, y, 0, -s, y, 0, w + s, s, shade);
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

        createTopSurface(data, w, d, h, LightingConstants.SHADE_SHADOW);

        return data;
    }

    private MeshData createWaterMesh(int blocksX, int blocksZ) {
        MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES);
        createTopSurface(data, blocksX, blocksZ, WATER_LEVEL - 1 + LiquidShape.LIQUID_SURFACE_HEIGHT, LightingConstants.SHADE_LIT);
        return data;
    }

    private MeshData createSkyMesh() {
        MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES);

        int y = 74;
        data.putIndices(0, 1, 2, 0, 2, 3);
        data.putVec3(-2048, y, -2048); data.putVec2(0, 0); data.putFloat(1);
        data.putVec3(-2048, y, +2048); data.putVec2(0, 0); data.putFloat(1);
        data.putVec3(+2048, y, +2048); data.putVec2(0, 0); data.putFloat(1);
        data.putVec3(+2048, y, -2048); data.putVec2(0, 0); data.putFloat(1);

        return data;
    }

    private MeshData createCloudsMesh(float subtick) {
        MeshData data = new MeshData(VERTEX_LAYOUT, PrimitiveType.TRIANGLES);

        // FIXME: For some reason the clouds move slightly jittery, not sure why
        int y = 66;
        float scroll = ((tickCount + subtick) * 0.025f) % 4096;

        data.putIndices(0, 1, 2, 0, 2, 3);
        // move westward -> -x
        data.putVec3(-2048 - scroll, y, -2048); data.putVec2(0, 0); data.putFloat(1);
        data.putVec3(-2048 - scroll, y, +2048); data.putVec2(0, 1); data.putFloat(1);
        data.putVec3(+6144 - scroll, y, +2048); data.putVec2(2, 1); data.putFloat(1);
        data.putVec3(+6144 - scroll, y, -2048); data.putVec2(2, 0); data.putFloat(1);

        return data;
    }

    public void tick() {
        tickCount++;
    }

    /**
     * Renders the opaque layer of the environment.
     *
     * @param renderer renderer to render with
     * @param view camera view matrix
     * @param proj camera projection matrix
     * @param fogInfo information for distance fog
     * @param subtick percentage elapsed of the current tick
     */
    public void renderOpaqueLayer(Renderer renderer, Matrix4f view, Matrix4f proj, FogInfo fogInfo, float subtick) {
        renderLayer(bedrockTex, bedrockMesh, renderer, view, proj, fogInfo);
        renderLayer(skyTex, skyMesh, renderer, view, proj, fogInfo);

        try (MeshData cloudsData = createCloudsMesh(subtick)) {
            cloudsMesh.setData(cloudsData, DataUsage.STREAM);
        }
        renderLayer(cloudsTex, cloudsMesh, renderer, view, proj, fogInfo);
    }

    /**
     * Renders the translucent layer of the environment.
     *
     * @param renderer renderer to render with
     * @param view camera view matrix
     * @param proj camera projection matrix
     * @param fogInfo information for distance fog
     */
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
            pipe.getUniform("u_TintColor").setVec4(fogInfo.tintColor);

            pipe.bindTexture(0, tex);
            pipe.draw(mesh);
        }
    }

    @Override
    public void close() {
        bedrockMesh.close();
        waterMesh.close();
        skyMesh.close();
        cloudsMesh.close();
        bedrockTex.close();
        waterTex.close();
        cloudsTex.close();
        skyTex.close();
        shader.close();
    }
}
