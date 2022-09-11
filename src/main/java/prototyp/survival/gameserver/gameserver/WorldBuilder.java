package prototyp.survival.gameserver.gameserver;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import prototyp.survival.gameserver.gameserver.command.StartCommand;
import prototyp.survival.gameserver.gameserver.data.Gruppe;
import prototyp.survival.gameserver.gameserver.timer.StepTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldBuilder extends StepTimer {


    public WorldBuilder(GameServer gameServer, Runnable callback) {
        super(gameServer);
        List<Runnable> runnables = new ArrayList<>();
        runnables.add(genWorld());
//        for (int x = 0; x < 5; x++) {
//            for (int z = 0; z < 5; z++) {
//                runnables.add(genChunk(x*16, z*16));
//            }
//        }
        runnables.add(purgeSpawnPositions());
        gameServer.getGruppes().forEach(gruppe -> runnables.add(calculateSpawnPosition(gruppe)));
        for (Gruppe gruppe : gameServer.getGruppes()) {
            runnables.add(generateCompassTarget(gruppe));
            runnables.add(pasteChunks(gruppe));
            runnables.add(buildSpawn(gruppe));
            runnables.add(setChunks(gruppe));
            runnables.add(prepareGruppe(gruppe));
        }
        runnables.add(callback);
        setRunnables(runnables.toArray(new Runnable[0]));
    }


    private Runnable genWorld() {
        return gameServer::regenerateWorld;
    }

    private Runnable genChunk(int x, int z) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("Lade Chunk " + x + " " + z, StartCommand.YELLOW));
            gameServer.getGameworld().getChunkAt(gameServer.getGameworld().getSpawnLocation().getBlockX()+x, gameServer.getGameworld().getSpawnLocation().getBlockZ()+z).load(true);
        };
    }

    private Runnable purgeSpawnPositions() {
        return () -> {
            for (Gruppe gruppe : gameServer.getGruppes()) {
                gruppe.setSpawn(null);
            }
        };
    }

    private Runnable calculateSpawnPosition(Gruppe gruppe) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("Berechne Spawn Position(" + gruppe.getGroupID() + ")...", StartCommand.YELLOW));

            int spawnBound = 19 + gameServer.getGruppes().size() - 1;

            Random random = new Random();
            boolean toNear = false;
            Location location;
            int runs = 0;
            long m1 = System.currentTimeMillis();
            long m3;
            do {
                toNear = false;
                location = gameServer.getGameworld().getSpawnLocation().clone().add((random.nextInt(spawnBound * 2) - spawnBound) * 16, 0, (random.nextInt(spawnBound * 2) - spawnBound) * 16);
                location.setY(60);
                long m2 = System.currentTimeMillis();
                System.out.println("m2-m1:" + (m2 - m1));
                for (Gruppe gameServerGruppe : gameServer.getGruppes()) {
                    if (gameServerGruppe.getSpawn() != null && gameServerGruppe.getSpawn().getWorld().getUID().equals(gameServer.getGameworld().getUID()) && gameServerGruppe.getSpawn().distanceSquared(location) < 90000) {
                        toNear = true;
                        break;
                    }
                }
                m3 = System.currentTimeMillis();
                System.out.println("m3-m2:" + (m3 - m2));
                runs++;
            } while (toNear);
            if (gruppe.getSpawn() == null) {
                location.setY(gameServer.getGameworld().getHighestBlockYAt(location));
            } else {
                location.setY(gruppe.getSpawn().getY());
            }
            long m4 = System.currentTimeMillis();
            System.out.println("m4-m3:" + (m4 - m3));
            System.out.println("Location:" + location);
            System.out.println("runs:" + runs);
            gruppe.setSpawn(location);
        };
    }

    private Runnable generateCompassTarget(Gruppe gruppe) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("Generiere Kompass Ziel(" + gruppe.getGroupID() + ")...", StartCommand.YELLOW));

            for (Player player : gruppe.getPlayers()) {
                player.setCompassTarget(gruppe.getSpawn());
            }
        };
    }

    private Runnable pasteChunks(Gruppe gruppe) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("platziere Chunks(" + gruppe.getGroupID() + ")...", StartCommand.YELLOW));

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
        };
    }

    private Runnable buildSpawn(Gruppe gruppe) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("platziere Spawn(" + gruppe.getGroupID() + ")...", StartCommand.YELLOW));
            World world = gruppe.getSpawn().getWorld();
            world.getBlockAt(gruppe.getSpawn().clone().add(-1, 0, 0)).setType(Material.BEACON);
            world.getBlockAt(gruppe.getSpawn().clone().add(-1, 0, -1)).setType(Material.BEACON);
            world.getBlockAt(gruppe.getSpawn().clone().add(0, 0, 0)).setType(Material.BEACON);
            world.getBlockAt(gruppe.getSpawn().clone().add(0, 0, -1)).setType(Material.BEACON);

            world.getBlockAt(gruppe.getSpawn().clone().add(0, 1, 0)).setType(Material.AIR);
            world.getBlockAt(gruppe.getSpawn().clone().add(0, 2, 0)).setType(Material.AIR);
            world.getBlockAt(gruppe.getSpawn().clone().add(0, 3, 0)).setType(Material.AIR);
        };
    }

    private Runnable setChunks(Gruppe gruppe) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("setze Chunks(" + gruppe.getGroupID() + ")...", StartCommand.YELLOW));
            World world = gruppe.getSpawn().getWorld();
            Chunk chunkAt = world.getChunkAt(gruppe.getSpawn().clone().add(0, 0, 0));
            Chunk chunkAt2 = world.getChunkAt(gruppe.getSpawn().clone().add(-1, 0, 0));
            Chunk chunkAt3 = world.getChunkAt(gruppe.getSpawn().clone().add(0, 0, -1));
            Chunk chunkAt4 = world.getChunkAt(gruppe.getSpawn().clone().add(-1, 0, -1));
            gruppe.setChunks(new int[][]{new int[]{chunkAt.getX(), chunkAt.getZ()}, new int[]{chunkAt2.getX(), chunkAt2.getZ()}, new int[]{chunkAt3.getX(), chunkAt3.getZ()}, new int[]{chunkAt4.getX(), chunkAt4.getZ()}});
        };
    }

    private Runnable prepareGruppe(Gruppe gruppe) {
        return () -> {
            gameServer.getAudience().sendActionBar(Component.text("bereite Gruppe vor(" + gruppe.getGroupID() + ")...", StartCommand.YELLOW));
            gruppe.disableBeacons();
            for (Player player : gruppe.getPlayers()) {
                player.teleport(gruppe.getSpawn().clone().add(0, 2, 0));
                player.setGameMode(GameMode.SURVIVAL);
            }
        };
    }


}
