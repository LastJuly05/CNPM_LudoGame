package ludo.controllers;

import ludo.models.*;
import ludo.views.GameUI;
import java.util.ArrayList;
import java.util.List;

public class GameController {
    // Khai báo cấu trúc các thực thể hệ thống
    private Player[] players;
    private Board board;
    private Dice dice;
    private int currentPlayerIndex;
    private GameUI ui;

    private boolean hasRolled = false;
    private int currentV1 = 0, currentV2 = 0;
    private List<Horse> highlightedHorses = new ArrayList<>();
    private List<String> rankings = new ArrayList<>();

    public GameController() {
        initGame();
    }

    /**
     * [Ngày 1/6] KHỞI TẠO BÀN CHƠI (System Setup)
     */
    private void initGame() {
        board = new Board();
        dice = new Dice();
        players = new Player[]{
                new Player("Đỏ", PlayerColor.RED),
                new Player("Xanh Dương", PlayerColor.BLUE),
                new Player("Xanh Lá", PlayerColor.GREEN),
                new Player("Vàng", PlayerColor.YELLOW)
        };
        currentPlayerIndex = 0;
        hasRolled = false;
        currentV1 = 0;
        currentV2 = 0;
        highlightedHorses.clear();
        rankings.clear();
    }

    public void setUI(GameUI ui) {
        this.ui = ui;
    }


}