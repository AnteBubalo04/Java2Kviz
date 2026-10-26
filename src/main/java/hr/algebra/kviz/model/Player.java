package hr.algebra.kviz.model;

import hr.algebra.kviz.annotation.GameDoc;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Bazna klasa igraca. */
@GameDoc("Igrac s imenom, bodovima i brojem tocnih odgovora.")
public abstract sealed class Player implements Serializable permits LocalPlayer, RemotePlayer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private int score;
    private int correctAnswers;

    protected Player(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    @GameDoc("Biljezi odgovor igraca: tocan odgovor donosi bodove i povecava broj tocnih odgovora.")
    public void registerAnswer(Answer answer) {
        if (answer.correct()) {
            correctAnswers++;
            score += answer.points();
        }
    }

    /** Vraca stanje igraca iz spremljene igre. */
    public void restore(int score, int correctAnswers) {
        this.score = score;
        this.correctAnswers = correctAnswers;
    }

    /** true ako igrac igra na ovom racunalu. */
    public abstract boolean isLocal();

    @Override
    public String toString() {
        return name + " (" + score + ")";
    }
}
