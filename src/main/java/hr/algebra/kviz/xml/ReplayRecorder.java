package hr.algebra.kviz.xml;

import hr.algebra.kviz.annotation.GameDoc;
import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.Player;
import hr.algebra.kviz.model.Question;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Zapisuje tijek igre u XML (DOM). Datoteka se azurira nakon svakog poteza,
 * tako da replay postoji i ako se igra prekine.
 */
@GameDoc("Dinamicko zapisivanje poteza u replay XML nakon svakog odgovora.")
public class ReplayRecorder {

    public static final Path DEFAULT_PATH = Path.of("replay", "last-game.xml");

    private final Path path;
    private Document document;
    private Element movesElement;

    public ReplayRecorder() {
        this(DEFAULT_PATH);
    }

    public ReplayRecorder(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public synchronized void startGame(GameMode mode, List<Player> players, List<Question> questions) {
        try {
            document = XmlSupport.newDocumentBuilder().newDocument();
            Element root = document.createElement("replay");
            root.setAttribute("mode", mode.name());
            root.setAttribute("startedAt", LocalDateTime.now().toString());
            document.appendChild(root);

            Element playersElement = document.createElement("players");
            for (Player player : players) {
                Element element = document.createElement("player");
                element.setAttribute("name", player.getName());
                element.setAttribute("local", String.valueOf(player.isLocal()));
                playersElement.appendChild(element);
            }
            root.appendChild(playersElement);

            Element questionsElement = document.createElement("questions");
            for (int i = 0; i < questions.size(); i++) {
                questionsElement.appendChild(createQuestionElement(i, questions.get(i)));
            }
            root.appendChild(questionsElement);

            movesElement = document.createElement("moves");
            root.appendChild(movesElement);
            flush();
        } catch (Exception e) {
            System.err.println("Replay nije moguce zapoceti: " + e.getMessage());
            document = null;
        }
    }

    private Element createQuestionElement(int index, Question question) {
        Element element = document.createElement("question");
        element.setAttribute("index", String.valueOf(index));
        element.setAttribute("id", String.valueOf(question.getId()));
        element.setAttribute("type", question.getType());
        element.setAttribute("category", question.getCategory());
        element.setAttribute("correct", question.getCorrectAnswer());

        Element text = document.createElement("text");
        text.setTextContent(question.getText());
        element.appendChild(text);
        for (String option : question.getOptions()) {
            Element optionElement = document.createElement("option");
            optionElement.setTextContent(option);
            element.appendChild(optionElement);
        }
        return element;
    }

    /** Dodaje jedan potez i odmah zapisuje datoteku. */
    public synchronized void recordMove(Answer answer) {
        if (document == null) {
            return;
        }
        Element move = document.createElement("move");
        move.setAttribute("questionIndex", String.valueOf(answer.questionIndex()));
        move.setAttribute("player", answer.playerName());
        if (answer.selectedAnswer() != null) {
            move.setAttribute("answer", answer.selectedAnswer());
        }
        move.setAttribute("correct", String.valueOf(answer.correct()));
        move.setAttribute("points", String.valueOf(answer.points()));
        move.setAttribute("secondsLeft", String.valueOf(answer.secondsLeft()));
        movesElement.appendChild(move);
        flush();
    }

    public synchronized void finishGame(List<Player> players) {
        if (document == null) {
            return;
        }
        Element result = document.createElement("result");
        result.setAttribute("finishedAt", LocalDateTime.now().toString());
        for (Player player : players) {
            Element element = document.createElement("score");
            element.setAttribute("player", player.getName());
            element.setAttribute("points", String.valueOf(player.getScore()));
            element.setAttribute("correctAnswers", String.valueOf(player.getCorrectAnswers()));
            result.appendChild(element);
        }
        document.getDocumentElement().appendChild(result);
        flush();
        document = null;
    }

    private void flush() {
        try {
            XmlSupport.write(document, path);
        } catch (Exception e) {
            System.err.println("Replay nije moguce zapisati: " + e.getMessage());
        }
    }
}
