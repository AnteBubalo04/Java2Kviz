package hr.algebra.kviz.network.message;

import java.io.Serializable;

/** Sve poruke koje se salju preko TCP socketa izmedu dva igraca. */
public sealed interface GameMessage extends Serializable
        permits HelloMessage, StartGameMessage, AnswerMessage, ByeMessage {
}
