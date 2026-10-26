package hr.algebra.kviz.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Zamjenjuje sadrzaj sredisnjeg dijela glavnog prozora. */
public class Navigator {

    private final StackPane contentArea;
    private Object currentController;
    private View currentView;

    public Navigator(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    /** Pita trenutni ekran smije li se napustiti (npr. igra u tijeku). */
    public boolean confirmLeave() {
        return !(currentController instanceof ViewLifecycle lifecycle) || lifecycle.confirmLeave();
    }

    public <T> T show(View view) {
        if (currentController instanceof ViewLifecycle lifecycle) {
            lifecycle.onLeave();
        }
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(view.resource()));
            Parent root = loader.load();
            contentArea.getChildren().setAll(root);
            currentController = loader.getController();
            currentView = view;
            return loader.getController();
        } catch (IOException e) {
            throw new UncheckedIOException("Ekran " + view + " nije moguce ucitati", e);
        }
    }

    public View getCurrentView() {
        return currentView;
    }

    public void leaveCurrent() {
        if (currentController instanceof ViewLifecycle lifecycle) {
            lifecycle.onLeave();
        }
        currentController = null;
    }
}
