#version 330 core

layout(location = 0) in vec3 aPos;

uniform mat4 view;
uniform mat4 projection;

// block ID (tile index)
uniform int blockId;

// atlas info
uniform int atlasWidth;
uniform int atlasHeight;

out vec2 UV;

void main()
{
    // -----------------------------------------
    // Compute tile position in atlas
    // -----------------------------------------
    int tilesX = atlasWidth / 16;
    int tilesY = atlasHeight / 16;

    int tx = blockId % tilesX;
    int ty = blockId / tilesX;

    float u0 = float(tx) / float(tilesX);
    float v0 = float(ty) / float(tilesY);
    float u1 = float(tx + 1) / float(tilesX);
    float v1 = float(ty + 1) / float(tilesY);

    // -----------------------------------------
    // Determine face direction from vertex position
    // (Chunk gives only positions, so we infer face)
    // -----------------------------------------
    vec3 p = aPos;

    // X face
    if (abs(p.x - floor(p.x + 0.5)) > 0.49) {
        UV = vec2(
            (p.z - floor(p.z)) > 0.5 ? u1 : u0,
            (p.y - floor(p.y)) > 0.5 ? v1 : v0
        );
    }
    // Z face
    else if (abs(p.z - floor(p.z + 0.5)) > 0.49) {
        UV = vec2(
            (p.x - floor(p.x)) > 0.5 ? u1 : u0,
            (p.y - floor(p.y)) > 0.5 ? v1 : v0
        );
    }
    // Y face (top/bottom)
    else {
        UV = vec2(
            (p.x - floor(p.x)) > 0.5 ? u1 : u0,
            (p.z - floor(p.z)) > 0.5 ? v1 : v0
        );
    }

    gl_Position = projection * view * vec4(aPos, 1.0);
}
