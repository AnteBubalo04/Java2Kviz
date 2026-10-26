package hr.algebra.kviz.controller;

import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.PlayerInfo;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.network.GameClient;
import hr.algebra.kviz.network.GameConnection;
import hr.algebra.kviz.network.GameHost;
import hr.algebra.kviz.network.message.HelloMessage;
import hr.algebra.kviz.network.message.StartGameMessage;
import hr.algebra.kviz.rmi.RemoteServices;
import hr.algebra.kviz.service.AppExecutors;
import hr.algebra.kviz.service.MultiplayerSession;
import hr.algebra.kviz.ui.AppContext;
import hr.algebra.kviz.ui.ViewLifecycle;
import hr.algebra.kviz.xml.GameConfig;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Pocetni ekran: unos nadimka, igra za jednog igraca, otvaranje mrezne igre
 * i odabir suigraca iz liste otvorenih igara (lobby preko RMI-ja).
 */
public class LobbyController implements ViewLifecycle {

    @FXML
    private TextField nameField;
    @FXML
    private Button singleplayerButton;
    @FXML
    private Button hostButton;
    @FXML
    private Button cancelHostButton;
    @FXML
    private Button joinButton;
    @FXML
    private ListView<PlayerInfo> openGamesList;
    @FXML
    private Label lobbyStatusLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final BooleanProperty hosting = new SimpleBooleanProperty(false);

    private AppContext context;
    private ScheduledService<List<PlayerInfo>> lobbyRefresh;
    private GameHost gameHost;
    private String hostedName;
    private MultiplayerSession pendingSession;
    private boolean gameStarted;

    @FXML
    private void initialize() {
        context = AppContext.get();
        nameField.setText(context.getConfig().getPlayerName());

        BooleanBinding nameMissing = Bindings.createBooleanBinding(
                () -> nameField.getText() == null || nameField.getText().isBlank(), nameField.textProperty());
        singleplayerButton.disableProperty().bind(busy.or(nameMissing));
        hostButton.disableProperty().bind(busy.or(nameMissing));
        joinButton.disableProperty().bind(busy.or(nameMissing)
                .or(openGamesList.getSelectionModel().selectedItemProperty().isNull()));
        nameField.disableProperty().bind(busy);
        openGamesList.disableProperty().bind(busy);
        cancelHostButton.visibleProperty().bind(hosting);
        cancelHostButton.managedProperty().bind(hosting);
        progressIndicator.visibleProperty().bind(busy);
        openGamesList.setPlaceholder(new Label("Trenutno nema otvorenih igara."));

        startLobbyRefresh();
    }

    // ------------------------------------------------------------------ lista otvorenih igara

    private void startLobbyRefresh() {
        lobbyRefresh = new ScheduledService<>() {
            @Override
            protected Task<List<PlayerInfo>> createTask() {
                RemoteServices services = context.getRemoteServices();
                return new Task<>() {
                    @Override
                    protected List<PlayerInfo> call() throws Exception {
                        return services.lobby().getOpenGames();
                    }
                };
            }
        };
        lobbyRefresh.setPeriod(Duration.seconds(2));
        lobbyRefresh.setExecutor(AppExecutors.background());
        lobbyRefresh.setOnSucceeded(event -> updateOpenGames(lobbyRefresh.getValue()));
        lobbyRefresh.setOnFailed(event -> {
            openGamesList.getItems().clear();
            if (!busy.get()) {
                setStatus("Lobby server nije dostupan. Provjeri RMI adresu u postavkama.");
            }
        });
        lobbyRefresh.start();
    }

    private void updateOpenGames(List<PlayerInfo> games) {
        PlayerInfo selected = openGamesList.getSelectionModel().getSelectedItem();
        List<PlayerInfo> others = games.stream()
                .filter(game -> !game.name().equals(hostedName))
                .toList();
        openGamesList.getItems().setAll(others);
        if (selected != null && others.contains(selected)) {
            openGamesList.getSelectionModel().select(selected);
        }
        if (!busy.get()) {
            setStatus(others.isEmpty()
                    ? "Otvori igru ili pričekaj da je netko drugi otvori."
                    : "Odaberi protivnika i klikni „Pridruži se”.");
        }
    }

    // ------------------------------------------------------------------ igra za jednog igraca

    @FXML
    private void onSingleplayer() {
        String name = rememberName();
        busy.set(true);
        loadQuestions(questions -> {
            gameStarted = true;
            context.getCoordinator().startSingleplayer(name, questions);
        });
    }

    // ------------------------------------------------------------------ domacin

    @FXML
    private void onHost() {
        String name = rememberName();
        busy.set(true);
        hosting.set(true);
        setStatus("Otvaram igru...");

        RemoteServices services = context.getRemoteServices();
        Task<GameHost> openTask = new Task<>() {
            @Override
            protected GameHost call() throws Exception {
                GameHost host = new GameHost();
                try {
                    services.lobby().openGame(new PlayerInfo(name, advertisedHost(services), host.getPort()));
                } catch (Exception e) {
                    host.close();
                    throw e;
                }
                return host;
            }
        };
        openTask.setOnSucceeded(event -> {
            gameHost = openTask.getValue();
            hostedName = name;
            setStatus("Igra je otvorena (port " + gameHost.getPort() + "). Čekam protivnika...");
            gameHost.acceptOpponentAsync(AppExecutors.background())
                    .whenComplete((connection, error) -> Platform.runLater(() -> onOpponentConnected(name, connection, error)));
        });
        openTask.setOnFailed(event -> {
            hosting.set(false);
            busy.set(false);
            Dialogs.error("Mrežna igra", "Igru nije moguće otvoriti: " + Dialogs.rootMessage(openTask.getException()));
        });
        AppExecutors.background().submit(openTask);
    }

