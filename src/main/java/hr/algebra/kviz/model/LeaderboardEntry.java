package hr.algebra.kviz.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record LeaderboardEntry(String playerName, int score, int correctAnswers,
                               int totalQuestions, GameMode mode, LocalDateTime playedAt) implements Serializable {
}
