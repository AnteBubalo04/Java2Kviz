package hr.algebra.kviz.model;

import hr.algebra.kviz.annotation.GameDoc;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@GameDoc("Pitanje s jednim tocnim i vise netocnih odgovora.")
public final class MultipleChoiceQuestion extends Question {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "multiple";

    private final String correctAnswer;
    private final List<String> options;

    public MultipleChoiceQuestion(int id, String text, String category, String correctAnswer, List<String> wrongAnswers) {
        super(id, text, category);
        if (wrongAnswers == null || wrongAnswers.isEmpty()) {
            throw new IllegalArgumentException("Pitanje mora imati barem jedan netocan odgovor");
        }
        this.correctAnswer = correctAnswer;
        this.options = new ArrayList<>();
        this.options.add(correctAnswer);
        this.options.addAll(wrongAnswers);
    }

    /** Konstruktor za ucitavanje vec poredanih odgovora (npr. iz replay XML-a). */
    public static MultipleChoiceQuestion withOrderedOptions(int id, String text, String category, String correctAnswer, List<String> orderedOptions) {
        List<String> wrong = new ArrayList<>(orderedOptions);
        wrong.remove(correctAnswer);
        MultipleChoiceQuestion q = new MultipleChoiceQuestion(id, text, category, correctAnswer, wrong);
        q.options.clear();
        q.options.addAll(orderedOptions);
        return q;
    }

    @GameDoc("Mijesa redoslijed odgovora; domacin to radi jednom pa oba igraca vide isti raspored.")
    public void shuffleOptions(Random random) {
        Collections.shuffle(options, random);
    }

    @Override
    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    @Override
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
