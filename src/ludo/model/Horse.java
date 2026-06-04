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
    public void sendToBase() {
        this.state = HorseState.IN_BASE;
        this.currentPosition = -1;
        this.distanceTraveled = 0; // Reset quãng đường về 0
        this.homeStep = 0;
    }

    // --- GETTERS & SETTERS ---

    public String getId() { return id; }
    
    public PlayerColor getColor() { return color; }
    
    public HorseState getState() { return state; }
    public void setState(HorseState state) { this.state = state; }
    
    public int getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }

    public int getDistanceTraveled() { return distanceTraveled; }
    public void setDistanceTraveled(int distanceTraveled) { this.distanceTraveled = distanceTraveled; }

    public int getHomeStep() { return homeStep; }
    public void setHomeStep(int homeStep) { this.homeStep = homeStep; }
}