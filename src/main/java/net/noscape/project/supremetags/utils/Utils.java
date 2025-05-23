package net.noscape.project.supremetags.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.lone.itemsadder.api.CustomStack;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static Pattern p1 = Pattern.compile("\\{#([0-9A-Fa-f]{6})\\}");
    private static Pattern p2 = Pattern.compile("&#([A-Fa-f0-9]){6}");
    private static Pattern p3 = Pattern.compile("#([A-Fa-f0-9]){6}");
    private static Pattern p4 = Pattern.compile("<#([A-Fa-f0-9])>{6}");
    private static Pattern p5 = Pattern.compile("<#&([A-Fa-f0-9])>{6}");

    public static String format(String message) {
        if (isVersionLessThan("1.16")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
            return message;
        } else {

            message = ChatColor.translateAlternateColorCodes('&', message);

            // Handle hex color codes
            Matcher hexMatcher = p1.matcher(message);
            while (hexMatcher.find()) {
                message = message.replace(hexMatcher.group(), ChatColor.of(hexMatcher.group().substring(1)).toString());
            }

            hexMatcher = p2.matcher(message);
            while (hexMatcher.find()) {
                message = message.replace(hexMatcher.group(), ChatColor.of(hexMatcher.group().substring(1)).toString());
            }

            Matcher[] matchers = {p3.matcher(message), p4.matcher(message), p5.matcher(message)};
            for (Matcher matcher : matchers) {
                while (matcher.find()) {
                    String hexColor = matcher.group().replaceAll("[<#&>]", "").substring(0, 6);
                    message = message.replace(matcher.group(), ChatColor.of(hexColor).toString());
                }
            }

            message = message.replace("<black>", "§0")
                    .replace("<dark_blue>", "§1")
                    .replace("<dark_green>", "§2")
                    .replace("<dark_aqua>", "§3")
                    .replace("<dark_red>", "§4")
                    .replace("<dark_purple>", "§5")
                    .replace("<gold>", "§6")
                    .replace("<gray>", "§7")
                    .replace("<dark_gray>", "§8")
                    .replace("<blue>", "§9")
                    .replace("<green>", "§a")
                    .replace("<aqua>", "§b")
                    .replace("<red>", "§c")
                    .replace("<light_purple>", "§d")
                    .replace("<yellow>", "§e")
                    .replace("<white>", "§f")
                    .replace("<obfuscated>", "§k")
                    .replace("<bold>", "§l")
                    .replace("<strikethrough>", "§m")
                    .replace("<underlined>", "§n")
                    .replace("<italic>", "§o")
                    .replace("<reset>", "§r");

            message = message.replace("$", "$");

            return message;
        }
    }

    public static boolean isValidVersion(String version) {
        return version.matches("\\d+(\\.\\d+)*"); // Matches version strings like "1", "1.2", "1.2.3", etc.
    }

    public static boolean isVersionLessThan(String version) {
        String serverVersion = Bukkit.getVersion();
        String[] serverParts = serverVersion.split(" ")[2].split("\\.");
        String[] targetParts = version.split("\\.");

        for (int i = 0; i < Math.min(serverParts.length, targetParts.length); i++) {
            if (!isValidVersion(serverParts[i]) || !isValidVersion(targetParts[i])) {
                // Handle invalid version format
                return false;
            }

            int serverPart = Integer.parseInt(serverParts[i]);
            int targetPart = Integer.parseInt(targetParts[i]);

            if (serverPart < targetPart) {
                return true;
            } else if (serverPart > targetPart) {
                return false;
            }
        }
        return serverParts.length < targetParts.length;
    }

    public static void addPerm(OfflinePlayer player, String permission) {
        SupremeTags.getPermissions().playerAdd(null, player, permission); // null = global
    }

    public static void removePerm(OfflinePlayer player, String permission) {
        SupremeTags.getPermissions().playerRemove(null, player, permission);
    }

    public static boolean hasAmount(Player player, double cost) {
        SupremeTags instance = SupremeTags.getInstance();
        String economyType = instance.getConfig().getString("settings.economy");

        if (economyType != null) {
            if (economyType.equalsIgnoreCase("VAULT")) {
                return SupremeTags.getEconomy().has(player, cost);
            } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
                return SupremeTags.getInstance().getPpAPI().look(player.getUniqueId()) >= cost;
            } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
                return player.getLevel() >= cost;
            } else if (economyType.startsWith("COINSENGINE-")) {
                String eco_name = economyType.replace("COINSENGINE-", "");
                return CoinsEngineAPI.getBalance(player.getUniqueId(), eco_name) >= cost;
            }
        } else {
            msgPlayer(player, "&cSeems like economy in config.yml for SupremeTags is missing.. Please tell the admins of the server!");
        }

        return false;
    }

    public static void take(Player player, double cost) {
        SupremeTags instance = SupremeTags.getInstance();
        String economyType = instance.getConfig().getString("settings.economy");

        if (economyType != null) {
            if (economyType.equalsIgnoreCase("VAULT")) {
                SupremeTags.getEconomy().withdrawPlayer(player, cost);
            } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
                instance.getPpAPI().take(player.getUniqueId(), (int) cost);
            } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
                player.setLevel((int) (player.getLevel() - cost));
            } else if (economyType.startsWith("COINSENGINE-")) {
                String eco_name = economyType.replace("COINSENGINE-", "");
                CoinsEngineAPI.removeBalance(player.getUniqueId(), eco_name, cost);
            }
        } else {
            msgPlayer(player, "&cSeems like economy in config.yml for SupremeTags is missing.. Please tell the admins of the server!");
        }
    }
    public static String deformat(String str) {
        return ChatColor.stripColor(format(str));
    }

    public static void msgPlayer(Player player, String... str) {
        for (String msg : str) {
            player.sendMessage(format(msg));
        }
    }

    public static void msgPlayer(CommandSender player, String... str) {
        for (String msg : str) {
            player.sendMessage(format(msg));
        }
    }

    public static void titlePlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
    }

    public static void soundPlayer(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static List<String> color(List<String> lore){
        return lore.stream().map(Utils::format).collect(Collectors.toList());
    }

    private static Pattern rgbPat = Pattern.compile("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)");
    public static String getRGB(String msg) {
        String temp = msg;
        try {

            String status = "none";
            String r = "";
            String g = "";
            String b = "";
            Matcher match = rgbPat.matcher(msg);
            while (match.find()) {
                String color = msg.substring(match.start(), match.end());
                for (char character : msg.substring(match.start(), match.end()).toCharArray()) {
                    switch (character) {
                        case '(':
                            status = "r";
                            continue;
                        case ',':
                            switch (status) {
                                case "r":
                                    status = "g";
                                    continue;
                                case "g":
                                    status = "b";
                                    continue;
                                default:
                                    break;
                            }
                        default:
                            switch (status) {
                                case "r":
                                    r = r + character;
                                    continue;
                                case "g":
                                    g = g + character;
                                    continue;
                                case "b":
                                    b = b + character;
                                    continue;
                            }
                            break;
                    }


                }
                b = b.replace(")", "");
                Color col = new Color(Integer.parseInt(r), Integer.parseInt(g), Integer.parseInt(b));
                temp = temp.replaceFirst("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)", ChatColor.of(col) + "");
                r = "";
                g = "";
                b = "";
                status = "none";
            }
        } catch (Exception e) {
            return msg;
        }
        return temp;
    }

    public static ItemStack getItemWithIA(String id) {
        if (CustomStack.isInRegistry(id)) {
            CustomStack stack = CustomStack.getInstance(id);
            if (stack != null) {
                return stack.getItemStack();
            }
        }

        return null;
    }

    public static String replacePlaceholders(Player user, String base) {
        if (SupremeTags.getInstance().getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return base;
        }

        return PlaceholderAPI.setPlaceholders(user, base);
    }

    public static ItemStack createSkull(String baseheadtexture64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

        if (baseheadtexture64 == null || baseheadtexture64.isEmpty()) {
            return skull;
        }

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setOwningPlayer(null);

        GameProfile profile = new GameProfile(UUID.randomUUID(), "Dummy");
        profile.getProperties().put("textures", new Property("textures", baseheadtexture64));

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        skull.setItemMeta(skullMeta);
        return skull;
    }

}