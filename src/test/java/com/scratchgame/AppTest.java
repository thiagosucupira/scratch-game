package com.scratchgame;

import org.junit.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;

public class AppTest {

    @Test
    public void testWinningScenarioWithBonus() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Config config = mapper.readValue(new File("config.json"), Config.class);

        adjustWinCombinationsFor4x4(config);

        // Set up a predefined matrix that will win
        String[][] matrix = {
            {"A", "A", "A", "A"},
            {"B", "B", "B", "+1000"},
            {"C", "C", "C", "C"},
            {"D", "D", "D", "D"}
        };

        double betAmount = 100;

        // Calculate rewards
        App.GameResult result = App.calculateRewards(config, matrix, betAmount);

        // Expected reward calculation
        double expectedReward = 0;

        // Symbol A: 4 times, same_symbol_4_times (1.5), same_symbols_horizontally (2)
        double symbolAReward = betAmount * 5 * 1.5 * 2; // 100 * 5 * 1.5 * 2 = 1500
        expectedReward += symbolAReward;

        // Symbol B: 3 times, same_symbol_3_times (1)
        double symbolBReward = betAmount * 3 * 1; // 100 * 3 * 1 = 300
        expectedReward += symbolBReward;

        // Symbol C: 4 times, same_symbol_4_times (1.5), same_symbols_horizontally (2)
        double symbolCReward = betAmount * 2.5 * 1.5 * 2; // 100 * 2.5 * 1.5 * 2 = 750
        expectedReward += symbolCReward;

        // Symbol D: 4 times, same_symbol_4_times (1.5), same_symbols_horizontally (2)
        double symbolDReward = betAmount * 2 * 1.5 * 2; // 100 * 2 * 1.5 * 2 = 600
        expectedReward += symbolDReward;

        expectedReward += 1000; // 3150 + 1000 = 4150

        assertEquals(expectedReward, result.reward, 0.001);

        assertTrue(result.appliedWinningCombinations.containsKey("A"));
        assertTrue(result.appliedWinningCombinations.get("A").contains("same_symbol_4_times"));
        assertTrue(result.appliedWinningCombinations.get("A").contains("same_symbols_horizontally"));

        assertTrue(result.appliedWinningCombinations.containsKey("B"));
        assertTrue(result.appliedWinningCombinations.get("B").contains("same_symbol_3_times"));

        assertTrue(result.appliedWinningCombinations.containsKey("C"));
        assertTrue(result.appliedWinningCombinations.get("C").contains("same_symbol_4_times"));
        assertTrue(result.appliedWinningCombinations.get("C").contains("same_symbols_horizontally"));

        assertTrue(result.appliedWinningCombinations.containsKey("D"));
        assertTrue(result.appliedWinningCombinations.get("D").contains("same_symbol_4_times"));
        assertTrue(result.appliedWinningCombinations.get("D").contains("same_symbols_horizontally"));

