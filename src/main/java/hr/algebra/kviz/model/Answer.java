package hr.algebra.kviz.model;

import java.io.Serializable;

/**
 * Jedan potez igraca: odgovor na pitanje s rednim brojem questionIndex.
 * selectedAnswer je null ako je igracu isteklo vrijeme.
 */
public record Answer(int questionIndex, String playerName, String selectedAnswer,
                     boolean correct, int points, int secondsLeft) implements Serializable {

    public boolean timedOut() {
        return selectedAnswer == null;
    }
}
