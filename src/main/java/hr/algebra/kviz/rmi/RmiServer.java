package hr.algebra.kviz.rmi;

import hr.algebra.kviz.network.GameClient;
import hr.algebra.kviz.xml.ConfigRepository;
import hr.algebra.kviz.xml.GameConfig;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI server s lobby i chat servisom. Pokrece ga prva instanca aplikacije
 * (ako server jos ne radi), a moze se pokrenuti i samostalno preko main metode.
 */
public final class RmiServer {

    public static final String LOBBY_SERVICE = "LobbyService";
    public static final String CHAT_SERVICE = "ChatService";

    // drzimo reference kako ih garbage collector ne bi pokupio
    private static Registry registry;
    private static LobbyServiceImpl lobbyImpl;
    private static ChatServiceImpl chatImpl;

    private RmiServer() {
    }

    public static synchronized void start(int port) throws RemoteException {
        if (registry != null) {
            return;
        }
        // stubovi nose adresu servera; bez ovoga Windows zna upisati adresu virtualnog adaptera
        if (System.getProperty("java.rmi.server.hostname") == null) {
            System.setProperty("java.rmi.server.hostname", GameClient.localAddressTowards(null));
        }
        Registry created = LocateRegistry.createRegistry(port);
        lobbyImpl = new LobbyServiceImpl();
        chatImpl = new ChatServiceImpl();
        LobbyService lobbyStub = (LobbyService) UnicastRemoteObject.exportObject(lobbyImpl, 0);
        ChatService chatStub = (ChatService) UnicastRemoteObject.exportObject(chatImpl, 0);
        created.rebind(LOBBY_SERVICE, lobbyStub);
        created.rebind(CHAT_SERVICE, chatStub);
        registry = created;
        System.out.println("RMI server pokrenut na portu " + port);
    }

    /** Gasi server pokrenut u ovoj instanci (inace RMI dretve drze aplikaciju zivom). */
    public static synchronized void stop() {
        if (registry == null) {
            return;
        }
        try {
            UnicastRemoteObject.unexportObject(lobbyImpl, true);
            UnicastRemoteObject.unexportObject(chatImpl, true);
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (java.rmi.NoSuchObjectException ignored) {
            // vec ugaseno
        }
        registry = null;
    }

    public static synchronized boolean isRunningHere() {
        return registry != null;
    }

    public static void main(String[] args) throws Exception {
        GameConfig config = new ConfigRepository().load();
        int port = args.length > 0 ? Integer.parseInt(args[0]) : config.getRmiPort();
        start(port);
        System.out.println("Pritisni Ctrl+C za gasenje.");
        Thread.currentThread().join();
    }
}
