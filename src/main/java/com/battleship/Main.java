package com.battleship;

import com.battleship.system.GameManager;


public class Main {
    public static void main(String[] args) {
        System.out.println("Launching Runewater window...");
        new GameManager().start();
    }
}
