package hr.algebra.kviz.model;

import java.io.Serial;

/** Protivnik ciji potezi stizu preko mreze. */
public final class RemotePlayer extends Player {
    @Serial
    private static final long serialVersionUID = 1L;

    public RemotePlayer(String name) {
        super(name);
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
