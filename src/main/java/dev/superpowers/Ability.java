package dev.superpowers;

public enum Ability {
    DASH(60),
    PUNCH(40),
    SLAM(140),
    FLIGHT(20);

    public final int cooldownTicks;

    Ability(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public static Ability byId(int id) {
        Ability[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
