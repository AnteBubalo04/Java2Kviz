package hr.algebra.kviz.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Domacin igre: otvara ServerSocket na slobodnom portu i u pozadinskoj dretvi
 * ceka jednog protivnika.
 */
public class GameHost implements Closeable {

    private final ServerSocket serverSocket;

    public GameHost() throws IOException {
        this.serverSocket = new ServerSocket(0);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public CompletableFuture<GameConnection> acceptOpponentAsync(Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (serverSocket) {
                Socket socket = serverSocket.accept();
                return new GameConnection(socket);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executor);
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // vec zatvoreno
        }
    }
}
