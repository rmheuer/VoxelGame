#version 330 core

in float v_Shade;

layout(location = 0) out vec4 o_Color;

void main(void) {
    o_Color = vec4(v_Shade, v_Shade, v_Shade, 1.0);
}
