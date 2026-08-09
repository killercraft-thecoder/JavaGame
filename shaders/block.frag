#version 330 core

in vec2 UV;
in float Light;   // NEW

uniform sampler2D tex;

out vec4 FragColor;

void main()
{
    vec4 c = texture(tex, UV);
    FragColor = c * Light;   // NEW
}
