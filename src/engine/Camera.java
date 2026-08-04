package engine;

public class Camera {

    // -----------------------------------------
    // Perspective matrix
    // -----------------------------------------
    public float[] perspective(float fov, float aspect, float near, float far) {
        float f = (float)(1.0 / Math.tan(Math.toRadians(fov) / 2));

        return new float[]{
            f / aspect, 0, 0, 0,
            0, f, 0, 0,
            0, 0, (far + near) / (near - far), -1,
            0, 0, (2 * far * near) / (near - far), 0
        };
    }

    // -----------------------------------------
    // View matrix from yaw/pitch + position
    // -----------------------------------------
    public float[] look(float px, float py, float pz, float yaw, float pitch) {

        float cy = (float)Math.cos(Math.toRadians(yaw));
        float sy = (float)Math.sin(Math.toRadians(yaw));
        float cp = (float)Math.cos(Math.toRadians(pitch));
        float sp = (float)Math.sin(Math.toRadians(pitch));

        // forward vector
        float fx = sy * cp;
        float fy = sp;
        float fz = -cy * cp;

        // right vector
        float rx = cy;
        float ry = 0;
        float rz = sy;

        // up vector
        float ux = -sy * sp;
        float uy = cp;
        float uz = cy * sp;

        return new float[]{
            rx,  ux, -fx, 0,
            ry,  uy, -fy, 0,
            rz,  uz, -fz, 0,
            -(rx*px + ry*py + rz*pz),
            -(ux*px + uy*py + uz*pz),
            fx*px + fy*py + fz*pz,
            1
        };
    }
}
