package test;

import ludo.model.*;
import ludo.view.GameUI;
import ludo.controller.GameController; // ĐÃ THÊM IMPORT NÀY ĐỂ FIX LỖI

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests — GameController.java
 * Nhóm 3 | 23130270 — Lương Hoài Sang
 *
 * KHÔNG dùng Mockito.
 * Dùng FakeGameUI (anonymous subclass + headless) để stub GameUI.
 * Framework: JUnit 5 (junit-jupiter-5.x.jar)
 */
@DisplayName("GameController — Unit Tests")
class GameControllerTest {

    // =========================================================================
    // FakeGameUI — subclass stub, không khởi động Swing thật
    // =========================================================================
    /**
     * Tạo FakeGameUI bằng cách set headless TRƯỚC khi gọi constructor JFrame.
     * Các method được override để ghi lại lời gọi thay vì vẽ UI.
     */
    static class FakeGameUI extends GameUI {
        final List<String> messages = new ArrayList<>();
        final List<String> popups   = new ArrayList<>();
        boolean playerSelectionShown = false;
        int renderCount      = 0;
        int diceUpdateCount  = 0;

        FakeGameUI(GameController gc) {
            super(gc);
        }

        @Override public void showMessage(String msg)                        { messages.add(msg); }
        @Override public void showPopup(String msg)                          { popups.add(msg); }
        @Override public void renderBoard(Board b, Player[] p)               { renderCount++; }
        @Override public void updateDiceDisplay(int v1, int v2)              { diceUpdateCount++; }
        @Override public void resetDiceDisplay()                             {}
        @Override public void showPlayerSelectionDialog()                    { playerSelectionShown = true; }
        @Override public void setVisible(boolean b)                          {} // chặn JFrame hiện ra
    }

