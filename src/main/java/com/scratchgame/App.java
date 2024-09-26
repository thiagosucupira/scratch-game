// App.java
package com.scratchgame;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.*;

public class App {
    public static void main(String[] args) {
        // Parse command-line arguments
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "Config file path");
        configOption.setRequired(true);
        options.addOption(configOption);

        Option betOption = new Option("b", "betting-amount", true, "Betting amount");
        betOption.setRequired(true);
        options.addOption(betOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("scratch-game", options);

            System.exit(1);
            return;
        }

        String configFilePath = cmd.getOptionValue("config");
        double betAmount = Double.parseDouble(cmd.getOptionValue("betting-amount"));

        // Load config json
        ObjectMapper mapper = new ObjectMapper();
        Config config;

        try {
            config = mapper.readValue(new File(configFilePath), Config.class);
        } catch (Exception e) {
            System.out.println("Failed to load config: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Generate the game matrix
        String[][] matrix = generateMatrix(config);

        // Calculate rewards
        GameResult result = calculateRewards(config, matrix, betAmount);

        // Prepare output
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("matrix", matrix);
        output.put("reward", result.reward);
        if (result.reward > 0) {
            output.put("applied_winning_combinations", result.appliedWinningCombinations);
            output.put("applied_bonus_symbol", result.appliedBonusSymbol);
        }

        try {
            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
            System.out.println(jsonOutput);
        } catch (Exception e) {
            System.out.println("Failed to output result: " + e.getMessage());
        }
    }

    // Method to generate the game matrix
    private static String[][] generateMatrix(Config config) {
        int rows = config.rows;
        int columns = config.columns;
        String[][] matrix = new String[rows][columns];

        Random rand = new Random();

        int standardTotalProb = 0;
        if (!config.probabilities.standard_symbols.isEmpty()) {
            standardTotalProb = config.probabilities.standard_symbols.get(0).symbols.values().stream().mapToInt(Integer::intValue).sum();
        }

        // Calculate total probability numbers for bonus symbols
        int bonusTotalProb = config.probabilities.bonus_symbols.symbols.values().stream().mapToInt(Integer::intValue).sum();

        double bonusProbability = (double) bonusTotalProb / (standardTotalProb + bonusTotalProb);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // Decide whether to place a bonus symbol based on configured probabilities
                boolean placeBonusSymbol = rand.nextDouble() < bonusProbability;

                if (placeBonusSymbol) {
                    // Place a bonus symbol
                    Map<String, Double> bonusSymbolProbabilities = getBonusSymbolProbabilities(config);
                    String bonusSymbol = getRandomSymbol(bonusSymbolProbabilities);
                    matrix[i][j] = bonusSymbol;
                } else {
                    // Place a standard symbol
                    Map<String, Double> standardSymbolProbabilities = getStandardSymbolProbabilities(config, i, j);
                    String standardSymbol = getRandomSymbol(standardSymbolProbabilities);
                    matrix[i][j] = standardSymbol;
                }
            }
        }

