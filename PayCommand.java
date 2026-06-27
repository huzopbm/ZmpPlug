package cz.ekonomika.gui;

import cz.ekonomika.EkonomikaPlugin;
import cz.ekonomika.managers.AHManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AHGui {

    public static final String AH_TITLE_PREFIX = "§6Aukční dům";
    private static final int PAGE_SIZE = 45; // 5 řad × 9

    public static void openMain(EkonomikaPlugin plugin, Player player, int page) {
        List<AHManager.AHListing> listings = plugin.getAhManager().getActiveListings();
        open(plugin, player, listings, page, AH_TITLE_PREFIX);
    }

    public static void openSearch(EkonomikaPlugin plugin, Player player, String query) {
        List<AHManager.AHListing> all = plugin.getAhManager().getActiveListings();
        List<AHManager.AHListing> filtered = new ArrayList<>();
        for (AHManager.AHListing l : all) {
            String itemName = l.getItem().getType().name().toLowerCase().replace('_', ' ');
            if (itemName.contains(query.toLowerCase())) {
                filtered.add(l);
            }
        }
        open(plugin, player, filtered, 0, AH_TITLE_PREFIX + " §7– Hledání: §e" + query);
    }

    private static void open(EkonomikaPlugin plugin, Player player,
                              List<AHManager.AHListing> listings, int page, String baseTitle) {
        int totalPages = Math.max(1, (int) Math.ceil(listings.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = baseTitle + (totalPages > 1 ? " §8(" + (page + 1) + "/" + totalPages + ")" : "");
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));

        // Výpisy na stránce
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, listings.size());

        for (int i = start; i < end; i++) {
            AHManager.AHListing listing = listings.get(i);
            ItemStack display = listing.getItem();
            ItemMeta meta = display.getItemMeta();

            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.lore());
                lore.add(Component.text(""));
            }
            lore.add(Component.text("§7Prodávající: §e" + listing.getSellerName()));
            lore.add(Component.text("§7Cena: §6" + plugin.formatMena(listing.getPrice())));
            lore.add(Component.text("§7Zbývá: §f" + listing.getTimeLeft()));
            lore.add(Component.text("§7ID: §8" + listing.getId()));
            lore.add(Component.text(""));
            lore.add(Component.text("§aKlikni pro koupi"));

            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(i - start, display);
        }

        // Navigace (poslední řada – sloty 45-53)
        if (listings.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta m = empty.getItemMeta();
            m.displayName(Component.text("§cŽádné výpisy"));
            empty.setItemMeta(m);
            inv.setItem(49, empty);
        }

        // Předchozí stránka
        if (page > 0) {
            inv.setItem(45, makeNavItem(Material.ARROW, "§ePředchozí stránka", "§7Stránka " + page));
        }

        // Info uprostřed
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("§6Aukční dům"));
        infoMeta.lore(List.of(
                Component.text("§7Celkem výpisů: §e" + listings.size()),
                Component.text("§7Stránka: §e" + (page + 1) + "/" + totalPages),
                Component.text(""),
                Component.text("§7/ah list <cena> §8– přidat výpis"),
                Component.text("§7/ah cancel <id> §8– zrušit výpis")
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        // Další stránka
        if (page < totalPages - 1) {
            inv.setItem(53, makeNavItem(Material.ARROW, "§eDalší stránka", "§7Stránka " + (page + 2)));
        }

        // Moje výpisy (slot 48)
        ItemStack myListings = new ItemStack(Material.CHEST);
        ItemMeta mlMeta = myListings.getItemMeta();
        mlMeta.displayName(Component.text("§eMoje výpisy"));
        long myCount = plugin.getAhManager().getListingsByPlayer(player.getUniqueId()).size();
        mlMeta.lore(List.of(Component.text("§7Aktivní: §e" + myCount)));
        myListings.setItemMeta(mlMeta);
        inv.setItem(48, myListings);

        player.openInventory(inv);
    }

    private static ItemStack makeNavItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(List.of(Component.text(lore)));
        item.setItemMeta(meta);
        return item;
    }

    public static int getPage(String title) {
        // Parsuje stránku z názvu inventáře
        if (title.contains("(") && title.contains("/")) {
            try {
                String[] parts = title.split("\\(")[1].split("/")[0].trim().split("");
                return Integer.parseInt(title.split("\\(")[1].split("/")[0].trim()) - 1;
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}
