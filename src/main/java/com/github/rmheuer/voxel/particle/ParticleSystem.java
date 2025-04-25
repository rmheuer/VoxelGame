package com.github.rmheuer.voxel.particle;

import com.github.rmheuer.azalea.io.ResourceUtil;
import com.github.rmheuer.azalea.math.AABB;
import com.github.rmheuer.azalea.math.Axis;
import com.github.rmheuer.azalea.render.Renderer;
import com.github.rmheuer.azalea.render.mesh.*;
import com.github.rmheuer.azalea.render.pipeline.ActivePipeline;
import com.github.rmheuer.azalea.render.pipeline.CullMode;
import com.github.rmheuer.azalea.render.pipeline.FaceWinding;
import com.github.rmheuer.azalea.render.pipeline.PipelineInfo;
import com.github.rmheuer.azalea.render.shader.ShaderProgram;
import com.github.rmheuer.azalea.render.texture.Texture2D;
import com.github.rmheuer.azalea.render.utils.SharedIndexBuffer;
import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.level.BlockMap;
import com.github.rmheuer.voxel.level.LightMap;
import com.github.rmheuer.voxel.render.AtlasSprite;
import com.github.rmheuer.voxel.render.LightingConstants;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class ParticleSystem implements SafeCloseable {
    private static final VertexLayout LAYOUT = new VertexLayout(
            AttribType.VEC3, // Position
            AttribType.VEC2, // Color
            AttribType.FLOAT // Shade
    );

    private static final int BREAK_PARTICLES_PER_AXIS = 4;

    private static final float GRAVITY = 0.04f;
    private static final float AIR_RESISTANCE = 0.98f;
    private static final float FRICTION = 0.7f;

    private static final class Particle {
        public final float size;
        public final Vector2f uv1, uv2;

        public final Vector3f position;
        public final Vector3f prevPosition;
        private final Vector3f velocity;

        private int ticksLeft;

        public Particle(float size, Vector2f uv1, Vector2f uv2, Vector3f position, Vector3f velocity, int lifeTime) {
            this.size = size;
            this.uv1 = uv1;
            this.uv2 = uv2;
            this.position = position;
            this.velocity = velocity;

            prevPosition = new Vector3f(position);
            ticksLeft = lifeTime;
        }

        public void tick(BlockMap map) {
            prevPosition.set(position);

            velocity.y -= GRAVITY;

            AABB box = AABB.fromCenterSize(position.x, position.y, position.z, 0.2f, 0.2f, 0.2f);
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

            if (moveX != velocity.x)
                velocity.x = 0;
            if (moveZ != velocity.z)
                velocity.z = 0;

            boolean onGround = false;
            if (moveY != velocity.y) {
                if (velocity.y < 0)
                    onGround = true;
                velocity.y = 0;
            }

            position.add(moveX, moveY, moveZ);
            velocity.mul(AIR_RESISTANCE);
            if (onGround) {
                velocity.x *= FRICTION;
                velocity.z *= FRICTION;
            }

            ticksLeft--;
        }

        public boolean shouldRemove() {
            return ticksLeft <= 0;
        }
    }

    private final Texture2D atlasTexture;

    private final ShaderProgram shader;
    private final PipelineInfo pipeline;
    private final VertexBuffer vertexBuffer;
    private final SharedIndexBuffer indexBuffer;

    private final List<Particle> particles;
    private final Random random;

    public ParticleSystem(Renderer renderer, Texture2D atlasTexture) throws IOException {
        this.atlasTexture = atlasTexture;

        shader = renderer.createShaderProgram(
                ResourceUtil.readAsStream("shaders/particle-vert.glsl"),
                ResourceUtil.readAsStream("shaders/particle-frag.glsl")
        );
        pipeline = new PipelineInfo(shader)
                .setWinding(FaceWinding.CCW_FRONT)
                .setCullMode(CullMode.BACK)
                .setBlend(true)
                .setDepthTest(true);

        vertexBuffer = renderer.createVertexBuffer();
        indexBuffer = new SharedIndexBuffer(renderer, PrimitiveType.TRIANGLES, 4, 0, 1, 2, 0, 2, 3);

        particles = new ArrayList<>();
        random = new Random();
    }

    private float particlePos(int index) {
        return (index + 0.5f) / BREAK_PARTICLES_PER_AXIS;
    }

    private float velocityRand() {
        return (random.nextFloat() * 2 - 1) * 0.4f;
    }

    public void spawnBreakParticles(int blockX, int blockY, int blockZ, AtlasSprite sprite) {
        for (int i = 0; i < BREAK_PARTICLES_PER_AXIS; i++) {
            for (int j = 0; j < BREAK_PARTICLES_PER_AXIS; j++) {
                for (int k = 0; k < BREAK_PARTICLES_PER_AXIS; k++) {
                    Vector3f pos = new Vector3f(blockX + particlePos(i), blockY + particlePos(j), blockZ + particlePos(k));
                    Vector3f vel = new Vector3f(pos)
                            .sub(blockX + 0.5f, blockY + 0.5f, blockZ + 0.5f)
                            .add(velocityRand(), velocityRand(), velocityRand())
                            .normalize()
                            .mul((random.nextFloat() + random.nextFloat() + 1) * 0.06f);
                    vel.y += 0.1f;

                    float size = 0.2f * (random.nextFloat() * 0.5f + 0.5f);
                    int lifeTime = (int) (4 / (random.nextFloat() * 0.9f + 0.1f));

                    float texOffsetU = (1 - size) * random.nextFloat();
                    float texOffsetV = (1 - size) * random.nextFloat();
                    AtlasSprite part = sprite.getSection(
                            texOffsetU, texOffsetV,
                            texOffsetU + size, texOffsetV + size
                    );

                    Vector2f uv1 = new Vector2f(part.u1, part.v1);
                    Vector2f uv2 = new Vector2f(part.u2, part.v2);

                    particles.add(new Particle(size, uv1, uv2, pos, vel, lifeTime));
                }
            }
        }
    }

    public void tickParticles(BlockMap map) {
        for (Iterator<Particle> iter = particles.iterator(); iter.hasNext(); ) {
            Particle particle = iter.next();
            if (particle.shouldRemove()) {
                iter.remove();
            } else {
                particle.tick(map);
            }
        }
    }

    public void renderParticles(Renderer renderer, Matrix4f view, Matrix4f proj, float subtick, LightMap lightMap) {
        if (particles.isEmpty())
            return;

        Matrix4f viewInv = new Matrix4f(view).invert();
        Vector3f right = viewInv.getColumn(0, new Vector3f());
        Vector3f up = viewInv.getColumn(1, new Vector3f());

        try (VertexData data = new VertexData(LAYOUT)) {
            for (Particle particle : particles) {
                Vector3f pos = new Vector3f(particle.prevPosition).lerp(particle.position, subtick);

                float halfSz = particle.size / 2;
                Vector3f v1 = new Vector3f(pos).fma(-halfSz, right).fma(halfSz, up);
                Vector3f v2 = new Vector3f(pos).fma(-halfSz, right).fma(-halfSz, up);
                Vector3f v3 = new Vector3f(pos).fma(halfSz, right).fma(-halfSz, up);
                Vector3f v4 = new Vector3f(pos).fma(halfSz, right).fma(halfSz, up);

                int blockX = (int) Math.floor(pos.x);
                int blockY = (int) Math.floor(pos.y);
                int blockZ = (int) Math.floor(pos.z);

                float shade = !lightMap.isInBounds(blockX, blockZ) || lightMap.isLit(blockX, blockY, blockZ)
                        ? LightingConstants.SHADE_LIT
                        : LightingConstants.SHADE_SHADOW;
                shade *= 0.6f;

                data.putVec3(v1); data.putVec2(particle.uv1); data.putFloat(shade);
                data.putVec3(v2); data.putVec2(particle.uv1.x, particle.uv2.y); data.putFloat(shade);
                data.putVec3(v3); data.putVec2(particle.uv2); data.putFloat(shade);
                data.putVec3(v4); data.putVec2(particle.uv2.x, particle.uv1.y); data.putFloat(shade);
            }

            vertexBuffer.setData(data, DataUsage.STREAM);
        }
        indexBuffer.ensureCapacity(particles.size());

        Matrix4f viewProj = new Matrix4f(proj).mul(view);
        try (ActivePipeline pipe = renderer.bindPipeline(pipeline)) {
            pipe.bindTexture(0, atlasTexture);
            pipe.getUniform("u_ViewProj").setMat4(viewProj);
            pipe.draw(vertexBuffer, indexBuffer.getIndexBuffer(), 0, particles.size() * 6);
        }
    }

    @Override
    public void close() {
        shader.close();
        vertexBuffer.close();
        indexBuffer.close();
    }
}
