package hr.algebra.kviz.rmi;

import hr.algebra.kviz.model.PlayerInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** Udaljeni servis za odabir suigraca: domacini objavljuju igru, gosti biraju protivnika. */
public interface LobbyService extends Remote {

    void openGame(PlayerInfo host) throws RemoteException;

    void closeGame(String hostName) throws RemoteException;

    List<PlayerInfo> getOpenGames() throws RemoteException;
}
