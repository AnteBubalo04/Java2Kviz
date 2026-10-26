package hr.algebra.kviz.network;

import hr.algebra.kviz.network.message.GameMessage;
import hr.algebra.kviz.service.AppExecutors;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * TCP veza izmedu dva igraca. Citanje poruka radi u zasebnoj dretvi koja
 * svaku primljenu poruku predaje handleru; slanje je sinkronizirano jer
 * vise dretvi moze slati istovremeno.
 */
public class GameConnection implements Closeable {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final ExecutorService reader =
            Executors.newSingleThreadExecutor(AppExecutors.daemonFactory("socket-reader"));

    private volatile Consumer<GameMessage> messageHandler = message -> { };
    private volatile Runnable disconnectHandler = () -> { };
    private volatile boolean closed;

    public GameConnection(Socket socket) throws IOException {
        this.socket = socket;
        // izlazni stream se mora stvoriti i flushati prije ulaznog, inace obje strane cekaju zaglavlje
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void setMessageHandler(Consumer<GameMessage> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void setDisconnectHandler(Runnable disconnectHandler) {
        this.disconnectHandler = disconnectHandler;
    }

    /** Pokrece dretvu koja slusa poruke s druge strane. */
    public void startListening() {
        reader.submit(this::readLoop);
    }

    private void readLoop() {
        try {
            while (!closed) {
                Object received = in.readObject();
                if (received instanceof GameMessage message) {
                    messageHandler.accept(message);
                }
            }
        } catch (EOFException | SocketException e) {
            // druga strana je zatvorila vezu
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Greska pri citanju poruke: " + e.getMessage());
        } finally {
            if (!closed) {
                disconnectHandler.run();
            }
            close();
        }
    }

    public synchronized void send(GameMessage message) throws IOException {
        if (closed) {
            throw new IOException("Veza je zatvorena");
        }
        out.writeObject(message);
        out.reset();
        out.flush();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            // zatvaramo svejedno
        }
        reader.shutdownNow();
    }
}