    // =========================================================================
    // Helpers — reflection
    // =========================================================================
    private static Object getField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        while (cls != null) {
            try { return cls.getDeclaredField(name); }
            catch (NoSuchFieldException e) { cls = cls.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object callMethod(Object obj, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method m = obj.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(obj, args);
    }

    /** Bật headless mode cho Swing (không mở cửa sổ). */
    @BeforeAll
    static void enableHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    /** Tạo GameController với FakeGameUI, khởi động chế độ chỉ định. */
    private GameController buildController(String mode) {
        GameController gc = new GameController();
        FakeGameUI fake   = new FakeGameUI(gc); // setUI được gọi trong constructor GameUI
        gc.startNewGame(mode);
        return gc;
    }

    /** Đặt tất cả ngựa của player về FINISHED tại homeStep chỉ định. */
    private void finishAllHorses(Player p, int homeStep) {
        for (Horse h : p.getHorses()) {
            h.setState(HorseState.FINISHED);
            h.setHomeStep(homeStep);
        }
    }

    // =========================================================================
    // A. KHỞI TẠO & VÒNG ĐỜI GAME
    // =========================================================================
    @Nested
    @DisplayName("A. Khởi tạo & Vòng đời Game")
    class InitAndLifecycle {

        @Test
        @DisplayName("GameController() — khởi tạo không ném ngoại lệ")
        void constructor_noException() {
            assertDoesNotThrow(GameController::new);
        }

        @Test
        @DisplayName("setUI() — field ui không null sau khi gán")
        void setUI_assignsUI() throws Exception {
            GameController gc = new GameController();
            FakeGameUI fake   = new FakeGameUI(gc);
            assertNotNull(getField(gc, "ui"), "field ui phải khác null sau setUI");
        }

        @ParameterizedTest(name = "mode=\"{0}\"")
        @ValueSource(strings = {"2p", "3p", "4p", "2b", "3b", "4b", "bot"})
        @DisplayName("startNewGame(mode) — khởi động 7 chế độ không ném ngoại lệ")
        void startNewGame_allModes_noException(String mode) {
            assertDoesNotThrow(() -> buildController(mode));
        }

        @Test
        @DisplayName("startNewGame(\"2p\") — 2 player, cả hai là người")
        void startNewGame_2p_twoHumans() throws Exception {
            GameController gc  = buildController("2p");
            Player[]  players  = (Player[])  getField(gc, "players");
            boolean[] isBot    = (boolean[]) getField(gc, "isBot");
            assertEquals(2, players.length);
            assertFalse(isBot[0], "player[0] phải là người");
            assertFalse(isBot[1], "player[1] phải là người");
        }

        @Test
        @DisplayName("startNewGame(\"3p\") — đúng 3 player")
        void startNewGame_3p_threePlayers() throws Exception {
            assertEquals(3, ((Player[]) getField(buildController("3p"), "players")).length);
        }

        @Test
        @DisplayName("startNewGame(\"4p\") — đúng 4 player")
        void startNewGame_4p_fourPlayers() throws Exception {
            assertEquals(4, ((Player[]) getField(buildController("4p"), "players")).length);
        }

        @Test
        @DisplayName("startNewGame(\"2b\") — player[0] người, player[1] bot")
        void startNewGame_2b_botFlags() throws Exception {
            boolean[] isBot = (boolean[]) getField(buildController("2b"), "isBot");
            assertFalse(isBot[0], "player[0] phải là người");
            assertTrue (isBot[1], "player[1] phải là bot");
        }

        @Test
        @DisplayName("startNewGame(\"3b\") — player[0] người, player[1..2] bot")
        void startNewGame_3b_botFlags() throws Exception {
            boolean[] isBot = (boolean[]) getField(buildController("3b"), "isBot");
            assertFalse(isBot[0]);
            assertTrue (isBot[1]);
            assertTrue (isBot[2]);
        }

        @Test
        @DisplayName("startNewGame(\"4b\") — player[0] người, player[1..3] bot")
        void startNewGame_4b_botFlags() throws Exception {
            boolean[] isBot = (boolean[]) getField(buildController("4b"), "isBot");
            assertFalse(isBot[0]);
            assertTrue (isBot[1]);
            assertTrue (isBot[2]);
            assertTrue (isBot[3]);
        }

        @Test
        @DisplayName("startNewGame(\"bot\") — tất cả 4 player là bot")
        void startNewGame_bot_allBots() throws Exception {
            boolean[] isBot = (boolean[]) getField(buildController("bot"), "isBot");
            for (boolean b : isBot) assertTrue(b, "Mọi player đều phải là bot");
        }

        @Test
        @DisplayName("startNewGame(int) — overload số nguyên hoạt động đúng")
        void startNewGame_intOverload() throws Exception {
            GameController gc = new GameController();
            new FakeGameUI(gc);
            assertDoesNotThrow(() -> gc.startNewGame(4));
            assertEquals(4, ((Player[]) getField(gc, "players")).length);
        }

        @Test
        @DisplayName("initGame — reset đúng toàn bộ state sau startNewGame")
        void initGame_resetsAllState() throws Exception {
            GameController gc = buildController("4p");
            assertFalse((boolean) getField(gc, "hasRolled"),      "hasRolled");
            assertEquals(0,       getField(gc, "currentV1"),      "currentV1");
            assertEquals(0,       getField(gc, "currentV2"),      "currentV2");
            assertFalse((boolean) getField(gc, "v1Used"),         "v1Used");
            assertFalse((boolean) getField(gc, "v2Used"),         "v2Used");
            assertFalse((boolean) getField(gc, "bonusTurnEarned"),"bonusTurnEarned");
            assertFalse((boolean) getField(gc, "gameOver"),       "gameOver");
            assertEquals(0,       getField(gc, "currentPlayerIndex"), "currentPlayerIndex");
        }

        @Test
        @DisplayName("initGame — rankings rỗng khi bắt đầu")
        void initGame_clearsRankings() throws Exception {
            @SuppressWarnings("unchecked")
            List<String> r = (List<String>) getField(buildController("4p"), "rankings");
            assertTrue(r.isEmpty());
        }

        @Test
        @DisplayName("initGame — mỗi player có 4 ngựa ở IN_BASE")
        void initGame_allHorsesInBase() throws Exception {
            GameController gc = buildController("4p");
            for (Player p : (Player[]) getField(gc, "players"))
                for (Horse h : p.getHorses())
                    assertEquals(HorseState.IN_BASE, h.getState(),
                            h.getId() + " phải IN_BASE");
        }

        @Test
        @DisplayName("restartGame — gọi showPlayerSelectionDialog trên UI")
        void restartGame_callsUIDialog() throws Exception {
            GameController gc   = new GameController();
            FakeGameUI fake     = new FakeGameUI(gc);
            gc.restartGame();
            assertTrue(fake.playerSelectionShown,
                    "restartGame phải gọi showPlayerSelectionDialog");
        }

        @Test
        @DisplayName("getPlayerCount — trả về đúng số người chơi")
        void getPlayerCount_correct() {
            assertEquals(2, buildController("2p").getPlayerCount());
            assertEquals(3, buildController("3p").getPlayerCount());
            assertEquals(4, buildController("4p").getPlayerCount());
        }
    }

    // =========================================================================
    // B. XÁC ĐỊNH THẮNG — checkWinCondition
    // =========================================================================
    @Nested
    @DisplayName("B. Xác định thắng — checkWinCondition")
    class WinCondition {

        @Test
        @DisplayName("checkWinCondition — false khi player chưa thắng")
        void notWon_returnsFalse() {
            assertFalse(buildController("2p").checkWinCondition());
        }

        @Test
        @DisplayName("checkWinCondition — false khi gameOver đã = true")
        void gameAlreadyOver_returnsFalse() throws Exception {
            GameController gc = buildController("2p");
            setField(gc, "gameOver", true);
            assertFalse(gc.checkWinCondition());
        }

        @Test
        @DisplayName("checkWinCondition (2p) — true khi player[0] hoàn thành 4 ngựa")
        void allFinished_2p_returnsTrue() throws Exception {
            GameController gc = buildController("2p");
            finishAllHorses(((Player[]) getField(gc, "players"))[0], 6);
            assertTrue(gc.checkWinCondition());
        }

        @Test
        @DisplayName("checkWinCondition (2p) — gameOver = true sau khi thắng")
        void setsGameOver_2p() throws Exception {
            GameController gc = buildController("2p");
            finishAllHorses(((Player[]) getField(gc, "players"))[0], 6);
            gc.checkWinCondition();
            assertTrue((boolean) getField(gc, "gameOver"));
        }

        @Test
        @DisplayName("checkWinCondition — rankings chứa tên người thắng")
        void rankingsContainsWinner() throws Exception {
            GameController gc   = buildController("2p");
            Player[] players    = (Player[]) getField(gc, "players");
            finishAllHorses(players[0], 6);
            gc.checkWinCondition();
            @SuppressWarnings("unchecked")
            List<String> ranks = (List<String>) getField(gc, "rankings");
            assertTrue(ranks.contains(players[0].getName()));
        }

        @Test
        @DisplayName("checkWinCondition — tên player không xuất hiện 2 lần trong rankings")
        void noDuplicateInRankings() throws Exception {
            GameController gc   = buildController("2p");
            Player[] players    = (Player[]) getField(gc, "players");
            finishAllHorses(players[0], 6);
            gc.checkWinCondition();
            gc.checkWinCondition(); // gọi lần 2
            @SuppressWarnings("unchecked")
            List<String> ranks = (List<String>) getField(gc, "rankings");
            assertEquals(1, ranks.stream().filter(n -> n.equals(players[0].getName())).count(),
                    "Tên player không được lặp trong rankings");
        }

        @Test
        @DisplayName("checkWinCondition (4p) — người đầu thắng, game chưa kết thúc")
        void firstWinner_4p_gameNotOver() throws Exception {
            GameController gc = buildController("4p");
            finishAllHorses(((Player[]) getField(gc, "players"))[0], 6);
            assertFalse(gc.checkWinCondition(), "Chưa kết thúc khi còn 3 người");
            assertFalse((boolean) getField(gc, "gameOver"));
        }

        @Test
        @DisplayName("checkWinCondition (4p) — game kết thúc khi chỉ còn 1 người chưa xong")
        void threeWinners_gameEnds() throws Exception {
            GameController gc   = buildController("4p");
            Player[] players    = (Player[]) getField(gc, "players");
            @SuppressWarnings("unchecked")
            List<String> ranks  = (List<String>) getField(gc, "rankings");

            finishAllHorses(players[1], 6); ranks.add(players[1].getName());
            finishAllHorses(players[2], 6); ranks.add(players[2].getName());
            finishAllHorses(players[0], 6); // currentPlayer thắng → còn 1 người

            assertTrue(gc.checkWinCondition());
            assertTrue((boolean) getField(gc, "gameOver"));
        }

        @Test
        @DisplayName("checkWinCondition (4p) — khi kết thúc, rankings chứa đủ 4 player")
        void allPlayersInRankings_when4pEnds() throws Exception {
            GameController gc   = buildController("4p");
            Player[] players    = (Player[]) getField(gc, "players");
            @SuppressWarnings("unchecked")
            List<String> ranks  = (List<String>) getField(gc, "rankings");

            finishAllHorses(players[1], 6); ranks.add(players[1].getName());
            finishAllHorses(players[2], 6); ranks.add(players[2].getName());
            finishAllHorses(players[0], 6);
            gc.checkWinCondition();

            assertEquals(4, ranks.size(), "Phải có đủ 4 player trong rankings");
        }

        @Test
        @DisplayName("nextPlayerTurn — bỏ qua player đã thắng")
        void nextPlayerTurn_skipsWonPlayer() throws Exception {
            GameController gc   = buildController("4p");
            Player[] players    = (Player[]) getField(gc, "players");
            @SuppressWarnings("unchecked")
            List<String> ranks  = (List<String>) getField(gc, "rankings");

            finishAllHorses(players[1], 6); ranks.add(players[1].getName());
            gc.nextPlayerTurn(); // từ player[0], phải nhảy qua player[1]

            int idx = (int) getField(gc, "currentPlayerIndex");
            assertNotEquals(1, idx, "Không được dừng ở player đã thắng");
            assertEquals(2, idx);
        }
    }

    // =========================================================================
    // C. BOT — nhận diện, nước đi hợp lệ, AI scoring
    // =========================================================================
    @Nested
    @DisplayName("C. Bot — nhận diện & AI logic")
    class BotLogic {

        @Test
        @DisplayName("isCurrentPlayerBot() — false khi player[0] là người (2b)")
        void human_player_false() {
            assertFalse(buildController("2b").isCurrentPlayerBot());
        }

        @Test
        @DisplayName("isCurrentPlayerBot() — true khi chuyển sang player[1] là bot")
        void bot_player_true() throws Exception {
            GameController gc = buildController("2b");
            setField(gc, "currentPlayerIndex", 1);
            assertTrue(gc.isCurrentPlayerBot());
        }

        @Test
        @DisplayName("isCurrentPlayerBot() — false khi isBot = null (edge case)")
        void null_isBot_false() throws Exception {
            GameController gc = new GameController();
            new FakeGameUI(gc);
            setField(gc, "isBot", null);
            assertFalse(gc.isCurrentPlayerBot());
        }

        @Test
        @DisplayName("isCurrentPlayerBot() — false khi index vượt ngoài mảng isBot")
        void outOfBounds_index_false() throws Exception {
            GameController gc = buildController("2p");
            setField(gc, "currentPlayerIndex", 99);
            assertFalse(gc.isCurrentPlayerBot());
        }

        @Test
        @DisplayName("getValidMovesForBot — rỗng khi v1=v2=0")
        void notRolled_empty() throws Exception {
            GameController gc = buildController("bot");
            setField(gc, "currentV1", 0);
            setField(gc, "currentV2", 0);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            setField(gc, "bonusTurnEarned", false);
            @SuppressWarnings("unchecked")
            List<Horse> moves = (List<Horse>) callMethod(gc, "getValidMovesForBot", new Class[]{});
            assertTrue(moves.isEmpty());
        }

        @Test
        @DisplayName("getValidMovesForBot — bao gồm ngựa ON_PATH khi có xúc xắc hợp lệ")
        void horseOnPath_included() throws Exception {
            GameController gc = buildController("bot");
            Player[] players  = (Player[]) getField(gc, "players");
            Board board       = (Board)    getField(gc, "board");

            Horse h = players[0].getHorses()[0];
            h.setState(HorseState.ON_PATH);
            h.setCurrentPosition(5);
            h.setDistanceTraveled(5);
            board.setHorseAt(5, h);

            setField(gc, "currentV1", 3);
            setField(gc, "currentV2", 2);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            setField(gc, "bonusTurnEarned", true);

            @SuppressWarnings("unchecked")
            List<Horse> moves = (List<Horse>) callMethod(gc, "getValidMovesForBot", new Class[]{});
            assertTrue(moves.contains(h));
        }

        @Test
        @DisplayName("getValidMovesForBot — không bao gồm ngựa FINISHED")
        void finishedHorse_excluded() throws Exception {
            GameController gc = buildController("bot");
            Player[] players  = (Player[]) getField(gc, "players");
            Horse h = players[0].getHorses()[0];
            h.setState(HorseState.FINISHED);
            h.setHomeStep(6);

            setField(gc, "currentV1", 3);
            setField(gc, "currentV2", 2);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            setField(gc, "bonusTurnEarned", true);

            @SuppressWarnings("unchecked")
            List<Horse> moves = (List<Horse>) callMethod(gc, "getValidMovesForBot", new Class[]{});
            assertFalse(moves.contains(h));
        }

        @Test
        @DisplayName("getValidMovesForBot — ngựa IN_BASE: chỉ xuất quân khi bonusTurnEarned=true")
        void inBase_onlyWithBonus() throws Exception {
            GameController gc = buildController("bot");
            setField(gc, "currentV1", 3);
            setField(gc, "currentV2", 3);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);

            setField(gc, "bonusTurnEarned", false);
            @SuppressWarnings("unchecked")
            List<Horse> noBonus = (List<Horse>) callMethod(gc, "getValidMovesForBot", new Class[]{});
            assertTrue(noBonus.isEmpty(), "Không có bonus → rỗng");

            setField(gc, "bonusTurnEarned", true);
            @SuppressWarnings("unchecked")
            List<Horse> withBonus = (List<Horse>) callMethod(gc, "getValidMovesForBot", new Class[]{});
            assertFalse(withBonus.isEmpty(), "Có bonus → có nước đi");
        }

        @Test
        @DisplayName("scoreBotMove — IN_BASE luôn = 10")
        void inBase_score10() throws Exception {
            GameController gc = buildController("bot");
            Horse h = ((Player[]) getField(gc, "players"))[0].getHorses()[0];
            h.setState(HorseState.IN_BASE);
            int s = (int) callMethod(gc, "scoreBotMove",
                    new Class[]{Horse.class, int.class, int.class}, h, 3, 2);
            assertEquals(10, s);
        }

        @Test
        @DisplayName("scoreBotMove — IN_HOME score = 80 + homeStep")
        void inHome_scoreFormula() throws Exception {
            GameController gc = buildController("bot");
            Horse h = ((Player[]) getField(gc, "players"))[0].getHorses()[0];
            h.setState(HorseState.IN_HOME);
            h.setHomeStep(3);
            int s = (int) callMethod(gc, "scoreBotMove",
                    new Class[]{Horse.class, int.class, int.class}, h, 1, 1);
            assertEquals(83, s, "80 + homeStep(3) = 83");
        }

        @Test
        @DisplayName("scoreBotMove — IN_HOME score cao hơn ON_PATH")
        void inHome_higherThan_onPath() throws Exception {
            GameController gc = buildController("bot");
            Player[] players  = (Player[]) getField(gc, "players");

            Horse hh = players[0].getHorses()[0];
            hh.setState(HorseState.IN_HOME);
            hh.setHomeStep(4);

            Horse hp = players[0].getHorses()[1];
            hp.setState(HorseState.ON_PATH);
            hp.setCurrentPosition(10);
            hp.setDistanceTraveled(10);

            int sh = (int) callMethod(gc, "scoreBotMove",
                    new Class[]{Horse.class, int.class, int.class}, hh, 2, 1);
            int sp = (int) callMethod(gc, "scoreBotMove",
                    new Class[]{Horse.class, int.class, int.class}, hp, 2, 1);
            assertTrue(sh > sp, "IN_HOME(" + sh + ") phải > ON_PATH(" + sp + ")");
        }

        @Test
        @DisplayName("scoreBotMove — đá đối thủ ở ô thường được cộng +50")
        void kickOpponent_bonusScore() throws Exception {
            GameController gc = buildController("4p");
            Player[] players  = (Player[]) getField(gc, "players");
            Board board       = (Board)    getField(gc, "board");

            Horse att = players[0].getHorses()[0];
            att.setState(HorseState.ON_PATH);
            att.setCurrentPosition(1);
            att.setDistanceTraveled(1);
            board.setHorseAt(1, att);

            Horse opp = players[1].getHorses()[0];
            opp.setState(HorseState.ON_PATH);
            opp.setCurrentPosition(4); // ô 4 không phải safe cell
            board.setHorseAt(4, opp);

            int s = (int) callMethod(gc, "scoreBotMove",
                    new Class[]{Horse.class, int.class, int.class}, att, 3, 1);
            // base = 20 + dist(1) = 21, +50 kick = 71
            assertTrue(s >= 71, "Đá đối thủ phải +50, score=" + s);
        }

        @Test
        @DisplayName("chooseBestClimb — ưu tiên tổng (v1+v2) để lên bậc cao nhất")
        void climb_prefersSum() throws Exception {
            GameController gc = buildController("bot");
            int chosen = (int) callMethod(gc, "chooseBestClimb",
                    new Class[]{PlayerColor.class, int.class, int.class, int.class},
                    PlayerColor.RED, 2, 2, 1);
            assertEquals(3, chosen, "homeStep=2, v1=2, v2=1 → chọn sum=3 lên bậc 5");
        }

        @Test
        @DisplayName("chooseBestClimb — không vượt bậc 6")
        void climb_neverExceedsStep6() throws Exception {
            GameController gc = buildController("bot");
            int chosen = (int) callMethod(gc, "chooseBestClimb",
                    new Class[]{PlayerColor.class, int.class, int.class, int.class},
                    PlayerColor.RED, 5, 3, 1);
            assertTrue(chosen <= 1, "homeStep=5, v1=3(quá), v2=1 → chọn ≤1, chosen=" + chosen);
        }

        @Test
        @DisplayName("chooseBestClimb — trả 0 khi tất cả ô đích bị chặn")
        void climb_blocked_returns0() throws Exception {
            GameController gc = buildController("bot");
            Player[] players  = (Player[]) getField(gc, "players");
            for (int i = 0; i < 3; i++) {
                Horse b = players[0].getHorses()[i];
                b.setState(HorseState.IN_HOME);
                b.setHomeStep(3 + i); // chặn bậc 3, 4, 5
            }
            int chosen = (int) callMethod(gc, "chooseBestClimb",
                    new Class[]{PlayerColor.class, int.class, int.class, int.class},
                    PlayerColor.RED, 2, 1, 2);
            assertEquals(0, chosen, "Bậc 3,4,5 bị chặn → trả 0");
        }

        @Test
        @DisplayName("chooseBestPathMove — trả 0 khi không có bước hợp lệ")
        void path_noValidMove_returns0() throws Exception {
            GameController gc = buildController("bot");
            Player[] players  = (Player[]) getField(gc, "players");
            Board board       = (Board)    getField(gc, "board");

            Horse h = players[0].getHorses()[0];
            h.setState(HorseState.ON_PATH);
            h.setCurrentPosition(0);
            h.setDistanceTraveled(55); // chỉ còn 1 bước
            board.setHorseAt(0, h);

            int chosen = (int) callMethod(gc, "chooseBestPathMove",
                    new Class[]{Horse.class, int.class, int.class}, h, 2, 3);
            assertEquals(0, chosen, "v1=2,v2=3 > toGate(1) → trả 0");
        }

        @Test
        @DisplayName("chooseBestPathMove — ưu tiên đá đối thủ hơn đi xa")
        void path_prefersKick() throws Exception {
            GameController gc = buildController("4p");
            Player[] players  = (Player[]) getField(gc, "players");
            Board board       = (Board)    getField(gc, "board");

            Horse mv = players[0].getHorses()[0];
            mv.setState(HorseState.ON_PATH);
            mv.setCurrentPosition(0);
            mv.setDistanceTraveled(0);
            board.setHorseAt(0, mv);

            Horse op = players[1].getHorses()[0];
            op.setState(HorseState.ON_PATH);
            op.setCurrentPosition(2); // ô 2, không phải safe cell
            board.setHorseAt(2, op);

            int chosen = (int) callMethod(gc, "chooseBestPathMove",
                    new Class[]{Horse.class, int.class, int.class}, mv, 2, 4);
            assertEquals(2, chosen, "v1=2 đá đối thủ (score=100) > v2=4 đi xa");
        }

        @Test
        @DisplayName("scheduleBot — không khởi động khi gameOver = true")
        void scheduleBot_stopsWhenGameOver() throws Exception {
            GameController gc = buildController("bot");
            setField(gc, "gameOver", true);
            setField(gc, "botThinking", false);
            callMethod(gc, "scheduleBot", new Class[]{});
            assertFalse((boolean) getField(gc, "botThinking"),
                    "botThinking vẫn false → scheduleBot đã early-return");
        }

        @Test
        @DisplayName("scheduleBot — không khởi động lại khi botThinking = true")
        void scheduleBot_noDoubleStart() throws Exception {
            GameController gc = buildController("bot");
            setField(gc, "gameOver", false);
            setField(gc, "botThinking", true);
            assertDoesNotThrow(() -> callMethod(gc, "scheduleBot", new Class[]{}));
        }

        @Test
        @DisplayName("rollDiceRequest — hiện cảnh báo 'máy' khi đang là lượt bot")
        void rollDice_botTurn_showsWarning() throws Exception {
            GameController gc = new GameController();
            FakeGameUI fake   = new FakeGameUI(gc);
            gc.startNewGame("bot"); // player[0] là bot

            fake.messages.clear();
            gc.rollDiceRequest();

            assertTrue(fake.messages.stream().anyMatch(m -> m.contains("máy")),
                    "Phải hiện thông báo 'máy' khi rollDice lúc lượt bot");
        }
    }

    // =========================================================================
    // D. HELPERS — canClimb & canMove
    // =========================================================================
    @Nested
    @DisplayName("D. Helpers — canClimb & canMove")
    class HelperMethods {

        @Test
        @DisplayName("canClimb — false khi bậc đã có ngựa cùng màu")
        void canClimb_occupied_false() throws Exception {
            GameController gc = buildController("4p");
            Player[] players  = (Player[]) getField(gc, "players");
            players[0].getHorses()[0].setState(HorseState.IN_HOME);
            players[0].getHorses()[0].setHomeStep(3);
            assertFalse((boolean) callMethod(gc, "canClimb",
                    new Class[]{PlayerColor.class, int.class}, PlayerColor.RED, 3));
        }

        @Test
        @DisplayName("canClimb — true khi bậc trống")
        void canClimb_empty_true() throws Exception {
            GameController gc = buildController("4p");
            assertTrue((boolean) callMethod(gc, "canClimb",
                    new Class[]{PlayerColor.class, int.class}, PlayerColor.RED, 4));
        }

        @Test
        @DisplayName("canClimb — false khi targetStep = 0 hoặc 7 (ngoài phạm vi)")
        void canClimb_outOfRange_false() throws Exception {
            GameController gc = buildController("4p");
            assertFalse((boolean) callMethod(gc, "canClimb",
                    new Class[]{PlayerColor.class, int.class}, PlayerColor.RED, 0));
            assertFalse((boolean) callMethod(gc, "canClimb",
                    new Class[]{PlayerColor.class, int.class}, PlayerColor.RED, 7));
        }

        @Test
        @DisplayName("canMove — false khi steps = 0")
        void canMove_zeroSteps_false() throws Exception {
            GameController gc = buildController("4p");
            Player[] players  = (Player[]) getField(gc, "players");
            Board board       = (Board)    getField(gc, "board");
            Horse h = players[0].getHorses()[0];
            h.setState(HorseState.ON_PATH); h.setCurrentPosition(5); h.setDistanceTraveled(5);
            board.setHorseAt(5, h);
            assertFalse((boolean) callMethod(gc, "canMove",
                    new Class[]{Horse.class, int.class}, h, 0));
        }

        @Test
        @DisplayName("canMove — false khi đường bị chặn ở giữa bởi ngựa cùng màu")
        void canMove_blockedMid_false() throws Exception {
            GameController gc = buildController("4p");
            Player[] players  = (Player[]) getField(gc, "players");
            Board board       = (Board)    getField(gc, "board");

            Horse mv = players[0].getHorses()[0];
            mv.setState(HorseState.ON_PATH); mv.setCurrentPosition(0); mv.setDistanceTraveled(0);
            board.setHorseAt(0, mv);

            Horse bl = players[0].getHorses()[1];
            bl.setState(HorseState.ON_PATH); bl.setCurrentPosition(2);
            board.setHorseAt(2, bl); // chặn ô 2

            assertFalse((boolean) callMethod(gc, "canMove",
                            new Class[]{Horse.class, int.class}, mv, 3),
                    "Qua ô 2 bị chặn → false");
        }

        @Test
        @DisplayName("canMove — false khi ngựa vượt quá 55 bước")
        void canMove_exceeds55_false() throws Exception {
            GameController gc = buildController("4p");
            Player[] players  = (Player[]) getField(gc, "players");
            Horse h = players[0].getHorses()[0];
            h.setState(HorseState.ON_PATH);

            // Set ngựa sát cửa chuồng (đã đi được 54 bước, tức là chỉ còn 1 bước nữa là đến 55)
            h.setDistanceTraveled(54);

            // Thử cho đi 3 bước -> 54 + 3 = 57 (> 55) => Trả về false
            assertFalse((boolean) callMethod(gc, "canMove",
                            new Class[]{Horse.class, int.class}, h, 3),
                    "Điểm tổng kết vượt quá độ dài bản đồ (55) -> false");
        }
    }

    // =========================================================================
    // E. UC4 — Xuất quân
    // =========================================================================
    @Nested
    @DisplayName("E. UC4 - Xuất quân")
    class UC4_Tests {

        @Test
        @DisplayName("TC-01 – Kiểm tra xuất quân thất bại khi điểm số không hợp lệ")
        void TC01_DeployFailsWhenInvalidDice() throws Exception {
            // Điều kiện tiên quyết: Quân ngựa mục tiêu hiện đang ở trong chuồng (IN_BASE). Đến lượt đi của người chơi.
            GameController gc = buildController("2p");
            FakeGameUI fakeUI = new FakeGameUI(gc);
            gc.setUI(fakeUI);

            Player currentP = ((Player[]) getField(gc, "players"))[0];
            Horse targetHorse = currentP.getHorses()[0]; // Đang ở IN_BASE

            // Dữ liệu đầu vào: Cặp điểm xúc xắc nhận được là 2 và 3.
            setField(gc, "currentV1", 2);
            setField(gc, "currentV2", 3);
            setField(gc, "hasRolled", true);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            
            // Hàm Dice.isDeployable() trả về false -> bonusTurnEarned = false
            setField(gc, "bonusTurnEarned", false); 
            
            // Bước 1: Người chơi thực hiện nhấp gieo xúc xắc (gọi updateHighlightedHorses)
            callMethod(gc, "updateHighlightedHorses", new Class[]{});
            
            // Kết quả mong đợi: Hệ thống không kích hoạt highlight quân ngựa trong chuồng.
            @SuppressWarnings("unchecked")
            List<Horse> highlighted = (List<Horse>) getField(gc, "highlightedHorses");
            assertFalse(highlighted.contains(targetHorse), "Hệ thống không được highlight quân ngựa trong chuồng khi điểm không hợp lệ");

            // Bước 2: Người chơi cố tình nhấp chuột chọn quân ngựa đang đứng trong chuồng.
            // Lệnh sẽ bị handleHorseClick từ chối vì quân chưa được highlight.
            gc.handleHorseClick(targetHorse);

            // Kết quả mong đợi: Từ chối xử lý di chuyển, giữ nguyên IN_BASE
            assertEquals(HorseState.IN_BASE, targetHorse.getState(), "Quân ngựa phải giữ nguyên trạng thái IN_BASE");
            
            // Bước 3: Quan sát thông báo (updateHighlightedHorses sẽ tự chuyển lượt & cảnh báo nếu không còn nước)
            assertTrue(fakeUI.messages.stream().anyMatch(m -> m.contains("Không có nước đi hợp lệ")), "Phải hiển thị cảnh báo không hợp lệ hoặc tự động chuyển lượt");
        }

        @Test
        @DisplayName("TC-02 – Kiểm tra xuất quân thành công")
        void TC02_DeploySuccess() throws Exception {
            // Điều kiện tiên quyết: Quân ngựa mục tiêu ở IN_BASE, ô xuất phát trống.
            GameController gc = buildController("2p");
            FakeGameUI fakeUI = new FakeGameUI(gc);
            gc.setUI(fakeUI);

            Player currentP = ((Player[]) getField(gc, "players"))[0];
            Horse targetHorse = currentP.getHorses()[0];

            // Dữ liệu đầu vào: Cặp điểm xúc xắc nhận được là 4 và 4.
            setField(gc, "currentV1", 4);
            setField(gc, "currentV2", 4);
            setField(gc, "hasRolled", true);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            setField(gc, "bonusTurnEarned", true); // Dice.isDeployable() = true
            
            // Bước 1: Người chơi gieo xúc xắc
            callMethod(gc, "updateHighlightedHorses", new Class[]{});
            
            // Kết quả mong đợi: Hệ thống làm nổi bật quân ngựa trong chuồng.
            @SuppressWarnings("unchecked")
            List<Horse> highlighted = (List<Horse>) getField(gc, "highlightedHorses");
            assertTrue(highlighted.contains(targetHorse), "Quân ngựa trong chuồng phải được làm nổi bật");

            // Bước 2: Người chơi nhấp chọn quân ngựa màu của mình
            gc.handleHorseClick(targetHorse);

            // Kết quả mong đợi: Đặt quân ngựa vào vị trí xuất phát, trạng thái ON_PATH, quãng đường = 0
            assertEquals(HorseState.ON_PATH, targetHorse.getState(), "Trạng thái chuyển sang ON_PATH");
            assertEquals(0, targetHorse.getDistanceTraveled(), "Quãng đường bằng 0");
            
            Board board = (Board) getField(gc, "board");
            int startPos = board.getStartPosition(currentP.getColor());
            assertEquals(startPos, targetHorse.getCurrentPosition(), "Quân ngựa phải ở chính xác ô xuất phát");

            // Bước 3: Kiểm tra thông báo thưởng thêm lượt
            assertTrue(fakeUI.messages.stream().anyMatch(m -> m.contains("được thưởng thêm 1 lượt!")), "Hệ thống báo thưởng thêm lượt");
        }

        @Test
        @DisplayName("TC-03 – Kiểm tra xuất quân có đá ngựa đối phương tại ô xuất phát")
        void TC03_DeployWithKick() throws Exception {
            // Điều kiện tiên quyết: Có quân ngựa Xanh dương (đối phương) đang chiếm giữ ô xuất phát của Đỏ.
            GameController gc = buildController("2p"); // Giả sử player 0 là Đỏ, player 1 là Lục/Xanh
            FakeGameUI fakeUI = new FakeGameUI(gc);
            gc.setUI(fakeUI);

            Player pRed = ((Player[]) getField(gc, "players"))[0];
            Player pOpponent = ((Player[]) getField(gc, "players"))[1];
            Horse redHorse = pRed.getHorses()[0];
            Horse opponentHorse = pOpponent.getHorses()[0];

            Board board = (Board) getField(gc, "board");
            int startPosRed = board.getStartPosition(pRed.getColor());

            // Thiết lập quân đối phương tại ô xuất phát
            opponentHorse.setState(HorseState.ON_PATH);
            opponentHorse.setCurrentPosition(startPosRed);
            board.setHorseAt(startPosRed, opponentHorse);

            // Dữ liệu đầu vào: Người chơi Đỏ gieo được 1 và 6.
            setField(gc, "currentV1", 1);
            setField(gc, "currentV2", 6);
            setField(gc, "hasRolled", true);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            setField(gc, "bonusTurnEarned", true);

            // Bước 1: Gieo xúc xắc và click chọn
            callMethod(gc, "updateHighlightedHorses", new Class[]{});
            gc.handleHorseClick(redHorse);

            // Bước 2: Kiểm tra va chạm
            // Kết quả mong đợi: Ngựa đối phương bị đẩy về chuồng (IN_BASE).
            assertEquals(HorseState.IN_BASE, opponentHorse.getState(), "Ngựa đối phương bị đẩy bay trở lại chuồng");
            assertEquals(-1, opponentHorse.getCurrentPosition(), "Xóa vị trí cũ của ngựa đối phương");

            // Bước 3: Kiểm tra vị trí quân Đỏ và hiệu ứng thông báo
            assertEquals(HorseState.ON_PATH, redHorse.getState(), "Ngựa Đỏ xuất quân");
            assertEquals(startPosRed, redHorse.getCurrentPosition(), "Ngựa Đỏ thế chỗ an toàn tại ô xuất phát");
            assertTrue(fakeUI.messages.stream().anyMatch(m -> m.contains("Đá văng quân")), "Hiển thị thông báo đá ngựa đối phương");
        }

        
        @Test
        @DisplayName("TC-04 – Kiểm tra xuất quân bị chặn bởi quân mình")
        void TC04_DeployBlockedBySelf() throws Exception {
            // Điều kiện tiên quyết: Ô xuất phát bị chiếm đóng bởi một quân cờ cùng màu Đỏ.
            GameController gc = buildController("2p");
            FakeGameUI fakeUI = new FakeGameUI(gc);
            gc.setUI(fakeUI);

            Player pRed = ((Player[]) getField(gc, "players"))[0];
            Horse redHorse1 = pRed.getHorses()[0]; // Muốn xuất
            Horse redHorse2 = pRed.getHorses()[1]; // Đang chặn

            Board board = (Board) getField(gc, "board");
            int startPosRed = board.getStartPosition(pRed.getColor());

            // Đặt redHorse2 chặn cửa
            redHorse2.setState(HorseState.ON_PATH);
            redHorse2.setCurrentPosition(startPosRed);
            board.setHorseAt(startPosRed, redHorse2);

            // Dữ liệu đầu vào: Cặp điểm gieo được là 5 và 5.
            setField(gc, "currentV1", 5);
            setField(gc, "currentV2", 5);
            setField(gc, "hasRolled", true);
            setField(gc, "v1Used", false);
            setField(gc, "v2Used", false);
            setField(gc, "bonusTurnEarned", true);

            // Highlight (thực tế redHorse1 không được highlight vì bị chặn)
            callMethod(gc, "updateHighlightedHorses", new Class[]{});
            
            // Bước 2: Nhấp chuột thử chọn quân ngựa
            gc.handleHorseClick(redHorse1);

            // Bước 3: Kết quả mong đợi: Hủy bỏ luồng, giữ nguyên trong chuồng
            assertEquals(HorseState.IN_BASE, redHorse1.getState(), "Quân ngựa phải được giữ nguyên trong chuồng, không đổi vị trí");
        }
    }

    
}