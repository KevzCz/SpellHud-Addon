package net.pixeldreamstudios.spellhudaddon;

public class SpellHudState {
    public static boolean hasVisibleSpells = false;

    public static float rotationRad = 0f;
    public static float targetRotationRad = 0f;
    public static float tangentGap = 4f;
    public static int visibleGroup = 0;

    public static int anchorCorner = 2;

    public static float rotateLerp = 0.22f;

    public static float radius = 50f;

    public static float[] lastCooldowns = new float[9];
    public static boolean[] lastKeyDown = new boolean[9];

    public static void ensureCapacity(int count) {
        int n = Math.max(9, count);
        if (lastCooldowns.length != n) lastCooldowns = new float[n];
        if (lastKeyDown.length != n) lastKeyDown = new boolean[n];
    }

    public static void setAnchorCorner(int cornerIndex) {
        anchorCorner = cornerIndex;
        refreshTargetRotation();
    }

    public static void setVisibleGroup(int group) {
        int g = group <= 0 ? 0 : 1;
        if (visibleGroup != g) {
            visibleGroup = g;
            refreshTargetRotation();
        }
    }

    public static void refreshTargetRotation() {
        targetRotationRad = visibleGroup == 0 ? 0f : (float)Math.PI;
    }

    public static void tickRotation() {
        float diff = angleDiff(targetRotationRad, rotationRad);
        rotationRad += diff * rotateLerp;
    }

    private static float angleDiff(float target, float current) {
        float d = (float)Math.atan2(Math.sin(target - current), Math.cos(target - current));
        return d;
    }
}
