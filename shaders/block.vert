#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in float aLight;  

uniform mat4 view;
uniform mat4 projection;

out vec2 UV;
out float Light;

void main()
{
    UV = aTexCoord;
    Light = aLight;   // pass to fragment shader

    gl_Position = projection * view * vec4(aPos, 1.0);
}
