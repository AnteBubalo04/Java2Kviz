package hr.algebra.kviz.model;

import java.io.Serializable;

/** Otvorena igra u lobbyju: ime domacina i adresa na kojoj ceka protivnika. */
public record PlayerInfo(String name, String host, int port) implements Serializable {
    @Override
    public String toString() {
        return name + "  (" + host + ":" + port + ")";
    }
}
