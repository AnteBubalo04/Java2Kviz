package hr.algebra.kviz.controller;

import hr.algebra.kviz.docs.DocumentationGenerator;
import hr.algebra.kviz.service.AppExecutors;
import hr.algebra.kviz.ui.AppContext;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.nio.file.Files;
import java.nio.file.Path;

/** Generiranje dokumentacije koda pomocu Reflection API-ja. */
public class DocumentationController {

    @FXML
    private TextArea documentationArea;
    @FXML
    private Label documentationStatusLabel;
    @FXML
    private Button generateButton;
    @FXML
    private Button openButton;

    private Path generatedFile;

    @FXML
    private void initialize() {
        openButton.setDisable(true);
        onGenerate();
    }

    @FXML
    private void onGenerate() {
        DocumentationGenerator generator = AppContext.get().getDocumentationGenerator();
        Task<DocumentationGenerator.Result> task = new Task<>() {
            @Override
            protected DocumentationGenerator.Result call() throws Exception {
                return generator.generate();
            }
        };
        generateButton.disableProperty().bind(task.runningProperty());
        documentationStatusLabel.setText("Generiram dokumentaciju...");
        task.setOnSucceeded(event -> {
            DocumentationGenerator.Result result = task.getValue();
            generatedFile = result.file();
            openButton.setDisable(false);
            documentationStatusLabel.setText("Dokumentirano " + result.classCount() + " klasa, "
                    + result.businessRuleCount() + " opisa poslovne logike. Datoteka: " + result.file().toAbsolutePath());
            documentationArea.setText(toPlainText(result.file()));
        });
        task.setOnFailed(event -> documentationStatusLabel.setText(
                "Generiranje nije uspjelo: " + Dialogs.rootMessage(task.getException())));
        AppExecutors.background().submit(task);
    }

    @FXML
    private void onOpenInBrowser() {
        if (generatedFile != null) {
            AppContext.get().getHostServices().showDocument(generatedFile.toAbsolutePath().toUri().toString());
        }
    }

    private static String toPlainText(Path file) {
        try {
            return Files.readString(file)
                    .replaceAll("(?s)<style>.*?</style>", "")
                    .replaceAll("<nav>.*?</nav>", "")
                    .replaceAll("<h2[^>]*>", "\n\n==== ")
                    .replaceAll("<h3>", "\n")
                    .replaceAll("<div class=\"doc\">", "      » ")
                    .replaceAll("<div class=\"member\">", "    ")
                    .replaceAll("</(h1|h2|h3|div|p)>", "\n")
                    .replaceAll("<[^>]+>", "")
                    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                    .replaceAll("\n{3,}", "\n\n")
                    .trim();
        } catch (Exception e) {
            return "";
        }
    }
}
