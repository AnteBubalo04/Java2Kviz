package hr.algebra.kviz.xml;

/** Konfiguracija igre koja se cita iz i zapisuje u config/game-config.xml. */
public class GameConfig {

    public static final int MIN_QUESTIONS = 3;
    public static final int MAX_QUESTIONS = 30;
    public static final int MIN_SECONDS = 5;
    public static final int MAX_SECONDS = 60;

    private String playerName = "Igrac";
    private int questionsPerGame = 10;
    private int secondsPerQuestion = 15;
    private String rmiHost = "localhost";
    private int rmiPort = 1099;
    private int replayDelayMillis = 1500;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            this.playerName = playerName.trim();
        }
    }

    public int getQuestionsPerGame() {
        return questionsPerGame;
    }

    public void setQuestionsPerGame(int questionsPerGame) {
        this.questionsPerGame = clamp(questionsPerGame, MIN_QUESTIONS, MAX_QUESTIONS);
    }

    public int getSecondsPerQuestion() {
        return secondsPerQuestion;
    }

    public void setSecondsPerQuestion(int secondsPerQuestion) {
        this.secondsPerQuestion = clamp(secondsPerQuestion, MIN_SECONDS, MAX_SECONDS);
    }

    public String getRmiHost() {
        return rmiHost;
    }

    public void setRmiHost(String rmiHost) {
        if (rmiHost != null && !rmiHost.isBlank()) {
            this.rmiHost = rmiHost.trim();
        }
    }

    public int getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(int rmiPort) {
        this.rmiPort = clamp(rmiPort, 1024, 65535);
    }

    public int getReplayDelayMillis() {
        return replayDelayMillis;
    }

    public void setReplayDelayMillis(int replayDelayMillis) {
        this.replayDelayMillis = clamp(replayDelayMillis, 300, 10000);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
