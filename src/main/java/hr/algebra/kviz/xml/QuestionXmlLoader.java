package hr.algebra.kviz.xml;

import hr.algebra.kviz.annotation.GameDoc;
import hr.algebra.kviz.model.MultipleChoiceQuestion;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.model.TrueFalseQuestion;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Ucitava bazu pitanja iz XML-a pomocu SAX parsera. */
@GameDoc("SAX citanje baze pitanja (questions.xml).")
public class QuestionXmlLoader {

    public static final String RESOURCE = "/hr/algebra/kviz/questions.xml";

    public List<Question> load() throws IOException {
        try (InputStream input = QuestionXmlLoader.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("Nema datoteke s pitanjima: " + RESOURCE);
            }
            QuestionHandler handler = new QuestionHandler();
            XmlSupport.newSaxParser().parse(input, handler);
            return handler.questions;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Neispravan XML s pitanjima", e);
        }
    }

    private static final class QuestionHandler extends DefaultHandler {
        private final List<Question> questions = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();

        private int id;
        private String type;
        private String category;
        private boolean trueFalseAnswer;
        private String questionText;
        private String correct;
        private List<String> wrong;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            text.setLength(0);
            if ("question".equals(qName)) {
                id = Integer.parseInt(attributes.getValue("id"));
                type = attributes.getValue("type");
                category = attributes.getValue("category");
                trueFalseAnswer = Boolean.parseBoolean(attributes.getValue("answer"));
                questionText = null;
                correct = null;
                wrong = new ArrayList<>();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String value = text.toString().trim();
            switch (qName) {
                case "text" -> questionText = value;
                case "correct" -> correct = value;
                case "wrong" -> wrong.add(value);
                case "question" -> questions.add(buildQuestion());
                default -> { }
            }
            text.setLength(0);
        }

        private Question buildQuestion() throws SAXException {
            if (TrueFalseQuestion.TYPE.equals(type)) {
                return new TrueFalseQuestion(id, questionText, category, trueFalseAnswer);
            }
            if (MultipleChoiceQuestion.TYPE.equals(type)) {
                return new MultipleChoiceQuestion(id, questionText, category, correct, wrong);
            }
            throw new SAXException("Nepoznata vrsta pitanja: " + type);
        }
    }
}
