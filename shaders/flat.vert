// shaders/flat.vert
#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUV;    // present in mesh layout but unused here
layout(location = 2) in float aLight; // present but unused

uniform mat4 view;
uniform mat4 projection;

void main() {
    // Transform vertex to clip space
    gl_Position = projection * view * vec4(aPos, 1.0);
}
