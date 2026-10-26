package hr.algebra.kviz.network.message;

/** Gost se predstavlja domacinu odmah nakon spajanja. */
public record HelloMessage(String playerName) implements GameMessage {
}
