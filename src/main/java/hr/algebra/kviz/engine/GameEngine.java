package hr.algebra.kviz.engine;

import hr.algebra.kviz.annotation.GameDoc;
import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.GameState;
import hr.algebra.kviz.model.LocalPlayer;
import hr.algebra.kviz.model.Player;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.model.RemotePlayer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Game engine: sva pravila igre na jednom mjestu, bez ovisnosti o ekranima.
 * <p>
 * Stanje igre izlozeno je kao JavaFX property, pa se sucelje na njega veze
 * bindingom i automatski osvjezava. Kontroleri samo prosljeduju akcije
 * igraca (submitAnswer, pause, resume), a engine javlja dogadaje
 * (potez, kraj igre) preko listenera.
 * <p>
 * Engine se koristi iskljucivo iz JavaFX dretve; odbrojavanje radi u
 * pozadinskoj dretvi, a svaki otkucaj vraca se u JavaFX dretvu preko uiExecutor-a.
 */
@GameDoc("Game engine: tijek pitanja, odbrojavanje, bodovanje i sinkronizacija poteza dvaju igraca.")
public class GameEngine {

    /** Koliko dugo se prikazuje tocan odgovor prije sljedeceg pitanja. */
    public static final long RESULT_DISPLAY_MILLIS = 2000;

    private final Executor uiExecutor;
    private final ScheduledExecutorService scheduler;

    private final ObjectProperty<Question> currentQuestion = new SimpleObjectProperty<>();
    private final IntegerProperty questionIndex = new SimpleIntegerProperty(0);
    private final IntegerProperty totalQuestions = new SimpleIntegerProperty(0);
    private final IntegerProperty secondsLeft = new SimpleIntegerProperty(0);
    private final IntegerProperty secondsPerQuestion = new SimpleIntegerProperty(15);
    private final IntegerProperty localScore = new SimpleIntegerProperty(0);
    private final IntegerProperty opponentScore = new SimpleIntegerProperty(0);
    private final ObjectProperty<GamePhase> phase = new SimpleObjectProperty<>(GamePhase.NOT_STARTED);
    private final BooleanProperty opponentAnswered = new SimpleBooleanProperty(false);
    private final ObjectProperty<Answer> localAnswer = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty("");
    private final ObservableList<Answer> answers = FXCollections.observableArrayList();

    private final Map<Integer, Answer> remoteAnswers = new HashMap<>();
    private final List<Question> questions = new ArrayList<>();

    private GameMode mode = GameMode.SINGLEPLAYER;
    private LocalPlayer localPlayer;
    private RemotePlayer opponent;
    private boolean opponentLeft;
    private GamePhase phaseBeforePause;

    private ScheduledFuture<?> countdown;
    private ScheduledFuture<?> advance;
    /** Povecava se za svako pitanje, kako bi se zakasnjeli otkucaji starog pitanja ignorirali. */
    private int roundToken;

    private Consumer<Answer> onLocalAnswer = answer -> { };
    private Consumer<Answer> onAnyAnswer = answer -> { };
    private Runnable onFinished = () -> { };

    public GameEngine(Executor uiExecutor, ScheduledExecutorService scheduler) {
        this.uiExecutor = uiExecutor;
        this.scheduler = scheduler;
    }

    // ------------------------------------------------------------------ pokretanje

    public void start(GameMode mode, String localName, String opponentName,
                      List<Question> gameQuestions, int seconds) {
        reset(mode, localName, opponentName, gameQuestions, seconds);
        showQuestion(0);
    }

    /** Nastavlja spremljenu singleplayer igru. */
    @GameDoc("Vraca spremljeno stanje: pitanja, bodove i odgovore te nastavlja od prvog neodgovorenog pitanja.")
    public void restore(GameState state) {
        reset(GameMode.SINGLEPLAYER, state.playerName(), null, state.questions(), state.secondsPerQuestion());
        localPlayer.restore(state.score(), state.correctAnswers());
        localScore.set(state.score());
        answers.setAll(state.answers());
        if (state.currentIndex() >= questions.size()) {
            finish();
        } else {
            showQuestion(state.currentIndex());
        }
    }

    private void reset(GameMode newMode, String localName, String opponentName,
                       List<Question> gameQuestions, int seconds) {
        if (gameQuestions == null || gameQuestions.isEmpty()) {
            throw new IllegalArgumentException("Igra mora imati barem jedno pitanje");
        }
        cancelTimers();
        mode = newMode;
        localPlayer = new LocalPlayer(localName);
        opponent = newMode.isMultiplayer() ? new RemotePlayer(opponentName) : null;
        opponentLeft = false;
        questions.clear();
        questions.addAll(gameQuestions);
        remoteAnswers.clear();
        answers.clear();
        totalQuestions.set(questions.size());
        secondsPerQuestion.set(seconds);
        localScore.set(0);
        opponentScore.set(0);
    }

