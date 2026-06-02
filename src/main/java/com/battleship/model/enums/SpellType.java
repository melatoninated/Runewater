package com.battleship.model.enums;

import com.battleship.interfaces.Describable;

/**
 * 6 jurus total, 2 per elemen. Setiap Mage mendapat satu jurus saat dibuat.
 * Jurus hanya bisa dipakai SEKALI per battle — keputusan kapan memakainya
 * adalah salah satu keputusan strategis utama dalam game.
 *
 *  INFERNO    [FIRE]  — Damage x1.6 dengan elemen. Mage kelelahan.
 *  IGNITE     [FIRE]  — Damage normal + BURNED 3 turn (DoT 8 HP/turn).
 *  TIDAL_WAVE [WATER] — Damage x1.45 + heal diri sendiri 35 HP.
 *  FREEZE     [WATER] — Damage normal + FROZEN 1 turn (skip + kebal status).
 *  CHAIN_BOLT [STORM] — Damage x1.55 + WEAKENED musuh 3 turn (-30% dmg).
 *  OVERCHARGE [STORM] — Setiap Mage di roster menyerang x0.85 masing-masing.
 */
public enum SpellType implements Describable {
    // FIRE spells
    INFERNO   ("Inferno",    "[FIRE]  Damage x1.6 dengan elemen. Mage kelelahan."),
    IGNITE    ("Ignite",     "[FIRE]  Damage normal + BURNED 3 turn (DoT)."),
    // WATER spells
    TIDAL_WAVE("Tidal Wave", "[WATER] Damage x1.45 + heal diri 35 HP."),
    FREEZE    ("Freeze",     "[WATER] Damage normal + FROZEN 1 turn (skip + kebal status)."),
    // STORM spells
    CHAIN_BOLT("Chain Bolt", "[STORM] Damage x1.55 + WEAKENED musuh 3 turn (-30% dmg)."),
    OVERCHARGE("Overcharge", "[STORM] Tiap Mage di roster menyerang x0.85 masing-masing.");

    private final String displayName;
    private final String description;

    SpellType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String getFullDescription() {
        return String.format("%-12s — %s", displayName, description);
    }
}
