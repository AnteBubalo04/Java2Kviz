package hr.algebra.kviz.controller;

import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.LeaderboardEntry;
import hr.algebra.kviz.service.LeaderboardService;
import hr.algebra.kviz.ui.AppContext;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;

/** Ljestvica najboljih rezultata; tablica je vezana na ObservableList servisa. */
public class LeaderboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm");

    @FXML
    private TableView<LeaderboardEntry> leaderboardTable;
    @FXML
    private TableColumn<LeaderboardEntry, Void> rankColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> playerColumn;
    @FXML
    private TableColumn<LeaderboardEntry, Integer> scoreColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> correctColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> modeColumn;
    @FXML
    private TableColumn<LeaderboardEntry, String> dateColumn;

    @FXML
    private void initialize() {
        LeaderboardService service = AppContext.get().getLeaderboardService();

        rankColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1) + ".");
            }
        });
        playerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().playerName()));
        scoreColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().score()));
        correctColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().correctAnswers() + " / " + c.getValue().totalQuestions()));
        modeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().mode() == GameMode.SINGLEPLAYER ? "Sam" : "Mrežna"));
        dateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().playedAt().format(DATE_FORMAT)));

        leaderboardTable.setPlaceholder(new Label("Još nema rezultata. Odigraj prvu igru!"));
        leaderboardTable.setItems(service.getEntries());
        service.refreshAsync();
    }
}
