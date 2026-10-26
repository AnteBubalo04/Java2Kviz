package hr.algebra.kviz.controller;

import hr.algebra.kviz.ui.AppContext;
import hr.algebra.kviz.xml.GameConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** Uredivanje konfiguracije igre koja se sprema u XML (DOM). */
public class SettingsController {

    @FXML
    private TextField nameField;
    @FXML
    private Spinner<Integer> questionsSpinner;
    @FXML
    private Spinner<Integer> secondsSpinner;
    @FXML
    private TextField rmiHostField;
    @FXML
    private Spinner<Integer> rmiPortSpinner;
    @FXML
    private Spinner<Integer> replayDelaySpinner;
    @FXML
    private TextArea xmlPreview;
    @FXML
    private Label settingsStatusLabel;

    private AppContext context;

    @FXML
    private void initialize() {
        context = AppContext.get();
        questionsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                GameConfig.MIN_QUESTIONS, GameConfig.MAX_QUESTIONS, 10));
        secondsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                GameConfig.MIN_SECONDS, GameConfig.MAX_SECONDS, 15));
        rmiPortSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1024, 65535, 1099));
        replayDelaySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(300, 10000, 1500, 100));
        fill(context.getConfig());
    }

    private void fill(GameConfig config) {
        nameField.setText(config.getPlayerName());
        questionsSpinner.getValueFactory().setValue(config.getQuestionsPerGame());
        secondsSpinner.getValueFactory().setValue(config.getSecondsPerQuestion());
        rmiHostField.setText(config.getRmiHost());
        rmiPortSpinner.getValueFactory().setValue(config.getRmiPort());
        replayDelaySpinner.getValueFactory().setValue(config.getReplayDelayMillis());
        xmlPreview.setText(context.getConfigRepository().readRaw());
    }

    @FXML
    private void onSave() {
        GameConfig config = new GameConfig();
        config.setPlayerName(nameField.getText());
        config.setQuestionsPerGame(questionsSpinner.getValue());
        config.setSecondsPerQuestion(secondsSpinner.getValue());
        config.setRmiHost(rmiHostField.getText());
        config.setRmiPort(rmiPortSpinner.getValue());
        config.setReplayDelayMillis(replayDelaySpinner.getValue());
        try {
            context.updateConfig(config);
            fill(context.getConfig());
            settingsStatusLabel.setText("Postavke su spremljene u " + context.getConfigRepository().getPath().toAbsolutePath());
        } catch (RuntimeException e) {
            Dialogs.error("Postavke", "Postavke nije moguće spremiti: " + Dialogs.rootMessage(e));
        }
    }

    @FXML
    private void onReload() {
        fill(context.reloadConfig());
        settingsStatusLabel.setText("Postavke su ponovno učitane iz XML datoteke.");
    }
}
