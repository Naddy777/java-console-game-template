package com.example.dungeon.core;

import com.example.dungeon.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private final GameState state = new GameState();
    private final Map<String, Command> commands = new LinkedHashMap<>();

    static {
        WorldInfo.touch("Game");
    }

    public Game() {
        registerCommands();
        bootstrapWorld();
    }

    private void registerCommands() {
        commands.put("help", (ctx, a) -> System.out.println("Команды: " + String.join(", ", commands.keySet())));
        commands.put("gc-stats", (ctx, a) -> {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory(), used = total - free;
            System.out.println("Память: used=" + used + " free=" + free + " total=" + total);
        });
        commands.put("look", (ctx, a) -> System.out.println(ctx.getCurrent().describe()));
        commands.put("move", (ctx, args) -> {
//            System.out.println("Куда идем? Выбери направление move north, move south, move east, move west");
            //Проверка на кол-во аргументов:
            if (args.isEmpty()) {
                throw new InvalidCommandException("Неверно задано направление. Выбери, куда идем: move north, move south, move east, move west");
            }
            String direction = String.join(" ", args).toLowerCase();
            //Получаем следующую комнату из карты
            Room currentRoom = ctx.getCurrent();
            Room nextRoom = currentRoom.getNeighbors().get(direction);
            //Если нашли, то перемещаемся
            if (nextRoom != null) {
//                Проверяем, не закрыта ли дверь
                if (nextRoom.isLocked()) {
                    System.out.println("Дверь заперта. Нужен ключ");
                    return;
                }
                ctx.setCurrent(nextRoom);
                System.out.println("Вы перешли в: "+nextRoom.getName());
                //Описание локации
                System.out.println(nextRoom.describe());
            }
            else {
                throw new InvalidCommandException("Нет пути: "+direction);
            }
        });
        commands.put("take", (ctx, args) -> {
            if (args.isEmpty()) {
                throw new InvalidCommandException("Не указан предмет");
            }
            String itemName = String.join(" ", args);
            Room currentRoom = ctx.getCurrent();
            //Ищем предмет в комнате по наименованию
            Optional<Item> foundItem = currentRoom.getItems().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if (foundItem.isPresent()) {
                Item item = foundItem.get();
                //Удаляем из комнаты
                currentRoom.getItems().remove(item);
                //Добавляем в инвентарь игрока
                ctx.getPlayer().getInventory().add(item);
                System.out.println("Взято: "+item.getName());
            } else {
                throw new InvalidCommandException("Предмет \"+itemName+\" не найден");}
        });
        commands.put("inventory", (ctx, args) -> {
            List<Item> inventory = ctx.getPlayer().getInventory();
            //Проверяем состояние инвентаря
            if (inventory.isEmpty()) {
                System.out.println("Инвентарь пуст");
                return;
            }
            //Группировка предметов по классам
            Map<String, List<Item>> groupedItems = inventory.stream()
                    .collect(Collectors.groupingBy(item -> item.getClass().getSimpleName()));
            //Проходим по каждой группе и выводим информацию
            groupedItems.forEach((type, items) -> {
                //Сортировка по имени в группе
                List<String> itemNames = items.stream()
                        .map(Item::getName)
                        .sorted()
                        .toList();
                System.out.println("- "+type+" ("+items.size()+"): "+String.join(", ", itemNames));
            });
        });

        commands.put("use", (ctx, args) -> {
            if (args.isEmpty()) {
                throw new InvalidCommandException("Не указан предмет");
            }
            //Объединяем в одну строку аргументы
            String itemName = String.join(" ", args);
            Player player = ctx.getPlayer();
            //Ищем в инвентаре
            Optional<Item> foundItem = player.getInventory().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if (foundItem.isPresent()) {
                Item item = foundItem.get();
                item.apply(ctx);
            } else {
                throw new InvalidCommandException("В вашем инвентаре нет " + itemName);
            }
        });
        commands.put("fight", (ctx, args) -> {
            Room currentRoom = ctx.getCurrent();
            Monster monster = currentRoom.getMonster();
            Player player = ctx.getPlayer();
            //Проверяем, что есть монстр
            if (monster == null) {
                throw new InvalidCommandException("Здесь не с кем сражаться");
            }
            //Бой>
            //Ход игрока
            monster.setHp(monster.getHp() - player.getAttack());
            System.out.println("Вы бьёте "+monster.getName()+" на "+player.getAttack()+". HP монстра: "+monster.getHp());
            //Проверяем, жив ли монстр
            if (monster.getHp() <= 0) {
                System.out.println("Вы победили "+monster.getName()+" (ур. "+monster.getLevel()+")");
                currentRoom.setMonster(null);
                Item loot = monster.getLoot();
                if (loot != null) {
                    currentRoom.getItems().add(loot);
                    System.out.println(monster.getName()+" (ур. "+monster.getLevel()+")"+" оставил после себя "+loot.getName());
                }
                int scoreForWin = monster.getLevel(); //Начисляем кол-во очков от уровня монстра
                ctx.addScore(scoreForWin); //Записываем счет
            } else {
                //Ход монстра
                //Принимаем урон монстра = уровень монстра
                int monsterDamage = monster.getLevel();
                player.setHp(player.getHp()-monsterDamage);
                System.out.println("Монстр отвечает на "+monsterDamage+". Ваше HP: "+player.getHp());

                if (player.getHp() <= 0) {
                    System.out.println("Вы потерпели поражение");
                    System.exit(0);
                }
            }
        });
        commands.put("save", (ctx, a) -> SaveLoad.save(ctx));
        commands.put("load", (ctx, a) -> SaveLoad.load(ctx));
        commands.put("scores", (ctx, a) -> SaveLoad.printScores());
        commands.put("exit", (ctx, a) -> {
            System.out.println("Пока!");
            System.exit(0);
        });
    }

    private void bootstrapWorld() {
        Scanner scanner = new Scanner(System.in);// надо добавить условие на загрузку: загрузить? или новый игрок?
        System.out.print("Введите ваше имя: ");
        String player = scanner.nextLine();
        Player hero = new Player(player, 20, 5);
        state.setPlayer(hero);

        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");
        Room castle = new Room("Замок", "Мощные ворота возвышаются и уходят к небу.");
        Room lawn = new Room("Поляна", "В центре огромный камень.");
        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        forest.getNeighbors().put("west", lawn);
        cave.getNeighbors().put("west", forest);
        castle.getNeighbors().put("east", square);
        lawn.getNeighbors().put("east", forest);
        square.getNeighbors().put("west", castle);

        forest.getItems().add(new Potion("Малое зелье", 5));
        forest.setMonster(new Monster("Волк", 1, 8, new Potion("Среднее зелье", 10)));
        cave.setMonster(new Monster("Змея", 2, 16, new Weapon("Стальной клинок", 11)));
        lawn.getItems().add(new Key("Ключ от замка"));
        castle.setLocked(1);

        state.setCurrent(square);
    }

    public void run() {
        System.out.println("Добро пожаловать в игру DungeonMini (TEMPLATE).  Если Вы хотите зашрузить предыдущую игру, введите load. Для вызова команд введите 'help'.");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> parts = Arrays.asList(line.split("\s+"));
                String cmd = parts.getFirst().toLowerCase(Locale.ROOT);
                List<String> args = parts.subList(1, parts.size());
                Command c = commands.get(cmd);
                try {
                    if (c == null) throw new InvalidCommandException("Неизвестная команда: " + cmd);
                    c.execute(state, args);
                    state.addScore(1);
                } catch (InvalidCommandException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Непредвиденная ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}
