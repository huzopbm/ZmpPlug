package cz.ekonomika.managers;

import cz.ekonomika.EkonomikaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class EkonomikaManager {

    private final EkonomikaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;

    public EkonomikaManager(EkonomikaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "balances.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nelze vytvořit balances.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveAll() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nelze uložit balances.yml: " + e.getMessage());
        }
    }

    // Vrátí zůstatek hráče (UUID jako string)
    public double getBalance(UUID uuid) {
        return data.getDouble(uuid.toString(), plugin.getConfig().getDouble("mena.pocatecni-zustatek", 0));
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public void setBalance(UUID uuid, double amount) {
        data.set(uuid.toString(), Math.max(0, amount));
        saveAll();
    }

    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }

    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    public void add(Player player, double amount) {
        setBalance(player, getBalance(player) + amount);
    }

    public void add(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    /**
     * Odebere peníze hráči. Vrací true pokud měl dost.
     */
    public boolean take(Player player, double amount) {
        double current = getBalance(player);
        if (current < amount) return false;
        setBalance(player, current - amount);
        return true;
    }

    public boolean take(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) return false;
        setBalance(uuid, current - amount);
        return true;
    }

    /**
     * Převede peníze s daní. Vrací skutečně přijatou částku (po dani).
     */
    public double pay(Player from, Player to, double amount) {
        if (!take(from, amount)) return -1;
        double danProcent = plugin.getConfig().getDouble("dan.pay-procent", 5);
        double dan = amount * (danProcent / 100.0);
        double prijato = amount - dan;
        add(to, prijato);
        return prijato;
    }
}