    // ------------------------------------------------------------------ tijek runde

    private void showQuestion(int index) {
        roundToken++;
        questionIndex.set(index);
        localAnswer.set(null);
        opponentAnswered.set(remoteAnswers.containsKey(index));
        secondsLeft.set(secondsPerQuestion.get());
        currentQuestion.set(questions.get(index));
        phase.set(GamePhase.ANSWERING);
        status.set("Odaberi odgovor!");
        startCountdown();
    }

    private void startCountdown() {
        cancelCountdown();
        int token = roundToken;
        countdown = scheduler.scheduleAtFixedRate(
                () -> uiExecutor.execute(() -> tick(token)), 1, 1, TimeUnit.SECONDS);
    }

    @GameDoc("Svake sekunde smanjuje preostalo vrijeme; kad istekne, igracu se biljezi odgovor bez bodova.")
    private void tick(int token) {
        if (token != roundToken || phase.get() != GamePhase.ANSWERING) {
            return;
        }
        secondsLeft.set(Math.max(0, secondsLeft.get() - 1));
        if (secondsLeft.get() == 0) {
            status.set("Vrijeme je isteklo!");
            registerLocalAnswer(null);
        }
    }

    /** Igrac je kliknuo odgovor. */
    public void submitAnswer(String option) {
        if (phase.get() != GamePhase.ANSWERING || option == null) {
            return;
        }
        registerLocalAnswer(option);
    }

    @GameDoc("Biljezi lokalni potez, racuna bodove i salje potez dalje (mreza, replay).")
    private void registerLocalAnswer(String option) {
        cancelCountdown();
        Question question = currentQuestion.get();
        boolean correct = question.isCorrect(option);
        int points = ScoreCalculator.points(correct, secondsLeft.get(), secondsPerQuestion.get());
        Answer answer = new Answer(questionIndex.get(), localPlayer.getName(), option, correct, points, secondsLeft.get());

        localPlayer.registerAnswer(answer);
        localScore.set(localPlayer.getScore());
        answers.add(answer);
        localAnswer.set(answer);

        onLocalAnswer.accept(answer);
        onAnyAnswer.accept(answer);

        if (waitsForOpponent()) {
            phase.set(GamePhase.WAITING_OPPONENT);
            status.set("Čekam protivnika...");
        }
        checkRoundComplete();
    }

    /** Potez protivnika primljen preko mreze (poziva se u JavaFX dretvi). */
    @GameDoc("Protivnikov potez moze stici prije ili poslije lokalnog; runda zavrsava kad odgovore oba igraca.")
    public void receiveRemoteAnswer(Answer answer) {
        if (opponent == null || phase.get() == GamePhase.FINISHED || remoteAnswers.containsKey(answer.questionIndex())) {
            return;
        }
        remoteAnswers.put(answer.questionIndex(), answer);
        opponent.registerAnswer(answer);
        opponentScore.set(opponent.getScore());
        answers.add(answer);
        onAnyAnswer.accept(answer);

        if (answer.questionIndex() == questionIndex.get()) {
            opponentAnswered.set(true);
            checkRoundComplete();
        }
    }

    public void opponentDisconnected() {
        if (opponent == null || phase.get() == GamePhase.FINISHED) {
            return;
        }
        opponentLeft = true;
        status.set(opponent.getName() + " je napustio igru. Nastavljaš sam.");
        checkRoundComplete();
    }

    private boolean waitsForOpponent() {
        return mode.isMultiplayer() && !opponentLeft && !remoteAnswers.containsKey(questionIndex.get());
    }

