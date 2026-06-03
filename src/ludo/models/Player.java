package ludo.models;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private PlayerColor color;
    private Horse[] horses;

    public Player(String name, PlayerColor color) {
        this.name = name;
        this.color = color;
        this.horses = new Horse[4];
        for (int i = 0; i < 4; i++) {
            horses[i] = new Horse(color.name() + "_" + (i + 1), color);
        }
    }

    public boolean hasWon() {
        for (Horse h : horses) {
            if (h.getState() != HorseState.FINISHED) return false;
        }
        return true;
    }

    /**
     * Thuật toán lọc nước đi hợp lệ của Vy dựa trên điểm xúc xắc
     */
    public List<Horse> getValidMoves(int v1, int v2) {
        int sum = v1 + v2;
        boolean canDeploy = (v1 == v2) || (v1 == 1 && v2 == 6) || (v1 == 6 && v2 == 1);
        List<Horse> validHorses = new ArrayList<>();

        for (Horse h : horses) {
            if (h.getState() == HorseState.FINISHED) continue;

            if (h.getState() == HorseState.IN_BASE) {
                if (canDeploy) validHorses.add(h);
            } else if (h.getState() == HorseState.ON_PATH) {
                int dist = h.getDistanceTraveled();
                if (dist < 55) {
                    if (dist + sum <= 55 || dist + v1 == 55 || dist + v2 == 55) {
                        validHorses.add(h);
                    }
                } else if (dist == 55) {
                    if (v1 == 1 || v2 == 1 || sum == 1) validHorses.add(h);
                }
            } else if (h.getState() == HorseState.IN_HOME) {
                int target = h.getHomeStep() + 1;
                if (target <= 6 && (v1 == target || v2 == target || sum == target)) {
                    validHorses.add(h);
                }
            }
        }
        return validHorses;
    }

    // --- Getters ---
    public String getName() { return name; }
    public PlayerColor getColor() { return color; }
    public Horse[] getHorses() { return horses; }
}