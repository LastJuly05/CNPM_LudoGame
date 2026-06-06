package test;

import ludo.model.*; 
import org.junit.Test;
import static org.junit.Assert.*;

public class DiceTest {

    @Test
    public void testDiceRollBounds() {
        Dice dice = new Dice();
        int[] result = dice.roll();
        // Kiểm tra xem hàm random có sinh số đúng từ 1 đến 6 không
        assertTrue("Giá trị xúc xắc 1 nằm ngoài khoảng 1-6", result[0] >= 1 && result[0] <= 6);
        assertTrue("Giá trị xúc xắc 2 nằm ngoài khoảng 1-6", result[1] >= 1 && result[1] <= 6);
        assertTrue("Cờ rolled phải chuyển sang true", dice.isRolled());
    }

    @Test
    public void testIsDoubleAndIsDeployable() {
        Dice dice = new Dice();
        // Giả lập đổ xúc xắc nhiều lần cho đến khi ra cặp đôi
        do {
            dice.roll();
        } while (dice.getValue1() != dice.getValue2()); 
        
        assertTrue("Xúc xắc trùng giá trị phải được nhận diện là isDouble", dice.isDouble());
        assertTrue("Xúc xắc đôi phải thỏa mãn điều kiện isDeployable (ra quân)", dice.isDeployable());
    }
}