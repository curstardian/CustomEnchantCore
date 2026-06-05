//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.custardcream.cusSmpEnchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ShieldEnchantManager implements Listener {
    public static final int MAX_LEVEL = 5;
    private static final double BASE_CHANCE_NORMAL = (double)0.25F;
    private static final double BASE_CHANCE_SPRINT = (double)0.5F;
    private static final double EFFICIENCY_BONUS_PER_LEVEL = 0.05;
    private static final double SHIELD_REDUCTION_PER_LEVEL = (double)0.125F;
    private static final double MIN_CHANCE = 0.1;
    private static final double ENCHANT_TABLE_CHANCE = (double)0.25F;
    private final JavaPlugin plugin;
    private final NamespacedKey shieldResistKey;
    private final Enchantment EFFICIENCY;

    public ShieldEnchantManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shieldResistKey = new NamespacedKey(plugin, "shield_resistance");
        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        this.EFFICIENCY = (Enchantment)registry.get(NamespacedKey.minecraft("efficiency"));
    }

    public int getShieldResistLevel(ItemStack shield) {
        if (shield != null && shield.getType() == Material.SHIELD) {
            ItemMeta meta = shield.getItemMeta();
            if (meta == null) {
                return 0;
            } else {
                Integer level = (Integer)meta.getPersistentDataContainer().get(this.shieldResistKey, PersistentDataType.INTEGER);
                return level != null ? level : 0;
            }
        } else {
            return 0;
        }
    }

    public void setShieldResistLevel(ItemStack shield, int level) {
        if (shield != null && shield.getType() == Material.SHIELD) {
            ItemMeta meta = shield.getItemMeta();
            if (meta != null) {
                if (level <= 0) {
                    meta.getPersistentDataContainer().remove(this.shieldResistKey);
                    this.removeResistLore(meta);
                } else {
                    meta.getPersistentDataContainer().set(this.shieldResistKey, PersistentDataType.INTEGER, level);
                    this.updateResistLore(meta, level);
                }

                shield.setItemMeta(meta);
            }
        }
    }

    public ItemStack createResistBook(int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) {
            return book;
        } else {
            meta.getPersistentDataContainer().set(this.shieldResistKey, PersistentDataType.INTEGER, level);
            List<Component> lore = new ArrayList();
            String var10001 = this.toRoman(level);
            lore.add(((TextComponent)Component.text("파괴 저항 " + var10001).color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            book.setItemMeta(meta);
            return book;
        }
    }

    public boolean isResistBook(ItemStack item) {
        if (item != null && item.getType() == Material.ENCHANTED_BOOK) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.getPersistentDataContainer().has(this.shieldResistKey, PersistentDataType.INTEGER);
        } else {
            return false;
        }
    }

    public int getResistBookLevel(ItemStack item) {
        if (!this.isResistBook(item)) {
            return 0;
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return 0;
            } else {
                Integer level = (Integer)meta.getPersistentDataContainer().get(this.shieldResistKey, PersistentDataType.INTEGER);
                return level != null ? level : 0;
            }
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.SHIELD) {
            if (this.getShieldResistLevel(item) <= 0) {
                if (!(ThreadLocalRandom.current().nextDouble() >= (double)0.25F)) {
                    int resistLevel = ThreadLocalRandom.current().nextInt(1, 6);
                    this.setShieldResistLevel(item, resistLevel);
                    Player var10000 = event.getEnchanter();
                    String var10001 = this.toRoman(resistLevel);
                    var10000.sendMessage(Component.text("파괴 저항 " + var10001 + " 추가 부여!").color(NamedTextColor.GREEN));
                }
            }
        }
    }

    private void updateResistLore(ItemMeta meta, int level) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList();
        }

        lore.removeIf((c) -> PlainTextComponentSerializer.plainText().serialize(c).startsWith("파괴 저항"));
        String var10001 = this.toRoman(level);
        lore.add(((TextComponent)Component.text("파괴 저항 " + var10001).color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
    }

    private void removeResistLore(ItemMeta meta) {
        List<Component> lore = meta.lore();
        if (lore != null) {
            lore.removeIf((c) -> PlainTextComponentSerializer.plainText().serialize(c).startsWith("파괴 저항"));
            meta.lore(lore);
        }
    }

    @EventHandler
    public void onAxeHitBlockingPlayer(EntityDamageByEntityEvent event) {
        Entity var3 = event.getDamager();
        if (var3 instanceof Player attacker) {
            Entity var4 = event.getEntity();
            if (var4 instanceof Player defender) {
                if (defender.isBlocking()) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    if (this.isAxe(weapon)) {
                        ItemStack offhand = defender.getInventory().getItemInOffHand();
                        if (offhand.getType() == Material.SHIELD) {
                            int resistLevel = this.getShieldResistLevel(offhand);
                            if (resistLevel > 0) {
                                boolean isSprint = attacker.isSprinting();
                                double baseChance = isSprint ? (double)0.5F : (double)0.25F;
                                if (this.EFFICIENCY != null) {
                                    int effLevel = weapon.getEnchantmentLevel(this.EFFICIENCY);
                                    baseChance += (double)effLevel * 0.05;
                                }

                                double finalChance = Math.max(0.1, baseChance - (double)resistLevel * (double)0.125F);
                                int cooldownBefore = defender.getCooldown(Material.SHIELD);
                                Bukkit.getScheduler().runTask(this.plugin, () -> {
                                    int cooldownAfter = defender.getCooldown(Material.SHIELD);
                                    if (cooldownBefore == 0 && cooldownAfter > 0 && Math.random() > finalChance) {
                                        defender.setCooldown(Material.SHIELD, 0);
                                        defender.sendMessage(Component.text("파괴 저항 발동!").color(NamedTextColor.GREEN));
                                    }

                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isAxe(ItemStack item) {
        return item != null && !item.getType().isAir() ? item.getType().name().endsWith("_AXE") : false;
    }

    private String toRoman(int num) {
        String var10000;
        switch (num) {
            case 1 -> var10000 = "I";
            case 2 -> var10000 = "II";
            case 3 -> var10000 = "III";
            case 4 -> var10000 = "IV";
            case 5 -> var10000 = "V";
            default -> var10000 = String.valueOf(num);
        }

        return var10000;
    }
}
