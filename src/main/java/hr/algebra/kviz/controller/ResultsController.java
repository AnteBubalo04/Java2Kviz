package hr.algebra.kviz.controller;

import hr.algebra.kviz.engine.GameEngine;
import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.LocalPlayer;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.model.RemotePlayer;
import hr.algebra.kviz.ui.AppContext;
import hr.algebra.kviz.ui.View;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Rezultati zavrsene igre s pregledom odgovora po pitanju. */
public class ResultsController {

    public record ResultRow(int number, String question, String yourAnswer, String opponentAnswer, String correctAnswer) {
    }

    @FXML
    private Label winnerLabel;
    @FXML
    private Label localNameLabel;
    @FXML
    private Label localScoreLabel;
    @FXML
    private Label localCorrectLabel;
    @FXML
    private VBox opponentBox;
    @FXML
    private Label opponentNameLabel;
    @FXML
    private Label opponentScoreLabel;
    @FXML
    private Label opponentCorrectLabel;
    @FXML
    private TableView<ResultRow> detailsTable;
    @FXML
    private TableColumn<ResultRow, Integer> numberColumn;
    @FXML
    private TableColumn<ResultRow, String> questionColumn;
    @FXML
    private TableColumn<ResultRow, String> yourAnswerColumn;
    @FXML
    private TableColumn<ResultRow, String> opponentAnswerColumn;
    @FXML
    private TableColumn<ResultRow, String> correctColumn;

    private AppContext context;

    @FXML
    private void initialize() {
        context = AppContext.get();
        GameEngine engine = context.getCurrentEngine();
        LocalPlayer local = engine.getLocalPlayer();
        RemotePlayer opponent = engine.getOpponent();
        int total = engine.getQuestions().size();

        localNameLabel.setText(local.getName());
        localScoreLabel.setText(local.getScore() + " bodova");
        localCorrectLabel.setText(local.getCorrectAnswers() + " / " + total + " točnih");

        boolean multiplayer = opponent != null;
        opponentBox.setVisible(multiplayer);
        opponentBox.setManaged(multiplayer);
        opponentAnswerColumn.setVisible(multiplayer);
        if (multiplayer) {
            opponentNameLabel.setText(opponent.getName());
            opponentScoreLabel.setText(opponent.getScore() + " bodova");
            opponentCorrectLabel.setText(opponent.getCorrectAnswers() + " / " + total + " točnih");
            opponentAnswerColumn.setText(opponent.getName());
        }
        winnerLabel.setText(winnerText(local, opponent));

        numberColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().number()));
        questionColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().question()));
        yourAnswerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().yourAnswer()));
        opponentAnswerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().opponentAnswer()));
        correctColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().correctAnswer()));
        detailsTable.getItems().setAll(buildRows(engine));
    }

    private static String winnerText(LocalPlayer local, RemotePlayer opponent) {
        if (opponent == null) {
            return "Bravo, " + local.getName() + "!";
        }
        if (local.getScore() == opponent.getScore()) {
            return "Neriješeno!";
        }
        return (local.getScore() > opponent.getScore() ? local.getName() : opponent.getName()) + " pobjeđuje!";
    }

    private static List<ResultRow> buildRows(GameEngine engine) {
        List<Question> questions = engine.getQuestions();
        List<ResultRow> rows = new ArrayList<>();
        String localName = engine.getLocalPlayer().getName();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            int index = i;
            Optional<Answer> mine = engine.getAnswers().stream()
                    .filter(a -> a.questionIndex() == index && a.playerName().equals(localName)).findFirst();
            Optional<Answer> theirs = engine.getAnswers().stream()
                    .filter(a -> a.questionIndex() == index && !a.playerName().equals(localName)).findFirst();
            rows.add(new ResultRow(i + 1, question.getText(), describe(mine), describe(theirs), question.getCorrectAnswer()));
        }
        return rows;
    }

    private static String describe(Optional<Answer> answer) {
        return answer.map(a -> a.timedOut() ? "⏱ isteklo vrijeme" : (a.correct() ? "✔ " : "✘ ") + a.selectedAnswer())
                .orElse("—");
    }

    @FXML
    private void onNewGame() {
        context.getNavigator().show(View.LOBBY);
    }

    @FXML
    private void onReplay() {
        context.getNavigator().show(View.REPLAY);
    }

    @FXML
    private void onLeaderboard() {
        context.getNavigator().show(View.LEADERBOARD);
    }
}
