package com.example.dungeon.model;

public class Key extends Item {
    public Key(String name) {
        super(name);
    }

    @Override
    public void apply(GameState ctx) {// переписан метод на открытие двери ключом
        Room currentRoom = ctx.getCurrent();
        //System.out.println("Ключ звенит. Возможно, где-то есть дверь...");
        boolean keyUnlock = currentRoom.getNeighbors().values().stream()
                .anyMatch(room -> room.getLocked() == 1);
        if (keyUnlock) {
            currentRoom.getNeighbors().values().stream()
                    .filter(room -> room.getLocked() == 1)
                    .forEach(room -> room.setLocked(0));
            System.out.println("Дверь открыта ключом! ");
        }
    }
}
