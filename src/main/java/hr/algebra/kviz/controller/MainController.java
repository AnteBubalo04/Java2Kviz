package hr.algebra.kviz.controller;

import hr.algebra.kviz.model.GameState;
import hr.algebra.kviz.service.SaveGameService;
import hr.algebra.kviz.ui.AppContext;
import hr.algebra.kviz.ui.View;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/** Glavni prozor: izbornik lijevo, trenutni ekran u sredini, statusna traka dolje. */
public class MainController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label playerLabel;
    @FXML
    private Label statusLabel;

    private AppContext context;

    @FXML
    private void initialize() {
        context = AppContext.get();
        context.initNavigation(contentArea);
        playerLabel.textProperty().bind(context.playerNameProperty());
        statusLabel.textProperty().bind(context.statusProperty());
        context.getNavigator().show(View.LOBBY);
    }

    private void navigate(View view) {
        if (context.getNavigator().confirmLeave()) {
            context.getNavigator().show(view);
        }
    }

    @FXML
    private void onNewGame() {
        navigate(View.LOBBY);
    }

    @FXML
    private void onLoadGame() {
        SaveGameService saves = context.getSaveGameService();
        if (!saves.hasSavedGame()) {
            Dialogs.info("Učitaj igru", "Još nema spremljene igre. Igru spremaš tipkom „Spremi” tijekom igre za jednog igrača.");
            return;
        }
        if (!context.getNavigator().confirmLeave()) {
            return;
        }
        context.setStatus("Učitavam spremljenu igru...");
        saves.loadAsync().whenComplete((state, error) -> Platform.runLater(() -> {
            if (error != null) {
                Dialogs.error("Učitaj igru", "Spremljenu igru nije moguće učitati: " + Dialogs.rootMessage(error));
                return;
            }
            resume(state);
        }));
    }

    private void resume(GameState state) {
        context.setStatus("Nastavljam igru spremljenu " + state.savedAt().toLocalDate() + " (" + state.playerName() + ")");
        context.getCoordinator().resume(state);
    }

    @FXML
    private void onReplay() {
        navigate(View.REPLAY);
    }

    @FXML
    private void onLeaderboard() {
        navigate(View.LEADERBOARD);
    }

    @FXML
    private void onSettings() {
        navigate(View.SETTINGS);
    }

    @FXML
    private void onDocumentation() {
        navigate(View.DOCUMENTATION);
    }

    @FXML
    private void onExit() {
        if (context.getNavigator().confirmLeave()) {
            Platform.exit();
        }
    }
}
