package hr.algebra.kviz.rmi;

import hr.algebra.kviz.model.PlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyServiceImpl implements LobbyService {

    private final Map<String, PlayerInfo> openGames = new ConcurrentHashMap<>();

    @Override
    public void openGame(PlayerInfo host) {
        openGames.put(host.name(), host);
        System.out.println("LOBBY: otvorena igra " + host);
    }

    @Override
    public void closeGame(String hostName) {
        if (openGames.remove(hostName) != null) {
            System.out.println("LOBBY: zatvorena igra " + hostName);
        }
    }

    @Override
    public List<PlayerInfo> getOpenGames() {
        List<PlayerInfo> games = new ArrayList<>(openGames.values());
        games.sort(Comparator.comparing(PlayerInfo::name));
        return games;
    }
}