    private void onOpponentConnected(String name, GameConnection connection, Throwable error) {
        if (error != null || !hosting.get()) {
            if (connection != null) {
                connection.close();
            }
            return;
        }
        closeLobbyEntry();
        setStatus("Protivnik se spojio, pripremam pitanja...");

        MultiplayerSession session = new MultiplayerSession(connection, GameMode.HOST, name);
        pendingSession = session;
        session.setOnOpponentHello(opponentName -> startHostedGame(session, opponentName));
        session.setOnDisconnected(() -> {
            if (!gameStarted) {
                resetHosting("Protivnik je prekinuo vezu prije početka igre.");
            }
        });
        session.start();
    }

    private void startHostedGame(MultiplayerSession session, String opponentName) {
        int seconds = context.getConfig().getSecondsPerQuestion();
        loadQuestions(questions -> {
            String gameId = session.getLocalName() + "-" + System.currentTimeMillis();
            session.setGameId(gameId);
            session.send(new StartGameMessage(session.getLocalName(), gameId, questions, seconds));
            gameStarted = true;
            hosting.set(false);
            context.getCoordinator().startMultiplayer(session, opponentName, questions, seconds);
        });
    }

    @FXML
    private void onCancelHost() {
        resetHosting("Igra je zatvorena.");
    }

    private void resetHosting(String message) {
        hosting.set(false);
        busy.set(false);
        if (gameHost != null) {
            gameHost.close();
            gameHost = null;
        }
        if (pendingSession != null) {
            pendingSession.close();
            pendingSession = null;
        }
        closeLobbyEntry();
        setStatus(message);
    }

    private void closeLobbyEntry() {
        String name = hostedName;
        hostedName = null;
        if (name == null) {
            return;
        }
        RemoteServices services = context.getRemoteServices();
        AppExecutors.background().submit(() -> {
            try {
                services.lobby().closeGame(name);
            } catch (Exception e) {
                System.err.println("Igru nije moguce maknuti iz lobbyja: " + e.getMessage());
            }
        });
    }

    /** Adresa koju domacin objavljuje u lobbyju: IP ovog racunala u lokalnoj mrezi. */
    private static String advertisedHost(RemoteServices services) {
        return GameClient.localAddressTowards(services.getHost());
    }

    // ------------------------------------------------------------------ gost

    @FXML
    private void onJoin() {
        PlayerInfo selected = openGamesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String name = rememberName();
        if (selected.name().equalsIgnoreCase(name)) {
            Dialogs.info("Mrežna igra", "Protivnik već koristi nadimak „" + name + "”. Odaberi drugi nadimak.");
            return;
        }
        busy.set(true);
        setStatus("Spajam se na " + selected.name() + "...");

        Task<GameConnection> connectTask = new Task<>() {
            @Override
            protected GameConnection call() throws Exception {
                // prvo objavljena adresa, zatim adresa RMI servera (domacin je cesto i RMI server), pa localhost
                return GameClient.connectFirstReachable(
                        List.of(selected.host(), context.getRemoteServices().getHost(), "localhost"), selected.port());
            }
        };
        connectTask.setOnSucceeded(event -> {
            MultiplayerSession session = new MultiplayerSession(connectTask.getValue(), GameMode.GUEST, name);
            pendingSession = session;
            session.setOnStartGame(start -> {
                gameStarted = true;
                context.getCoordinator().startMultiplayer(session, start.hostName(),
                        start.questions(), start.secondsPerQuestion());
            });
            session.setOnDisconnected(() -> {
                if (!gameStarted) {
                    pendingSession = null;
                    busy.set(false);
                    setStatus("Domaćin je prekinuo vezu.");
                }
            });
            session.start();
            session.send(new HelloMessage(name));
            setStatus("Spojeno! Čekam da " + selected.name() + " pokrene igru...");
        });
        connectTask.setOnFailed(event -> {
            busy.set(false);
            Dialogs.error("Mrežna igra", "Spajanje nije uspjelo: " + Dialogs.rootMessage(connectTask.getException()));
        });
        AppExecutors.background().submit(connectTask);
    }

    // ------------------------------------------------------------------ zajednicko

    private void loadQuestions(Consumer<List<Question>> onLoaded) {
        Task<List<Question>> task = context.getQuestionService()
                .createLoadTask(context.getConfig().getQuestionsPerGame());
        lobbyStatusLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> {
            lobbyStatusLabel.textProperty().unbind();
            onLoaded.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            lobbyStatusLabel.textProperty().unbind();
            busy.set(false);
            Dialogs.error("Pitanja", "Pitanja nije moguće učitati: " + Dialogs.rootMessage(task.getException()));
        });
        AppExecutors.background().submit(task);
    }

    private void setStatus(String message) {
        lobbyStatusLabel.textProperty().unbind();
        lobbyStatusLabel.setText(message);
    }

    /** Sprema nadimak u XML konfiguraciju kako bi bio ponuden i sljedeci put. */
    private String rememberName() {
        String name = nameField.getText().trim();
        GameConfig config = context.getConfig();
        if (!name.equals(config.getPlayerName())) {
            config.setPlayerName(name);
            context.updateConfig(config);
        }
        return name;
    }

    @Override
    public void onLeave() {
        if (lobbyRefresh != null) {
            lobbyRefresh.cancel();
        }
        if (!gameStarted) {
            if (gameHost != null) {
                gameHost.close();
            }
            if (pendingSession != null) {
                pendingSession.close();
            }
            closeLobbyEntry();
        }
    }
}
