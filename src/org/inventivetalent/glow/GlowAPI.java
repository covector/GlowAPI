package org.inventivetalent.glow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class GlowAPI implements Listener {
    private static final Map<UUID, GlowData> dataMap = new HashMap<>();

    private static final Class<?> CHAT_FORMATTING;

    static {
        try {
            CHAT_FORMATTING = Class.forName("net.minecraft.EnumChatFormat");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //Options
    /**
     * Default name-tag visibility (always, hideForOtherTeams, hideForOwnTeam, never)
     */
    public static String TEAM_TAG_VISIBILITY = "always";
    /**
     * Default push behaviour (always, pushOtherTeams, pushOwnTeam, never)
     */
    public static String TEAM_PUSH = "always";

    /**
     * Set the glowing-color of an entity
     *
     * @param entity        {@link Entity} to update
     * @param color         {@link org.inventivetalent.glow.GlowAPI.Color} of the glow, or <code>null</code> to stop glowing
     * @param tagVisibility visibility of the name-tag (always, hideForOtherTeams, hideForOwnTeam, never)
     * @param push          push behaviour (always, pushOtherTeams, pushOwnTeam, never)
     * @param receiver      {@link Player} that will see the update
     */
    public static void setGlowing(Entity entity, Color color, String tagVisibility, String push, Player receiver) {
        if (receiver == null) return;

        boolean glowing = color != null;
        if (entity == null) {
            glowing = false;
        }
        if (entity instanceof OfflinePlayer) {
            if (!((OfflinePlayer) entity).isOnline()) {
                glowing = false;
            }
        }

        boolean wasGlowing = dataMap.containsKey(entity != null ? entity.getUniqueId() : null);
        GlowData glowData;
        if (wasGlowing && entity != null) {glowData = dataMap.get(entity.getUniqueId());} else {glowData = new GlowData();}

        Color oldColor = wasGlowing ? glowData.colorMap.get(receiver.getUniqueId()) : null;

        if (glowing) {
            glowData.colorMap.put(receiver.getUniqueId(), color);
        } else {
            glowData.colorMap.remove(receiver.getUniqueId());
        }
        if (glowData.colorMap.isEmpty()) {
            dataMap.remove(entity != null ? entity.getUniqueId() : null);
        } else {
            if (entity != null) {
                dataMap.put(entity.getUniqueId(), glowData);
            }
        }

        if (color != null && oldColor == color) return;
        if (entity == null) return;
        if (entity instanceof OfflinePlayer) {
            if (!((OfflinePlayer) entity).isOnline()) return;
        }
        if (!receiver.isOnline()) return;

        entity.setGlowing(true);
        if (oldColor != null && oldColor != Color.NONE/*We never add to NONE, so no need to remove*/) {
            sendTeamPacket(entity, oldColor/*use the old color to remove the player from its team*/, false, false, tagVisibility, push, receiver);
        }
        if (glowing) {
            sendTeamPacket(entity, color, false, color != Color.NONE, tagVisibility, push, receiver);
        }
    }

    /**
     * Set the glowing-color of an entity
     *
     * @param entity   {@link Entity} to update
     * @param color    {@link org.inventivetalent.glow.GlowAPI.Color} of the glow, or <code>null</code> to stop glowing
     * @param receiver {@link Player} that will see the update
     */
    public static void setGlowing(Entity entity, Color color, Player receiver) {
        setGlowing(entity, color, "always", "always", receiver);
    }

    /**
     * Set the glowing-color of an entity
     *
     * @param entity   {@link Entity} to update
     * @param glowing  whether the entity is glowing or not
     * @param receiver {@link Player} that will see the update
     * @see #setGlowing(Entity, Color, Player)
     */
    public static void setGlowing(Entity entity, boolean glowing, Player receiver) {
        setGlowing(entity, glowing ? Color.NONE : null, receiver);
    }

    /**
     * Set the glowing-color of an entity
     *
     * @param entity    {@link Entity} to update
     * @param glowing   whether the entity is glowing or not
     * @param receivers Collection of {@link Player}s that will see the update
     * @see #setGlowing(Entity, Color, Player)
     */
    @SuppressWarnings("unused")
    public static void setGlowing(Entity entity, boolean glowing, Collection<? extends Player> receivers) {
        for (Player receiver : receivers) {
            setGlowing(entity, glowing, receiver);
        }
    }

    /**
     * Set the glowing-color of an entity
     *
     * @param entity    {@link Entity} to update
     * @param color     {@link org.inventivetalent.glow.GlowAPI.Color} of the glow, or <code>null</code> to stop glowing
     * @param receivers Collection of {@link Player}s that will see the update
     */
    public static void setGlowing(Entity entity, Color color, Collection<? extends Player> receivers) {
        for (Player receiver : receivers) {
            setGlowing(entity, color, receiver);
        }
    }

    /**
     * Set the glowing-color of an entity
     *
     * @param entities Collection of {@link Entity} to update
     * @param color    {@link org.inventivetalent.glow.GlowAPI.Color} of the glow, or <code>null</code> to stop glowing
     * @param receiver {@link Player} that will see the update
     */
    @SuppressWarnings("unused")
    public static void setGlowing(Collection<? extends Entity> entities, Color color, Player receiver) {
        for (Entity entity : entities) {
            setGlowing(entity, color, receiver);
        }
    }

    /**
     * Set the glowing-color of an entity
     *
     * @param entities  Collection of {@link Entity} to update
     * @param color     {@link org.inventivetalent.glow.GlowAPI.Color} of the glow, or <code>null</code> to stop glowing
     * @param receivers Collection of {@link Player}s that will see the update
     */
    @SuppressWarnings("unused")
    public static void setGlowing(Collection<? extends Entity> entities, Color color, Collection<? extends Player> receivers) {
        for (Entity entity : entities) {
            setGlowing(entity, color, receivers);
        }
    }

    /**
     * Check if an entity is glowing
     *
     * @param entity   {@link Entity} to check
     * @param receiver {@link Player} receiver to check (as used in the setGlowing methods)
     * @return <code>true</code> if the entity appears glowing to the player
     */
    public static boolean isGlowing(Entity entity, Player receiver) {
        return getGlowColor(entity, receiver) != null;
    }

    @SuppressWarnings("unused")
    private static boolean isGlowing(UUID entity, Player receiver) {
        return getGlowColor(entity, receiver) != null;
    }

    /**
     * Checks if an entity is glowing
     *
     * @param entity    {@link Entity} to check
     * @param receivers Collection of {@link Player} receivers to check
     * @param checkAll  if <code>true</code>, this only returns <code>true</code> if the entity is glowing for all receivers; if <code>false</code> this returns <code>true</code> if the entity is glowing for any of the receivers
     * @return <code>true</code> if the entity appears glowing to the players
     */
    @SuppressWarnings("unused")
    public static boolean isGlowing(Entity entity, Collection<? extends Player> receivers, boolean checkAll) {
        if (checkAll) {
            boolean glowing = true;
            for (Player receiver : receivers) {
                if (!isGlowing(entity, receiver)) {
                    glowing = false;
                }
            }
            return glowing;
        } else {
            for (Player receiver : receivers) {
                if (isGlowing(entity, receiver)) return true;
            }
        }
        return false;
    }

    /**
     * Get the glow-color of an entity
     *
     * @param entity   {@link Entity} to get the color for
     * @param receiver {@link Player} receiver of the color (as used in the setGlowing methods)
     * @return the {@link org.inventivetalent.glow.GlowAPI.Color}, or <code>null</code> if the entity doesn't appear glowing to the player
     */
    public static Color getGlowColor(Entity entity, Player receiver) {
        return getGlowColor(entity.getUniqueId(), receiver);
    }

    private static Color getGlowColor(UUID entityUniqueId, Player receiver) {
        if (!dataMap.containsKey(entityUniqueId)) return null;
        GlowData data = dataMap.get(entityUniqueId);
        return data.colorMap.get(receiver.getUniqueId());
    }

    /**
     * Initializes the teams for a player
     *
     * @param receiver      {@link Player} receiver
     * @param tagVisibility visibility of the name-tag (always, hideForOtherTeams, hideForOwnTeam, never)
     * @param push          push behaviour (always, pushOtherTeams, pushOwnTeam, never)
     */
    public static void initTeam(Player receiver, String tagVisibility, String push) {
        for (GlowAPI.Color color : GlowAPI.Color.values()) {
            GlowAPI.sendTeamPacket(null, color, true, false, tagVisibility, push, receiver);
        }
    }

    /**
     * Initializes the teams for a player
     *
     * @param receiver {@link Player} receiver
     */
    public static void initTeam(Player receiver) {
        initTeam(receiver, TEAM_TAG_VISIBILITY, TEAM_PUSH);
    }

    protected static void sendTeamPacket(Entity entity, Color color, boolean createNewTeam/*If true, we don't add any entities*/, boolean addEntity/*true->add the entity, false->remove the entity*/, String tagVisibility, String push, Player receiver) {
        final int mode = (createNewTeam ? 0 : addEntity ? 3 : 4); // Mode (0 = create, 3 = add entity, 4 = remove entity)

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getIntegers().write(0, mode);
        packet.getStrings().write(0, color.getTeamName());

        if (createNewTeam) {
            InternalStructure team = packet.getOptionalStructures().read(0).orElse(null);
            assert team != null;

            // Display name
            team.getChatComponents().write(0, WrappedChatComponent.fromLegacyText(color.getTeamName()));
            // Prefix
            team.getChatComponents().write(1, WrappedChatComponent.fromLegacyText("\u00a7" + color.colorCode));
            // Name tag visibility rule
            team.getStrings().write(0, tagVisibility);
            // Collision rule
            team.getStrings().write(1, push);
            // Color
            team.getEnumModifier(ChatColor.class, CHAT_FORMATTING).write(0, color.chatColor);

            packet.getOptionalStructures().write(0, Optional.of(team));
        } else {
            List<String> entitiesList;
            if (entity instanceof OfflinePlayer) { //Players still use the name...
                entitiesList = List.of(entity.getName());
            } else {
                entitiesList = List.of(entity.getUniqueId().toString());
            }
            packet.getSpecificModifier(Collection.class).write(0, entitiesList);
        }

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Team Colors
     */
    public enum Color {
        BLACK("0"),
        DARK_BLUE("1"),
        DARK_GREEN("2"),
        DARK_AQUA("3"),
        DARK_RED("4"),
        DARK_PURPLE("5"),
        GOLD("6"),
        GRAY("7"),
        DARK_GRAY("8"),
        BLUE("9"),
        GREEN("a"),
        AQUA("b"),
        RED("c"),
        PURPLE("d"),
        YELLOW("e"),
        WHITE("f"),
        NONE("r");

        final ChatColor chatColor;
        final String colorCode;

        Color(String colorCode) {
            this.chatColor = ChatColor.getByChar(colorCode);
            this.colorCode = colorCode;
        }

        String getTeamName() {
            String name = String.format("GAPI#%s", name());
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            return name;
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        //Initialize the teams
        GlowAPI.initTeam(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        for (Player receiver : Bukkit.getOnlinePlayers()) {
            if (GlowAPI.isGlowing(event.getPlayer(), receiver)) {
                GlowAPI.setGlowing(event.getPlayer(), null, receiver);
            }
        }
    }
}
