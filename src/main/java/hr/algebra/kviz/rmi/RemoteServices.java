package hr.algebra.kviz.rmi;

import hr.algebra.kviz.annotation.GameDoc;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.util.Hashtable;

/**
 * Dohvat udaljenih servisa preko JNDI-ja. JNDI kontekst koristi RMI registry
 * kao imenik, pa aplikacija servise trazi po imenu, a ne preko izravnog
 * RMI API-ja.
 */
@GameDoc("JNDI pristup RMI servisima za lobby (odabir suigraca) i chat.")
public class RemoteServices {

    private static final String RMI_CONTEXT_FACTORY = "com.sun.jndi.rmi.registry.RegistryContextFactory";

    private final String host;
    private final int port;

    public RemoteServices(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public LobbyService lobby() throws NamingException {
        return (LobbyService) lookup(RmiServer.LOBBY_SERVICE);
    }

    public ChatService chat() throws NamingException {
        return (ChatService) lookup(RmiServer.CHAT_SERVICE);
    }

    private Object lookup(String name) throws NamingException {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, RMI_CONTEXT_FACTORY);
        environment.put(Context.PROVIDER_URL, "rmi://" + host + ":" + port);
        Context context = new InitialContext(environment);
        try {
            return context.lookup(name);
        } finally {
            context.close();
        }
    }

    /**
     * Ako RMI server jos ne radi i adresa je lokalna, pokrece ga u ovoj
     * instanci aplikacije. Vraca true ako je server dostupan.
     */
    @GameDoc("Prva pokrenuta instanca igre postaje RMI server za ostale.")
    public boolean ensureServerRunning() {
        if (isAvailable()) {
            return true;
        }
        if (!isLocalHost()) {
            return false;
        }
        try {
            RmiServer.start(port);
            return isAvailable();
        } catch (RemoteException e) {
            // port je zauzet (npr. druga instanca ga je upravo pokrenula)
            return isAvailable();
        }
    }

    public boolean isAvailable() {
        try {
            lobby().getOpenGames();
            return true;
        } catch (NamingException | RemoteException e) {
            return false;
        }
    }

    public boolean isLocalHost() {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
