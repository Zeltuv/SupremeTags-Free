package net.noscape.project.supremetags.managers;

import lombok.Getter;
import net.noscape.project.supremetags.*;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

@Getter
public class CategoryManager {

    private final List<String> catorgies = new ArrayList<>();
    private final Map<String, Integer> catorgiesTags = new HashMap<>();

    public CategoryManager() {}

    public void loadCategories() {
        catorgies.clear();
        catorgies.addAll(Objects.requireNonNull(getCategoryConfig().getConfigurationSection("categories")).getKeys(false));
    }

    public void loadCategoriesTags() {
        catorgiesTags.clear();

        for (String cats : getCatorgies()) {
            int value = 0;
            for (String tags : Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getConfigurationSection("tags")).getKeys(false)) {
                String cat = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + tags + ".category");
                if (cats.equals(cat)) {
                    value++;
                }
            }
            catorgiesTags.put(cats, value);
        }
    }

    public void saveCategoryConfig() {
        SupremeTags.getInstance().getConfigManager().saveConfig("categories.yml");
    }

    public FileConfiguration getCategoryConfig() {
        return SupremeTags.getInstance().getConfigManager().getConfig("categories.yml").get();
    }

    public void reloadCategoryConfig() {
        SupremeTags.getInstance().getConfigManager().reloadConfig("categories.yml");
    }
}