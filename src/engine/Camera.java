package engine;

public class Camera {

    // -----------------------------------------
    // Perspective matrix
    // -----------------------------------------
    public float[] perspective(float fov, float aspect, float near, float far) {
        float f = (float)(1.0 / Math.tan(Math.toRadians(fov) / 2.0));
        float nf = 1.0f / (near - far);

        return new float[] {
            f / aspect, 0.0f, 0.0f, 0.0f,
            0.0f, f, 0.0f, 0.0f,
            0.0f, 0.0f, (far + near) * nf, -1.0f,
            0.0f, 0.0f, (2.0f * far * near) * nf, 0.0f
        };
    }

    // -----------------------------------------
    // View matrix from yaw/pitch + position
    // -----------------------------------------
    public float[] look(float px, float py, float pz, float yaw, float pitch) {
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);

        float sinY = (float)Math.sin(yawRad);
        float cosY = (float)Math.cos(yawRad);
        float sinP = (float)Math.sin(pitchRad);
        float cosP = (float)Math.cos(pitchRad);

        float[] forward = {
            sinY * cosP,
            sinP,
            -cosY * cosP
        };

        float[] right = {
            cosY,
            0.0f,
            sinY
        };

        float[] up = {
            -sinY * sinP,
            cosP,
            cosY * sinP
        };

        float[] matrix = new float[16];

        // OpenGL column-major view matrix for camera basis (right, up, forward)
        matrix[0] = right[0];
        matrix[1] = up[0];
        matrix[2] = -forward[0];
        matrix[3] = 0.0f;

        matrix[4] = right[1];
        matrix[5] = up[1];
        matrix[6] = -forward[1];
        matrix[7] = 0.0f;

        matrix[8] = right[2];
        matrix[9] = up[2];
        matrix[10] = -forward[2];
        matrix[11] = 0.0f;

        matrix[12] = -(right[0] * px + right[1] * py + right[2] * pz);
        matrix[13] = -(up[0] * px + up[1] * py + up[2] * pz);
        matrix[14] = forward[0] * px + forward[1] * py + forward[2] * pz;
        matrix[15] = 1.0f;

        return matrix;
    }
}
