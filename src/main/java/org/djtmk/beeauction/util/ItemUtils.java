package org.djtmk.beeauction.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ItemUtils {

    public static final NamespacedKey LEGACY_ITEM_KEY = new NamespacedKey("beeauction", "legacy_item");

    public static void serializeItem(ItemStack item, ConfigurationSection section) {
        if (item == null || section == null) {
            return;
        }
        section.set("item_data", serializeItemToBase64(item));
    }

    public static ItemStack deserializeItem(ConfigurationSection section) {
        if (section == null) return null;

        if (section.contains("item_data")) {
            try {
                return deserializeItemFromBase64(section.getString("item_data"));
            } catch (IOException e) {
                // Fallback to legacy deserialization
            }
        }

        // Legacy deserialization
        String materialName = section.getString("material");
        if (materialName == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(LEGACY_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);

        if (section.contains("name")) {
            meta.setDisplayName(MessageUtil.colorize(section.getString("name")));
        }
        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            List<String> colorizedLore = new ArrayList<>();
            for (String line : lore) {
                colorizedLore.add(MessageUtil.colorize(line));
            }
            meta.setLore(colorizedLore);
        }
        if (section.contains("enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String key : enchantSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, enchantSection.getInt(key), true);
                    }
                }
            }
        }
        if (section.contains("flags")) {
            List<String> flags = section.getStringList("flags");
            for (String flag : flags) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public static String getItemDisplayName(ItemStack item) {
        if (item == null) return "null";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return formatMaterialName(item.getType().name());
    }

    public static String formatMaterialName(String materialName) {
        if (materialName == null) return "";
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

    public static String serializeItemToBase64(ItemStack item) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack deserializeItemFromBase64(String data) throws IOException {
        try {
            byte[] decodedData = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedData);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
