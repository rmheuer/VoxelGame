#version 330 core

uniform sampler2D u_TextureAtlas;

in vec2 v_UV;
in float v_Shade;

layout(location = 0) out vec4 o_Color;

void main(void) {
    vec4 texColor = texture(u_TextureAtlas, v_UV);
    if (texColor.a < 0.1) {
        discard;
    }

    o_Color = texColor * vec4(v_Shade, v_Shade, v_Shade, 1.0);
}