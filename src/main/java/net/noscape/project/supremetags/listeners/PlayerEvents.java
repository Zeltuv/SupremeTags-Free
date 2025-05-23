package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.handlers.*;
import net.noscape.project.supremetags.storage.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public class PlayerEvents implements Listener {

    private final Map<String, Tag> tags;

    public PlayerEvents() {
        tags = SupremeTags.getInstance().getTagManager().getTags();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UserData.createPlayer(player);

        String activeTag = UserData.getActive(player.getUniqueId());
        String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");
        boolean forcedTag = SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag");

        // Set default if forced tag is on and active is "None"
        if (forcedTag && activeTag.equalsIgnoreCase("None")) {
            UserData.setActive(player, defaultTag);
            activeTag = defaultTag;
        }

        // Check if active tag exists
        if (!activeTag.equalsIgnoreCase("None")) {
            if (!tags.containsKey(activeTag)) {
                UserData.setActive(player, defaultTag);
            } else {
                Tag tag = SupremeTags.getInstance().getTagManager().getTag(activeTag);
                if (tag != null && !player.hasPermission(tag.getPermission())) {
                    UserData.setActive(player, defaultTag);
                }
            }
        }

        // Update check for OPs
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.update-check") && player.isOp()) {
            new UpdateChecker(SupremeTags.getInstance()).getVersionAsync(version -> {
                if (!SupremeTags.getInstance().getDescription().getVersion().equals(version)) {
                    msgPlayer(player, "&6&lSupremeTags &8&l> &7An update is available! &b" + version,
                            "&eDownload at &bhttps://www.spigotmc.org/resources/103140/");
                }
            });
        }
    }


    public Map<String, Tag> getTags() {
        return tags;
    }
}