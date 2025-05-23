package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.*;

import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class TagManager {

    private final Map<String, Tag> tags = new HashMap<>();
    private final Map<Integer, String> dataItem = new HashMap<>();

    private boolean isCost;

    private final String reload = SupremeTags.getInstance().getConfig().getString("messages.reload");
    private final String noperm = SupremeTags.getInstance().getConfig().getString("messages.no-permission");
    private final String notags = SupremeTags.getInstance().getConfig().getString("messages.no-tags");
    private final String commanddisabled = SupremeTags.getInstance().getConfig().getString("messages.tag-command-disabled");
    private final String invalidtag = SupremeTags.getInstance().getConfig().getString("messages.invalid-tag");
    private final String validtag = SupremeTags.getInstance().getConfig().getString("messages.valid-tag");
    private final String invalidcategory = SupremeTags.getInstance().getConfig().getString("messages.invalid-category");

    public TagManager(boolean isCost) {
        this.sort();
        this.isCost = isCost;
    }

    public void createTag(Player player, String identifier, String tag_string, String description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            int orderID = tags.size() + 1;

            Tag tag = new Tag(identifier, tag_string, default_category, permission, description, cost, orderID);
            tags.put(identifier, tag);

            getTagConfig().set("tags." + identifier + ".tag", tag_string);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".display-item", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".cost", cost);
            saveTagConfig();

            msgPlayer(player, "&8[&6&lTAG&8] &7New tag created &6" + identifier + " &f- " + tag_string);
        } else {
            msgPlayer(player, validtag);
        }
    }

    public void createTag(CommandSender player, String identifier, String tag_string, String description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            int orderID = tags.size() + 1;

            Tag tag = new Tag(identifier, tag_string, default_category, permission, description, cost, orderID);
            tags.put(identifier, tag);

            getTagConfig().set("tags." + identifier + ".tag", tag_string);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".display-item", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".cost", cost);
            saveTagConfig();

            msgPlayer(player, "&8[&6&lTAG&8] &7New tag created &6" + identifier + " &f- " + tag_string);
        } else {
            msgPlayer(player, validtag);
        }
    }

    public void createTag(String identifier, String tag_string, String description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            int orderID = tags.size() + 1;

            Tag tag = new Tag(identifier, tag_string, default_category, permission, description, cost, orderID);
            tags.put(identifier, tag);

            getTagConfig().set("tags." + identifier + ".tag", tag_string);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".display-item", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".cost", cost);
            saveTagConfig();
        }
    }

    public void createTag(String identifier, String material, String tag_string, String description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            int orderID = tags.size() + 1;

            Tag tag = new Tag(identifier, tag_string, default_category, permission, description, cost, orderID);
            tags.put(identifier, tag);

            getTagConfig().set("tags." + identifier + ".tag", tag_string);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".display-item", material);
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".cost", cost);
            saveTagConfig();
        }
    }

    public void deleteTag(CommandSender player, String identifier) {
        if (tags.containsKey(identifier)) {
            tags.remove(identifier);

            try {
                getTagConfig().set("tags." + identifier, null);
                saveTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &7Tag &6" + identifier + " &7is now deleted!");
        } else {
            msgPlayer(player, invalidtag);
        }
    }

    public void deleteTag(Player player, String identifier) {
        if (tags.containsKey(identifier)) {
            tags.remove(identifier);

            try {
                getTagConfig().set("tags." + identifier, null);
                saveTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &7Tag &6" + identifier + " &7is now deleted!");
        } else {
            msgPlayer(player, invalidtag);
        }
    }

    public void loadTags() {
        ConfigurationSection tagsSection = Objects.requireNonNull(getTagConfig().getConfigurationSection("tags"));
        int count = 0;

        for (String identifier : tagsSection.getKeys(false)) {
            ConfigurationSection tagSection = tagsSection.getConfigurationSection(identifier);
            if (tagSection == null) continue;

            count++;

            String tag = tagSection.getString("tag", "");
            String category = tagSection.getString("category", "");
            String description = tagSection.getString("description", "");
            String permission = tagSection.getString("permission", "none");
            double cost = tagSection.getDouble("cost", 0.0);
            int order = tagSection.getInt("order", count);

            Tag t = new Tag(identifier, tag, category, permission, description, cost, order);
            tags.put(identifier, t);
        }

        Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + count + " tag(s) successfully.");
    }

    public Tag getTag(String tag) {
        Tag t = null;
        for (Tag tg : tags.values()) {
            if (tg.getIdentifier().equalsIgnoreCase(tag)) {
                t = tg;
            }
        }

        return t;
    }

    public void unloadTags() {
        if (!tags.isEmpty()) {
            tags.clear();
        }
    }

    public Map<String, Tag> getTags() {
        return tags;
    }
    public Map<Integer, String> getDataItem() { return dataItem; }

    public void setTag(Player player, String identifier, String tag) {
        if (tags.containsKey(identifier)) {
            Tag t = tags.get(identifier);
            t.setTag(tag);

            try {
                getTagConfig().set("tags." + identifier + ".tag", t.getTag());
                SupremeTags.getInstance().saveConfig();
                SupremeTags.getInstance().reloadConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &6" + t.getIdentifier() + "'s tag &7changed to " + t.getTag());
        } else {
            msgPlayer(player, invalidtag);
        }
    }

    public void setCategory(Player player, String identifier, String category) {
        if (tags.containsKey(identifier)) {
            Tag t = tags.get(identifier);
            t.setCategory(category);

            try {
                getTagConfig().set("tags." + identifier + ".category", t.getCategory());
                saveTagConfig();
                reloadTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &6" + t.getIdentifier() + "'s category &7changed to " + t.getCategory());
        } else {
            msgPlayer(player, invalidcategory);
        }
    }

    public void setTag(CommandSender player, String identifier, String tag) {
        if (tags.containsKey(identifier)) {
            Tag t = tags.get(identifier);
            t.setTag(tag);

            try {
                getTagConfig().set("tags." + identifier + ".tag", t.getTag());
                saveTagConfig();
                reloadTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &6" + t.getIdentifier() + "'s tag &7changed to " + t.getTag());
        } else {
            msgPlayer(player, invalidtag);
        }
    }

    public void sort() {
        // Create a comparator that compares Map.Entry objects by their key in ascending order
        Comparator<Map.Entry<String, Tag>> comparator = Map.Entry.comparingByKey();

        // Create a sorted set with the comparator
        SortedSet<Map.Entry<String, Tag>> sortedSet = new TreeSet<>(comparator);

        // Add all the entries from the tags map to the sorted set
        sortedSet.addAll(tags.entrySet());
    }

    public boolean isCost() {
        return isCost;
    }

    public void setCost(boolean isCost) {
        this.isCost = isCost;
    }

    public void saveTag(Tag tag) {
        getTagConfig().set("tags." + tag.getIdentifier() + ".tag", tag.getTag());
        getTagConfig().set("tags." + tag.getIdentifier() + ".permission", tag.getPermission());
        getTagConfig().set("tags." + tag.getIdentifier() + ".description", tag.getDescription());
        getTagConfig().set("tags." + tag.getIdentifier() + ".category", tag.getCategory());
        getTagConfig().set("tags." + tag.getIdentifier() + ".cost", tag.getCost());
        saveTagConfig();
        reloadTagConfig();
    }

    public void saveTagConfig() {
        SupremeTags.getInstance().getConfigManager().saveConfig("tags.yml");
    }

    public FileConfiguration getTagConfig() {
        return SupremeTags.getInstance().getConfigManager().getConfig("tags.yml").get();
    }

    public void reloadTagConfig() {
        SupremeTags.getInstance().getConfigManager().reloadConfig("tags.yml");
    }
}
