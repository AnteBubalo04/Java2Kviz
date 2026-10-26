package hr.algebra.kviz.controller;

import hr.algebra.kviz.engine.GameEngine;
import hr.algebra.kviz.engine.GamePhase;
import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.ChatMessage;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.rmi.ChatService;
import hr.algebra.kviz.service.AppExecutors;
import hr.algebra.kviz.service.MultiplayerSession;
import hr.algebra.kviz.ui.AppContext;
import hr.algebra.kviz.ui.ViewLifecycle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalTime;
import java.util.List;

/**
 * Ekran igre. Kontroler ne sadrzi pravila igre: sve vrijednosti na ekranu
 * vezane su bindingom na propertyje GameEngine-a, a klik na odgovor samo se
 * prosljeduje engineu.
 */
public class GameController implements ViewLifecycle {

    private static final String[] RESULT_CLASSES = {"correct", "dim", "selected"};

    @FXML
    private Label questionNumberLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private ProgressBar timerBar;
    @FXML
    private Button answerButton1;
    @FXML
    private Button answerButton2;
    @FXML
    private Button answerButton3;
    @FXML
    private Button answerButton4;
    @FXML
    private Label localNameLabel;
    @FXML
    private Label localScoreLabel;
    @FXML
    private VBox opponentBox;
    @FXML
    private Label opponentNameLabel;
    @FXML
    private Label opponentScoreLabel;
    @FXML
    private Label opponentStatusLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button pauseButton;
    @FXML
    private Button resumeButton;
    @FXML
    private Button saveButton;
    @FXML
    private Node pauseOverlay;
    @FXML
    private VBox chatPane;
    @FXML
    private ListView<ChatMessage> chatList;
    @FXML
    private TextField chatField;
    @FXML
    private Button sendButton;

    private AppContext context;
    private GameEngine engine;
    private List<Button> answerButtons;
    private ScheduledService<List<ChatMessage>> chatRefresh;

    @FXML
    private void initialize() {
        context = AppContext.get();
        engine = context.getCurrentEngine();
        answerButtons = List.of(answerButton1, answerButton2, answerButton3, answerButton4);

        bindQuestion();
        bindPlayers();
        bindControls();

        engine.currentQuestionProperty().addListener((observable, oldQuestion, question) -> showOptions(question));
        engine.phaseProperty().addListener((observable, oldPhase, phase) -> {
            if (phase == GamePhase.SHOWING_RESULT) {
                revealCorrectAnswer();
            }
        });
        engine.localAnswerProperty().addListener((observable, oldAnswer, answer) -> markSelected(answer));
        showOptions(engine.currentQuestionProperty().get());

        if (engine.getMode().isMultiplayer()) {
            startChat();
        }
    }

