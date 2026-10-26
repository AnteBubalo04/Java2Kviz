package hr.algebra.kviz.network.message;

/** Igrac je napustio igru. */
public record ByeMessage(String playerName) implements GameMessage {
}
