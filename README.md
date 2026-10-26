# Java2 Kviz

Kviz za jednog ili dva igrača (mrežna igra), projektni zadatak iz kolegija **Programiranje u Javi 2**.

Autor: **Ante Bubalo**

## Pokretanje

Potrebno: **JDK 17 ili noviji** i Maven (IntelliJ ima ugrađen Maven).

**IntelliJ IDEA:** `File → Open` → odaberi mapu `Java2Kviz` (ili `pom.xml`) → *Open as Project*.
Pričekaj da Maven skine ovisnosti, zatim pokreni `hr.algebra.kviz.KvizApplication` (zelena strelica pokraj `main`).
Može i iz Maven panela: `Plugins → javafx → javafx:run`.

**Iz terminala:** `mvn clean javafx:run`

### Mrežna igra na jednom računalu
1. Pokreni aplikaciju dvaput. Za drugu instancu u IntelliJ-u uključi
   `Run → Edit Configurations → Modify options → Allow multiple instances`.
2. Prva pokrenuta instanca sama pokreće RMI server (lobby i chat) na portu iz postavki (zadano 1099).
3. U prvoj instanci upiši nadimak i klikni **Otvori igru**.
4. U drugoj instanci upiši **drugi** nadimak, odaberi igru s popisa i klikni **Pridruži se**.

### Mrežna igra na dva računala
1. Na računalu A pokreni igru (ono postaje RMI server) i otvori igru.
2. Na računalu B u **Postavkama** upiši IP adresu računala A kao *RMI server* i spremi.
3. Na B odaberi igru i pridruži se. Ako Windows pita za vatrozid, dopusti pristup (privatna mreža).

RMI server se može pokrenuti i samostalno: klasa `hr.algebra.kviz.rmi.RmiServer`.

### Datoteke koje igra stvara (u mapi projekta)
| Datoteka | Sadržaj |
|---|---|
| `config/game-config.xml` | postavke igre (XML, DOM) |
| `replay/last-game.xml` | potezi zadnje partije (XML, zapisuje se nakon svakog poteza) |
| `saves/savegame.ser` | spremljena igra (serijalizacija) |
| `data/leaderboard.ser` | ljestvica (serijalizacija) |
| `docs/dokumentacija.html` | dokumentacija generirana Reflectionom |

## Što je gdje (po ishodima učenja)

### I1–I3: game engine, hijerarhija klasa, JavaFX, MVC, obavještavanje o promjenama
- **Hijerarhija podatkovnih klasa** (`model`):
  - `Question` (apstraktna, `sealed`) → `MultipleChoiceQuestion`, `TrueFalseQuestion`
  - `Player` (apstraktna, `sealed`) → `LocalPlayer`, `RemotePlayer`
  - `GameMessage` (`sealed interface`) → `HelloMessage`, `StartGameMessage`, `AnswerMessage`, `ByeMessage` (recordi)
  - recordi `Answer`, `GameState`, `LeaderboardEntry`, `PlayerInfo`, `ChatMessage`
- **Game engine**: `engine/GameEngine` sadrži sva pravila igre: tijek pitanja, odbrojavanje, bodovanje
  (`ScoreCalculator`), čekanje protivnika, pauzu i snimku stanja. Ne zna ništa o ekranima.
- **MVC**: model = `model` + `engine`, view = FXML datoteke u `resources/.../view`, controller = `controller`.
  Kontroleri ne sadrže pravila igre, samo prosljeđuju klikove engineu.
- **Automatsko obavještavanje**: engine izlaže stanje kao JavaFX `Property` (`currentQuestion`, `secondsLeft`,
  `localScore`, `phase`...). `GameController` ih veže **bindingom** (`bind`, `Bindings.createStringBinding`,
  `Bindings.when`), a koristi i **listenere** (`addListener`). Ljestvica je `ObservableList` vezana na `TableView`.
  U lobbyju je `disableProperty` gumba vezan na stanje forme.

### I4: višedretvenost
- `javafx.concurrent.Task`: asinkrono učitavanje pitanja (`QuestionService`), spajanje na mrežu, generiranje dokumentacije, učitavanje replaya
- `ScheduledService`: osvježavanje liste otvorenih igara (lobby) i chata
- `ScheduledExecutorService`: odbrojavanje vremena za odgovor u `GameEngine`; rezultat se vraća u JavaFX dretvu (`Platform::runLater`)
- `CompletableFuture`: Save/Load, ljestvica, prihvat protivnika (`GameHost`)
- `ExecutorService` s daemon dretvama: `service/AppExecutors`; jedna sekvencijalna dretva čuva redoslijed zapisa u replay
- sinkronizacija: `synchronized` slanje na socketu i zapisi u XML, `volatile` handleri, `ConcurrentHashMap` i `CopyOnWriteArrayList` u RMI servisima

### I5: serijalizacija i dokumentacija
- **Save/Load**: tipka *Spremi* u igri za jednog igrača → `SaveGameService` serijalizira `GameState`.
  Izbornik *Učitaj igru* deserijalizira stanje i nastavlja od prvog neodgovorenog pitanja (`GameEngine.restore`).
- **Dokumentacija**: `docs/DocumentationGenerator` preko Reflection API-ja čita sve klase, polja, konstruktore,
  metode, parametre, iznimke, `sealed`/`permits` i komponente recorda.
  Poslovna logika označena je vlastitom anotacijom `@GameDoc` (`RetentionPolicy.RUNTIME`) i istaknuta u HTML-u.

### I6: mreža, JNDI i RMI
- **TCP socketi**: `network/GameHost` (`ServerSocket`), `GameClient`, `GameConnection` (posebna dretva čita poruke).
  Potezi se šalju kao serijalizirani `AnswerMessage` objekti.
- **RMI**: `rmi/LobbyService` (odabir suigrača: domaćin objavi igru, gost bira s popisa) i `rmi/ChatService` (chat tijekom igre).
- **JNDI**: `rmi/RemoteServices` servise traži preko JNDI konteksta (`InitialContext` s `RegistryContextFactory`,
  `PROVIDER_URL = rmi://host:port`), a ne izravno preko RMI registryja.

### I7: XML (SAX i DOM)
- **Konfiguracija igre** u `config/game-config.xml`: `xml/ConfigRepository` (DOM). Čitanje je dinamičko:
  postavke se prepoznaju po atributu `key` preko tablice, a nepoznati ključevi se preskaču. Uređuje se na ekranu *Postavke*.
- **Replay**: `xml/ReplayRecorder` (DOM) dopisuje potez u `replay/last-game.xml` **nakon svakog odgovora**,
  a `xml/ReplayReader` (SAX) ga čita za prikaz.
- **Baza pitanja** `questions.xml` čita se SAX parserom (`xml/QuestionXmlLoader`).

## Pravila igre
- Točan odgovor nosi 500 bodova i do 500 bodova bonusa za brzinu. Netočan ili zakašnjeli odgovor nosi 0 bodova.
- U mrežnoj igri sljedeće pitanje dolazi tek kad oba igrača odgovore ili im istekne vrijeme.
- Pauza i spremanje postoje samo u igri za jednog igrača, jer u mrežnoj igri vrijeme teče za oba igrača.
