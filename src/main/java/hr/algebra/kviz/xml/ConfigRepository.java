package hr.algebra.kviz.xml;

import hr.algebra.kviz.annotation.GameDoc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;


@GameDoc("Konfiguracija igre (ime, broj pitanja, vrijeme po pitanju, RMI adresa) u XML-u preko DOM-a.")
public class ConfigRepository {

    public static final Path DEFAULT_PATH = Path.of("config", "game-config.xml");

    private record Setting(String group, Function<GameConfig, Object> getter, BiConsumer<GameConfig, String> setter) {
    }

    private static final Map<String, Setting> SETTINGS = new LinkedHashMap<>();

    static {
        SETTINGS.put("playerName", new Setting("player", GameConfig::getPlayerName, GameConfig::setPlayerName));
        SETTINGS.put("questionsPerGame", new Setting("rules", GameConfig::getQuestionsPerGame,
                (c, v) -> c.setQuestionsPerGame(Integer.parseInt(v))));
        SETTINGS.put("secondsPerQuestion", new Setting("rules", GameConfig::getSecondsPerQuestion,
                (c, v) -> c.setSecondsPerQuestion(Integer.parseInt(v))));
        SETTINGS.put("rmiHost", new Setting("network", GameConfig::getRmiHost, GameConfig::setRmiHost));
        SETTINGS.put("rmiPort", new Setting("network", GameConfig::getRmiPort,
                (c, v) -> c.setRmiPort(Integer.parseInt(v))));
        SETTINGS.put("replayDelayMillis", new Setting("replay", GameConfig::getReplayDelayMillis,
                (c, v) -> c.setReplayDelayMillis(Integer.parseInt(v))));
    }

    private final Path path;

    public ConfigRepository() {
        this(DEFAULT_PATH);
    }

    public ConfigRepository(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    /** Ucitava konfiguraciju; ako datoteka ne postoji, stvara je sa zadanim vrijednostima. */
    public GameConfig load() {
        GameConfig config = new GameConfig();
        if (!Files.exists(path)) {
            save(config);
            return config;
        }
        try {
            Document document = XmlSupport.newDocumentBuilder().parse(path.toFile());
            NodeList settings = document.getElementsByTagName("setting");
            for (int i = 0; i < settings.getLength(); i++) {
                Node node = settings.item(i);
                if (node instanceof Element element) {
                    applySetting(config, element.getAttribute("key"), element.getTextContent().trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Konfiguraciju nije moguce procitati, koriste se zadane vrijednosti: " + e.getMessage());
        }
        return config;
    }

    private void applySetting(GameConfig config, String key, String value) {
        Setting setting = SETTINGS.get(key);
        if (setting == null) {
            System.err.println("Nepoznata postavka u XML-u: " + key);
            return;
        }
        try {
            setting.setter().accept(config, value);
        } catch (NumberFormatException e) {
            System.err.println("Neispravna vrijednost za " + key + ": " + value);
        }
    }

    /** Zapisuje konfiguraciju grupiranu po sekcijama (player, rules, network, replay). */
    public void save(GameConfig config) {
        try {
            Document document = XmlSupport.newDocumentBuilder().newDocument();
            Element root = document.createElement("gameConfig");
            root.setAttribute("version", "1");
            document.appendChild(root);

            Map<String, Element> groups = new LinkedHashMap<>();
            SETTINGS.forEach((key, setting) -> {
                Element group = groups.computeIfAbsent(setting.group(), name -> {
                    Element element = document.createElement(name);
                    root.appendChild(element);
                    return element;
                });
                Element element = document.createElement("setting");
                element.setAttribute("key", key);
                element.setTextContent(String.valueOf(setting.getter().apply(config)));
                group.appendChild(element);
            });

            XmlSupport.write(document, path);
        } catch (Exception e) {
            throw new IllegalStateException("Konfiguraciju nije moguce spremiti", e);
        }
    }

    /** Sirovi sadrzaj XML datoteke (za prikaz u postavkama). */
    public String readRaw() {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            return "";
        }
    }
}
