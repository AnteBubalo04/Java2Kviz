package hr.algebra.kviz.model;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record ChatMessage(String sender, String text, LocalTime time) implements Serializable {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String toString() {
        return "[" + time.format(FORMAT) + "] " + sender + ": " + text;
    }
}
