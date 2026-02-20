package com.naoyamod.ability;

/**
 * The three speed tiers of Naoya's Divine Speed technique.
 * Burst is handled as a boolean overlay on Level 3 (not a separate enum).
 */
public enum AbilityLevel {
    //          speedMult  dmgBonus  knockback
    NONE  (0,   0.0f,      0.0f,     0.0f),
    LEVEL_1(1,  3.8f,      2.5f,     2.0f),
    LEVEL_2(2,  7.5f,      5.0f,     3.5f),
    LEVEL_3(3,  13.0f,     9.0f,     6.0f);

    /** Speed multiplier on top of base 0.1 b/t walk speed. */
    public final int    level;
    public final float  speedMultiplier;
    public final float  damageBonus;
    public final float  knockbackStrength;

    /** Burst adds this extra multiplier on top of Level 3. */
    public static final float BURST_EXTRA_MULTIPLIER = 1.6f; // 13 * 1.6 ≈ 20.8x

    AbilityLevel(int l, float s, float d, float k) {
        level = l; speedMultiplier = s; damageBonus = d; knockbackStrength = k;
    }

    public static AbilityLevel fromLevel(int l) {
        for (AbilityLevel a : values()) if (a.level == l) return a;
        return NONE;
    }

    public boolean isActive()         { return this != NONE; }
    public boolean slowRotation()     { return this == LEVEL_2 || this == LEVEL_3; }
    public boolean canWalkOnWater()   { return this == LEVEL_2 || this == LEVEL_3; }
    public boolean hasSoundBarrier()  { return this == LEVEL_2 || this == LEVEL_3; }
    public boolean canUseBurst()      { return this == LEVEL_3; }
}
