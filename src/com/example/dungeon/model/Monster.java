package com.example.dungeon.model;

public class Monster extends Entity {
    private int level;
    private Item loot;

    public Monster(String name, int level, int hp) {
        super(name, hp);
        this.level = level;
    }
    public Monster(String name, int level, int hp, Item loot) {
        super(name, hp);
        this.level = level;
        this.loot = loot;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Item getLoot() {return loot;}

    public void setLoot(Item loot) {this.loot = loot;}
}
