package hr.algebra.kviz.ui;

/** Ekrani aplikacije i njihove FXML datoteke. */
public enum View {
    LOBBY("lobby.fxml"),
    GAME("game.fxml"),
    RESULTS("results.fxml"),
    LEADERBOARD("leaderboard.fxml"),
    REPLAY("replay.fxml"),
    SETTINGS("settings.fxml"),
    DOCUMENTATION("documentation.fxml");

    private final String fxml;

    View(String fxml) {
        this.fxml = fxml;
    }

    public String resource() {
        return "/hr/algebra/kviz/view/" + fxml;
    }
}
