//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.custardcream.cusSmpEnchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomEnchantCore extends JavaPlugin implements Listener, TabExecutor {
    private NamespacedKey managedDisplayKey;
    private NamespacedKey originalLoreKey;
    private Enchantment SHARPNESS;
    private Enchantment IMPALING;
    private Enchantment PROTECTION;
    private Enchantment FIRE_PROTECTION;
    private Enchantment BLAST_PROTECTION;
    private Enchantment PROJECTILE_PROTECTION;
    private Enchantment THORNS;
    private Enchantment DENSITY;
    private ShieldEnchantManager shieldEnchantManager;
    private final Map<Enchantment, Integer> customCaps = new LinkedHashMap();
    private static final double SHARPNESS_EXTRA_DAMAGE_PER_LEVEL = (double)1.0F;
    private static final double IMPALING_EXTRA_DAMAGE_PER_LEVEL = 0.9;
    private static final double DENSITY_EXTRA_FLAT_PER_LEVEL = (double)2.0F;
    private static final double PROTECTION_EXTRA_REDUCTION_COMBAT_PER_LEVEL = 0.01;
    private static final double PROTECTION_EXTRA_REDUCTION_NON_COMBAT_PER_LEVEL = 0.015;
    private static final double SPECIAL_PROTECTION_EXTRA_REDUCTION_PER_LEVEL = 0.025;
    private static final double MAX_EXTRA_REDUCTION = 0.4;
    private static final double THORNS_EXTRA_DAMAGE_PER_LEVEL = (double)1.5F;
    private final Set<UUID> pendingThornsDamageTargets = new HashSet();
    private static final String GZIP_B64_PREFIX = "H4sI";

    public void onEnable() {
        this.managedDisplayKey = new NamespacedKey(this, "managed_display");
        this.originalLoreKey = new NamespacedKey(this, "original_lore");
        Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        this.SHARPNESS = this.requiredEnchant(enchantmentRegistry, "sharpness");
        this.IMPALING = this.requiredEnchant(enchantmentRegistry, "impaling");
        this.PROTECTION = this.requiredEnchant(enchantmentRegistry, "protection");
        this.FIRE_PROTECTION = this.requiredEnchant(enchantmentRegistry, "fire_protection");
        this.BLAST_PROTECTION = this.requiredEnchant(enchantmentRegistry, "blast_protection");
        this.PROJECTILE_PROTECTION = this.requiredEnchant(enchantmentRegistry, "projectile_protection");
        this.THORNS = this.requiredEnchant(enchantmentRegistry, "thorns");
        this.DENSITY = this.requiredEnchant(enchantmentRegistry, "density");
        this.saveDefaultConfig();
        this.customCaps.put(this.SHARPNESS, this.capFromConfig("sharpness", this.SHARPNESS, 10));
        this.customCaps.put(this.IMPALING, this.capFromConfig("impaling", this.IMPALING, 10));
        this.customCaps.put(this.PROTECTION, this.capFromConfig("protection", this.PROTECTION, 10));
        this.customCaps.put(this.FIRE_PROTECTION, this.capFromConfig("fire_protection", this.FIRE_PROTECTION, 10));
        this.customCaps.put(this.BLAST_PROTECTION, this.capFromConfig("blast_protection", this.BLAST_PROTECTION, 10));
        this.customCaps.put(this.PROJECTILE_PROTECTION, this.capFromConfig("projectile_protection", this.PROJECTILE_PROTECTION, 10));
        this.customCaps.put(this.THORNS, this.capFromConfig("thorns", this.THORNS, 8));
        this.customCaps.put(this.DENSITY, this.capFromConfig("density", this.DENSITY, 8));
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new WindBurstManager(this), this);
        this.shieldEnchantManager = new ShieldEnchantManager(this);
        Bukkit.getPluginManager().registerEvents(this.shieldEnchantManager, this);
        if (this.getCommand("overenchant") != null) {
            this.getCommand("overenchant").setExecutor(this);
            this.getCommand("overenchant").setTabCompleter(this);
        } else {
            this.getLogger().warning("plugin.yml에 overenchant 명령어가 없습니다. 명령어 없이도 모루 병합은 동작합니다.");
        }

    }

    private Enchantment requiredEnchant(Registry<Enchantment> registry, String minecraftKey) {
        Enchantment enchant = (Enchantment)registry.get(NamespacedKey.minecraft(minecraftKey));
        if (enchant == null) {
            throw new IllegalStateException("Missing enchantment in registry: " + minecraftKey);
        } else {
            return enchant;
        }
    }

    private int capFromConfig(String key, Enchantment enchant, int defaultCap) {
        int value = this.getConfig().getInt("enchant-caps." + key, defaultCap);
        return Math.max(value, 1);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);
        if (first != null && second != null) {
            event.getView().bypassEnchantmentLevelRestriction(true);
            if (this.getConfig().getBoolean("bypass-too-expensive", true)) {
                event.getView().setMaximumRepairCost(Integer.MAX_VALUE);
            }
            if (first.getType() == Material.SHIELD && this.shieldEnchantManager.isResistBook(second)) {
                int currentLevel = this.shieldEnchantManager.getShieldResistLevel(first);
                int bookLevel = this.shieldEnchantManager.getResistBookLevel(second);
                int newLevel = this.mergeLevels(currentLevel, bookLevel);
                newLevel = Math.min(newLevel, 5);
                if (newLevel > currentLevel) {
                    ItemStack resultShield = first.clone();
                    this.shieldEnchantManager.setShieldResistLevel(resultShield, newLevel);
                    event.setResult(resultShield);
                }

            } else {
                ItemStack vanillaResult = event.getResult();
                if (vanillaResult != null && !vanillaResult.getType().isAir()) {
                    ItemStack result = vanillaResult.clone();
                    boolean normalized = this.normalizeResultBeforeMerge(result, first, second);
                    boolean touched = false;

                    for(Enchantment ench : this.customCaps.keySet()) {
                        int firstLevel = this.getEffectiveLevel(first, ench);
                        int secondLevel = this.getEffectiveLevel(second, ench);
                        if ((firstLevel > 0 || secondLevel > 0) && this.canCarryOver(first, second, vanillaResult, ench)) {
                            int merged = this.mergeLevels(firstLevel, secondLevel);
                            if (merged > 0) {
                                int cap = (Integer)this.customCaps.get(ench);
                                if (merged > cap) {
                                    merged = cap;
                                }

                                int currentLevelInResult = this.getActualLevel(result, ench);
                                if (merged > currentLevelInResult) {
                                    this.setLevel(result, ench, merged);
                                }

                                touched = true;
                            }
                        }
                    }

                    if (touched || normalized) {
                        this.refreshDisplay(result);
                        event.setResult(result);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        ItemStack result = event.getResult();
        if (result != null && !result.getType().isAir()) {
            ItemStack cleaned = result.clone();
            this.cleanupManagedDisplay(cleaned);
            event.setResult(cleaned);
        }
    }

    private boolean canCarryOver(ItemStack first, ItemStack second, ItemStack result, Enchantment ench) {
        if (ench.equals(this.IMPALING)) {
            return this.isTridentOrBook(first) && this.isTridentOrBook(second) && this.isTridentOrBook(result);
        } else if (ench.equals(this.THORNS)) {
            return this.isArmorOrBook(first) && this.isArmorOrBook(second) && this.isArmorOrBook(result);
        } else if (!ench.equals(this.DENSITY)) {
            return true;
        } else {
            return this.isMaceOrBook(first) && this.isMaceOrBook(second) && this.isMaceOrBook(result);
        }
    }

    private boolean isMaceOrBook(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            return item.getType() == Material.MACE || item.getType() == Material.ENCHANTED_BOOK;
        } else {
            return false;
        }
    }

    private boolean isArmorOrBook(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            Material m = item.getType();
            if (m == Material.ENCHANTED_BOOK) {
                return true;
            } else {
                String name = m.name();
                return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || m == Material.TURTLE_HELMET || m == Material.ELYTRA;
            }
        } else {
            return false;
        }
    }

    private boolean isTridentOrBook(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            return item.getType() == Material.TRIDENT || item.getType() == Material.ENCHANTED_BOOK;
        } else {
            return false;
        }
    }

    private int mergeLevels(int first, int second) {
        if (first <= 0 && second <= 0) {
            return 0;
        } else if (first <= 0) {
            return second;
        } else if (second <= 0) {
            return first;
        } else {
            return first == second ? first + 1 : Math.max(first, second);
        }
    }

    private int getActualLevel(ItemStack item, Enchantment ench) {
        if (item != null && !item.getType().isAir()) {
            if (item.getType() == Material.ENCHANTED_BOOK) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof EnchantmentStorageMeta) {
                    EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta)meta;
                    return bookMeta.getStoredEnchantLevel(ench);
                } else {
                    return 0;
                }
            } else {
                return item.getEnchantmentLevel(ench);
            }
        } else {
            return 0;
        }
    }

    private int getEffectiveLevel(ItemStack item, Enchantment ench) {
        return this.getActualLevel(item, ench);
    }

    private void setLevel(ItemStack item, Enchantment ench, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta)meta;
                bookMeta.removeStoredEnchant(ench);
                if (level > 0) {
                    bookMeta.addStoredEnchant(ench, level, true);
                }

                item.setItemMeta(bookMeta);
            } else {
                meta.removeEnchant(ench);
                if (level > 0) {
                    meta.addEnchant(ench, level, true);
                }

                item.setItemMeta(meta);
            }
        }
    }

    private boolean hasSupportedOverCap(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            for(Map.Entry<Enchantment, Integer> entry : this.customCaps.entrySet()) {
                int level = this.getActualLevel(item, (Enchantment)entry.getKey());
                if (level > ((Enchantment)entry.getKey()).getMaxLevel()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    void refreshDisplay(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean overCap = this.hasSupportedOverCap(item);
            boolean managed = meta.getPersistentDataContainer().has(this.managedDisplayKey, PersistentDataType.BYTE);
            if (!overCap) {
                if (managed) {
                    this.clearManagedDisplay(meta);
                    item.setItemMeta(meta);
                }

            } else {
                if (!managed && !meta.getPersistentDataContainer().has(this.originalLoreKey, PersistentDataType.STRING)) {
                    this.saveOriginalLore(meta);
                }

                meta.getPersistentDataContainer().set(this.managedDisplayKey, PersistentDataType.BYTE, (byte)1);
                if (item.getType() == Material.ENCHANTED_BOOK) {
                    meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_STORED_ENCHANTS});
                } else {
                    meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                }

                List<Component> originalLore = this.loadOriginalLore(meta);
                List<Component> finalLore = new ArrayList();
                if (originalLore != null && !originalLore.isEmpty()) {
                    finalLore.addAll(originalLore);
                    finalLore.add(Component.empty());
                }

                Map<Enchantment, Integer> enchants = this.getAllEnchantments(item);

                for(Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    finalLore.add(this.renderEnchantLine((Enchantment)entry.getKey(), (Integer)entry.getValue()));
                }

                meta.lore(finalLore);
                item.setItemMeta(meta);
            }
        }
    }

    private void cleanupManagedDisplay(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                boolean managed = meta.getPersistentDataContainer().has(this.managedDisplayKey, PersistentDataType.BYTE);
                if (managed) {
                    if (this.hasSupportedOverCap(item)) {
                        this.refreshDisplay(item);
                    } else {
                        this.clearManagedDisplay(meta);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private void clearManagedDisplay(ItemMeta meta) {
        this.restoreOriginalLore(meta);
        meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_STORED_ENCHANTS});
        meta.getPersistentDataContainer().remove(this.managedDisplayKey);
        meta.getPersistentDataContainer().remove(this.originalLoreKey);
    }

    private Map<Enchantment, Integer> getAllEnchantments(ItemStack item) {
        Map<Enchantment, Integer> map = new LinkedHashMap();
        if (item.getType() == Material.ENCHANTED_BOOK) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta)meta;
                map.putAll(bookMeta.getStoredEnchants());
            }

            return map;
        } else {
            map.putAll(item.getEnchantments());
            return map;
        }
    }

    private Component renderEnchantLine(Enchantment ench, int level) {
        String name = this.getKoreanName(ench);
        return name != null ? ((TextComponent)Component.text(name + " " + this.toRoman(level)).color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false) : ench.displayName(level).colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private @Nullable String getKoreanName(Enchantment ench) {
        if (ench.equals(this.SHARPNESS)) {
            return "날카로움";
        } else if (ench.equals(this.IMPALING)) {
            return "찌르기";
        } else if (ench.equals(this.PROTECTION)) {
            return "보호";
        } else if (ench.equals(this.FIRE_PROTECTION)) {
            return "화염 보호";
        } else if (ench.equals(this.BLAST_PROTECTION)) {
            return "폭발 보호";
        } else if (ench.equals(this.PROJECTILE_PROTECTION)) {
            return "발사체 보호";
        } else if (ench.equals(this.THORNS)) {
            return "가시";
        } else {
            return ench.equals(this.DENSITY) ? "육중" : null;
        }
    }

    private String toRoman(int num) {
        String var10000;
        switch (num) {
            case 1 -> var10000 = "I";
            case 2 -> var10000 = "II";
            case 3 -> var10000 = "III";
            case 4 -> var10000 = "IV";
            case 5 -> var10000 = "V";
            case 6 -> var10000 = "VI";
            case 7 -> var10000 = "VII";
            case 8 -> var10000 = "VIII";
            case 9 -> var10000 = "IX";
            case 10 -> var10000 = "X";
            default -> var10000 = String.valueOf(num);
        }

        return var10000;
    }

    private void saveOriginalLore(ItemMeta meta) {
        List<Component> lore = meta.lore();
        if (lore != null && !lore.isEmpty()) {
            List<String> jsonLines = new ArrayList();

            for(Component component : lore) {
                jsonLines.add((String)GsonComponentSerializer.gson().serialize(component));
            }

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                    gzip.write(String.join("\n", jsonLines).getBytes(StandardCharsets.UTF_8));
                }

                meta.getPersistentDataContainer().set(this.originalLoreKey, PersistentDataType.STRING, Base64.getEncoder().encodeToString(baos.toByteArray()));
            } catch (IOException var10) {
                List<String> encoded = new ArrayList();

                for(String json : jsonLines) {
                    encoded.add(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
                }

                meta.getPersistentDataContainer().set(this.originalLoreKey, PersistentDataType.STRING, String.join("\n", encoded));
            }

        } else {
            meta.getPersistentDataContainer().set(this.originalLoreKey, PersistentDataType.STRING, "");
        }
    }

    private @Nullable List<Component> loadOriginalLore(ItemMeta meta) {
        String raw = (String)meta.getPersistentDataContainer().get(this.originalLoreKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        } else if (raw.isEmpty()) {
            return new ArrayList();
        } else {
            List<Component> lore = new ArrayList();
            if (raw.startsWith("H4sI")) {
                try {
                    byte[] compressed = Base64.getDecoder().decode(raw);

                    String combined;
                    try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                        combined = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
                    }

                    for(String json : combined.split("\n")) {
                        lore.add(GsonComponentSerializer.gson().deserialize(json));
                    }
                } catch (IOException var12) {
                    return new ArrayList();
                }
            } else {
                for(String line : raw.split("\n")) {
                    String json = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
                    lore.add(GsonComponentSerializer.gson().deserialize(json));
                }
            }

            return lore;
        }
    }

    private void restoreOriginalLore(ItemMeta meta) {
        List<Component> lore = this.loadOriginalLore(meta);
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        } else {
            meta.lore((List)null);
        }
    }

    private boolean normalizeResultBeforeMerge(ItemStack result, ItemStack first, ItemStack second) {
        if (result != null && !result.getType().isAir()) {
            ItemMeta resultMeta = result.getItemMeta();
            if (resultMeta == null) {
                return false;
            } else {
                boolean changed = false;
                if (resultMeta.getPersistentDataContainer().has(this.managedDisplayKey, PersistentDataType.BYTE)) {
                    this.restoreOriginalLore(resultMeta);
                    resultMeta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                    resultMeta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_STORED_ENCHANTS});
                    resultMeta.getPersistentDataContainer().remove(this.managedDisplayKey);
                    resultMeta.getPersistentDataContainer().remove(this.originalLoreKey);
                    result.setItemMeta(resultMeta);
                    resultMeta = result.getItemMeta();
                    if (resultMeta == null) {
                        return true;
                    }

                    changed = true;
                }

                List<Component> baseLore = this.extractOriginalLoreFromInput(first);
                if (baseLore == null) {
                    baseLore = this.extractOriginalLoreFromInput(second);
                }

                if (baseLore != null) {
                    resultMeta.lore(new ArrayList(baseLore));
                    resultMeta.getPersistentDataContainer().remove(this.originalLoreKey);
                    result.setItemMeta(resultMeta);
                    changed = true;
                }

                return changed;
            }
        } else {
            return false;
        }
    }

    private @Nullable List<Component> extractOriginalLoreFromInput(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return null;
            } else if (!meta.getPersistentDataContainer().has(this.originalLoreKey, PersistentDataType.STRING)) {
                return null;
            } else {
                List<Component> lore = this.loadOriginalLore(meta);
                return lore == null ? null : new ArrayList(lore);
            }
        } else {
            return null;
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            double extraDamage = (double)0.0F;
            boolean targetInWater = ((LivingEntity)event.getEntity()).isInWater();
            Entity damager = event.getDamager();
            if (damager instanceof Player) {
                Player attacker = (Player)damager;
                ItemStack heldItem = attacker.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType().isAir()) {
                    return;
                }

                int sharpnessLevel = this.getActualLevel(heldItem, this.SHARPNESS);
                if (sharpnessLevel > this.SHARPNESS.getMaxLevel()) {
                    extraDamage += this.getSharpnessDeltaBonus(sharpnessLevel);
                }

                if (heldItem.getType() == Material.TRIDENT) {
                    int impalingLevel = this.getActualLevel(heldItem, this.IMPALING);
                    if (impalingLevel > 0) {
                        if (!targetInWater) {
                            extraDamage += this.getImpalingDesiredTotalBonus(impalingLevel);
                        } else if (impalingLevel > this.IMPALING.getMaxLevel()) {
                            extraDamage += this.getImpalingDeltaBonus(impalingLevel);
                        }
                    }
                }

                if (heldItem.getType() == Material.MACE) {
                    int densityLevel = this.getActualLevel(heldItem, this.DENSITY);
                    if (densityLevel > this.DENSITY.getMaxLevel()) {
                        int extraLevels = densityLevel - this.DENSITY.getMaxLevel();
                        float fallDist = attacker.getFallDistance();
                        extraDamage += fallDist > 0.0F ? (double)extraLevels * (double)0.5F * (double)fallDist : (double)extraLevels * (double)2.0F;
                    }
                }
            }

            damager = event.getDamager();
            if (damager instanceof Trident) {
                Trident trident = (Trident)damager;
                ItemStack tridentItem = trident.getItemStack();
                if (tridentItem != null && tridentItem.getType() == Material.TRIDENT) {
                    int impalingLevel = this.getActualLevel(tridentItem, this.IMPALING);
                    if (impalingLevel > 0) {
                        if (!targetInWater) {
                            extraDamage += this.getImpalingDesiredTotalBonus(impalingLevel);
                        } else if (impalingLevel > this.IMPALING.getMaxLevel()) {
                            extraDamage += this.getImpalingDeltaBonus(impalingLevel);
                        }
                    }
                }
            }

            if (extraDamage > (double)0.0F) {
                event.setDamage(event.getDamage() + extraDamage);
            }

        }
    }

    private double getSharpnessVanillaBonusCapped(int level) {
        if (level <= 0) {
            return (double)0.0F;
        } else {
            int capped = Math.min(level, this.SHARPNESS.getMaxLevel());
            return (double)0.5F * (double)capped + (double)0.5F;
        }
    }

    private double getSharpnessDesiredTotalBonus(int level) {
        if (level <= 0) {
            return (double)0.0F;
        } else {
            int vanillaCap = this.SHARPNESS.getMaxLevel();
            if (level <= vanillaCap) {
                return (double)0.5F * (double)level + (double)0.5F;
            } else {
                double total = (double)0.5F * (double)vanillaCap + (double)0.5F;
                total += (double)(level - vanillaCap) * (double)1.0F;
                return total;
            }
        }
    }

    private double getSharpnessDeltaBonus(int level) {
        double desired = this.getSharpnessDesiredTotalBonus(level);
        double vanilla = this.getSharpnessVanillaBonusCapped(level);
        return Math.max((double)0.0F, desired - vanilla);
    }

    private double getImpalingVanillaBonusCapped(int level) {
        if (level <= 0) {
            return (double)0.0F;
        } else {
            int capped = Math.min(level, this.IMPALING.getMaxLevel());
            return (double)2.5F * (double)capped;
        }
    }

    private double getImpalingDesiredTotalBonus(int level) {
        if (level <= 0) {
            return (double)0.0F;
        } else {
            int vanillaCap = this.IMPALING.getMaxLevel();
            if (level <= vanillaCap) {
                return (double)2.5F * (double)level;
            } else {
                double total = (double)2.5F * (double)vanillaCap;
                int extraLevels = level - vanillaCap;
                total += (double)extraLevels * 0.9;
                return total;
            }
        }
    }

    private double getImpalingDeltaBonus(int level) {
        double desired = this.getImpalingDesiredTotalBonus(level);
        double vanilla = this.getImpalingVanillaBonusCapped(level);
        return Math.max((double)0.0F, desired - vanilla);
    }

    @EventHandler
    public void onAttackerThorns(EntityDamageByEntityEvent event) {
        Entity var3 = event.getEntity();
        if (var3 instanceof Player defender) {
            if (!this.pendingThornsDamageTargets.contains(defender.getUniqueId())) {
                Entity thornsDamager = event.getDamager();
                if (thornsDamager instanceof LivingEntity) {
                    LivingEntity attacker = (LivingEntity)thornsDamager;
                    double extraThorns = (double)0.0F;

                    for(ItemStack armor : defender.getInventory().getArmorContents()) {
                        if (armor != null && !armor.getType().isAir()) {
                            int level = this.getActualLevel(armor, this.THORNS);
                            if (level > this.THORNS.getMaxLevel()) {
                                extraThorns += (double)(level - this.THORNS.getMaxLevel()) * (double)1.5F;
                            }
                        }
                    }

                    final double finalThorns = extraThorns;
                    if (!(finalThorns <= (double)0.0F)) {
                        this.pendingThornsDamageTargets.add(attacker.getUniqueId());
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (attacker.isValid() && !attacker.isDead()) {
                                attacker.damage(finalThorns, defender);
                            }

                            this.pendingThornsDamageTargets.remove(attacker.getUniqueId());
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDefend(EntityDamageEvent event) {
        Entity var3 = event.getEntity();
        if (var3 instanceof Player player) {
            double var13 = (double)0.0F;
            boolean directCombatDamage = this.isDirectCombatDamage(event);

            for(ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && !armor.getType().isAir()) {
                    int protectionLevel = this.getActualLevel(armor, this.PROTECTION);
                    if (protectionLevel > this.PROTECTION.getMaxLevel()) {
                        int extraLevels = protectionLevel - this.PROTECTION.getMaxLevel();
                        if (directCombatDamage) {
                            var13 += (double)extraLevels * 0.01;
                        } else {
                            var13 += (double)extraLevels * 0.015;
                        }
                    }

                    if (this.isFireDamage(event.getCause())) {
                        int fireProtLevel = this.getActualLevel(armor, this.FIRE_PROTECTION);
                        if (fireProtLevel > this.FIRE_PROTECTION.getMaxLevel()) {
                            int extraLevels = fireProtLevel - this.FIRE_PROTECTION.getMaxLevel();
                            var13 += (double)extraLevels * 0.025;
                        }
                    }

                    if (this.isBlastDamage(event.getCause())) {
                        int blastProtLevel = this.getActualLevel(armor, this.BLAST_PROTECTION);
                        if (blastProtLevel > this.BLAST_PROTECTION.getMaxLevel()) {
                            int extraLevels = blastProtLevel - this.BLAST_PROTECTION.getMaxLevel();
                            var13 += (double)extraLevels * 0.025;
                        }
                    }

                    if (this.isProjectileDamage(event.getCause())) {
                        int projProtLevel = this.getActualLevel(armor, this.PROJECTILE_PROTECTION);
                        if (projProtLevel > this.PROJECTILE_PROTECTION.getMaxLevel()) {
                            int extraLevels = projProtLevel - this.PROJECTILE_PROTECTION.getMaxLevel();
                            var13 += (double)extraLevels * 0.025;
                        }
                    }
                }
            }

            if (!(var13 <= (double)0.0F)) {
                var13 = Math.min(var13, 0.4);
                event.setDamage(event.getDamage() * ((double)1.0F - var13));
            }
        }
    }

    private boolean isDirectCombatDamage(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return false;
        } else {
            return byEntity.getDamager() instanceof Player || byEntity.getDamager() instanceof Arrow || byEntity.getDamager() instanceof SpectralArrow || byEntity.getDamager() instanceof Trident || byEntity.getDamager() instanceof Fireball;
        }
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.LAVA || cause == DamageCause.HOT_FLOOR;
    }

    private boolean isBlastDamage(EntityDamageEvent.DamageCause cause) {
        return cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION;
    }

    private boolean isProjectileDamage(EntityDamageEvent.DamageCause cause) {
        return cause == DamageCause.PROJECTILE;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("cusenchant.admin")) {
            sender.sendMessage("권한이 없습니다.");
            return true;
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("give")) {
            if (!args[2].equalsIgnoreCase("파괴저항") && !args[2].equalsIgnoreCase("destruction_resistance")) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("플레이어를 찾을 수 없습니다.");
                    return true;
                } else {
                    Enchantment ench = this.parseEnchantAlias(args[2]);
                    if (ench == null) {
                        sender.sendMessage("알 수 없는 인챈트입니다.");
                        return true;
                    } else {
                        int amount = 1;

                        int level;
                        try {
                            level = Integer.parseInt(args[3]);
                        } catch (NumberFormatException var15) {
                            sender.sendMessage("level은 숫자여야 합니다.");
                            return true;
                        }

                        if (args.length >= 5) {
                            try {
                                amount = Integer.parseInt(args[4]);
                            } catch (NumberFormatException var14) {
                                sender.sendMessage("amount는 숫자여야 합니다.");
                                return true;
                            }
                        }

                        int cap = (Integer)this.customCaps.getOrDefault(ench, ench.getMaxLevel());
                        if (level >= 1 && level <= cap) {
                            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, Math.max(1, amount));
                            ItemMeta rawMeta = book.getItemMeta();
                            if (rawMeta instanceof EnchantmentStorageMeta) {
                                EnchantmentStorageMeta meta = (EnchantmentStorageMeta)rawMeta;
                                meta.addStoredEnchant(ench, level, true);
                                book.setItemMeta(meta);
                                this.refreshDisplay(book);
                                HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(new ItemStack[]{book});
                                leftover.values().forEach((left) -> target.getWorld().dropItemNaturally(target.getLocation(), left));
                                sender.sendMessage("책 지급 완료: " + target.getName());
                                return true;
                            } else {
                                sender.sendMessage("책 메타 생성에 실패했습니다.");
                                return true;
                            }
                        } else {
                            sender.sendMessage("허용 레벨 범위: 1 ~ " + cap);
                            return true;
                        }
                    }
                }
            } else {
                Player targetPlayer = Bukkit.getPlayerExact(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage("플레이어를 찾을 수 없습니다.");
                    return true;
                } else {
                    int level;
                    try {
                        level = Integer.parseInt(args[3]);
                    } catch (NumberFormatException var16) {
                        sender.sendMessage("level은 숫자여야 합니다.");
                        return true;
                    }

                    if (level >= 0 && level <= 5) {
                        ItemStack held = targetPlayer.getInventory().getItemInMainHand();
                        if (held.getType() != Material.SHIELD) {
                            sender.sendMessage(targetPlayer.getName() + "의 메인핸드에 방패가 없습니다.");
                            return true;
                        } else {
                            this.shieldEnchantManager.setShieldResistLevel(held, level);
                            sender.sendMessage("파괴 저항 " + level + " 적용 완료: " + targetPlayer.getName());
                            return true;
                        }
                    } else {
                        sender.sendMessage("허용 레벨 범위: 0 ~ 5");
                        return true;
                    }
                }
            }
        } else {
            sender.sendMessage("사용법: /overenchant give <player> <sharpness|impaling|protection|fire|blast|projectile|thorns|density|파괴저항> <level> [amount]");
            return true;
        }
    }

    private @Nullable Enchantment parseEnchantAlias(String arg) {
        Enchantment var10000;
        switch (arg.toLowerCase(Locale.ROOT)) {
            case "sharpness":
            case "sharp":
                var10000 = this.SHARPNESS;
                break;
            case "impaling":
            case "찌르기":
                var10000 = this.IMPALING;
                break;
            case "protection":
            case "prot":
                var10000 = this.PROTECTION;
                break;
            case "fire":
            case "fire_protection":
            case "fireprot":
                var10000 = this.FIRE_PROTECTION;
                break;
            case "blast":
            case "blast_protection":
            case "blastprot":
                var10000 = this.BLAST_PROTECTION;
                break;
            case "projectile":
            case "projectile_protection":
            case "proj":
            case "projprot":
                var10000 = this.PROJECTILE_PROTECTION;
                break;
            case "thorns":
            case "가시":
                var10000 = this.THORNS;
                break;
            case "density":
            case "육중":
                var10000 = this.DENSITY;
                break;
            default:
                var10000 = null;
        }

        return var10000;
    }

    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList();

            for(Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }

            return names;
        } else {
            return args.length == 3 && args[0].equalsIgnoreCase("give") ? Arrays.asList("sharpness", "impaling", "protection", "fire", "blast", "projectile", "thorns", "density", "파괴저항") : Collections.emptyList();
        }
    }
}
