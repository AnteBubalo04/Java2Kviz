package hr.algebra.kviz.ui;

import hr.algebra.kviz.docs.DocumentationGenerator;
import hr.algebra.kviz.engine.GameEngine;
import hr.algebra.kviz.rmi.RemoteServices;
import hr.algebra.kviz.rmi.RmiServer;
import hr.algebra.kviz.service.AppExecutors;
import hr.algebra.kviz.service.LeaderboardService;
import hr.algebra.kviz.service.MultiplayerSession;
import hr.algebra.kviz.service.QuestionService;
import hr.algebra.kviz.service.SaveGameService;
import hr.algebra.kviz.xml.ConfigRepository;
import hr.algebra.kviz.xml.GameConfig;
import hr.algebra.kviz.xml.ReplayRecorder;
import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.StackPane;

/**
 * Jedno mjesto s kojeg kontroleri dohvacaju servise i trenutnu igru, kako
 * kontroleri ne bi sami stvarali ovisnosti.
 */
public final class AppContext {

    private static AppContext instance;

    private final HostServices hostServices;
    private final ConfigRepository configRepository = new ConfigRepository();
    private final QuestionService questionService = new QuestionService();
    private final SaveGameService saveGameService = new SaveGameService();
    private final LeaderboardService leaderboardService = new LeaderboardService();
    private final ReplayRecorder replayRecorder = new ReplayRecorder();
    private final DocumentationGenerator documentationGenerator = new DocumentationGenerator();
    private final StringProperty status = new SimpleStringProperty("");
    private final StringProperty playerName = new SimpleStringProperty("");

    private GameConfig config;
    private RemoteServices remoteServices;
    private Navigator navigator;
    private GameCoordinator coordinator;

    private GameEngine currentEngine;
    private MultiplayerSession currentSession;

    private AppContext(HostServices hostServices) {
        this.hostServices = hostServices;
        applyConfig(configRepository.load());
    }

    public static synchronized AppContext init(HostServices hostServices) {
        instance = new AppContext(hostServices);
        return instance;
    }

    public static AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("AppContext nije inicijaliziran");
        }
        return instance;
    }

    public void initNavigation(StackPane contentArea) {
        navigator = new Navigator(contentArea);
        coordinator = new GameCoordinator(this);
    }

    // ------------------------------------------------------------ konfiguracija

    public GameConfig getConfig() {
        return config;
    }

    /** Sprema konfiguraciju u XML i primjenjuje je (npr. nova RMI adresa). */
    public void updateConfig(GameConfig newConfig) {
        configRepository.save(newConfig);
        applyConfig(newConfig);
    }

    public GameConfig reloadConfig() {
        applyConfig(configRepository.load());
        return config;
    }

    private void applyConfig(GameConfig newConfig) {
        this.config = newConfig;
        this.remoteServices = new RemoteServices(newConfig.getRmiHost(), newConfig.getRmiPort());
        this.playerName.set(newConfig.getPlayerName());
    }

    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    // ------------------------------------------------------------ trenutna igra

    public void setCurrentGame(GameEngine engine, MultiplayerSession session) {
        endCurrentGame();
        this.currentEngine = engine;
        this.currentSession = session;
    }

    public void endCurrentGame() {
        if (currentEngine != null) {
            currentEngine.dispose();
        }
        if (currentSession != null) {
            currentSession.close();
            currentSession = null;
        }
    }

    public GameEngine getCurrentEngine() {
        return currentEngine;
    }

    public MultiplayerSession getCurrentSession() {
        return currentSession;
    }

    // ------------------------------------------------------------ servisi

    public RemoteServices getRemoteServices() {
        return remoteServices;
    }

    public QuestionService getQuestionService() {
        return questionService;
    }

    public SaveGameService getSaveGameService() {
        return saveGameService;
    }

    public LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }

    public ReplayRecorder getReplayRecorder() {
        return replayRecorder;
    }

    public DocumentationGenerator getDocumentationGenerator() {
        return documentationGenerator;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public Navigator getNavigator() {
        return navigator;
    }

    public GameCoordinator getCoordinator() {
        return coordinator;
    }

    public ReadOnlyStringProperty statusProperty() {
        return status;
    }

    public void setStatus(String message) {
        status.set(message);
    }

    public ReadOnlyStringProperty playerNameProperty() {
        return playerName;
    }

    public void shutdown() {
        if (navigator != null) {
            navigator.leaveCurrent();
        }
        endCurrentGame();
        RmiServer.stop();
        AppExecutors.shutdown();
    }
}