    private void checkRoundComplete() {
        if (localAnswer.get() == null || waitsForOpponent()) {
            return;
        }
        GamePhase current = phase.get();
        if (current == GamePhase.SHOWING_RESULT || current == GamePhase.FINISHED) {
            return;
        }
        phase.set(GamePhase.SHOWING_RESULT);
        Answer mine = localAnswer.get();
        status.set(mine.correct() ? "Točno! +" + mine.points() : (mine.timedOut() ? "Vrijeme je isteklo." : "Netočno."));

        int token = roundToken;
        advance = scheduler.schedule(() -> uiExecutor.execute(() -> nextQuestion(token)),
                RESULT_DISPLAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void nextQuestion(int token) {
        if (token != roundToken || phase.get() != GamePhase.SHOWING_RESULT) {
            return;
        }
        int next = questionIndex.get() + 1;
        if (next < questions.size()) {
            showQuestion(next);
        } else {
            finish();
        }
    }

    private void finish() {
        cancelTimers();
        phase.set(GamePhase.FINISHED);
        status.set("Kraj igre!");
        onFinished.run();
    }

    // ------------------------------------------------------------------ pauza i spremanje

    public boolean canPause() {
        return mode == GameMode.SINGLEPLAYER
                && (phase.get() == GamePhase.ANSWERING || phase.get() == GamePhase.SHOWING_RESULT);
    }

    @GameDoc("Pauza je moguca samo u igri za jednog igraca jer u mreznoj igri vrijeme tece za oba igraca.")
    public void pause() {
        if (!canPause()) {
            return;
        }
        cancelTimers();
        phaseBeforePause = phase.get();
        phase.set(GamePhase.PAUSED);
        status.set("Pauza");
    }

    public void resume() {
        if (phase.get() != GamePhase.PAUSED) {
            return;
        }
        if (phaseBeforePause == GamePhase.ANSWERING) {
            phase.set(GamePhase.ANSWERING);
            status.set("Odaberi odgovor!");
            startCountdown();
        } else {
            // odgovor je vec dan, nastavljamo na sljedece pitanje
            phase.set(GamePhase.SHOWING_RESULT);
            nextQuestion(roundToken);
        }
    }

    public boolean canSave() {
        return mode == GameMode.SINGLEPLAYER && phase.get() != GamePhase.NOT_STARTED && phase.get() != GamePhase.FINISHED;
    }

    /** Snimka stanja za serijalizaciju. Ako je trenutno pitanje odgovoreno, nastavlja se od sljedeceg. */
    public GameState snapshot() {
        int resumeIndex = localAnswer.get() == null ? questionIndex.get() : questionIndex.get() + 1;
        return new GameState(localPlayer.getName(), questions, resumeIndex, localPlayer.getScore(),
                localPlayer.getCorrectAnswers(), answers, secondsPerQuestion.get(), LocalDateTime.now());
    }

    public void dispose() {
        cancelTimers();
        roundToken++;
    }

    private void cancelTimers() {
        cancelCountdown();
        if (advance != null) {
            advance.cancel(false);
            advance = null;
        }
    }

    private void cancelCountdown() {
        if (countdown != null) {
            countdown.cancel(false);
            countdown = null;
        }
    }

    // ------------------------------------------------------------------ listeneri i podaci

    public void setOnLocalAnswer(Consumer<Answer> onLocalAnswer) {
        this.onLocalAnswer = onLocalAnswer;
    }

    public void setOnAnyAnswer(Consumer<Answer> onAnyAnswer) {
        this.onAnyAnswer = onAnyAnswer;
    }

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public GameMode getMode() {
        return mode;
    }

    public LocalPlayer getLocalPlayer() {
        return localPlayer;
    }

    public RemotePlayer getOpponent() {
        return opponent;
    }

    public List<Player> getPlayers() {
        return opponent == null ? List.of(localPlayer) : List.of(localPlayer, opponent);
    }

    public List<Question> getQuestions() {
        return List.copyOf(questions);
    }

    public ObservableList<Answer> getAnswers() {
        return FXCollections.unmodifiableObservableList(answers);
    }

    public ReadOnlyObjectProperty<Question> currentQuestionProperty() {
        return currentQuestion;
    }

    public ReadOnlyIntegerProperty questionIndexProperty() {
        return questionIndex;
    }

    public ReadOnlyIntegerProperty totalQuestionsProperty() {
        return totalQuestions;
    }

    public ReadOnlyIntegerProperty secondsLeftProperty() {
        return secondsLeft;
    }

    public ReadOnlyIntegerProperty secondsPerQuestionProperty() {
        return secondsPerQuestion;
    }

    public ReadOnlyIntegerProperty localScoreProperty() {
        return localScore;
    }

    public ReadOnlyIntegerProperty opponentScoreProperty() {
        return opponentScore;
    }

    public ReadOnlyObjectProperty<GamePhase> phaseProperty() {
        return phase;
    }

    public ReadOnlyBooleanProperty opponentAnsweredProperty() {
        return opponentAnswered;
    }

    public ReadOnlyObjectProperty<Answer> localAnswerProperty() {
        return localAnswer;
    }

    public ReadOnlyStringProperty statusProperty() {
        return status;
    }
}
