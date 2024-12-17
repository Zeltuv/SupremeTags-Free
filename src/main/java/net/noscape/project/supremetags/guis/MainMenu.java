package net.noscape.project.supremetags.guis;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.handlers.menu.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public class MainMenu extends Menu {

    private final List<String> catorgies;
    private final Map<Integer, String> dataItem = new HashMap<>();
    private final Map<String, Integer> categoriesTags;

    public MainMenu(MenuUtil menuUtil) {
        super(menuUtil);
        this.catorgies = SupremeTags.getInstance().getCategoryManager().getCatorgies();
        this.categoriesTags = SupremeTags.getInstance().getCategoryManager().getCatorgiesTags();
    }

    @Override
    public String getMenuName() {
        return format(SupremeTags.getInstance().getConfig().getString("gui.main-menu.title"));
    }

    @Override
    public int getSlots() {
        return SupremeTags.getInstance().getConfig().getInt("gui.main-menu.size");
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {

        Player player = (Player) e.getWhoClicked();

        String category = dataItem.get(e.getSlot());

        String material = SupremeTags.getInstance().getCategoryManager().getCategoryConfig().getString("categories." + category + ".material");
        String permission = SupremeTags.getInstance().getCategoryManager().getCategoryConfig().getString("categories." + category + ".permission");

        boolean hasMinTags = false;

        if (e.getCurrentItem() == null) return;

        for (String cats : getCatorgies()) {
            if (cats != null) {
                if (categoriesTags.get(cats) != null) {
                    hasMinTags = true;
                } else {
                    hasMinTags = false;
                }
                break;
            }
        }

        if (category != null) {
            if (material != null && Objects.requireNonNull(e.getCurrentItem()).getType().equals(Material.valueOf(material.toUpperCase()))) {
                if (hasMinTags) {
                    if (permission != null && player.hasPermission(permission)) {
                        menuUtil.setCategory(category);
                        new CategoryMenu(SupremeTags.getMenuUtil(player, category)).open();
                        menuUtil.getOwner().updateInventory();
                    } else {
                        msgPlayer(player, "&cYou don't have permission to access these tags.");
                    }
                } else {
                    e.setCancelled(true);
                    msgPlayer(player, "&cThere are no tags in this category.");
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        // Loop through categories items
        for (String cats : getCatorgies()) {
            if (cats != null) {
                // Fetch category configurations
                FileConfiguration categoryConfig = SupremeTags.getInstance().getCategoryManager().getCategoryConfig();
                boolean canSee = categoryConfig.getBoolean("categories." + cats + ".permission-see-category");
                String permission = categoryConfig.getString("categories." + cats + ".permission");
                String material = categoryConfig.getString("categories." + cats + ".material");
                int slot = categoryConfig.getInt("categories." + cats + ".slot");
                String displayName = categoryConfig.getString("categories." + cats + ".id_display");

                // Check permission
                if (permission != null && shouldAddItem(menuUtil.getOwner().hasPermission(permission), canSee)) {
                    ItemStack catItem = createCategoryItem(material, displayName, categoryConfig.getStringList("categories." + cats + ".lore"), categoriesTags.getOrDefault(cats, 0));
                    dataItem.put(slot, cats);
                    inventory.setItem(slot, catItem);
                }
            }
        }

        // Fill empty slots if configured
        if (SupremeTags.getInstance().getCategoryManager().getCategoryConfig().getBoolean("categories-menu-fill-empty")) {
            fillEmpty();
        }
    }

    /**
     * Determines if the item should be added based on permission and visibility.
     */
    private boolean shouldAddItem(boolean hasPermission, boolean canSee) {
        return hasPermission || canSee; // Adjust based on required logic
    }

    /**
     * Creates a category item based on material and other properties.
     */
    private ItemStack createCategoryItem(String material, String displayName, List<String> lore, int tagsAmount) {
        ItemStack item;
        ItemMeta meta;

        if (material.contains("hdb-")) {
            int id = Integer.parseInt(material.replace("hdb-", ""));
            item = new HeadDatabaseAPI().getItemHead(String.valueOf(id));
        } else if (material.contains("basehead-")) {
            item = createSkull(material.replace("basehead-", ""));
        } else if (material.contains("itemsadder-")) {
            item = getItemWithIA(material.replace("itemsadder-", ""));
        } else {
            item = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
        }

        meta = item.getItemMeta();
        assert meta != null;

        meta.setDisplayName(format(displayName));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_DESTROYS);

        // Set lore with formatted tags amount
        lore.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line).replace("%tags_amount%", String.valueOf(tagsAmount)));
        meta.setLore(color(lore));

        item.setItemMeta(meta);
        return item;
    }


    public List<String> getCatorgies() {
        return catorgies;
    }

    public Map<Integer, String> getDataItem() {
        return dataItem;
    }
}
