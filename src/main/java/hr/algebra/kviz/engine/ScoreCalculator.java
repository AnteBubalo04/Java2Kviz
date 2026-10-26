package hr.algebra.kviz.engine;

import hr.algebra.kviz.annotation.GameDoc;

/** Pravila bodovanja. */
@GameDoc("Tocan odgovor nosi 500 bodova plus do 500 bodova za brzinu; netocan ili zakasnjeli 0.")
public final class ScoreCalculator {

    public static final int BASE_POINTS = 500;
    public static final int MAX_SPEED_BONUS = 500;

    private ScoreCalculator() {
    }

    @GameDoc("Bonus za brzinu proporcionalan je preostalom vremenu.")
    public static int points(boolean correct, int secondsLeft, int secondsPerQuestion) {
        if (!correct || secondsPerQuestion <= 0) {
            return 0;
        }
        double remaining = Math.max(0, Math.min(secondsLeft, secondsPerQuestion)) / (double) secondsPerQuestion;
        return BASE_POINTS + (int) Math.round(MAX_SPEED_BONUS * remaining);
    }
}
