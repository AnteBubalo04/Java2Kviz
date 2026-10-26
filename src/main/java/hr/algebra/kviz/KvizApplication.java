package hr.algebra.kviz;

import hr.algebra.kviz.rmi.RemoteServices;
import hr.algebra.kviz.service.AppExecutors;
import hr.algebra.kviz.ui.AppContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Java2 Kviz - kviz za jednog ili dva igraca.
 *
 * @author Ante Bubalo
 */
public class KvizApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppContext context = AppContext.init(getHostServices());

        Parent root = FXMLLoader.load(Objects.requireNonNull(
                KvizApplication.class.getResource("/hr/algebra/kviz/view/main.fxml")));
        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(Objects.requireNonNull(
                KvizApplication.class.getResource("/hr/algebra/kviz/styles.css")).toExternalForm());

        stage.setTitle("Java2 Kviz - Ante Bubalo");
        stage.setMinWidth(720);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();

        connectToLobbyServer(context);
    }

    /** RMI server se trazi (i po potrebi pokrece) u pozadini kako prozor ne bi cekao. */
    private void connectToLobbyServer(AppContext context) {
        RemoteServices services = context.getRemoteServices();
        context.setStatus("Spajam se na lobby server " + services.getHost() + ":" + services.getPort() + "...");
        CompletableFuture.supplyAsync(services::ensureServerRunning, AppExecutors.background())
                .thenAccept(available -> Platform.runLater(() -> context.setStatus(available
                        ? "Lobby server: " + services.getHost() + ":" + services.getPort()
                        : "Lobby server nije dostupan - igra za jednog igraca radi, mrezna ne.")));
    }

    @Override
    public void stop() {
        AppContext.get().shutdown();
        // RMI moze ostaviti ne-daemon dretve; aplikacija se mora ugasiti kad se zatvori prozor
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
