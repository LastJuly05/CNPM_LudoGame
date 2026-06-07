package ludo.model;

public class Horse {
    private String id;
    private PlayerColor color;
    private HorseState state;
    private int currentPosition; // -1: Trong chuồng, 0-55: Vị trí trên bàn cờ
    private int distanceTraveled; // Quãng đường đã đi được (dùng để check điều kiện lên chuồng)
    private int homeStep; // Bậc ở chuồng đích (1 đến 6)

    public Horse(String id, PlayerColor color) {
        this.id = id;
        this.color = color;
        this.state = HorseState.IN_BASE;
        this.currentPosition = -1;
        this.distanceTraveled = 0;
        this.homeStep = 0;
    }

    // Cập nhật vị trí và quãng đường khi di chuyển
    public void move(int steps) {
        // Logic di chuyển vòng tròn trên 56 ô
        if (this.currentPosition != -1) {
            this.currentPosition = (this.currentPosition + steps) % 56;
            this.distanceTraveled += steps;
        }
    }

    // Hành động khi bị đá: Reset toàn bộ trạng thái về ban đầu
    /**
     * [UC4 - Bước 4.2.1] Hệ thống gửi lệnh yêu cầu xóa quân đối phương khỏi ô chạy, đẩy thực thể ngựa đối thủ quay về trạng thái chuồng ban đầu.
     */
    public void sendToBase() {
        this.state = HorseState.IN_BASE;
        this.currentPosition = -1;
        this.distanceTraveled = 0; // Reset quãng đường về 0
        this.homeStep = 0;
    }

    // --- GETTERS & SETTERS ---

    public String getId() {
        return id;
    }

    public PlayerColor getColor() {
        return color;
    }

    public HorseState getState() {
        return state;
    }

    /**
     * [UC4 - Bước 4.1.5] Hệ thống đặt quân ngựa vào ô xuất phát, chuyển trạng thái ngựa sang ON_PATH và thiết lập quãng đường đã di chuyển bằng 0.
     * [UC7 - Bước 7.1.5] Hệ thống xóa vị trí cũ của ngựa, dịch chuyển thực thể quân ngựa lên bậc đích mới và cập nhật trạng thái sang HorseState.IN_HOME.
     * [UC7 - Bước 7.2.1] Hệ thống thực hiện dịch chuyển ngựa lên đỉnh chuồng, đồng thời cập nhật vĩnh viễn trạng thái của quân ngựa thành HorseState.FINISHED.
     */
    public void setState(HorseState state) {
        this.state = state;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int getDistanceTraveled() {
        return distanceTraveled;
    }

    /**
     * [UC4 - Bước 4.1.5] Hệ thống đặt quân ngựa vào ô xuất phát, chuyển trạng thái ngựa sang ON_PATH và thiết lập quãng đường đã di chuyển bằng 0.
     */
    public void setDistanceTraveled(int distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }

    public int getHomeStep() {
        return homeStep;
    }

    /**
     * [UC7 - Bước 7.1.5] Hệ thống xóa vị trí cũ của ngựa, dịch chuyển thực thể quân ngựa lên bậc đích mới và cập nhật trạng thái sang HorseState.IN_HOME.
     */
    public void setHomeStep(int homeStep) {
        this.homeStep = homeStep;
    }
}