    private void bindQuestion() {
        questionNumberLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Pitanje " + (engine.questionIndexProperty().get() + 1) + " / " + engine.totalQuestionsProperty().get(),
                engine.questionIndexProperty(), engine.totalQuestionsProperty()));
        questionLabel.textProperty().bind(Bindings.createStringBinding(
                () -> textOf(engine.currentQuestionProperty().get()), engine.currentQuestionProperty()));
        categoryLabel.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    Question question = engine.currentQuestionProperty().get();
                    return question == null ? "" : question.getCategory().toUpperCase();
                }, engine.currentQuestionProperty()));
        timerLabel.textProperty().bind(engine.secondsLeftProperty().asString());
        timerBar.progressProperty().bind(Bindings.createDoubleBinding(
                () -> engine.secondsLeftProperty().get() / (double) Math.max(1, engine.secondsPerQuestionProperty().get()),
                engine.secondsLeftProperty(), engine.secondsPerQuestionProperty()));
        statusLabel.textProperty().bind(engine.statusProperty());
    }

    private void bindPlayers() {
        localNameLabel.setText(engine.getLocalPlayer().getName());
        localScoreLabel.textProperty().bind(engine.localScoreProperty().asString());

        boolean multiplayer = engine.getMode().isMultiplayer();
        opponentBox.setVisible(multiplayer);
        opponentBox.setManaged(multiplayer);
        chatPane.setVisible(multiplayer);
        chatPane.setManaged(multiplayer);
        if (multiplayer) {
            opponentNameLabel.setText(engine.getOpponent().getName());
            opponentScoreLabel.textProperty().bind(engine.opponentScoreProperty().asString());
            opponentStatusLabel.textProperty().bind(Bindings.when(engine.opponentAnsweredProperty())
                    .then("odgovorio ✔").otherwise("razmišlja..."));
        }
    }

    private void bindControls() {
        BooleanBinding notAnswering = engine.phaseProperty().isNotEqualTo(GamePhase.ANSWERING);
        answerButtons.forEach(button -> button.disableProperty().bind(notAnswering));

        boolean singleplayer = !engine.getMode().isMultiplayer();
        for (Button button : List.of(pauseButton, resumeButton, saveButton)) {
            button.setVisible(singleplayer);
            button.setManaged(singleplayer);
        }
        pauseButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !engine.canPause(), engine.phaseProperty()));
        resumeButton.disableProperty().bind(engine.phaseProperty().isNotEqualTo(GamePhase.PAUSED));
        saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !engine.canSave(), engine.phaseProperty()));
        pauseOverlay.visibleProperty().bind(engine.phaseProperty().isEqualTo(GamePhase.PAUSED));
    }

    // ------------------------------------------------------------------ odgovori

    private void showOptions(Question question) {
        List<String> options = question == null ? List.of() : question.getOptions();
        for (int i = 0; i < answerButtons.size(); i++) {
            Button button = answerButtons.get(i);
            button.getStyleClass().removeAll(RESULT_CLASSES);
            boolean used = i < options.size();
            button.setVisible(used);
            button.setManaged(used);
            button.setText(used ? options.get(i) : "");
        }
    }

    private void markSelected(Answer answer) {
        if (answer == null || answer.selectedAnswer() == null) {
            return;
        }
        answerButtons.stream()
                .filter(button -> button.getText().equals(answer.selectedAnswer()))
                .forEach(button -> button.getStyleClass().add("selected"));
    }

    private void revealCorrectAnswer() {
        Question question = engine.currentQuestionProperty().get();
        for (Button button : answerButtons) {
            if (button.getText().equals(question.getCorrectAnswer())) {
                button.getStyleClass().add("correct");
            } else {
                button.getStyleClass().add("dim");
            }
        }
    }

    @FXML
    private void onAnswer(ActionEvent event) {
        Button button = (Button) event.getSource();
        engine.submitAnswer(button.getText());
    }

    // ------------------------------------------------------------------ pauza i spremanje

    @FXML
    private void onPause() {
        engine.pause();
    }

    @FXML
    private void onResume() {
        engine.resume();
    }

    @FXML
    private void onSave() {
        context.getSaveGameService().saveAsync(engine.snapshot())
                .whenComplete((path, error) -> Platform.runLater(() -> context.setStatus(error == null
                        ? "Igra je spremljena u " + path.toAbsolutePath()
                        : "Spremanje nije uspjelo: " + Dialogs.rootMessage(error))));
    }

    // ------------------------------------------------------------------ chat (RMI)

    private void startChat() {
        MultiplayerSession session = context.getCurrentSession();
        String gameId = session.getGameId();
        chatList.setPlaceholder(new Label("Pozdravi protivnika!"));

        chatRefresh = new ScheduledService<>() {
            @Override
            protected Task<List<ChatMessage>> createTask() {
                int alreadyShown = chatList.getItems().size();
                return new Task<>() {
                    @Override
                    protected List<ChatMessage> call() throws Exception {
                        return chatService().getMessages(gameId, alreadyShown);
                    }
                };
            }
        };
        chatRefresh.setPeriod(Duration.seconds(1));
        chatRefresh.setExecutor(AppExecutors.background());
        chatRefresh.setOnSucceeded(event -> {
            List<ChatMessage> messages = chatRefresh.getValue();
            if (!messages.isEmpty()) {
                chatList.getItems().addAll(messages);
                chatList.scrollTo(chatList.getItems().size() - 1);
            }
        });
        chatRefresh.start();

        sendButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> chatField.getText() == null || chatField.getText().isBlank(), chatField.textProperty()));
    }

    @FXML
    private void onSendChat() {
        String text = chatField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        chatField.clear();
        String gameId = context.getCurrentSession().getGameId();
        ChatMessage message = new ChatMessage(engine.getLocalPlayer().getName(), text.trim(), LocalTime.now());
        AppExecutors.background().submit(() -> {
            try {
                chatService().sendMessage(gameId, message);
            } catch (Exception e) {
                Platform.runLater(() -> context.setStatus("Poruka nije poslana: " + Dialogs.rootMessage(e)));
            }
        });
    }

    private ChatService chatService() throws Exception {
        return context.getRemoteServices().chat();
    }

    // ------------------------------------------------------------------ napustanje ekrana

    @Override
    public boolean confirmLeave() {
        if (engine.phaseProperty().get() == GamePhase.FINISHED) {
            return true;
        }
        boolean wasPaused = engine.phaseProperty().get() == GamePhase.PAUSED;
        engine.pause();
        boolean leave = Dialogs.confirm("Prekid igre", "Igra je u tijeku. Želiš li je prekinuti?"
                + (engine.canSave() || wasPaused ? "\n(U igri za jednog igrača možeš je prvo spremiti.)" : ""));
        if (!leave && !wasPaused) {
            engine.resume();
        }
        return leave;
    }

    @Override
    public void onLeave() {
        if (chatRefresh != null) {
            chatRefresh.cancel();
        }
        if (engine.phaseProperty().get() != GamePhase.FINISHED) {
            context.endCurrentGame();
        }
    }

    private static String textOf(Question question) {
        return question == null ? "" : question.getText();
    }
}
