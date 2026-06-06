package test; // Đã đổi tên package

import ludo.model.*; // Import toàn bộ model của game
import org.junit.Test;
import static org.junit.Assert.*;

public class HorseTest {

    @Test
    public void testHorseMovement() {
        Horse horse = new Horse("RED_1", PlayerColor.RED);
        horse.setState(HorseState.ON_PATH);
        horse.setCurrentPosition(10); // Vị trí giả định
        
        // Mô phỏng UC5: Ngựa di chuyển thêm 6 bước
        horse.move(6);
        assertEquals("Vị trí tuyệt đối phải là 16", 16, horse.getCurrentPosition());
        assertEquals("Quãng đường đã đi phải được cộng dồn là 6", 6, horse.getDistanceTraveled());
    }

    @Test
    public void testHorseMoveWrapAround() {
        Horse horse = new Horse("BLUE_1", PlayerColor.BLUE);
        horse.setState(HorseState.ON_PATH);
        horse.setCurrentPosition(54); // Gần hết vòng mảng 56 ô
        
        // Mô phỏng di chuyển vượt qua mốc index 55
        horse.move(4);
        assertEquals("Vị trí phải xoay vòng về 2 (54+4 % 56)", 2, horse.getCurrentPosition());
        assertEquals("Quãng đường tổng phải là 4", 4, horse.getDistanceTraveled());
    }

    @Test
    public void testSendToBase() {
        Horse horse = new Horse("GREEN_1", PlayerColor.GREEN);
        horse.setState(HorseState.ON_PATH);
        horse.setCurrentPosition(30);
        horse.setDistanceTraveled(30);
        
        // Mô phỏng UC5: Ngựa bị đối phương đá
        horse.sendToBase();
        
        assertEquals("Trạng thái phải quay về IN_BASE", HorseState.IN_BASE, horse.getState());
        assertEquals("Vị trí trên bàn cờ bị xóa (-1)", -1, horse.getCurrentPosition());
        assertEquals("Quãng đường bị reset về 0", 0, horse.getDistanceTraveled());
    }
}