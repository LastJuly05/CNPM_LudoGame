package ludo.model;

import java.util.Random;

public class Dice {
    private static final int MAX_VALUE = 6;
    private static final int MIN_VALUE = 1;
    
    private int value1;
    private int value2;
    private final Random rand;
    private boolean rolled; // Cờ kiểm tra xem đã đổ xúc xắc chưa

    public Dice() {
        this.rand = new Random();
        this.value1 = 1;
        this.value2 = 1;
        this.rolled = false;
    }

    /**
     * Đổ xúc xắc và trả về kết quả
     */
    public int[] roll() {
        this.value1 = rand.nextInt(MAX_VALUE) + MIN_VALUE;
        this.value2 = rand.nextInt(MAX_VALUE) + MIN_VALUE;
        this.rolled = true;
        return new int[]{value1, value2};
    }

    /**
     * Kiểm tra điều kiện xuất quân: Đổ được đôi hoặc cặp (1, 6)
     */
    public boolean isDeployable() {
        if (!rolled) return false;
        return isDouble() || isOneSix();
    }

    /**
     * Kiểm tra xem có đổ được 2 viên giống nhau không
     */
    public boolean isDouble() {
        return value1 == value2;
    }

    /**
     * Kiểm tra xem có phải cặp 1 và 6 không
     */
    private boolean isOneSix() {
        return (value1 == 1 && value2 == 6) || (value1 == 6 && value2 == 1);
    }

    /**
     * Lấy tổng giá trị 2 xúc xắc
     */
    public int getTotal() {
        return value1 + value2;
    }

    // --- Getters ---
    public int getValue1() { return value1; }
    public int getValue2() { return value2; }
    public boolean isRolled() { return rolled; }

    /**
     * Reset trạng thái xúc xắc sau mỗi lượt đi
     */
    public void reset() {
        this.rolled = false;
    }

    /**
     * Hỗ trợ lấy tên file ảnh xúc xắc (Ví dụ: "dice_1.png")
     * Giúp bạn dễ dàng vẽ lên giao diện GameUI
     */
    public String getDiceImageName(int diceNumber) {
        int val = (diceNumber == 1) ? value1 : value2;
        return "dice_" + val + ".png";
    }
}