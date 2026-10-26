package hr.algebra.kviz.service;

import hr.algebra.kviz.engine.GameEngine;
import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.network.GameConnection;
import hr.algebra.kviz.network.message.AnswerMessage;
import hr.algebra.kviz.network.message.ByeMessage;
import hr.algebra.kviz.network.message.GameMessage;
import hr.algebra.kviz.network.message.HelloMessage;
import hr.algebra.kviz.network.message.StartGameMessage;
import javafx.application.Platform;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Mrezna sesija jedne partije. Prima poruke iz dretve socketa i prosljeduje ih
 * u JavaFX dretvu (Platform.runLater); slanje radi u pozadinskoj dretvi kako
 * sucelje nikad ne bi cekalo mrezu.
 */
public class MultiplayerSession {

    private final GameConnection connection;
    private final GameMode role;
    private final String localName;

    private volatile GameEngine engine;
    private volatile Consumer<String> onOpponentHello = name -> { };
    private volatile Consumer<StartGameMessage> onStartGame = message -> { };
    private volatile Runnable onDisconnected = () -> { };

    private String opponentName;
    private String gameId;

    public MultiplayerSession(GameConnection connection, GameMode role, String localName) {
        this.connection = connection;
        this.role = role;
        this.localName = localName;
        connection.setMessageHandler(message -> Platform.runLater(() -> handle(message)));
        connection.setDisconnectHandler(() -> Platform.runLater(this::handleDisconnect));
    }

    public void start() {
        connection.startListening();
    }

    private void handle(GameMessage message) {
        if (message instanceof HelloMessage hello) {
            opponentName = hello.playerName();
            onOpponentHello.accept(hello.playerName());
        } else if (message instanceof StartGameMessage start) {
            opponentName = start.hostName();
            gameId = start.gameId();
            onStartGame.accept(start);
        } else if (message instanceof AnswerMessage answerMessage) {
            if (engine != null) {
                engine.receiveRemoteAnswer(answerMessage.answer());
            }
        } else if (message instanceof ByeMessage) {
            handleDisconnect();
        }
    }

    private void handleDisconnect() {
        if (engine != null) {
            engine.opponentDisconnected();
        }
        onDisconnected.run();
    }

    public void send(GameMessage message) {
        AppExecutors.background().submit(() -> {
            try {
                connection.send(message);
            } catch (IOException e) {
                System.err.println("Poruku nije moguce poslati: " + e.getMessage());
            }
        });
    }

    public void close() {
        if (connection.isClosed()) {
            return;
        }
        AppExecutors.background().submit(() -> {
            try {
                connection.send(new ByeMessage(localName));
            } catch (IOException ignored) {
                // veza je mozda vec prekinuta
            }
            connection.close();
        });
    }

    public void attachEngine(GameEngine engine) {
        this.engine = engine;
    }

    public void setOnOpponentHello(Consumer<String> onOpponentHello) {
        this.onOpponentHello = onOpponentHello;
    }

    public void setOnStartGame(Consumer<StartGameMessage> onStartGame) {
        this.onStartGame = onStartGame;
    }

    public void setOnDisconnected(Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public GameMode getRole() {
        return role;
    }

    public String getLocalName() {
        return localName;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
}