        assertEquals("+1000", result.appliedBonusSymbol);
    }

    @Test
    public void testLosingScenario() throws Exception {
        // Load configuration
        ObjectMapper mapper = new ObjectMapper();
        Config config = mapper.readValue(new File("config.json"), Config.class);

        // Adjust win_combinations for 4x4 grid
        adjustWinCombinationsFor4x4(config);

        // Set up a predefined matrix that will not win
        String[][] matrix = {
            {"A", "B", "C", "D"},
            {"E", "F", "MISS", "10x"},
            {"C", "D", "E", "F"},
            {"A", "B", "C", "D"}
        };

        double betAmount = 100;

        // Calculate rewards
        App.GameResult result = App.calculateRewards(config, matrix, betAmount);

        // Expected reward calculation
        double expectedReward = 0.0;

        // Symbols C and D each have 3 occurrences, satisfying "same_symbol_3_times"

        // Symbol C Reward
        double symbolCReward = betAmount * 2.5 * 1; // 100 * 2.5 * 1 = 250
        expectedReward += symbolCReward;

        // Symbol D Reward
        double symbolDReward = betAmount * 2 * 1; // 100 * 2 * 1 = 200
        expectedReward += symbolDReward;

        // Total Reward before bonus: expectedReward = 250 + 200 = 450

        // Since the applied bonus symbol is "MISS", there is no change to the reward

        assertEquals(expectedReward, result.reward, 0.001);

        // Verify applied winning combinations
        assertTrue(result.appliedWinningCombinations.containsKey("C"));
        assertTrue(result.appliedWinningCombinations.get("C").contains("same_symbol_3_times"));

        assertTrue(result.appliedWinningCombinations.containsKey("D"));
        assertTrue(result.appliedWinningCombinations.get("D").contains("same_symbol_3_times"));

        // Verify that the applied bonus symbol is "MISS"
        assertEquals("MISS", result.appliedBonusSymbol);
    }

    @Test
    public void testHorizontalWinningScenario() throws Exception {
        // Load configuration
        ObjectMapper mapper = new ObjectMapper();
        Config config = mapper.readValue(new File("config.json"), Config.class);

        // Adjust win_combinations for 4x4 grid
        adjustWinCombinationsFor4x4(config);

        // Set up a matrix with a horizontal win
        String[][] matrix = {
            {"A", "A", "A", "A"},
            {"B", "C", "D", "E"},
            {"F", "E", "D", "C"},
            {"B", "C", "D", "+500"}
        };

        double betAmount = 100;

        // Calculate rewards
        App.GameResult result = App.calculateRewards(config, matrix, betAmount);

        // Symbol "A" has a horizontal win in the first row
        // Winning combinations: same_symbol_4_times (1.5), same_symbols_horizontally (2)
        double symbolAReward = betAmount * 5 * 1.5 * 2; // 100 * 5 * 1.5 * 2 = 1500

        // Symbols C and D each have 3 occurrences, satisfying "same_symbol_3_times"
        // Symbol C Reward
        double symbolCReward = betAmount * 2.5 * 1; // 100 * 2.5 * 1 = 250

        // Symbol D Reward
        double symbolDReward = betAmount * 2 * 1; // 100 * 2 * 1 = 200

        // Total before bonus
        double expectedReward = symbolAReward + symbolCReward + symbolDReward; // 1500 + 250 + 200 = 1950

        // Apply bonus "+500" (extra 500)
        expectedReward += 500; // 1950 + 500 = 2450

        assertEquals(expectedReward, result.reward, 0.001);

        assertTrue(result.appliedWinningCombinations.containsKey("A"));
        assertTrue(result.appliedWinningCombinations.get("A").contains("same_symbol_4_times"));
        assertTrue(result.appliedWinningCombinations.get("A").contains("same_symbols_horizontally"));

        assertTrue(result.appliedWinningCombinations.containsKey("C"));
        assertTrue(result.appliedWinningCombinations.get("C").contains("same_symbol_3_times"));

        assertTrue(result.appliedWinningCombinations.containsKey("D"));
        assertTrue(result.appliedWinningCombinations.get("D").contains("same_symbol_3_times"));

        assertEquals("+500", result.appliedBonusSymbol);
    }

    private void adjustWinCombinationsFor4x4(Config config) {
        // Update covered_areas in win_combinations to match 4x4 grid
        config.win_combinations.get("same_symbols_horizontally").covered_areas = List.of(
                List.of("0:0", "0:1", "0:2", "0:3"),
                List.of("1:0", "1:1", "1:2", "1:3"),
                List.of("2:0", "2:1", "2:2", "2:3"),
                List.of("3:0", "3:1", "3:2", "3:3")
        );

        config.win_combinations.get("same_symbols_vertically").covered_areas = List.of(
                List.of("0:0", "1:0", "2:0", "3:0"),
                List.of("0:1", "1:1", "2:1", "3:1"),
                List.of("0:2", "1:2", "2:2", "3:2"),
                List.of("0:3", "1:3", "2:3", "3:3")
        );

        config.win_combinations.get("same_symbols_diagonally_left_to_right").covered_areas = List.of(
                List.of("0:0", "1:1", "2:2", "3:3")
        );

        config.win_combinations.get("same_symbols_diagonally_right_to_left").covered_areas = List.of(
                List.of("0:3", "1:2", "2:1", "3:0")
        );
    }
}
