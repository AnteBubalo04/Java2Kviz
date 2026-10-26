package hr.algebra.kviz.model;

import hr.algebra.kviz.annotation.GameDoc;

import java.io.Serial;
import java.util.List;

@GameDoc("Pitanje s odgovorom Tocno/Netocno.")
public final class TrueFalseQuestion extends Question {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "truefalse";
    public static final String TRUE = "Točno";
    public static final String FALSE = "Netočno";

    private final boolean statementTrue;

    public TrueFalseQuestion(int id, String text, String category, boolean statementTrue) {
        super(id, text, category);
        this.statementTrue = statementTrue;
    }

    public boolean isStatementTrue() {
        return statementTrue;
    }

    @Override
    public List<String> getOptions() {
        return List.of(TRUE, FALSE);
    }

    @Override
    public String getCorrectAnswer() {
        return statementTrue ? TRUE : FALSE;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
