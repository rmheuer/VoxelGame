#version 330 core

uniform sampler2D u_TextureAtlas;

uniform float u_FogStart;
uniform float u_FogEnd;
uniform vec4 u_FogColor;

in vec2 v_UV;
in float v_Shade;
in vec3 v_ViewPos;

layout(location = 0) out vec4 o_Color;

void main(void) {
    vec4 color = texture(u_TextureAtlas, v_UV);
    if (color.a < 0.1) {
        discard;
    }

    color.xyz *= v_Shade;

    float fogDistance = length(v_ViewPos);
    float fogAmount = clamp((fogDistance - u_FogStart) / (u_FogEnd - u_FogStart), 0.0, 1.0);
    o_Color = mix(color, u_FogColor, fogAmount);
}