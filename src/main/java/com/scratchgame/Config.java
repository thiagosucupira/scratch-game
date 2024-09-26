package com.scratchgame;

import java.util.Map;

public class Config {
    public int columns = 3;
    public int rows = 3;
    public Map<String, Symbol> symbols;
    public Probabilities probabilities;
    public Map<String, WinCombination> win_combinations;

    // Getters and Setters
    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, Symbol> symbols) {
        this.symbols = symbols;
    }

    public Probabilities getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(Probabilities probabilities) {
        this.probabilities = probabilities;
    }

    public Map<String, WinCombination> getWinCombinations() {
        return win_combinations;
    }

    public void setWinCombinations(Map<String, WinCombination> win_combinations) {
        this.win_combinations = win_combinations;
    }
}