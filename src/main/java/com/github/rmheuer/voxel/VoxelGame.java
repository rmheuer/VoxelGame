package com.github.rmheuer.voxel;

import com.github.rmheuer.azalea.imgui.ImGuiBackend;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.WindowSettings;
import com.github.rmheuer.azalea.render.camera.Camera;
import com.github.rmheuer.azalea.render.camera.PerspectiveProjection;
import com.github.rmheuer.azalea.runtime.BaseGame;
import com.github.rmheuer.voxel.level.Blocks;
import com.github.rmheuer.voxel.level.Level;
import com.github.rmheuer.voxel.render.LevelRenderData;
import com.github.rmheuer.voxel.render.LevelRenderer;
import imgui.ImGui;
import org.joml.Matrix4f;
import org.joml.Vector2i;

import java.io.IOException;

public final class VoxelGame extends BaseGame {
    private static final WindowSettings WINDOW_SETTINGS =
            new WindowSettings(640, 480, "Voxel")
                .setVSync(false);

    private final ImGuiBackend imGuiBackend;
    private final LevelRenderer levelRenderer;

    private final Camera camera;
    private final Level level;
    private final LevelRenderData levelRenderData;

    public VoxelGame() throws IOException {
        super(WINDOW_SETTINGS);

        imGuiBackend = new ImGuiBackend(getWindow(), getEventBus());
        levelRenderer = new LevelRenderer(getRenderer());

        camera = new Camera(new PerspectiveProjection((float) Math.toRadians(90), 0.01f, 1000f));

        level = new Level(4, 4, 4);
        levelRenderData = new LevelRenderData(4, 4, 4);

        for (int y = 0; y < 24; y++) {
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    level.setBlockId(x, y, z, Blocks.ID_STONE);
                }
            }
        }
    }

    @Override
    protected void tick(float v) {
        camera.getTransform().position.set(16, 32, 16);
        camera.getTransform().rotation.x = (float) Math.toRadians(60);
    }

    @Override
    protected void render(Renderer renderer) {
        Vector2i windowSize = getWindow().getFramebufferSize();
        Matrix4f proj = camera.getProjectionMatrix(windowSize.x, windowSize.y);
        Matrix4f view = camera.getViewMatrix();
        Matrix4f viewProj = new Matrix4f(proj).mul(view);

        levelRenderer.renderLevel(renderer, level, levelRenderData, viewProj);

        imGuiBackend.beginFrame();
        ImGui.showAboutWindow();
        imGuiBackend.endFrameAndRender();
    }

    @Override
    protected void cleanUp() {
        levelRenderData.close();
        levelRenderer.close();
        imGuiBackend.close();
    }

    public static void main(String[] args) {
        VoxelGame game;
        try {
            game = new VoxelGame();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resources", e);
        }

        game.run();
    }
}
