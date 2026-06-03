package ludo.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Board {
    private Horse[] path; // Mảng 56 ô vòng quanh bàn cờ công cộng
    private Map<PlayerColor, Integer> startPositions;
    private static final Set<Integer> SAFE_CELLS = new HashSet<>();

    static {
        // Cấu hình các ô xuất phát là ô an toàn không được đá
        SAFE_CELLS.add(0);   // Đỏ xuất phát
        SAFE_CELLS.add(14);  // Xanh Dương xuất phát
        SAFE_CELLS.add(28);  // Xanh Lá xuất phát
        SAFE_CELLS.add(42);  // Vàng xuất phát
        // Các ô an toàn bổ sung cố định trên bàn cờ
        SAFE_CELLS.add(7);  SAFE_CELLS.add(21);
        SAFE_CELLS.add(35); SAFE_CELLS.add(49);
    }

    public Board() {
        path = new Horse[56];
        startPositions = new HashMap<>();
        startPositions.put(PlayerColor.RED, 0);
        startPositions.put(PlayerColor.BLUE, 14);
        startPositions.put(PlayerColor.GREEN, 28);
        startPositions.put(PlayerColor.YELLOW, 42);
    }

    public boolean isSafeCell(int pos) {
        return SAFE_CELLS.contains(pos);
    }

    public int getStartPosition(PlayerColor color) {
        return startPositions.get(color);
    }

    public Horse getHorseAt(int pos) {
        if (pos >= 0 && pos < 56) return path[pos];
        return null;
    }

    public void setHorseAt(int pos, Horse h) {
        if (pos >= 0 && pos < 56) path[pos] = h;
    }

    public void clearPosition(int pos) {
        if (pos >= 0 && pos < 56) path[pos] = null;
    }
}