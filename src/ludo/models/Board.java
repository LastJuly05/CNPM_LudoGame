package ludo.model;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Board {
    private Horse[] path; // 56 ô vòng ngoài
    private Map<PlayerColor, Integer> startPositions;
    private static final Set<Integer> SAFE_CELLS = new HashSet<>();

    static {
        // Ô xuất phát của 4 màu là ô an toàn
        SAFE_CELLS.add(0);   // RED start
        SAFE_CELLS.add(14);  // BLUE start
        SAFE_CELLS.add(28);  // GREEN start
        SAFE_CELLS.add(42);  // YELLOW start
        // Ô an toàn thêm (giữa các khoảng)
        SAFE_CELLS.add(7);
        SAFE_CELLS.add(21);
        SAFE_CELLS.add(35);
        SAFE_CELLS.add(49);
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

    public Set<Integer> getSafeCells() { return SAFE_CELLS; }

    public Horse getHorseAt(int position) {
        if (position >= 0 && position < 56) return path[position];
        return null;
    }

    public void setHorseAt(int position, Horse horse) {
        if (position >= 0 && position < 56) path[position] = horse;
    }

    public void clearPosition(int position) {
        if (position >= 0 && position < 56) path[position] = null;
    }

    public int getStartPosition(PlayerColor color) {
        return startPositions.get(color);
    }
}