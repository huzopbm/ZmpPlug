package cz.ekonomika.managers;

import cz.ekonomika.EkonomikaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AHManager {

    private final EkonomikaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;

    // Unikátní ID výpisu → AHListing
    private final Map<String, AHListing> listings = new LinkedHashMap<>();

    public AHManager(EkonomikaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "ah_listings.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nelze vytvořit ah_listings.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Načti výpisy
        if (data.contains("listings")) {
            for (String id : data.getConfigurationSection("listings").getKeys(false)) {
                String path = "listings." + id;
                UUID sellerUUID = UUID.fromString(data.getString(path + ".seller"));
                String sellerName = data.getString(path + ".sellerName");
                double price = data.getDouble(path + ".price");
                long expiry = data.getLong(path + ".expiry");
                ItemStack item = data.getItemStack(path + ".item");
                if (item != null) {
                    AHListing listing = new AHListing(id, sellerUUID, sellerName, item, price, expiry);
                    listings.put(id, listing);
                }
            }
        }

        // Odstraň vypršené výpisy při startu (vrát item do unclaimed)
        cleanExpired();
    }

    public void saveAll() {
        data.set("listings", null); // clear
        for (AHListing listing : listings.values()) {
            String path = "listings." + listing.getId();
            data.set(path + ".seller", listing.getSellerUUID().toString());
            data.set(path + ".sellerName", listing.getSellerName());
            data.set(path + ".price", listing.getPrice());
            data.set(path + ".expiry", listing.getExpiry());
            data.set(path + ".item", listing.getItem());
        }
        // Ulož unclaimed items
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nelze uložit ah_listings.yml: " + e.getMessage());
        }
    }

    public boolean addListing(Player seller, ItemStack item, double price) {
        int max = plugin.getConfig().getInt("ah.max-polozek-na-hrace", 5);
        long count = listings.values().stream()
                .filter(l -> l.getSellerUUID().equals(seller.getUniqueId()))
                .count();
        if (count >= max) return false;

        long expiry = System.currentTimeMillis() + (plugin.getConfig().getLong("ah.expirace-hodin", 48) * 3600_000L);
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        AHListing listing = new AHListing(id, seller.getUniqueId(), seller.getName(), item.clone(), price, expiry);
        listings.put(id, listing);
        saveAll();
        return true;
    }

    public boolean cancelListing(String id, Player requester) {
        AHListing listing = listings.get(id);
        if (listing == null) return false;
        boolean isOwner = listing.getSellerUUID().equals(requester.getUniqueId());
        boolean isAdmin = requester.hasPermission("ekonomika.admin");
        if (!isOwner && !isAdmin) return false;

        listings.remove(id);
        // Vrať item hráči
        if (requester.isOnline()) {
            requester.getInventory().addItem(listing.getItem());
        } else {
            addUnclaimed(listing.getSellerUUID(), listing.getItem());
        }
        saveAll();
        return true;
    }

    public boolean buyListing(String id, Player buyer) {
        AHListing listing = listings.get(id);
        if (listing == null) return false;
        if (listing.getSellerUUID().equals(buyer.getUniqueId())) return false;
        if (!plugin.getEkonomikaManager().take(buyer, listing.getPrice())) return false;

        // Pošli peníze prodávajícímu
        plugin.getEkonomikaManager().add(listing.getSellerUUID(), listing.getPrice());

        // Dej item kupujícímu
        buyer.getInventory().addItem(listing.getItem());

        // Notifikuj prodávajícího
        var seller = plugin.getServer().getPlayer(listing.getSellerUUID());
        if (seller != null && seller.isOnline()) {
            seller.sendMessage(plugin.prefix() + EkonomikaPlugin.colorize(
                    "&aHráč &e" + buyer.getName() + "&a koupil tvůj výpis &e" + id +
                    "&a za &6" + plugin.formatMena(listing.getPrice()) + "&a!"));
        }

        listings.remove(id);
        saveAll();
        return true;
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (AHListing listing : listings.values()) {
            if (listing.getExpiry() < now) {
                expired.add(listing.getId());
            }
        }
        for (String id : expired) {
            AHListing listing = listings.remove(id);
            addUnclaimed(listing.getSellerUUID(), listing.getItem());
        }
        if (!expired.isEmpty()) saveAll();
    }

    private void addUnclaimed(UUID uuid, ItemStack item) {
        String path = "unclaimed." + uuid.toString();
        List<ItemStack> current = (List<ItemStack>) data.getList(path, new ArrayList<>());
        current.add(item);
        data.set(path, current);
    }

    public List<AHListing> getActiveListings() {
        cleanExpired();
        return new ArrayList<>(listings.values());
    }

    public List<AHListing> getListingsByPlayer(UUID uuid) {
        List<AHListing> result = new ArrayList<>();
        for (AHListing l : listings.values()) {
            if (l.getSellerUUID().equals(uuid)) result.add(l);
        }
        return result;
    }

    public AHListing getListing(String id) {
        return listings.get(id);
    }

    // ── Inner record ──────────────────────────────────────────────
    public static class AHListing {
        private final String id;
        private final UUID sellerUUID;
        private final String sellerName;
        private final ItemStack item;
        private final double price;
        private final long expiry;

        public AHListing(String id, UUID sellerUUID, String sellerName, ItemStack item, double price, long expiry) {
            this.id = id;
            this.sellerUUID = sellerUUID;
            this.sellerName = sellerName;
            this.item = item;
            this.price = price;
            this.expiry = expiry;
        }

        public String getId() { return id; }
        public UUID getSellerUUID() { return sellerUUID; }
        public String getSellerName() { return sellerName; }
        public ItemStack getItem() { return item.clone(); }
        public double getPrice() { return price; }
        public long getExpiry() { return expiry; }

        public String getTimeLeft() {
            long diff = expiry - System.currentTimeMillis();
            if (diff <= 0) return "Vypršelo";
            long hours = diff / 3600_000;
            long minutes = (diff % 3600_000) / 60_000;
            if (hours > 0) return hours + "h " + minutes + "m";
            return minutes + "m";
        }
    }
}
