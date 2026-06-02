package com.battleship;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.battleship.engine.BattleEngine;
import com.battleship.model.EnemyShip;
import com.battleship.model.Mage;
import com.battleship.model.PlayerShip;
import com.battleship.model.enums.Element;
import com.battleship.model.enums.EnemyTrait;
import com.battleship.model.enums.SpellType;
import com.battleship.model.enums.StatusEffect;
import com.battleship.util.InputHelper;
import com.battleship.util.TerminalUi;

class BattleEngineTest {

    @Test
    void constructor_createsEngine() {
        // Arrange
        PlayerShip player = new PlayerShip("Test", 100, 20, Element.FIRE);
        InputHelper input = new InputHelper(new Scanner(System.in));
        TerminalUi ui = new TerminalUi(input);
        Random rng = new Random(42);
        AtomicBoolean cannonDoubled = new AtomicBoolean(false);

        // Act
        BattleEngine engine = new BattleEngine(player, ui, rng, cannonDoubled);

        // Assert
        assertNotNull(engine, "BattleEngine should be created successfully");
    }

    @Test
    void victoryCondition_playerWins() {
        // Arrange - Player with high HP vs weak enemy
        PlayerShip player = new PlayerShip("Test", 100, 50, Element.FIRE);
        com.battleship.model.EnemyShip enemy = new com.battleship.model.EnemyShip(
            "WeakEnemy", 10, 1, Element.WATER, 10, 5, com.battleship.model.enums.EnemyTrait.NONE);

        // Act - Player attacks enemy
        player.attack(enemy);

        // Assert
        assertFalse(enemy.isAlive(), "Enemy should be dead after taking 50 damage");
        assertTrue(player.isAlive(), "Player should still be alive");
    }

    @Test
    void victoryCondition_enemyWins() {
        // Arrange - Weak player vs strong enemy
        PlayerShip player = new PlayerShip("Test", 10, 1, Element.FIRE);
        com.battleship.model.EnemyShip enemy = new com.battleship.model.EnemyShip(
            "StrongEnemy", 100, 50, Element.WATER, 10, 5, com.battleship.model.enums.EnemyTrait.NONE);

        // Act - Enemy attacks player
        enemy.attack(player);

        // Assert
        assertFalse(player.isAlive(), "Player should be dead after taking 50 damage");
        assertTrue(enemy.isAlive(), "Enemy should still be alive");
    }

    @Test
    void runBattle_frozenEnemySkipsTurn() {
        // Arrange
        PlayerShip player = new PlayerShip("Test", 100, 20, Element.FIRE);
        EnemyShip enemy = new EnemyShip("FrozenEnemy", 20, 10, Element.WATER, 10, 5, EnemyTrait.NONE);
        enemy.applyStatus(StatusEffect.FROZEN, 1);
        TerminalUi ui = new ScriptedTerminalUi(4, 1, 1);
        BattleEngine engine = new BattleEngine(player, ui, new Random(42), new AtomicBoolean(false));

        // Act
        boolean playerWon = engine.runBattle(enemy);

        // Assert
        assertTrue(playerWon, "Player should win after the scripted cannon shot");
        assertEquals(100, player.getCurrentHp(), "Frozen enemy should skip instead of attacking");
    }

    @Test
    void doSpellAttack_thornsReflectsSpellDamage() {
        // Arrange
        PlayerShip player = new PlayerShip("Test", 100, 10, Element.FIRE);
        player.recruitMage(new Mage("Ignis", Element.FIRE, 10, SpellType.INFERNO));
        EnemyShip enemy = new EnemyShip("Thorny", 100, 10, Element.WATER, 10, 5, EnemyTrait.THORNS);
        TerminalUi ui = new ScriptedTerminalUi(1);
        BattleEngine engine = new BattleEngine(player, ui, new Random(42), new AtomicBoolean(false));

        // Act
        engine.doSpellAttack(enemy, new ArrayList<>());

        // Assert
        assertEquals(84, enemy.getCurrentHp(), "Inferno should now respect element resistance");
        assertEquals(98, player.getCurrentHp(), "THORNS should reflect 15% of spell damage");
    }

    private static class ScriptedTerminalUi extends TerminalUi {
        private final Queue<Integer> choices;

        ScriptedTerminalUi(Integer... choices) {
            super(new InputHelper(new Scanner("")));
            this.choices = new ArrayDeque<>(List.of(choices));
        }

        @Override
        public int promptChoice(String title, List<String> body, int min, int max, int defaultValue, String prompt) {
            return choices.isEmpty() ? defaultValue : choices.remove();
        }

        @Override
        public void pause(String title, List<String> body) {
            // No interactive terminal during tests.
        }
    }
}
