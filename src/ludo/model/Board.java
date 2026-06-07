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
        SAFE_CELLS.add(0); // RED start
        SAFE_CELLS.add(14); // BLUE start
        SAFE_CELLS.add(28); // GREEN start
        SAFE_CELLS.add(42); // YELLOW start
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

    public Set<Integer> getSafeCells() {
        return SAFE_CELLS;
    }

    /**
     * [UC4 - Bước 4.1.4] Hệ thống kiểm tra thực thể tại ô xuất phát, xác nhận ô xuất phát hoàn toàn trống (không bị chặn bởi quân nào khác).
     * [UC4 - Bước 4.2.0] Tại bước 4.1.4 của luồng chính, hệ thống phát hiện tại ô xuất phát đang bị chiếm đóng bởi một quân ngựa của đối phương (khác màu).
     * [UC4 - Bước 4.3.0] Tại bước 4.1.4 của luồng chính, hệ thống phát hiện ô xuất phát đang bị chiếm giữ bởi một quân ngựa khác cùng màu của chính người chơi hiện tại.
     */
    public Horse getHorseAt(int position) {
        if (position >= 0 && position < 56)
            return path[position];
        return null;
    }

    /**
     * [UC4 - Bước 4.1.5] Hệ thống đặt quân ngựa vào ô xuất phát, chuyển trạng thái ngựa sang ON_PATH và thiết lập quãng đường đã di chuyển bằng 0.
     * [UC4 - Bước 4.2.3] Hệ thống tiếp tục đưa quân ngựa của người chơi hiện hành vào vị trí xuất phát và nhảy đến bước 4.1.5 của luồng chính.
     */
    public void setHorseAt(int position, Horse horse) {
        if (position >= 0 && position < 56)
            path[position] = horse;
    }

    /**
     * [UC4 - Bước 4.2.1] Hệ thống gửi lệnh yêu cầu xóa quân đối phương khỏi ô chạy, đẩy thực thể ngựa đối thủ quay về trạng thái chuồng ban đầu.
     * [UC7 - Bước 7.1.5] Hệ thống xóa vị trí cũ của ngựa, dịch chuyển thực thể quân ngựa lên bậc đích mới và cập nhật trạng thái sang HorseState.IN_HOME.
     */
    public void clearPosition(int position) {
        if (position >= 0 && position < 56)
            path[position] = null;
    }

    /**
     * [UC4 - Bước 4.1.3] Hệ thống tiếp nhận sự kiện, xác định tọa độ ô xuất phát thực tế trên bàn cờ dựa trên màu đại diện của người chơi hiện tại.
     */
    public int getStartPosition(PlayerColor color) {
        return startPositions.get(color);
    }
}