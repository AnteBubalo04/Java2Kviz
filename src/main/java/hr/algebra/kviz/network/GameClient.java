package hr.algebra.kviz.network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Gost se spaja na domacina cija je adresa dobivena iz lobbyja. */
public final class GameClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 4000;

    private GameClient() {
    }

    public static GameConnection connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            return new GameConnection(socket);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    /**
     * Pokusava redom zadane adrese (bez duplikata) dok jedna ne uspije.
     * Korisno kad domacin ima vise mreznih kartica (npr. VirtualBox adapter).
     */
    public static GameConnection connectFirstReachable(List<String> hosts, int port) throws IOException {
        Set<String> unique = new LinkedHashSet<>(hosts);
        IOException last = new IOException("Nema adrese za spajanje");
        for (String host : unique) {
            if (host == null || host.isBlank()) {
                continue;
            }
            try {
                return connect(host, port);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last;
    }

    /**
     * IP adresa ovog racunala u lokalnoj mrezi: adresa mrezne kartice preko koje
     * bi isao promet prema zadanom racunalu (UDP connect ne salje nikakve pakete).
     */
    public static String localAddressTowards(String remoteHost) {
        String target = remoteHost == null || isLoopback(remoteHost) ? "8.8.8.8" : remoteHost;
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(InetAddress.getByName(target), 9);
            InetAddress local = probe.getLocalAddress();
            if (local != null && !local.isAnyLocalAddress()) {
                return local.getHostAddress();
            }
        } catch (IOException ignored) {
            // nema mreze - vracamo localhost
        }
        return "localhost";
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || host.startsWith("127.");
    }
}
