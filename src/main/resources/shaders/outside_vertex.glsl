#version 330 core

uniform mat4 u_View;
uniform mat4 u_Proj;

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_UV;
layout(location = 2) in float a_Shade;

out vec2 v_UV;
out float v_Shade;
out vec3 v_ViewPos;

void main(void) {
    vec4 viewPos = u_View * vec4(a_Position, 1.0);
    gl_Position = u_Proj * viewPos;

    v_UV = a_UV;
    v_Shade = a_Shade;
    v_ViewPos = viewPos.xyz;
}
