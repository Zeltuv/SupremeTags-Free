package net.noscape.project.supremetags.handlers.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;

public class SystemChatPacketListener extends com.comphenix.protocol.events.PacketAdapter {

    public SystemChatPacketListener(Plugin plugin) {
        super(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SYSTEM_CHAT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        WrappedChatComponent chatComponent = packet.getChatComponents().readSafely(0);
        if (chatComponent != null) {
            String messageJson = chatComponent.getJson();
            try {
                JsonObject jsonObject = JsonParser.parseString(messageJson).getAsJsonObject();
                replacePlaceholdersInJson(jsonObject, player);
                String replacedJson = jsonObject.toString();
                packet.getChatComponents().write(0, WrappedChatComponent.fromJson(replacedJson));
            } catch (Exception e) {
                // Bukkit.getLogger().warning("[SupremeTags] Failed to parse chat JSON: " + e.getMessage());
            }
        }
    }

    private void replacePlaceholdersInJson(JsonElement element, Player player) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    String original = entry.getValue().getAsString();
                    String replaced = replaceTagPlaceholders(original, player);
                    obj.addProperty(entry.getKey(), replaced);
                } else {
                    replacePlaceholdersInJson(entry.getValue(), player); // Recursively go deeper
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                replacePlaceholdersInJson(item, player);
            }
        }
    }

    private String replaceTagPlaceholders(String text, Player player) {
        String activeTag = UserData.getActive(player.getUniqueId());
        String displayTag = SupremeTags.getInstance().getConfig().getString("settings.none-output");

        Tag tag = SupremeTags.getInstance().getTagManager().getTags().get(activeTag);

        if (tag != null && tag.getTag() != null) {
            displayTag = tag.getTag();
        }

        displayTag = replacePlaceholders(player, displayTag);
        displayTag = format(displayTag);

        return text
                .replace("{tag}", displayTag)
                .replace("{TAG}", displayTag)
                .replace("{supremetags_tag}", displayTag);
    }
}
