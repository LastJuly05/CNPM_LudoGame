package test; // ĐÃ SỬA: Khai báo đúng package ngang hàng

import ludo.model.*;
import ludo.view.BoardPanel;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class BoardPanelTest {

    private BoardPanel panel;

    @Before
    public void setUp() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                panel = new BoardPanel();
                panel.setSize(600, 600);
            });
        } catch (Exception e) {
            throw new RuntimeException("Không khởi tạo được BoardPanel trên EDT", e);
        }
    }

    @After
    public void tearDown() {
        panel = null;
    }

    private Set<PlayerColor> invokeGetActiveColors() throws Exception {
        Method method = BoardPanel.class.getDeclaredMethod("getActiveColors");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<PlayerColor> result = (Set<PlayerColor>) method.invoke(panel);
        return result;
    }

    // ============================================================
    //  PHẦN 1 — getActiveColors()
    // ============================================================
    /**
     * Khi chưa set players (null), getActiveColors() phải trả về tập rỗng.
     */
    @Test
    public void testGetActiveColors_noPlayers_returnsEmpty() throws Exception {
        Set<PlayerColor> result = invokeGetActiveColors();

        assertNotNull("Kết quả không được null", result);
        assertTrue("Phải rỗng khi chưa có player nào", result.isEmpty());
    }

    /**
     * Khi có 2 player (RED + BLUE), chỉ 2 màu đó phải có mặt.
     */
    @Test
    public void testGetActiveColors_twoPlayers_returnsTwoColors() throws Exception {
        Player red  = new Player("Người chơi Đỏ", PlayerColor.RED);
        Player blue = new Player("Người chơi Xanh", PlayerColor.BLUE);
        
        Player[] playersArray = new Player[]{red, blue};
        panel.updateData(new Board(), playersArray);

        Set<PlayerColor> result = invokeGetActiveColors();
        assertEquals("Phải có đúng 2 màu", (long) 2, (long) result.size());
        assertTrue("Phải chứa RED",  result.contains(PlayerColor.RED));
        assertTrue("Phải chứa BLUE", result.contains(PlayerColor.BLUE));
    }

    /**
     * Khi có đủ 4 player, tập phải chứa 4 màu.
     */
    @Test
    public void testGetActiveColors_fourPlayers_returnsFourColors() throws Exception {
        Player[] playersArray = {
            new Player("P1", PlayerColor.RED),
            new Player("P2", PlayerColor.BLUE),
            new Player("P3", PlayerColor.GREEN),
            new Player("P4", PlayerColor.YELLOW)
        };
        
        panel.updateData(new Board(), playersArray);

        Set<PlayerColor> result = invokeGetActiveColors();

        assertEquals("Phải có đúng 4 màu", (long) 4, (long) result.size());
        assertTrue("Phải chứa RED", result.contains(PlayerColor.RED));
        assertTrue("Phải chứa BLUE", result.contains(PlayerColor.BLUE));
        assertTrue("Phải chứa GREEN", result.contains(PlayerColor.GREEN));
        assertTrue("Phải chứa YELLOW", result.contains(PlayerColor.YELLOW));
    }

    /**
     * Màu không có trong danh sách player không được xuất hiện.
     */
    @Test
    public void testGetActiveColors_doesNotContainMissingColor() throws Exception {
        Player red   = new Player("P_Red", PlayerColor.RED);
        Player green = new Player("P_Green", PlayerColor.GREEN);
        
        Player[] playersArray = new Player[]{red, green};
        panel.updateData(new Board(), playersArray);

        Set<PlayerColor> result = invokeGetActiveColors();

        assertFalse("BLUE không tham gia → không có trong tập", result.contains(PlayerColor.BLUE));
        assertFalse("YELLOW không tham gia → không có trong tập", result.contains(PlayerColor.YELLOW));
    }

    /**
     * Mỗi màu chỉ xuất hiện 1 lần dù có nhiều player cùng màu (dedup bằng Set).
     */
    @Test
    public void testGetActiveColors_noDuplicates() throws Exception {
        Player r1 = new Player("Red 1", PlayerColor.RED);
        Player r2 = new Player("Red 2", PlayerColor.RED);
        
        Player[] playersArray = new Player[]{r1, r2};
        panel.updateData(new Board(), playersArray);

        Set<PlayerColor> result = invokeGetActiveColors();

        assertEquals("Set không được có phần tử trùng", (long) 1, (long) result.size());
    }
}