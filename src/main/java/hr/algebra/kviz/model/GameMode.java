package hr.algebra.kviz.model;

public enum GameMode {
    SINGLEPLAYER,
    HOST,
    GUEST;

    public boolean isMultiplayer() {
        return this != SINGLEPLAYER;
    }
}
