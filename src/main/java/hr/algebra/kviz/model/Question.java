package hr.algebra.kviz.model;

import hr.algebra.kviz.annotation.GameDoc;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Bazna klasa svih pitanja. Konkretne vrste pitanja odreduju ponudene odgovore
 * i tocan odgovor, a zajednicka logika provjere odgovora nalazi se ovdje.
 */
@GameDoc("Apstraktno pitanje kviza; podklase definiraju ponudene odgovore i tocan odgovor.")
public abstract sealed class Question implements Serializable
        permits MultipleChoiceQuestion, TrueFalseQuestion {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String text;
    private final String category;

    protected Question(int id, String text, String category) {
        this.id = id;
        this.text = Objects.requireNonNull(text);
        this.category = category == null ? "Opcenito" : category;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category;
    }

    /** Ponudeni odgovori redoslijedom kojim se prikazuju igracu. */
    public abstract List<String> getOptions();

    public abstract String getCorrectAnswer();

    /** Naziv vrste pitanja koji se zapisuje u XML. */
    public abstract String getType();

    @GameDoc("Odgovor je tocan ako je jednak tocnom odgovoru; null znaci da je isteklo vrijeme.")
    public boolean isCorrect(String answer) {
        return answer != null && getCorrectAnswer().equals(answer);
    }

    @Override
    public String toString() {
        return getType() + "#" + id + ": " + text;
    }
}
