package ludo.models;

import java.util.Random;

public class Dice {
    private int value1;
    private int value2;
    private boolean rolled;
    private Random rand;

    public Dice() {
        this.rand = new Random();
        reset();
    }

    public int[] roll() {
        value1 = rand.nextInt(6) + 1;
        value2 = rand.nextInt(6) + 1;
        rolled = true;
        return new int[]{value1, value2};
    }

    public boolean isDouble() {
        return value1 == value2;
    }

    public void reset() {
        value1 = 1;
        value2 = 1;
        rolled = false;
    }

    public int getValue1() { return value1; }
    public int getValue2() { return value2; }
}