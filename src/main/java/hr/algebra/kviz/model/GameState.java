package hr.algebra.kviz.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Snimka stanja igre koja se serijalizira na disk (Save/Load). */
public record GameState(String playerName,
                        List<Question> questions,
                        int currentIndex,
                        int score,
                        int correctAnswers,
                        List<Answer> answers,
                        int secondsPerQuestion,
                        LocalDateTime savedAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public GameState {
        questions = new ArrayList<>(questions);
        answers = new ArrayList<>(answers);
    }
}
