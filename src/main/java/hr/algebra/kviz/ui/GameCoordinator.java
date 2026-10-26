package hr.algebra.kviz.ui;

import hr.algebra.kviz.engine.GameEngine;
import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.GameState;
import hr.algebra.kviz.model.LeaderboardEntry;
import hr.algebra.kviz.model.LocalPlayer;
import hr.algebra.kviz.model.Player;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.network.message.AnswerMessage;
import hr.algebra.kviz.service.AppExecutors;
import hr.algebra.kviz.service.MultiplayerSession;
import hr.algebra.kviz.xml.ReplayRecorder;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Povezuje engine s ostatkom aplikacije: mrezom, replay zapisom, ljestvicom
 * i navigacijom. Engine zbog toga ne mora znati nista o tim dijelovima.
 */
public class GameCoordinator {

    private final AppContext context;

    public GameCoordinator(AppContext context) {
        this.context = context;
    }

    public void startSingleplayer(String playerName, List<Question> questions) {
        GameEngine engine = createEngine(null);
        context.setCurrentGame(engine, null);
        recordStart(GameMode.SINGLEPLAYER, List.of(new LocalPlayer(playerName)), questions);
        engine.start(GameMode.SINGLEPLAYER, playerName, null, questions, context.getConfig().getSecondsPerQuestion());
        context.getNavigator().show(View.GAME);
    }

    public void startMultiplayer(MultiplayerSession session, String opponentName,
                                 List<Question> questions, int secondsPerQuestion) {
        GameEngine engine = createEngine(session);
        context.setCurrentGame(engine, session);
        session.attachEngine(engine);
        engine.start(session.getRole(), session.getLocalName(), opponentName, questions, secondsPerQuestion);
        recordStart(session.getRole(), engine.getPlayers(), questions);
        context.getNavigator().show(View.GAME);
    }

    public void resume(GameState state) {
        GameEngine engine = createEngine(null);
        context.setCurrentGame(engine, null);
        recordStart(GameMode.SINGLEPLAYER, List.of(new LocalPlayer(state.playerName())), state.questions());
        state.answers().forEach(answer -> recordMove(context.getReplayRecorder(), answer));
        context.getNavigator().show(View.GAME);
        engine.restore(state);
    }

    private GameEngine createEngine(MultiplayerSession session) {
        GameEngine engine = new GameEngine(Platform::runLater, AppExecutors.scheduler());
        ReplayRecorder recorder = context.getReplayRecorder();

        if (session != null) {
            engine.setOnLocalAnswer(answer -> session.send(new AnswerMessage(answer)));
        }
        engine.setOnAnyAnswer(answer -> recordMove(recorder, answer));
        engine.setOnFinished(() -> onGameFinished(engine, session));
        return engine;
    }

    private void recordStart(GameMode mode, List<Player> players, List<Question> questions) {
        ReplayRecorder recorder = context.getReplayRecorder();
        AppExecutors.sequentialIo().submit(() -> recorder.startGame(mode, players, questions));
    }

    private static void recordMove(ReplayRecorder recorder, hr.algebra.kviz.model.Answer answer) {
        AppExecutors.sequentialIo().submit(() -> recorder.recordMove(answer));
    }

    private void onGameFinished(GameEngine engine, MultiplayerSession session) {
        List<Player> players = engine.getPlayers();
        AppExecutors.sequentialIo().submit(() -> context.getReplayRecorder().finishGame(players));

        LocalPlayer local = engine.getLocalPlayer();
        context.getLeaderboardService().addAsync(new LeaderboardEntry(local.getName(), local.getScore(),
                local.getCorrectAnswers(), engine.getQuestions().size(), engine.getMode(), LocalDateTime.now()));

        if (session != null) {
            session.close();
        }
        context.getNavigator().show(View.RESULTS);
    }
}
