package prototyp.survival.gameserver.gameserver.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import prototyp.survival.gameserver.gameserver.GameServer;
import prototyp.survival.gameserver.gameserver.data.GameState;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.timer.Timer;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class StartCommand implements CommandExecutor {
    private final GameServer gameServer;
    public StartCommand(GameServer gameServer) {

        this.gameServer = gameServer;

        lobbyTimer = new Timer(gameServer, 1, TimeUnit.SECONDS);
        timer = new Timer(gameServer, 7, TimeUnit.MINUTES);
        fightTimer = new Timer(gameServer, 15, TimeUnit.MINUTES);
    }
    private Timer timer;
    private Timer fightTimer;
    private Timer lobbyTimer;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        System.out.println("label:"+label);
        if (label.equals("start") || command.getName().contains("start")) {
            if (gameServer.getState() != GameState.LOBBY) return false;

            gameServer.setState(GameState.STARTING);
            Set<Gruppe> gruppen = gameServer.getGruppes();
            gruppen.forEach(this::calSpawns);
            for (Gruppe gruppe : gruppen) {
                pasteChunks(gruppe);
                buildSpawn(gruppe);
                setChunks(gruppe);
                gruppe.disableBeacons();
                preparePlayers(gruppe);
            }
            gameServer.discardOldWorld();
            gameServer.setState(GameState.RUNNING);
            System.out.println("State Running:" + gameServer.getState());
            timer.add(() -> {
                gameServer.getGruppes().forEach(Gruppe::enableBeacons);

                fightTimer.add(this::endGame);
                fightTimer.start();
            });
            timer.start();
        }else if (label.equals("skip") || command.getName().contains("skip")) {
            if (gameServer.getBlocked().contains((Player) sender)){
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
            fightTimer.end();
            endGame();
        }
        return false;
    }

    private void endGame() {
        Bukkit.broadcastMessage("Spiel beendet");
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
    }

    private void pasteChunks(Gruppe gruppe) {
        if (gruppe.getClipboard() == null) return;
        BukkitWorld bukkitWorld = new BukkitWorld(gameServer.getGameworld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(bukkitWorld)) {
            Operation operation = new ClipboardHolder(gruppe.getClipboard())
                    .createPaste(editSession)
                    .to(BlockVector3.at(gruppe.getSpawn().getX()-16, -64, gruppe.getSpawn().getZ()-16))
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    private void calSpawns(Gruppe gruppe) {
        int spawnBound = 19 + gameServer.getGruppes().size()-1;

            Random random = new Random();
            boolean toNear = false;
            Location location;
            do {
                toNear = false;
                location = new Location(gameServer.getGameworld(), (random.nextInt(spawnBound*2)-spawnBound)*16,60,(random.nextInt(spawnBound*2)-spawnBound)*16);
                for (Gruppe gameServerGruppe : gameServer.getGruppes()) {
                    if (gameServerGruppe.getSpawn() != null && gameServerGruppe.getSpawn().getWorld().equals(gameServer.getGameworld()) && gameServerGruppe.getSpawn().distance(location) < 400) {
                        toNear = true;
                        break;
                    }
                }
            }while (toNear);
            if (gruppe.getSpawn() == null){
                location = gameServer.getGameworld().getHighestBlockAt(location).getLocation();
            }else {
                location.setY(gruppe.getSpawn().getY());
            }
            System.out.println("Location:"+location);
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
        gruppe.setChunks(new int[][]{new int[] {chunkAt.getX(), chunkAt.getZ()}, new int[] {chunkAt2.getX(), chunkAt2.getZ()}, new int[] {chunkAt3.getX(), chunkAt3.getZ()}, new int[] {chunkAt4.getX(), chunkAt4.getZ()}});
    }

    private void preparePlayers(Gruppe gruppe) {
        for (Player player : gruppe.getPlayers()) {
            player.teleport(gruppe.getSpawn().clone().add(0, 2, 0));
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
}
