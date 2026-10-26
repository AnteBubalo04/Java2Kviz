package hr.algebra.kviz.rmi;

import hr.algebra.kviz.model.ChatMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** Udaljeni chat; svaka igra ima svoju sobu (gameId). */
public interface ChatService extends Remote {

    void sendMessage(String gameId, ChatMessage message) throws RemoteException;

    /** Vraca poruke od zadanog indeksa nadalje, kako bi klijent dohvatio samo nove. */
    List<ChatMessage> getMessages(String gameId, int fromIndex) throws RemoteException;
}
