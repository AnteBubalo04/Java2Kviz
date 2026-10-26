package hr.algebra.kviz.xml;

import hr.algebra.kviz.annotation.GameDoc;
import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.MultipleChoiceQuestion;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.model.TrueFalseQuestion;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Cita replay XML pomocu SAX parsera. */
@GameDoc("SAX citanje replay datoteke za ponovni prikaz odigrane partije.")
public class ReplayReader {

    public ReplayData read(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Jos nema odigrane partije za replay.");
        }
        try {
            ReplayHandler handler = new ReplayHandler();
            XmlSupport.newSaxParser().parse(path.toFile(), handler);
            return new ReplayData(handler.mode, handler.players, handler.questions, handler.moves);
        } catch (Exception e) {
            throw new IOException("Replay datoteka je neispravna", e);
        }
    }

    private static final class ReplayHandler extends DefaultHandler {
        private final StringBuilder text = new StringBuilder();
        private final List<String> players = new ArrayList<>();
        private final List<Question> questions = new ArrayList<>();
        private final List<Answer> moves = new ArrayList<>();
        private GameMode mode = GameMode.SINGLEPLAYER;

        private Attributes questionAttributes;
        private String questionText;
        private List<String> options;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            text.setLength(0);
            switch (qName) {
                case "replay" -> mode = GameMode.valueOf(attributes.getValue("mode"));
                case "player" -> players.add(attributes.getValue("name"));
                case "question" -> {
                    questionAttributes = new org.xml.sax.helpers.AttributesImpl(attributes);
                    options = new ArrayList<>();
                }
                case "move" -> moves.add(new Answer(
                        Integer.parseInt(attributes.getValue("questionIndex")),
                        attributes.getValue("player"),
                        attributes.getValue("answer"),
                        Boolean.parseBoolean(attributes.getValue("correct")),
                        Integer.parseInt(attributes.getValue("points")),
                        Integer.parseInt(attributes.getValue("secondsLeft"))));
                default -> { }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String value = text.toString().trim();
            switch (qName) {
                case "text" -> questionText = value;
                case "option" -> options.add(value);
                case "question" -> questions.add(buildQuestion());
                default -> { }
            }
            text.setLength(0);
        }

        private Question buildQuestion() {
            int id = Integer.parseInt(questionAttributes.getValue("id"));
            String category = questionAttributes.getValue("category");
            String correct = questionAttributes.getValue("correct");
            if (TrueFalseQuestion.TYPE.equals(questionAttributes.getValue("type"))) {
                return new TrueFalseQuestion(id, questionText, category, TrueFalseQuestion.TRUE.equals(correct));
            }
            return MultipleChoiceQuestion.withOrderedOptions(id, questionText, category, correct, options);
        }
    }
}
