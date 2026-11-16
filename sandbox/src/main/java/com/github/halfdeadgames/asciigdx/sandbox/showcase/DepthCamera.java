package com.github.halfdeadgames.asciigdx.sandbox.showcase;

public final class DepthCamera {

    private final float hiddenDistance;
    private final float fadeDistance;
    private float cameraDepth;

    public DepthCamera(float hiddenDistance, float fadeDistance) {
        if (!(hiddenDistance > 0f)) {
            throw new IllegalArgumentException("hiddenDistance must be > 0: " + hiddenDistance);
        }
        if (!(fadeDistance > hiddenDistance)) {
            throw new IllegalArgumentException(
                    "fadeDistance must be > hiddenDistance: " + fadeDistance);
        }
        this.hiddenDistance = hiddenDistance;
        this.fadeDistance = fadeDistance;
    }

    public void setCameraDepth(float depth) {
        cameraDepth = depth;
    }

    public float cameraDepth() {
        return cameraDepth;
    }

    public float zoomFor(float layerDepth) {
        float distance = layerDepth - cameraDepth;
        if (distance <= hiddenDistance) {
            return 0f;
        }
        return 1f / distance;
    }

    public float alphaFor(float layerDepth) {
        float distance = layerDepth - cameraDepth;
        if (distance <= hiddenDistance) {
            return 0f;
        }
        if (distance >= fadeDistance) {
            return 1f;
        }
        return (distance - hiddenDistance) / (fadeDistance - hiddenDistance);
    }

    public boolean isVisible(float layerDepth) {
        return layerDepth - cameraDepth > hiddenDistance;
    }
}
