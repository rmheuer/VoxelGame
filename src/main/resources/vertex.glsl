#version 330 core

uniform mat4 u_ViewProj;

layout(location = 0) in vec3 a_Position;
layout(location = 1) in float a_Shade;

out float v_Shade;

void main(void) {
    gl_Position = u_ViewProj * vec4(a_Position, 1.0);
    v_Shade = a_Shade;
}
