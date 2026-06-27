package cz.ekonomika;

import cz.ekonomika.commands.*;
import cz.ekonomika.listeners.AHInventoryListener;
import cz.ekonomika.listeners.ZebratResponseListener;
import cz.ekonomika.managers.EkonomikaManager;
import cz.ekonomika.managers.AHManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EkonomikaPlugin extends JavaPlugin {

    private static EkonomikaPlugin instance;
    private EkonomikaManager ekonomikaManager;
    private AHManager ahManager;

    @Override
    public void onEnable() {
        instance = this;

        // Ulož výchozí config
        saveDefaultConfig();

        // Inicializace managerů
        ekonomikaManager = new EkonomikaManager(this);
        ahManager = new AHManager(this);

        // Registrace příkazů
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("zebrat").setExecutor(new ZebratCommand(this));
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("ah").setExecutor(new AHCommand(this));
        getCommand("ekonomika").setExecutor(new AdminCommand(this));

        // Registrace listenerů
        getServer().getPluginManager().registerEvents(new AHInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ZebratResponseListener(this), this);

        getLogger().info("EkonomikaPlugin zapnut! Měna: drobáky:");
    }

    @Override
    public void onDisable() {
        if (ekonomikaManager != null) ekonomikaManager.saveAll();
        if (ahManager != null) ahManager.saveAll();
        getLogger().info("EkonomikaPlugin vypnut. Data uložena.");
    }

    public static EkonomikaPlugin getInstance() {
        return instance;
    }

    public EkonomikaManager getEkonomikaManager() {
        return ekonomikaManager;
    }

    public AHManager getAhManager() {
        return ahManager;
    }

    public String prefix() {
        return colorize(getConfig().getString("zpravy.prefix", "&6[Ekonomika]&r "));
    }

    public String msg(String key) {
        return colorize(getConfig().getString("zpravy." + key, "&cChybí zpráva: " + key));
    }

    public String msg(String key, String... replacements) {
        String text = msg(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return text;
    }

    public static String colorize(String text) {
        return text.replace("&", "\u00a7");
    }

    public String formatMena(double amount) {
        if (amount == Math.floor(amount)) {
            return (long) amount + " " + getConfig().getString("mena.symbol", "drobáky:");
        }
        return String.format("%.2f", amount) + " " + getConfig().getString("mena.symbol", "drobáky:");
    }
}
