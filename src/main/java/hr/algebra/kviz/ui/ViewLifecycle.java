package hr.algebra.kviz.ui;

/** Kontroler koji zeli znati kad korisnik napusta njegov ekran. */
public interface ViewLifecycle {

    /** Vraca false ako korisnik odustane od napustanja ekrana. */
    default boolean confirmLeave() {
        return true;
    }

    /** Poziva se prije zamjene ekrana; ovdje se zaustavljaju pozadinski poslovi. */
    default void onLeave() {
    }
}
