package hr.algebra.kviz.network.message;

import hr.algebra.kviz.model.Answer;

/** Potez igraca. */
public record AnswerMessage(Answer answer) implements GameMessage {
}
