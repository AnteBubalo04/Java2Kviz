package hr.algebra.kviz.engine;

/** Faze jedne runde (pitanja). */
public enum GamePhase {
    NOT_STARTED,
    ANSWERING,
    WAITING_OPPONENT,
    SHOWING_RESULT,
    PAUSED,
    FINISHED
}
