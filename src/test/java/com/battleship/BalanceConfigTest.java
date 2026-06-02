package com.battleship;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.util.BalanceConfig;

class BalanceConfigTest {

    @Test
    void playerStats_arePositive() {
        assertTrue(BalanceConfig.PLAYER_HP > 0, "Player HP should be positive");
        assertTrue(BalanceConfig.PLAYER_DAMAGE > 0, "Player damage should be positive");
        assertTrue(BalanceConfig.START_MAGE_PWR > 0, "Start mage power should be positive");
    }

    @Test
    void enemyScaling_isPositive() {
        assertTrue(BalanceConfig.BASE_ENEMY_HP > 0, "Base enemy HP should be positive");
        assertTrue(BalanceConfig.HP_SCALE >= 0, "HP scale should be non-negative");
        assertTrue(BalanceConfig.POST_BOSS_HP_SCALE >= 0, "Post-boss HP scale should be non-negative");
        assertTrue(BalanceConfig.BASE_ENEMY_DMG > 0, "Base enemy damage should be positive");
        assertTrue(BalanceConfig.DMG_SCALE >= 0, "DMG scale should be non-negative");
        assertTrue(BalanceConfig.POST_BOSS_DMG_SCALE >= 0, "Post-boss damage scale should be non-negative");
    }

    @Test
    void bossMultipliers_areGreaterThanOne() {
        assertTrue(BalanceConfig.BOSS_HP_MULT > 1.0, "Boss HP multiplier should be > 1");
        assertTrue(BalanceConfig.BOSS_DMG_MULT > 1.0, "Boss damage multiplier should be > 1");
    }

    @Test
    void rewardValues_arePositive() {
        assertTrue(BalanceConfig.BASE_XP > 0, "Base XP should be positive");
        assertTrue(BalanceConfig.BASE_BOUNTY > 0, "Base bounty should be positive");
        assertTrue(BalanceConfig.BOSS_BOUNTY_BON > 0, "Boss bounty bonus should be positive");
    }

    @Test
    void burstSpellMultipliers_areBelowOldOneShotValues() {
        assertTrue(BalanceConfig.INFERNO_MULT < 2.0, "Inferno should be bursty without being a universal one-shot");
        assertTrue(BalanceConfig.TIDAL_WAVE_MULT < 2.0, "Tidal Wave should pay for its heal utility");
        assertTrue(BalanceConfig.CHAIN_BOLT_MULT < 2.0, "Chain Bolt should pay for WEAKENED utility");
        assertTrue(BalanceConfig.OVERCHARGE_MULT < 1.0, "Overcharge should scale carefully with roster size");
    }
}
