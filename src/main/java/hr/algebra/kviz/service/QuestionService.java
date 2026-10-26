package hr.algebra.kviz.service;

import hr.algebra.kviz.annotation.GameDoc;
import hr.algebra.kviz.model.MultipleChoiceQuestion;
import hr.algebra.kviz.model.Question;
import hr.algebra.kviz.xml.QuestionXmlLoader;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Asinkrono pripremanje pitanja za novu igru. */
public class QuestionService {

    private final QuestionXmlLoader loader = new QuestionXmlLoader();

    /** Task koji u pozadinskoj dretvi ucita XML, nasumicno odabere pitanja i izmijesa odgovore. */
    @GameDoc("Za svaku igru nasumicno se bira zadani broj pitanja, a odgovori se mijesaju.")
    public Task<List<Question>> createLoadTask(int count) {
        return new Task<>() {
            @Override
            protected List<Question> call() throws Exception {
                updateMessage("Učitavam pitanja...");
                List<Question> all = new ArrayList<>(loader.load());
                Random random = new Random();
                Collections.shuffle(all, random);
                List<Question> selected = new ArrayList<>(all.subList(0, Math.min(count, all.size())));
                for (Question question : selected) {
                    if (question instanceof MultipleChoiceQuestion multipleChoice) {
                        multipleChoice.shuffleOptions(random);
                    }
                }
                updateMessage("Pitanja su spremna.");
                return selected;
            }
        };
    }
}