        return matrix;
    }

    // Helper method to get standard symbol probabilities for a specific cell
    private static Map<String, Double> getStandardSymbolProbabilities(Config config, int row, int column) {
        Map<String, Double> probabilities = new HashMap<>();
        StandardSymbolProbability cellProb = null;

        // Find the probability configuration for this cell
        for (StandardSymbolProbability probEntry : config.probabilities.standard_symbols) {
            if (probEntry.row == row && probEntry.column == column) {
                cellProb = probEntry;
                break;
            }
        }

        if (cellProb == null) {
            // Use default probabilities if none specified for this cell
            if (!config.probabilities.standard_symbols.isEmpty()) {
                cellProb = config.probabilities.standard_symbols.get(0);
            } else {
                // No probabilities specified, default to equal probabilities
                int numSymbols = (int) config.symbols.values().stream().filter(s -> s.type.equals("standard")).count();
                double equalProb = 1.0 / numSymbols;
                for (String symbol : config.symbols.keySet()) {
                    if (config.symbols.get(symbol).type.equals("standard")) {
                        probabilities.put(symbol, equalProb);
                    }
                }
                return probabilities;
            }
        }

        // Calculate total probability sum
        int total = cellProb.symbols.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : cellProb.symbols.entrySet()) {
            probabilities.put(entry.getKey(), entry.getValue() / (double) total);
        }

        return probabilities;
    }

    private static Map<String, Double> getBonusSymbolProbabilities(Config config) {
        Map<String, Double> probabilities = new HashMap<>();
        int total = config.probabilities.bonus_symbols.symbols.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : config.probabilities.bonus_symbols.symbols.entrySet()) {
            probabilities.put(entry.getKey(), entry.getValue() / (double) total);
        }
        return probabilities;
    }

    // Random symbol selection based on probabilities
    private static String getRandomSymbol(Map<String, Double> probabilities) {
        double p = Math.random();
        double cumulativeProbability = 0.0;
        for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (p <= cumulativeProbability) {
                return entry.getKey();
            }
        }
        // Fallback in case of rounding errors
        List<String> symbols = new ArrayList<>(probabilities.keySet());
        return symbols.get(symbols.size() - 1);
    }

    // GameResult class to hold the result
    static class GameResult {
        public double reward;
        public Map<String, List<String>> appliedWinningCombinations;
        public String appliedBonusSymbol;

        public GameResult(double reward, Map<String, List<String>> appliedWinningCombinations, String appliedBonusSymbol) {
            this.reward = reward;
            this.appliedWinningCombinations = appliedWinningCombinations;
            this.appliedBonusSymbol = appliedBonusSymbol;
        }
    }

   static GameResult calculateRewards(Config config, String[][] matrix, double betAmount) {
    double totalReward = 0;
    Map<String, List<String>> appliedWinningCombinations = new HashMap<>();
    String appliedBonusSymbol = null;

    Map<String, Integer> symbolCounts = new HashMap<>();

    // Count standard symbols
    for (String[] row : matrix) {
        for (String cell : row) {
            String symbol = cell;
            Symbol symObj = config.symbols.get(symbol);
            if (symObj.type.equals("standard")) {
                symbolCounts.put(symbol, symbolCounts.getOrDefault(symbol, 0) + 1);
            }
        }
    }

    Map<String, Double> symbolRewards = new HashMap<>();

    // Apply winning combinations for each symbol
    for (String symbol : symbolCounts.keySet()) {
        Symbol symObj = config.symbols.get(symbol);
        double symbolRewardMultiplier = symObj.reward_multiplier;
        List<String> wins = new ArrayList<>();
        Set<String> appliedGroups = new HashSet<>();
        double totalWinCombinationMultiplier = 1.0;

        // Check for "same_symbols" winning combinations
        int count = symbolCounts.get(symbol);
        double maxRewardMultiplier = 0;
        String maxWinCombKey = null;

        for (String winKey : config.win_combinations.keySet()) {
            WinCombination winComb = config.win_combinations.get(winKey);
            if (winComb.when.equals("same_symbols") && count >= winComb.count) {
                if (!appliedGroups.contains(winComb.group)) {
                    if (winComb.reward_multiplier > maxRewardMultiplier) {
                        maxRewardMultiplier = winComb.reward_multiplier;
                        maxWinCombKey = winKey;
                    }
                }
            }
        }

        if (maxWinCombKey != null) {
            WinCombination winComb = config.win_combinations.get(maxWinCombKey);
            appliedGroups.add(winComb.group);
            wins.add(maxWinCombKey);
            totalWinCombinationMultiplier *= winComb.reward_multiplier;
        }

        // Check for "linear_symbols" winning combinations
        for (String winKey : config.win_combinations.keySet()) {
            WinCombination winComb = config.win_combinations.get(winKey);
            if (winComb.when.equals("linear_symbols")) {
                for (List<String> area : winComb.covered_areas) {
                    // For each area, check if all symbols are the same and standard
                    boolean allSame = true;
                    for (String pos : area) {
                        String[] indices = pos.split(":");
                        int row = Integer.parseInt(indices[0]);
                        int col = Integer.parseInt(indices[1]);
                        if (row >= config.rows || col >= config.columns) {
                            allSame = false;
                            break;
                        }
                        String cellSymbol = matrix[row][col];
                        Symbol cellSymObj = config.symbols.get(cellSymbol);
                        if (!cellSymObj.type.equals("standard")) {
                            allSame = false;
                            break;
                        }
                        if (!cellSymbol.equals(symbol)) {
                            allSame = false;
                            break;
                        }
                    }
                    if (allSame) {
                        // Apply the winning combination to this symbol
                        if (!appliedGroups.contains(winComb.group)) {
                            wins.add(winKey);
                            appliedGroups.add(winComb.group);
                            totalWinCombinationMultiplier *= winComb.reward_multiplier;
                        } else {
                            // If a winning combination from this group is already applied, check if this one has a higher multiplier
                            for (String appliedWinKey : wins) {
                                WinCombination appliedWinComb = config.win_combinations.get(appliedWinKey);
                                if (appliedWinComb.group.equals(winComb.group)) {
                                    if (winComb.reward_multiplier > appliedWinComb.reward_multiplier) {
                                        // Replace the existing win combination
                                        wins.remove(appliedWinKey);
                                        wins.add(winKey);

                                        // Adjust the totalWinCombinationMultiplier
                                        totalWinCombinationMultiplier /= appliedWinComb.reward_multiplier;
                                        totalWinCombinationMultiplier *= winComb.reward_multiplier;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Calculate the reward for this symbol
        if (!wins.isEmpty()) {
            double symbolReward = betAmount * symbolRewardMultiplier * totalWinCombinationMultiplier;
            symbolRewards.put(symbol, symbolReward);
            appliedWinningCombinations.put(symbol, wins);
        }
    }

    // Sum up rewards from all symbols
    for (double reward : symbolRewards.values()) {
        totalReward += reward;
    }

    // Apply bonus symbols if totalReward > 0
    if (totalReward > 0) {
        outerLoop:
        for (int i = 0; i < config.rows; i++) {
            for (int j = 0; j < config.columns; j++) {
                String cell = matrix[i][j];
                Symbol symObj = config.symbols.get(cell);
                if (symObj.type.equals("bonus")) {
                    appliedBonusSymbol = cell;
                    if ("multiply_reward".equals(symObj.impact)) {
                        totalReward *= symObj.reward_multiplier;
                    } else if ("extra_bonus".equals(symObj.impact)) {
                        totalReward += symObj.extra;
                    }
                    // If bonus is MISS, do nothing
                    break outerLoop;
                }
            }
        }
    }

    return new GameResult(totalReward, appliedWinningCombinations, appliedBonusSymbol);
}


}