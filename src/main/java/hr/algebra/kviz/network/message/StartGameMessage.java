package hr.algebra.kviz.network.message;

import hr.algebra.kviz.model.Question;

import java.util.ArrayList;
import java.util.List;

/** Domacin salje pitanja (vec izmijesana) i pravila igre, cime igra pocinje. */
public record StartGameMessage(String hostName, String gameId, List<Question> questions,
                               int secondsPerQuestion) implements GameMessage {

    public StartGameMessage {
        // kopija osigurava serijalizabilnu listu (npr. subList nije serijalizabilan)
        questions = new ArrayList<>(questions);
    }
}
