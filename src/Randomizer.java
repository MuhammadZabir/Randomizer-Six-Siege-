import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Operator;
import entity.Weapon;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Randomizer {

    public static void main(String[] args) {
        try (Stream<Path> playerPaths = Files.walk(Paths.get(System.getProperty("user.dir") + "/players"))) {
            Path configurationFile = Paths.get(System.getProperty("user.dir") + "/configuration/config.txt");
            List<Path> playerFiles = playerPaths.filter(Files::isRegularFile).collect(Collectors.toList());

            String jsonConfig = new String(Files.readAllBytes(configurationFile));

            ObjectMapper objectMapper = new ObjectMapper();
            List<Operator> allOperators = objectMapper.readValue(jsonConfig, new TypeReference<List<Operator>>(){});
            Map<String, Operator> operatorWithName = allOperators.stream().collect(Collectors.toMap(Operator::getName, Function.identity()));
            Map<String, List<Operator>> playersOperator = new HashMap<>();

            for (Path player : playerFiles) {
                List<Operator> operators = new ArrayList<>();
                String playerName = "";
                String[] nameWithExtension = player.getFileName().toString().split(Pattern.quote("."));
                for (int x = 0; x < nameWithExtension.length ; x++) {
                    if (x != nameWithExtension.length - 1) {
                        if (x != 0) {
                            playerName = playerName + ".";
                        }
                        playerName = playerName + nameWithExtension[x];
                    }

                }
                try (Stream<String> stream = Files.lines(player)) {
                    stream.forEach((csv) -> {
                        String[] operatorNames = csv.split(",");
                        for (String name : operatorNames) {
                            operators.add(operatorWithName.get(name));
                        }
                    });
                }
                playersOperator.put(playerName, operators);
            }

            Map<String, List<Operator>> firstTeam = null;
            Map<String, List<Operator>> secondTeam = null;
            if (args[0].equalsIgnoreCase("custom")) {
                int limit = new BigDecimal(playersOperator.size()).divide(new BigDecimal(2), RoundingMode.HALF_UP).intValue();
                firstTeam = new HashMap<>();
                secondTeam = new HashMap<>();
                for (Map.Entry<String, List<Operator>> entry : playersOperator.entrySet()) {
                    int index = 2;
                    while (index == 2) {
                        index = ThreadLocalRandom.current().nextInt(3);
                        if (index == 0) {
                            if (firstTeam.size() < limit) {
                                firstTeam.put(entry.getKey(), entry.getValue());
                            } else {
                                secondTeam.put(entry.getKey(), entry.getValue());
                            }
                        }

                        if (index == 1) {
                            if (secondTeam.size() < limit) {
                                secondTeam.put(entry.getKey(), entry.getValue());
                            } else {
                                firstTeam.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                }
            }

            StringBuilder output = new StringBuilder();
            int count = 1;
            int totalRound = Integer.parseInt(args[1]);
            boolean attacker = true;

            while (count <= totalRound) {
                if (args[0].equalsIgnoreCase("normal")) {
                    randomizing(count, output, playersOperator, attacker);
                } else if (args[0].equalsIgnoreCase("custom")) {
                    randomizing(count, output, firstTeam, attacker);
                    randomizing(count, output, secondTeam, !attacker);
                }
                if (attacker) {
                    attacker = false;
                } else {
                    attacker = true;
                }
                count++;
            }

            File file = new File("result.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(output.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void randomizing(int count, StringBuilder output, Map<String, List<Operator>> playersOperator, boolean attacker) {
        if (attacker) {
            output.append("Round " + count + " (Attacker)");
        } else {
            output.append("Round " + count + " (Defender)");
        }
        output.append(System.getProperty("line.separator") + System.getProperty("line.separator"));
        Set<Operator> banOperators = new HashSet<>();
        for (Map.Entry<String, List<Operator>> entry : playersOperator.entrySet()) {
            output.append("Player\t\t: " + entry.getKey());
            output.append(System.getProperty("line.separator"));
            List<Operator> operators;
            if (attacker) {
                operators = entry.getValue().stream().filter(operator -> operator != null && operator.getType().equalsIgnoreCase("Attacker")).collect(Collectors.toList());
            } else {
                operators = entry.getValue().stream().filter(operator -> operator != null && operator.getType().equalsIgnoreCase("Defender")).collect(Collectors.toList());
            }

            boolean flag = false;
            Operator operator = null;
            while (!flag) {
                int index = ThreadLocalRandom.current().nextInt(operators.size());
                operator = operators.get(index);
                if (!banOperators.contains(operator) || operator.getName().equalsIgnoreCase("Recruit Attacker") || operator.getName().equalsIgnoreCase("Recruit Defender")) {
                    flag = true;
                }
            }
            banOperators.add(operator);
            output.append("Operator\t: " + operator.getName());
            output.append(System.getProperty("line.separator"));
            List<Weapon> primaryWeapons = operator.getWeapons().stream().filter(weapon -> weapon.getType().equalsIgnoreCase("Primary")).collect(Collectors.toList());
            List<Weapon> secondaryWeapons = operator.getWeapons().stream().filter(weapon -> weapon.getType().equalsIgnoreCase("Secondary")).collect(Collectors.toList());
            List<Weapon> gadgets = operator.getWeapons().stream().filter(weapon -> weapon.getType().equalsIgnoreCase("Gadget")).collect(Collectors.toList());
            int index = ThreadLocalRandom.current().nextInt(primaryWeapons.size());
            output.append("Primary\t\t: " + primaryWeapons.get(index).getName());
            output.append(System.getProperty("line.separator"));
            index = ThreadLocalRandom.current().nextInt(secondaryWeapons.size());
            output.append("Secondary\t: " + secondaryWeapons.get(index).getName());
            output.append(System.getProperty("line.separator"));
            index = ThreadLocalRandom.current().nextInt(gadgets.size());
            output.append("Gadget\t\t: " + gadgets.get(index).getName());
            output.append(System.getProperty("line.separator") + System.getProperty("line.separator"));
        }
        output.append("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        output.append(System.getProperty("line.separator") + System.getProperty("line.separator"));
    }
}
