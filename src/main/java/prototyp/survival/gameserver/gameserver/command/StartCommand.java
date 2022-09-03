package prototyp.survival.gameserver.gameserver.command;

import com.sk89q.worldedit.WorldEditException;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.timer.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class StartCommand implements CommandExecutor {
    private final GameServer gameServer;
    public StartCommand(GameServer gameServer) {

        this.gameServer = gameServer;

        lobbyTimer = new Timer(gameServer, 30, TimeUnit.SECONDS);
        timer = new Timer(gameServer, 7, TimeUnit.MINUTES);
        fightTimer = new Timer(gameServer, 15, TimeUnit.MINUTES);
    }
    private Timer timer;
    private Timer fightTimer;
    private Timer lobbyTimer;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (gameServer.getState() != GameState.LOBBY) return false;

        gameServer.setState(GameState.STARTING);
        Set<Gruppe> gruppen = gameServer.getGruppes();
        gruppen.forEach(this::calSpawns);
        for (Gruppe gruppe : gruppen) {
            buildSpawn(gruppe);
            setChunks(gruppe);
            gruppe.disableBeacons();
            preparePlayers(gruppe);
        }

        gameServer.setState(GameState.RUNNING);
        System.out.println("State Running:"+gameServer.getState());
        timer.add(() -> {
            gameServer.getGruppes().forEach(Gruppe::enableBeacons);

            fightTimer.add(() -> {
                gameServer.setState(GameState.ENDING);
                // TODO: Chunk speichern
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
                    gameServer.getBlocked().clear();
                    gameServer.setState(GameState.LOBBY);
                    try {
                        gameServer.zurNeuenWelt();
                    } catch (WorldEditException e) {
                       e.printStackTrace();
                    }
                    System.out.println("return to Lobby");
                });
                lobbyTimer.start();
            });
            fightTimer.start();
        });
        timer.start();
        return false;
    }

    private void calSpawns(Gruppe gruppe) {
        if (gruppe.getSpawn() == null) {
            Random random = new Random();
            boolean toNear = false;
            Location location;
            do {
                location = new Location(gameServer.getGameworld(), random.nextInt(19)*16,60,random.nextInt(19)*16);
                for (Gruppe gameServerGruppe : gameServer.getGruppes()) {
                    if (gameServerGruppe.getSpawn() != null && gameServerGruppe.getSpawn().distance(location) < 200) {
                        toNear = true;
                        break;
                    }
                }
            }while (toNear);
            System.out.println("Location:"+location);
            gruppe.setSpawn(location);
        }
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
        gruppe.setChunks(new int[][]{new int[] {chunkAt.getX(), chunkAt.getZ()}, new int[] {chunkAt2.getX(), chunkAt2.getZ()}, new int[] {chunkAt3.getX(), chunkAt3.getZ()}, new int[] {chunkAt4.getX(), chunkAt4.getZ()}});
    }

    private void preparePlayers(Gruppe gruppe) {
        for (Player player : gruppe.getPlayers()) {
            player.teleport(gruppe.getSpawn().clone().add(0, 2, 0));
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
}
