package org.djtmk.beeauction.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemUtils {

    public static void serializeItem(ItemStack item, ConfigurationSection section) {
        if (item == null || section == null) {
            return;
        }
        
        // Basic properties
        section.set("material", item.getType().name());
        section.set("amount", item.getAmount());
        
        // Item meta
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            
            // Display name
            if (meta.hasDisplayName()) {
                section.set("name", meta.getDisplayName());
            }
            
            // Lore
            if (meta.hasLore()) {
                section.set("lore", meta.getLore());
            }
            
            // Enchantments
            if (!meta.getEnchants().isEmpty()) {
                ConfigurationSection enchantSection = section.createSection("enchantments");
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    enchantSection.set(entry.getKey().getKey().getKey(), entry.getValue());
                }
            }
            
            // Item flags
            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) {
                    flags.add(flag.name());
                }
                section.set("flags", flags);
            }
        }
    }

    public static ItemStack deserializeItem(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        
        // Basic properties
        String materialName = section.getString("material");
        if (materialName == null) {
            return null;
        }
        
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        
        // Item meta
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        // Display name
        if (section.contains("name")) {
            meta.setDisplayName(MessageUtil.colorize(section.getString("name")));
        }
        
        // Lore
        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            List<String> colorizedLore = new ArrayList<>();
            for (String line : lore) {
                colorizedLore.add(MessageUtil.colorize(line));
            }
            meta.setLore(colorizedLore);
        }
        
        // Enchantments
        if (section.contains("enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String key : enchantSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(key));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, enchantSection.getInt(key), true);
                    }
                }
            }
        }
        
        // Item flags
        if (section.contains("flags")) {
            List<String> flags = section.getStringList("flags");
            for (String flag : flags) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flag));
                } catch (IllegalArgumentException ignored) {
                    // Invalid flag, skip
                }
            }
        }
        
        item.setItemMeta(meta);
        return item;
    }

    public static String getItemDisplayName(ItemStack item) {
        if (item == null) {
            return "null";
        }
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        return formatMaterialName(item.getType().name());
    }

    public static String formatMaterialName(String materialName) {
        if (materialName == null) {
            return "";
        }
        
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
