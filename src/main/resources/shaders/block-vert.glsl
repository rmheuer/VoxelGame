#version 330 core

uniform mat4 u_ViewProj;
uniform vec3 u_SectionOffset;

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;
layout(location = 2) in float a_Shade;

out vec2 v_UV;
out vec4 v_Color;
out float v_Shade;

vec2 TEX_COORDS[4] = vec2[](
        vec2(1.0 / 16.0, 0.0),
        vec2(1.0 / 16.0, 1.0 / 16.0),
        vec2(2.0 / 16.0, 1.0 / 16.0),
        vec2(2.0 / 16.0, 0.0)
);

void main(void) {
    gl_Position = u_ViewProj * vec4(a_Position + u_SectionOffset, 1.0);
    v_Color = a_Color;
    v_Shade = a_Shade;
    v_UV = TEX_COORDS[int(mod(gl_VertexID, 4))];
}
