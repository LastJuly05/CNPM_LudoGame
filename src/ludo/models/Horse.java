package ludo.models;

public class Horse {
    private String id;
    private PlayerColor color;
    private HorseState state;
    private int currentPosition;  // -1 nếu ở trong chuồng, 0-55 nếu trên bàn cờ
    private int distanceTraveled; // Quãng đường đã tích lũy (tối đa 55 trước khi lên chuồng)
    private int homeStep;         // Bậc chuồng đích từ 1 đến 6

    public Horse(String id, PlayerColor color) {
        this.id = id;
        this.color = color;
        this.state = HorseState.IN_BASE;
        this.currentPosition = -1;
        this.distanceTraveled = 0;
        this.homeStep = 0;
    }

    public void sendToBase() {
        this.state = HorseState.IN_BASE;
        this.currentPosition = -1;
        this.distanceTraveled = 0;
        this.homeStep = 0;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public PlayerColor getColor() { return color; }
    public HorseState getState() { return state; }
    public void setState(HorseState state) { this.state = state; }
    public int getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int pos) { this.currentPosition = pos; }
    public int getDistanceTraveled() { return distanceTraveled; }
    public void setDistanceTraveled(int dist) { this.distanceTraveled = dist; }
    public int getHomeStep() { return homeStep; }
    public void setHomeStep(int step) { this.homeStep = step; }
}