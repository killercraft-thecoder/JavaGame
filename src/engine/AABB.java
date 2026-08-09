package engine;

import world.World;
import world.Block;

public class AABB {

    public static final float HALF_WIDTH = 0.45f;
    public static final float HEIGHT = 2.0f;

    private final World world;

    public AABB(World world) {
        this.world = world;
    }

    // Check if the camera AABB intersects any solid block
    public boolean collides(float px, float py, float pz) {

        int minX = (int) Math.floor(px - HALF_WIDTH);
        int maxX = (int) Math.floor(px + HALF_WIDTH);

        int minY = (int) Math.floor(py);
        int maxY = (int) Math.floor(py + HEIGHT);

        int minZ = (int) Math.floor(pz - HALF_WIDTH);
        int maxZ = (int) Math.floor(pz + HALF_WIDTH);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (Block.isSolid(world.getBlock(x, y, z))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Resolve collisions and return corrected coordinates
    public float[] resolve(float px, float py, float pz,
            float oldX, float oldY, float oldZ) {

        float newX = px;
        float newY = py;
        float newZ = pz;

        // --- X AXIS ---
        if (collides(newX, oldY, oldZ)) {
            newX = oldX;
        }

        // --- Z AXIS ---
        if (collides(newX, oldY, newZ)) {
            newZ = oldZ;
        }

        // --- Y AXIS ---
        if (collides(newX, newY, newZ)) {
            newY = oldY;

            // If moving upward and inside block, push slightly up
            if (newY > oldY) {
                newY += 0.05f;
            }
        }

        // ---------------------------------------------------------
        // NEW: If STILL inside a block → shove upward until safe
        // ---------------------------------------------------------
        int safetyCounter = 0;
        while (collides(newX, newY, newZ) && safetyCounter < 8) {
            newY += 1.0f;   // shove upward one block
            safetyCounter++;
        }

        return new float[]{newX, newY, newZ};
    }

}
