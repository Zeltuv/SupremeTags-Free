package net.noscape.project.supremetags;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.noscape.project.supremetags.api.SupremeTagsAPI;
import net.noscape.project.supremetags.checkers.Metrics;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.commands.Tags;
import net.noscape.project.supremetags.commands.TagsComplete;
import net.noscape.project.supremetags.guis.tageditor.EditorListener;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.hooks.PAPI;
import net.noscape.project.supremetags.handlers.menu.MenuListener;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.packets.SystemChatPacketListener;
import net.noscape.project.supremetags.listeners.PlayerEvents;
import net.noscape.project.supremetags.managers.CategoryManager;
import net.noscape.project.supremetags.managers.ConfigManager;
import net.noscape.project.supremetags.managers.MergeManager;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.*;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class SupremeTags extends JavaPlugin {

    private static SupremeTags instance;
    private ConfigManager configManager;
    private TagManager tagManager;
    private CategoryManager categoryManager;
    private MergeManager mergeManager;

    private static SupremeTagsAPI api;

    private static Economy econ = null;
    private static Permission perms = null;

    private static MySQL mysql;
    private static H2Database h2;
    private final H2UserData h2user = new H2UserData();
    private static String connectionURL;
    private final MySQLUserData user = new MySQLUserData();

    private static final HashMap<Player, MenuUtil> menuUtilMap = new HashMap<>();
    private final HashMap<Player, Editor> editorList = new HashMap<>();

    private PlayerPointsAPI ppAPI;

    private boolean legacy_format;
    private boolean cmiHex;
    private boolean disabledWorldsTag;

    private ProtocolManager protocolManager;
    private PacketAdapter chatAdapter;

    public static File latestConfigFile;
    public static FileConfiguration latestConfigConfig;

    private final String host = getConfig().getString("data.address");
    private final int port = getConfig().getInt("data.port");
    private final String database = getConfig().getString("data.database");
    private final String username = getConfig().getString("data.username");
    private final String password = getConfig().getString("data.password");
    private final String options = getConfig().getString("data.options");
    private final boolean useSSL = getConfig().getBoolean("data.useSSL");

    @Override
    public void onEnable() {
        init();
    }

    @Override
    public void onDisable() {
        tagManager.unloadTags();
        editorList.clear();

        DataCache.clearCache();

        if (isMySQL()) {
            mysql.disconnect();
        }

        if(isH2()) {
            try {
                h2.getConnection().close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void init() {
        instance = this;

        Logger logger = Bukkit.getLogger();

        getConfig().options().copyDefaults(true);
        saveConfig();
        configManager = new ConfigManager(this);
        this.callMetrics();

        sendConsoleLog();

        if (isH2()) {
            connectionURL = "jdbc:h2:" + getDataFolder().getAbsolutePath() + "/database";
            h2 = new H2Database(connectionURL);
        }

        if (isMySQL()) {
            mysql = new MySQL(host, port, database, username, password, useSSL);
        }

        tagManager = new TagManager(getConfig().getBoolean("settings.cost-system"));
        categoryManager = new CategoryManager();
        mergeManager = new MergeManager();

        Objects.requireNonNull(getCommand("tags")).setExecutor(new Tags());
        Objects.requireNonNull(getCommand("tags")).setTabCompleter(new TagsComplete());

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
        getServer().getPluginManager().registerEvents(new EditorListener(), this);
        getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);

        if (isProtocolLib()) {
            try {
                this.protocolManager = ProtocolLibrary.getProtocolManager();
                this.chatAdapter = new SystemChatPacketListener(this);
            } catch (Exception e) {
                getLogger().warning("Failed to register ProtocolLib listener: " + e.getMessage());
            }
        } else {
            getLogger().warning("Requires ProtocolLib to be installed. Disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        if (isProtocolLib() && this.chatAdapter != null) {
            this.protocolManager.addPacketListener(this.chatAdapter);
        }

        legacy_format = getConfig().getBoolean("settings.legacy-hex-format");
        cmiHex = getConfig().getBoolean("settings.cmi-color-support");
        disabledWorldsTag = getConfig().getBoolean("settings.tag-command-in-disabled-worlds");

        merge(logger);

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            this.ppAPI = PlayerPoints.getInstance().getAPI();
            if (getConfig().getString("settings.economy").equalsIgnoreCase("PLAYERPOINTS")) {
                logger.info("> PlayerPoints: Found! (ECONOMY)");
            } else {
                logger.info("> PlayerPoints: Found!");
            }
        } else {
            logger.info("> PlayerPoints: Not Found!");
        }

        if (isCoinsEngine()) {
            if (getConfig().getString("settings.economy").startsWith("COINSENGINE-")) {
                logger.info("> CoinsEngine: Found! (ECONOMY)");
            } else {
                logger.info("> CoinsEngine: Found!");
            }
        } else {
            logger.info("> CoinsEngine: Not Found!");
        }

        if (isPlaceholderAPI()) {
            logger.info(ChatColor.YELLOW + "> PlaceholderAPI: Found");
            new PAPI(this).register();
        } else {
            logger.info(ChatColor.RED + "> PlaceholderAPI: Not Found!");
        }

        categoryManager.loadCategories();
        categoryManager.loadCategoriesTags();
        tagManager.getDataItem().clear();

        deleteCurrentLatestConfig();

        api = new SupremeTagsAPI();

        if (tagManager.getTags().size() == 0 || tagManager.getTags().isEmpty()) {
            tagManager.loadTags();
        }
    }

    public static SupremeTags getInstance() { return instance; }


    public static MenuUtil getMenuUtil(Player player) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, UserData.getActive(player.getUniqueId()));
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }

    public static MenuUtil getMenuUtilIdentifier(Player player, String identifier) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, identifier);
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }
 
    public static MenuUtil getMenuUtil(Player player, String category) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, UserData.getActive(player.getUniqueId()), category);
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }

    public HashMap<Player, MenuUtil> getMenuUtil() {
        return menuUtilMap;
    }

    public static String getConnectionURL() {
        return connectionURL;
    }

    public H2UserData getUserData() { return h2user; }

    public static H2Database getDatabase() { return h2; }

    public boolean isProtocolLib() {
        Plugin plugin = getServer().getPluginManager().getPlugin("ProtocolLib");
        return plugin != null && plugin.isEnabled();
    }

    public MySQLUserData getUser() {
        return instance.user;
    }

    public static MySQL getMysql() {
        return mysql;
    }

    public MergeManager getMergeManager() {
        return mergeManager;
    }

    public boolean isDisabledWorldsTag() {
        return disabledWorldsTag;
    }

    public boolean isCmiHex() {
        return cmiHex;
    }

    public boolean isLegacy_format() {
        return legacy_format;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public PlayerPointsAPI getPpAPI() {
        return ppAPI;
    }


    public HashMap<Player, Editor> getEditorList() {
        return editorList;
    }

    public static HashMap<Player, MenuUtil> getMenuUtilMap() {
        return menuUtilMap;
    }

    public static H2Database getH2() {
        return h2;
    }

    public void reload() {
        // Reload the config.yml
        super.reloadConfig();

        // Ensure default config is saved and loaded correctly
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        configManager.reloadConfig("tags.yml");
        configManager.reloadConfig("categories.yml");

        // Load configuration values
        legacy_format = getConfig().getBoolean("settings.legacy-hex-format");
        cmiHex = getConfig().getBoolean("settings.cmi-color-support");
        disabledWorldsTag = getConfig().getBoolean("settings.tag-command-in-disabled-worlds");

        // Reload any components or data that depend on config
        if (tagManager != null) {
            tagManager.unloadTags();
            tagManager.loadTags();
        }
    }

    public boolean isLegacyFormat() {
        return legacy_format;
    }

    public void merge(Logger log) {
        mergeManager.merge(log);
    }

    private void sendConsoleLog() {
        Logger logger = Bukkit.getLogger();

        logger.info("");
        logger.info("  ____  _   _ ____  ____  _____ __  __ _____ _____  _    ____ ____  ");
        logger.info(" / ___|| | | |  _ \\|  _ \\| ____|  \\/  | ____|_   _|/ \\  / ___/ ___| ");
        logger.info(" \\___ \\| | | | |_) | |_) |  _| | |\\/| |  _|   | | / _ \\| |  _\\___ \\ ");
        logger.info("  ___) | |_| |  __/|  _ <| |___| |  | | |___  | |/ ___ \\ |_| |___) |");
        logger.info(" |____/ \\___/|_|   |_| \\_\\_____|_|  |_|_____| |_/_/   \\_\\____|____/ ");
        logger.info(" Allow players to show off their supreme tags!");
        logger.info("");
        logger.info("> Version: " + getDescription().getVersion());
        logger.info("> Author: DevScape");

        if (getServer().getPluginManager().getPlugin("NBTAPI") == null) {
            logger.warning("> NBTAPI: Supremetags requires NBTAPI to run, disabling plugin....");
            getServer().getPluginManager().disablePlugin(this);
        } else {
            logger.info("> NBTAPI: Found!");
        }

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
            setupPermissions();
            logger.info("> Vault: Found!");
        } else {
            logger.info("> Vault: Not Found!");
        }

        if (isH2()) {
            logger.info("> Database: H2!");
        } else if (isMySQL()) {
            logger.info("> Database: MySQL!");
        }

        if (getConfig().getBoolean("settings.update-check")) {
            new UpdateChecker(this).getVersion(version -> {
                if (!this.getDescription().getVersion().equals(version)) {
                    logger.info("> An update is available! " + version);
                    logger.info("Download at https://www.spigotmc.org/resources/103140/");
                } else {
                    logger.info("> Plugin up to date!");
                }
            });
        }
    }

    public Boolean isH2() {
        return Objects.requireNonNull(getConfig().getString("data.type")).equalsIgnoreCase("H2");
    }

    public Boolean isMySQL() {
        return Objects.requireNonNull(getConfig().getString("data.type")).equalsIgnoreCase("MYSQL");
    }

    public boolean isCoinsEngine() {
        return getServer().getPluginManager().getPlugin("CoinsEngine") != null;
    }

    public Boolean isDataCache() {
        return getConfig().getBoolean("data.cache-data");
    }

    private void callMetrics() {
        int pluginId = 18038;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> getConfig().getString("language", "en")));

        metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);
            if (javaVersion.startsWith("1.7")) {
                map.put("Java 1.7", entry);
            } else if (javaVersion.startsWith("1.8")) {
                map.put("Java 1.8", entry);
            } else if (javaVersion.startsWith("1.9")) {
                map.put("Java 1.9", entry);
            } else {
                map.put("Other", entry);
            }
            return map;
        }));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }

    public static SupremeTagsAPI getTagAPI() {
        return api;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    private void deleteCurrentLatestConfig() {
        latestConfigFile = new File(getDataFolder(), "DEFAULT-CONFIG-LATEST.yml");

        if (latestConfigFile.exists()) {
            boolean deletionSuccessful = latestConfigFile.delete();
            if (deletionSuccessful) {
                Bukkit.getLogger().warning("Deleted DEFAULT-CONFIG-LATEST.yml, this file is not being used or needed anymore in latest versions of SupremeTags.");
            }
        }
    }

    public boolean isPlaceholderAPI() {
        return getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
