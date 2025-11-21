package br.com.ystoreplugins.product.economy;

public interface EconomyProvider {
    String getName();
    boolean has(String player, double amount);
    double get(String player);
    void add(String player, double amount);
    void remove(String player, double amount);
    void set(String player, double amount);
}

