package hr.algebra.kviz.xml;

import hr.algebra.kviz.model.Answer;
import hr.algebra.kviz.model.GameMode;
import hr.algebra.kviz.model.Question;

import java.util.List;

/** Sadrzaj replay datoteke: igraci, pitanja i potezi redom kojim su odigrani. */
public record ReplayData(GameMode mode, List<String> players, List<Question> questions, List<Answer> moves) {
}
