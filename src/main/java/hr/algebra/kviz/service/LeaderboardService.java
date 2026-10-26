package hr.algebra.kviz.service;

import hr.algebra.kviz.model.LeaderboardEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Ljestvica najboljih rezultata. Podaci se cuvaju serijalizirani u datoteci,
 * a sucelju se izlazu kao ObservableList koja se osvjezava u JavaFX dretvi.
 */
public class LeaderboardService {

    public static final Path DEFAULT_PATH = Path.of("data", "leaderboard.ser");
    private static final int MAX_ENTRIES = 50;

    private final Path path;
    private final ObservableList<LeaderboardEntry> entries = FXCollections.observableArrayList();
    private final Object fileLock = new Object();

    public LeaderboardService() {
        this(DEFAULT_PATH);
    }

    public LeaderboardService(Path path) {
        this.path = path;
    }

    public ObservableList<LeaderboardEntry> getEntries() {
        return entries;
    }

    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.supplyAsync(this::readFile, AppExecutors.background())
                .thenAccept(list -> Platform.runLater(() -> entries.setAll(list)));
    }

    public CompletableFuture<Void> addAsync(LeaderboardEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (fileLock) {
                // datoteku citamo ponovno jer je druga instanca igre mogla upisati rezultat
                List<LeaderboardEntry> list = readFile();
                list.add(entry);
                list.sort(Comparator.comparingInt(LeaderboardEntry::score).reversed()
                        .thenComparing(LeaderboardEntry::playedAt));
                if (list.size() > MAX_ENTRIES) {
                    list = new ArrayList<>(list.subList(0, MAX_ENTRIES));
                }
                writeFile(list);
                return list;
            }
        }, AppExecutors.background()).thenAccept(list -> Platform.runLater(() -> entries.setAll(list)));
    }

    @SuppressWarnings("unchecked")
    private List<LeaderboardEntry> readFile() {
        synchronized (fileLock) {
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            try (InputStream file = Files.newInputStream(path);
                 ObjectInputStream in = new ObjectInputStream(file)) {
                return new ArrayList<>((List<LeaderboardEntry>) in.readObject());
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Ljestvicu nije moguce procitati: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    private void writeFile(List<LeaderboardEntry> list) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (OutputStream file = Files.newOutputStream(path);
                 ObjectOutputStream out = new ObjectOutputStream(file)) {
                out.writeObject(new ArrayList<>(list));
            }
        } catch (IOException e) {
            System.err.println("Ljestvicu nije moguce spremiti: " + e.getMessage());
        }
    }
}
