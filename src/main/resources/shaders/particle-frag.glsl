#version 330 core

#define FOG_START 32.0
#define FOG_END 64.0

uniform sampler2D u_TextureAtlas;

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
    float fogAmount = clamp((fogDistance - FOG_START) / (FOG_END - FOG_START), 0.0, 1.0);

    vec4 fogColor = vec4(0.5, 0.8, 1.0, 1.0);
    o_Color = mix(color, fogColor, fogAmount);
}