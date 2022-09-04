package prototyp.survival.gameserver.gameserver.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.timer.Countdown;
import prototyp.survival.gameserver.gameserver.timer.Timer;

import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class StartCommand implements CommandExecutor {
    public static final TextColor GREEN = TextColor.fromHexString("#47EB38");
    public static final TextColor YELLOW = TextColor.fromHexString("#E1EB00");
    public static final TextColor RED = TextColor.fromHexString("#EB0701");
    public static final TextColor BLUE = TextColor.fromHexString("#0119EB");
    public static final TextColor GRAY = TextColor.fromHexString("#B3B3B3");
    private final GameServer gameServer;

    private int skips = 0;

    public StartCommand(GameServer gameServer) {

        this.gameServer = gameServer;

        lobbyTimer = new Timer(gameServer, 1, TimeUnit.SECONDS);
        timer = new Timer(gameServer, 7, TimeUnit.MINUTES);
        countdown = new Countdown(gameServer, 1, TimeUnit.MINUTES, 7);
        fightTimer = new Timer(gameServer, 7, TimeUnit.MINUTES);
        fightCountdown = new Countdown(gameServer, 1, TimeUnit.MINUTES, 7);
    }

    private Timer timer;
    private Timer fightTimer;
    private Timer lobbyTimer;
    private Countdown countdown;
    private Countdown fightCountdown;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equals("start") || command.getName().contains("start")) {
            if (gameServer.getState() != GameState.LOBBY) return false;

            skips = 0;
            gameServer.setState(GameState.STARTING);
            broadcastStart();
            gameServer.regenerateWorld();
            Set<Gruppe> gruppen = gameServer.getGruppes();
            gruppen.forEach(this::calSpawns);
            for (Gruppe gruppe : gruppen) {
                compassTarget(gruppe);
                pasteChunks(gruppe);
                buildSpawn(gruppe);
                setChunks(gruppe);
                gruppe.disableBeacons();
                preparePlayers(gruppe);

            }

            gameServer.setState(GameState.RUNNING);
            broadcastRun();
            countdown.add(integer -> broadcastTimeLeftToBeacons(7 - integer));
            timer.add(() -> {
                gameServer.getGruppes().forEach(Gruppe::enableBeacons);

                countdown.end();
                fightCountdown.add(integer -> broadcastTimeLeftToEnd(7 - integer));
                fightCountdown.start();
                fightTimer.add(this::endGame);
                fightTimer.start();
            });
            countdown.start();
            timer.start();


        } else if (label.equals("skip") || command.getName().contains("skip")) {
            if (gameServer.getBlocked().contains((Player) sender)) {
                sender.sendMessage("Du bist schon ausgeschieden");
                return false;
            }
            Optional<Gruppe> playerGruppeOpt = gameServer.getGruppe((Player) sender);
            if (playerGruppeOpt.isEmpty()) return false;
            Gruppe playerGruppe = playerGruppeOpt.get();
            for (Gruppe gruppe : gameServer.getGruppes()) {
                if (gruppe == playerGruppe) continue;
                for (Player player : gruppe.getPlayers()) {
                    if (!gameServer.getBlocked().contains(player)) {
                        sender.sendMessage("Es sind noch nicht alle anderen Spieler tot");
                        return false;
                    }
                }
            }
            skips++;
            if (skips >= playerGruppe.getPlayers().size()) {
                fightTimer.end();
                endGame();
            }
        }
        return false;
    }

    private void broadcastStart() {
        gameServer.getAudience().sendActionBar(Component.text("Starte die nächste Runde...", YELLOW));
    }

    private void broadcastRun() {
        gameServer.getAudience().sendActionBar(Component.join(JoinConfiguration.noSeparators(),
                Component.text("Die Runde Beginnt!!!", GREEN, TextDecoration.BOLD),
                Component.text(" Noch 7 Minuten bis die Beacons angehen", GRAY, TextDecoration.ITALIC)));
    }

    private void broadcastTimeLeftToBeacons(int minutesLeft) {
        gameServer.getAudience().sendActionBar(Component.text("Es sind noch " + minutesLeft + " Minuten bis die Beacons angehen", GRAY));
    }

    private void broadcastTimeLeftToEnd(int minutesLeft) {
        gameServer.getAudience().sendActionBar(Component.text("Es sind noch " + minutesLeft + " Minuten bis die Runde endet", minutesLeft <= 3 ? RED : GRAY));
    }

    private void broadcastEnd() {
        gameServer.getAudience().sendActionBar(Component.text("Spiel beendet", RED));
    }

    public void broadcastLobby() {
        gameServer.getAudience().sendActionBar(Component.text("In der Lobby", BLUE));
    }

    private void endGame() {
        broadcastEnd();
        fightCountdown.end();
        gameServer.setState(GameState.ENDING);
        gameServer.getGruppes().forEach(gruppe -> {
            gruppe.getPlayers().forEach(player -> {
                if (!gruppe.inside(player)) {
                    player.damage(1000000000, player);
                    gruppe.setPoints(gruppe.getPoints() - 100);
                }
            });
        });
        gameServer.getGruppes().forEach(gruppe -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("Gruppe: " + gruppe.getGroupID() + " hat " + gruppe.getPoints());
            }
        });
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setGameMode(GameMode.SPECTATOR);
        });

        lobbyTimer.add(() -> {
            gameServer.getGruppes().forEach(gruppe -> {
                gruppe.getPlayers().forEach(player -> {
                    Location location = player.getLocation().clone();
                    location.setWorld(gameServer.getLobbyWorld());
                    player.teleport(location);
                });
            });

            gameServer.getBlocked().clear();
            gameServer.setState(GameState.LOBBY);
            try {
                gameServer.copyChunks();
            } catch (WorldEditException e) {
                e.printStackTrace();
            }
            System.out.println("return to Lobby");
            broadcastLobby();
            gameServer.discardWorld();
        });
        lobbyTimer.start();
    }

    private void compassTarget(Gruppe gruppe) {
        for (Player player : gruppe.getPlayers()) {
            player.setCompassTarget(gruppe.getSpawn());
        }
    }

    private void pasteChunks(Gruppe gruppe) {
        if (gruppe.getClipboard() == null) return;

        gruppe.getSpawn().getWorld().setChunkForceLoaded((gruppe.getSpawn().getBlockX() - 16) / 16, (gruppe.getSpawn().getBlockZ() - 16) / 16, true);
        gruppe.getSpawn().getWorld().setChunkForceLoaded((gruppe.getSpawn().getBlockX()) / 16, (gruppe.getSpawn().getBlockZ() - 16) / 16, true);
        gruppe.getSpawn().getWorld().setChunkForceLoaded((gruppe.getSpawn().getBlockX()) / 16, (gruppe.getSpawn().getBlockZ()) / 16, true);
        gruppe.getSpawn().getWorld().setChunkForceLoaded((gruppe.getSpawn().getBlockX() - 16) / 16, (gruppe.getSpawn().getBlockZ()) / 16, true);

        BukkitWorld bukkitWorld = new BukkitWorld(gameServer.getGameworld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(bukkitWorld)) {
            Operation operation = new ClipboardHolder(gruppe.getClipboard())
                    .createPaste(editSession)
                    .to(BlockVector3.at(gruppe.getSpawn().getX() - 16, -64, gruppe.getSpawn().getZ() - 16))
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    private void calSpawns(Gruppe gruppe) {
        int spawnBound = 19 + gameServer.getGruppes().size() - 1;

        Random random = new Random();
        boolean toNear = false;
        Location location;
        do {
            toNear = false;
            location = new Location(gameServer.getGameworld(), (random.nextInt(spawnBound * 2) - spawnBound) * 16, 60, (random.nextInt(spawnBound * 2) - spawnBound) * 16);
            for (Gruppe gameServerGruppe : gameServer.getGruppes()) {
                if (gameServerGruppe.getSpawn() != null && gameServerGruppe.getSpawn().getWorld().equals(gameServer.getGameworld()) && gameServerGruppe.getSpawn().distance(location) < 400) {
                    toNear = true;
                    break;
                }
            }
        } while (toNear);
        if (gruppe.getSpawn() == null) {
            location = gameServer.getGameworld().getHighestBlockAt(location).getLocation();
        } else {
            location.setY(gruppe.getSpawn().getY());
        }
        System.out.println("Location:" + location);
        gruppe.setSpawn(location);
    }

    private void buildSpawn(Gruppe gruppe) {
        World world = gruppe.getSpawn().getWorld();
        world.getBlockAt(gruppe.getSpawn().clone().add(-1, 0, 0)).setType(Material.BEACON);
        world.getBlockAt(gruppe.getSpawn().clone().add(-1, 0, -1)).setType(Material.BEACON);
        world.getBlockAt(gruppe.getSpawn().clone().add(0, 0, 0)).setType(Material.BEACON);
        world.getBlockAt(gruppe.getSpawn().clone().add(0, 0, -1)).setType(Material.BEACON);

        world.getBlockAt(gruppe.getSpawn().clone().add(0, 1, 0)).setType(Material.AIR);
        world.getBlockAt(gruppe.getSpawn().clone().add(0, 2, 0)).setType(Material.AIR);
        world.getBlockAt(gruppe.getSpawn().clone().add(0, 3, 0)).setType(Material.AIR);
    }

    private void setChunks(Gruppe gruppe) {
        World world = gruppe.getSpawn().getWorld();
        Chunk chunkAt = world.getChunkAt(gruppe.getSpawn().clone().add(0, 0, 0));
        Chunk chunkAt2 = world.getChunkAt(gruppe.getSpawn().clone().add(-1, 0, 0));
        Chunk chunkAt3 = world.getChunkAt(gruppe.getSpawn().clone().add(0, 0, -1));
        Chunk chunkAt4 = world.getChunkAt(gruppe.getSpawn().clone().add(-1, 0, -1));
        gruppe.setChunks(new int[][]{new int[]{chunkAt.getX(), chunkAt.getZ()}, new int[]{chunkAt2.getX(), chunkAt2.getZ()}, new int[]{chunkAt3.getX(), chunkAt3.getZ()}, new int[]{chunkAt4.getX(), chunkAt4.getZ()}});
    }

    private void preparePlayers(Gruppe gruppe) {
        for (Player player : gruppe.getPlayers()) {
            player.teleport(gruppe.getSpawn().clone().add(0, 2, 0));
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
}
