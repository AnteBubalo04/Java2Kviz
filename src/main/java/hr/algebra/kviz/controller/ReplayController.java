package hr.algebra.kviz.controller;

import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.ui.AppContext;
import hr.algebra.kviz.ui.ViewLifecycle;
import hr.algebra.kviz.xml.ReplayData;
import hr.algebra.kviz.xml.ReplayReader;
import hr.algebra.kviz.service.AppExecutors;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ponovni prikaz zadnje partije iz replay XML-a, potez po potez. */
public class ReplayController implements ViewLifecycle {

    @FXML
    private Label infoLabel;
    @FXML
    private Label questionNumberLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private VBox optionsBox;
    @FXML
    private ListView<String> movesList;
    @FXML
    private ProgressBar replayProgress;
    @FXML
    private Button playButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button restartButton;

    private final IntegerProperty step = new SimpleIntegerProperty(0);
    private final List<Runnable> steps = new ArrayList<>();
    private final Map<String, Label> optionLabels = new LinkedHashMap<>();
    private Timeline timeline;
    private ReplayData data;

    @FXML
    private void initialize() {
        playButton.setDisable(true);
        pauseButton.setDisable(true);
        restartButton.setDisable(true);
        infoLabel.setText("Učitavam replay...");

        Task<ReplayData> loadTask = new Task<>() {
            @Override
            protected ReplayData call() throws Exception {
                return new ReplayReader().read(AppContext.get().getReplayRecorder().getPath());
            }
        };
        loadTask.setOnSucceeded(event -> prepare(loadTask.getValue()));
        loadTask.setOnFailed(event -> infoLabel.setText(Dialogs.rootMessage(loadTask.getException())));
        AppExecutors.background().submit(loadTask);
    }

    private void prepare(ReplayData replay) {
        data = replay;
        infoLabel.setText("Zadnja partija: " + String.join(" vs. ", replay.players())
                + "  (" + replay.moves().size() + " poteza)");

        for (int i = 0; i < replay.questions().size(); i++) {
            int index = i;
            List<Answer> movesForQuestion = replay.moves().stream().filter(m -> m.questionIndex() == index).toList();
            if (movesForQuestion.isEmpty()) {
                continue;
            }
            steps.add(() -> showQuestion(index));
            movesForQuestion.forEach(move -> steps.add(() -> showMove(move)));
            steps.add(() -> revealCorrect(index));
        }
        if (steps.isEmpty()) {
            infoLabel.setText("Replay ne sadrži nijedan potez.");
            return;
        }

        int delay = AppContext.get().getConfig().getReplayDelayMillis();
        timeline = new Timeline(new KeyFrame(Duration.millis(delay), event -> nextStep()));
        timeline.setCycleCount(Animation.INDEFINITE);

        replayProgress.progressProperty().bind(Bindings.createDoubleBinding(
                () -> step.get() / (double) steps.size(), step));
        playButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> timeline.getStatus() == Animation.Status.RUNNING || step.get() >= steps.size(),
                timeline.statusProperty(), step));
        pauseButton.disableProperty().bind(timeline.statusProperty().isNotEqualTo(Animation.Status.RUNNING));
        restartButton.setDisable(false);

        nextStep();
        timeline.play();
    }

    private void nextStep() {
        if (step.get() >= steps.size()) {
            timeline.stop();
            infoLabel.setText("Kraj replaya.");
            return;
        }
        steps.get(step.get()).run();
        step.set(step.get() + 1);
    }

    private void showQuestion(int index) {
        Question question = data.questions().get(index);
        questionNumberLabel.setText("Pitanje " + (index + 1) + " / " + data.questions().size());
        questionLabel.setText(question.getText());
        optionsBox.getChildren().clear();
        optionLabels.clear();
        for (String option : question.getOptions()) {
            Label label = new Label(option);
            label.getStyleClass().add("replay-option");
            label.setMaxWidth(Double.MAX_VALUE);
            optionLabels.put(option, label);
            optionsBox.getChildren().add(label);
        }
        movesList.getItems().add("— Pitanje " + (index + 1) + ": " + question.getText());
    }

    private void showMove(Answer move) {
        if (move.timedOut()) {
            movesList.getItems().add("   " + move.playerName() + ": isteklo vrijeme");
        } else {
            Label label = optionLabels.get(move.selectedAnswer());
            if (label != null) {
                label.setText(label.getText() + "   ← " + move.playerName());
                label.getStyleClass().add("selected");
            }
            movesList.getItems().add("   " + move.playerName() + ": " + move.selectedAnswer()
                    + (move.correct() ? "  ✔ +" + move.points() : "  ✘"));
        }
        movesList.scrollTo(movesList.getItems().size() - 1);
    }

    private void revealCorrect(int index) {
        String correct = data.questions().get(index).getCorrectAnswer();
        optionLabels.forEach((option, label) -> label.getStyleClass().add(option.equals(correct) ? "correct" : "dim"));
    }

    @FXML
    private void onPlay() {
        if (timeline != null) {
            timeline.play();
        }
    }

    @FXML
    private void onPause() {
        if (timeline != null) {
            timeline.pause();
        }
    }

    @FXML
    private void onRestart() {
        if (timeline == null) {
            return;
        }
        timeline.stop();
        step.set(0);
        movesList.getItems().clear();
        infoLabel.setText("Zadnja partija: " + String.join(" vs. ", data.players()));
        nextStep();
        timeline.play();
    }

    @Override
    public void onLeave() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
