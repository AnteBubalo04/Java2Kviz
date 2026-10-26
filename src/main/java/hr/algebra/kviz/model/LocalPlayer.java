package hr.algebra.kviz.model;

import java.io.Serial;

/** Igrac koji igra na ovom racunalu. */
public final class LocalPlayer extends Player {
    @Serial
    private static final long serialVersionUID = 1L;

    public LocalPlayer(String name) {
        super(name);
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
