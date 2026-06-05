//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.custardcream.cusSmpEnchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class WindBurstManager implements Listener {
    private final Logger logger;
    private final Enchantment WIND_BURST;
    private final Map<UUID, Long> buffExpiry = new HashMap();
    private final Map<UUID, Long> cooldownExpiry = new HashMap();
    private final Map<UUID, Integer> buffLevel = new HashMap();
    private static final long BUFF_DURATION_MS = 4000L;
    private static final long COOLDOWN_AFTER_BUFF_MS = 7000L;

    public WindBurstManager(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        Enchantment wb = (Enchantment)registry.get(NamespacedKey.minecraft("wind_burst"));
        this.WIND_BURST = wb;
        if (wb == null) {
            this.logger.warning("wind_burst 인챈트를 찾을 수 없습니다. WindBurstManager 비활성화.");
        }

    }

    @EventHandler
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if (this.WIND_BURST != null) {
            Entity var3 = event.getDamager();
            if (var3 instanceof Player) {
                Player attacker = (Player)var3;
                if (event.getEntity() instanceof Player) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    if (weapon.getType() == Material.MACE) {
                        int level = weapon.getEnchantmentLevel(this.WIND_BURST);
                        if (level > 0) {
                            UUID uid = attacker.getUniqueId();
                            long now = System.currentTimeMillis();
                            Long existingBuff = (Long)this.buffExpiry.get(uid);
                            if (existingBuff == null || existingBuff <= now) {
                                Long cd = (Long)this.cooldownExpiry.get(uid);
                                if (cd == null || cd <= now) {
                                    this.buffExpiry.put(uid, now + 4000L);
                                    this.cooldownExpiry.put(uid, now + 4000L + 7000L);
                                    this.buffLevel.put(uid, level);
                                    int reductionPct = Math.min(20 + level * 10, 50);
                                    attacker.sendMessage(Component.text("바람 강타! 낙하 피해 -%d%% (4초)".formatted(reductionPct)).color(NamedTextColor.AQUA));
                                    attacker.getWorld().spawnParticle(Particle.GUST, attacker.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 8, 0.3, 0.3, 0.3, (double)0.0F);
                                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0F, 1.2F);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == DamageCause.FALL) {
            Entity var3 = event.getEntity();
            if (var3 instanceof Player) {
                Player player = (Player)var3;
                UUID var8 = player.getUniqueId();
                Long expiry = (Long)this.buffExpiry.get(var8);
                if (expiry != null && System.currentTimeMillis() < expiry) {
                    int level = (Integer)this.buffLevel.getOrDefault(var8, 1);
                    double reduction = Math.min(0.2 + (double)level * 0.1, (double)0.5F);
                    event.setDamage(event.getDamage() * ((double)1.0F - reduction));
                }
            }
        }
    }
}
