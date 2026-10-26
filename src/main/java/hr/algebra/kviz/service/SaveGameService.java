package hr.algebra.kviz.service;

import hr.algebra.kviz.model.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Trajna pohrana stanja igre serijalizacijom. Citanje i pisanje rade u pozadinskoj dretvi. */
public class SaveGameService {

    public static final Path DEFAULT_PATH = Path.of("saves", "savegame.ser");

    private final Path path;

    public SaveGameService() {
        this(DEFAULT_PATH);
    }

    public SaveGameService(Path path) {
        this.path = path;
    }

    public boolean hasSavedGame() {
        return Files.exists(path);
    }

    public CompletableFuture<Path> saveAsync(GameState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(path.toAbsolutePath().getParent());
                try (OutputStream file = Files.newOutputStream(path);
                     ObjectOutputStream out = new ObjectOutputStream(file)) {
                    out.writeObject(state);
                }
                return path;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, AppExecutors.background());
    }

    public CompletableFuture<GameState> loadAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream file = Files.newInputStream(path);
                 ObjectInputStream in = new ObjectInputStream(file)) {
                return (GameState) in.readObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new IllegalStateException("Spremljena igra je neispravna", e);
            }
        }, AppExecutors.background());
    }
}
