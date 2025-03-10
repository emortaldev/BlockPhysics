package dev.emortal;

public enum PlayerDisplayPart {
    HEAD(0, "animated_java:blueprint/player_display/head"),
    RIGHT_ARM(-1024, "animated_java:blueprint/player_display/right_arm"),
    LEFT_ARM(-2048, "animated_java:blueprint/player_display/left_arm"),
    TORSO(-3072, "animated_java:blueprint/player_display/torso"),
    RIGHT_LEG(-4096, "animated_java:blueprint/player_display/right_leg"),
    LEFT_LEG(-5120, "animated_java:blueprint/player_display/left_leg");

    private final double yTranslation;
    private final String customModelData;
    PlayerDisplayPart(double yTranslation, String customModelData) {
        this.yTranslation = yTranslation;
        this.customModelData = customModelData;
    }

    public double getYTranslation() {
        return yTranslation;
    }

    public String getCustomModelData() {
        return customModelData;
    }
}
