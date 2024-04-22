package dev.emortal;

public enum PlayerDisplayPart {
    HEAD(0, 1),
    RIGHT_ARM(-1024, 2),
    LEFT_ARM(-2048, 3),
    TORSO(-3072, 4),
    RIGHT_LEG(-4096, 5),
    LEFT_LEG(-5120, 6);

    private final double yTranslation;
    private final int customModelData;
    PlayerDisplayPart(double yTranslation, int customModelData) {
        this.yTranslation = yTranslation;
        this.customModelData = customModelData;
    }

    public double getYTranslation() {
        return yTranslation;
    }

    public int getCustomModelData() {
        return customModelData;
    }
}
