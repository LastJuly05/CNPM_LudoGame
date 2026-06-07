package ludo.view;

import ludo.controller.GameController;
import ludo.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoardPanel extends JPanel {
    private Board board;
    private Player[] players;
    private Point[] pathCoords;
    private GameController controller;

    private static final Color COLOR_RED    = new Color(220, 50,  50);
    private static final Color COLOR_BLUE   = new Color(50,  100, 220);
    private static final Color COLOR_GREEN  = new Color(50,  180, 80);
    private static final Color COLOR_YELLOW = new Color(220, 190, 0);
    private static final Color COLOR_BG     = new Color(240, 235, 220);
    private static final Color COLOR_PATH   = new Color(255, 255, 255);
    private static final Color COLOR_SAFE   = new Color(200, 230, 255);
    private static final Color COLOR_INACTIVE_BASE = new Color(160, 160, 160); // Màu chuồng không dùng

    public BoardPanel() {
        initPathCoordinates();
        setBackground(COLOR_BG);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { handleMouseClick(e.getX(), e.getY()); }
        });
        Timer animationTimer = new Timer(30, e -> repaint());
        animationTimer.start();
    }

    public void setController(GameController controller) { this.controller = controller; }

    public void updateData(Board board, Player[] players) {
        this.board = board;
        this.players = players;
        repaint();
    }
  // Trả về tập màu đang thực sự tham gia game
    private Set<PlayerColor> getActiveColors() {
        Set<PlayerColor> active = new HashSet<>();
        if (players != null) {
            for (Player p : players) active.add(p.getColor());
        }
        return active;
    }

    // Trả về tập màu đang thực sự tham gia game
    private Set<PlayerColor> getActiveColors() {
        Set<PlayerColor> active = new HashSet<>();
        if (players != null) {
            for (Player p : players) active.add(p.getColor());
        }
        return active;
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        if (players == null || controller == null) return;
        int size = Math.min(getWidth(), getHeight()) - 40;
        int cs = size / 15;
        int ox = (getWidth() - size) / 2;
        int oy = (getHeight() - size) / 2;
        Map<PlayerColor, Integer> baseCounters = new HashMap<>();

        for (Player p : players) {
            baseCounters.putIfAbsent(p.getColor(), 0);
            for (Horse h : p.getHorses()) {
                int px = -1, py = -1;
                if (h.getState() == HorseState.IN_BASE) {
                    int count = baseCounters.get(p.getColor());
                    Point basePos = getBaseSlotPixel(p.getColor(), count, ox, oy, cs);
                    px = basePos.x; py = basePos.y;
                    baseCounters.put(p.getColor(), count + 1);
                } else if (h.getState() == HorseState.ON_PATH) {
                    int pos = h.getCurrentPosition();
                    if (pos >= 0 && pos < pathCoords.length) {
                        px = ox + pathCoords[pos].x * cs;
                        py = oy + pathCoords[pos].y * cs;
                    }
                } else if (h.getState() == HorseState.IN_HOME ) {
                    Point hp = getHomePathCoords(p.getColor(), h.getHomeStep());
                    px = ox + hp.x * cs;
                    py = oy + hp.y * cs;
                }
                if (px != -1) {
                    Rectangle hitBox = new Rectangle(px + 2, py + 2, cs - 4, cs - 4);
                    if (hitBox.contains(mouseX, mouseY)) {
                        controller.handleHorseClick(h);
                        return;
                    }
                }
            }
        }
    }

    private void initPathCoordinates() {
        pathCoords = new Point[56];
        int idx = 0;
        for (int i = 7; i <= 14; i++) pathCoords[idx++] = new Point(i, 0);
        for (int i = 1; i <= 14; i++) pathCoords[idx++] = new Point(14, i);
        for (int i = 13; i >= 0; i--) pathCoords[idx++] = new Point(i, 14);
        for (int i = 13; i >= 0; i--) pathCoords[idx++] = new Point(0, i);
        for (int i = 1; i <= 6; i++)  pathCoords[idx++] = new Point(i, 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 40;
        int cs = size / 15;
        int ox = (getWidth() - size) / 2;
        int oy = (getHeight() - size) / 2;

        Set<PlayerColor> active = getActiveColors();

        // Nền bàn cờ
        g2d.setColor(new Color(200, 190, 170));
        g2d.fillRoundRect(ox - 5, oy - 5, cs * 15 + 10, cs * 15 + 10, 12, 12);
        g2d.setColor(COLOR_BG);
        g2d.fillRect(ox, oy, cs * 15, cs * 15);

        // 4 chuồng — ẩn (xám) nếu màu đó không tham gia
        drawBase(g2d, ox, oy, cs, 0, 0, PlayerColor.RED,    "ĐỎ",   active);
        drawBase(g2d, ox, oy, cs, 9, 0, PlayerColor.BLUE,   "LAM",  active);
        drawBase(g2d, ox, oy, cs, 9, 9, PlayerColor.GREEN,  "LỤC",   active);
        drawBase(g2d, ox, oy, cs, 0, 9, PlayerColor.YELLOW, "VÀNG", active);

        // 56 ô vòng ngoài
        for (int i = 0; i < 56; i++) {
            Point p = pathCoords[i];
            Color bg = COLOR_PATH;
            if (board != null && board.isSafeCell(i)) bg = COLOR_SAFE;
            if (i == 0)  bg = new Color(255, 120, 120);
            if (i == 14) bg = new Color(100, 140, 255);
            if (i == 28) bg = new Color(100, 220, 120);
            if (i == 42) bg = new Color(255, 230, 60);
            drawCell(g2d, ox + p.x * cs, oy + p.y * cs, cs, bg);

            g2d.setColor(new Color(160, 160, 160));
            g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(7, cs / 5)));
            g2d.drawString(String.valueOf(i), ox + p.x * cs + 2, oy + p.y * cs + cs - 2);
        }

        // Đường lên chuồng đích — ẩn/mờ nếu màu không tham gia
        for (int i = 1; i <= 6; i++) {
            drawHomeCell(g2d, ox + 7 * cs,        oy + i * cs,        cs, COLOR_RED,    i, active.contains(PlayerColor.RED));
            drawHomeCell(g2d, ox + (14 - i) * cs, oy + 7 * cs,        cs, COLOR_BLUE,   i, active.contains(PlayerColor.BLUE));
            drawHomeCell(g2d, ox + 7 * cs,        oy + (14 - i) * cs, cs, COLOR_GREEN,  i, active.contains(PlayerColor.GREEN));
            drawHomeCell(g2d, ox + i * cs,         oy + 7 * cs,       cs, COLOR_YELLOW, i, active.contains(PlayerColor.YELLOW));
        }

        // Trung tâm
        drawCenter(g2d, ox + 7 * cs, oy + 7 * cs, cs);

        // Vẽ ngựa
        if (players != null) {
            List<Horse> highlighted = (controller != null) ? controller.getHighlightedHorses() : null;
            drawAllHorses(g2d, ox, oy, cs, highlighted);
        }
    }

    private void drawBase(Graphics2D g2d, int ox, int oy, int cs,
                          int col, int row, PlayerColor color, String label,
                          Set<PlayerColor> active) {
        int x = ox + col * cs;
        int y = oy + row * cs;
        int w = 6 * cs, h = 6 * cs;

        boolean isActive = active.contains(color);
        Color baseColor = isActive ? getAwtColor(color) : COLOR_INACTIVE_BASE;

        g2d.setColor(baseColor);
        g2d.fillRect(x, y, w, h);
        g2d.setColor(baseColor.darker());
        g2d.drawRect(x, y, w, h);

        int inner = 4 * cs;
        int ix = x + cs, iy = y + cs;
        g2d.setColor(isActive ? new Color(255, 255, 255, 180) : new Color(200, 200, 200, 120));
        g2d.fillRoundRect(ix, iy, inner, inner, 12, 12);
        g2d.setColor(baseColor.darker());
        g2d.drawRoundRect(ix, iy, inner, inner, 12, 12);

        // Nhãn tên — dùng màu tối trên nền màu sáng để đọc rõ, không bị lỗi font
        // Vẽ bóng đen trước để chữ nổi trên mọi nền
        int fontSize = Math.max(12, cs * 2 / 3);
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int lw = fm.stringWidth(label);
        int lx = x + (w - lw) / 2;
        int ly = y + h / 2 + fm.getAscent() / 2; // Giữa chuồng theo chiều dọc
        // Bóng đen
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.drawString(label, lx + 2, ly + 2);
        // Chữ trắng
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, lx, ly);

        // Nếu không active, vẽ chéo "không dùng"
        if (!isActive) {
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x + 4, y + 4, x + w - 4, y + h - 4);
            g2d.drawLine(x + w - 4, y + 4, x + 4, y + h - 4);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    private void drawCell(Graphics2D g2d, int x, int y, int size, Color bgColor) {
        g2d.setColor(bgColor);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawRect(x, y, size, size);
    }

    private void drawHomeCell(Graphics2D g2d, int x, int y, int size, Color color, int step, boolean active) {
        float alpha = active ? (0.3f + (step / 6f) * 0.5f) : 0.12f;
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
        g2d.setColor(c);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(active ? color.darker() : new Color(150, 150, 150));
        g2d.drawRect(x, y, size, size);
    }

    private void drawCenter(Graphics2D g2d, int x, int y, int cs) {
        int[] xp = {x + cs/2, x + cs, x + cs/2, x};
        int[] yp = {y, y + cs/2, y + cs, y + cs/2};
        GradientPaint gp = new GradientPaint(x, y, new Color(80, 80, 80), x + cs, y + cs, new Color(40, 40, 40));
        g2d.setPaint(gp);
        g2d.fillPolygon(xp, yp, 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, cs / 3)));
        g2d.drawString("*", x + cs / 4, y + cs * 3 / 4);
    }

    private void drawAllHorses(Graphics2D g2d, int ox, int oy, int cs, List<Horse> highlighted) {
        Map<PlayerColor, Integer> baseCounters = new HashMap<>();
        for (Player p : players) {
            Color guiColor = getAwtColor(p.getColor());
            baseCounters.putIfAbsent(p.getColor(), 0);
            for (Horse h : p.getHorses()) {
                int px = -1, py = -1;
                if (h.getState() == HorseState.IN_BASE) {
                    int count = baseCounters.get(p.getColor());
                    Point basePos = getBaseSlotPixel(p.getColor(), count, ox, oy, cs);
                    px = basePos.x; py = basePos.y;
                    baseCounters.put(p.getColor(), count + 1);
                } else if (h.getState() == HorseState.ON_PATH) {
                    int pos = h.getCurrentPosition();
                    if (pos >= 0 && pos < pathCoords.length) {
                        px = ox + pathCoords[pos].x * cs;
                        py = oy + pathCoords[pos].y * cs;
                    }
                } else if (h.getState() == HorseState.IN_HOME || h.getState() == HorseState.FINISHED) {
                    Point hp = getHomePathCoords(p.getColor(), h.getHomeStep());
                    px = ox + hp.x * cs;
                    py = oy + hp.y * cs;
                }
                if (px != -1) {
                    boolean isHighlighted = highlighted != null && highlighted.contains(h);
                    boolean isFinished = h.getState() == HorseState.FINISHED; // viền vàng khi đã về đích
                    drawHorseIcon(g2d, px, py, cs, guiColor, isHighlighted, isFinished);
                }
            }
        }
    }

    private void drawHorseIcon(Graphics2D g2d, int x, int y, int size, Color color,
                               boolean highlighted, boolean finished) {
        int pad = size / 6;
        int d = size - 2 * pad;

        // Hào quang vàng ngoài cùng cho ngựa bậc 6 (vô địch)
        if (finished) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(3.5f));
            g2d.drawOval(x + pad - 4, y + pad - 4, d + 8, d + 8);
            g2d.setStroke(new BasicStroke(1f));
        }

        if (highlighted) {
            double pulse = (Math.sin(System.currentTimeMillis() / 150.0) + 1.0) / 2.0;
            int glowOffset = 6 + (int)(pulse * 4);
            int alpha = 60 + (int)(pulse * 90);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2d.fillOval(x + pad - glowOffset, y + pad - glowOffset,
                    d + glowOffset * 2, d + glowOffset * 2);
        }

        GradientPaint gp = new GradientPaint(x + pad, y + pad, color.brighter(),
                x + pad + d, y + pad + d, color.darker());
        g2d.setPaint(gp);
        g2d.fillOval(x + pad, y + pad, d, d);

        // Viền: vàng đậm nếu bậc 6, bình thường nếu không
        g2d.setColor(finished ? new Color(255, 200, 0) : color.darker().darker());
        g2d.setStroke(new BasicStroke(finished ? 2.5f : 1.2f));
        g2d.drawOval(x + pad, y + pad, d, d);
        g2d.setStroke(new BasicStroke(1f));

        // Bóng sáng
        g2d.setColor(new Color(255, 255, 255, 160));
        g2d.fillOval(x + pad + d/4, y + pad + d/5, d/4, d/4);
    }

    private Point getHomePathCoords(PlayerColor color, int step) {
        int s = Math.max(1, Math.min(step, 6));
        switch (color) {
            case RED:    return new Point(7, s);
            case BLUE:   return new Point(14 - s, 7);
            case GREEN:  return new Point(7, 14 - s);
            case YELLOW: return new Point(s, 7);
            default:     return new Point(7, 7);
        }
    }

    private Point getBaseSlotPixel(PlayerColor color, int slotIndex, int ox, int oy, int cs) {
        int baseCol = 0, baseRow = 0;
        switch (color) {
            case RED:    baseCol = 0; baseRow = 0; break;
            case BLUE:   baseCol = 9; baseRow = 0; break;
            case GREEN:  baseCol = 9; baseRow = 9; break;
            case YELLOW: baseCol = 0; baseRow = 9; break;
        }
        int dx = (slotIndex % 2 == 0) ? 1 : 3;
        int dy = (slotIndex < 2) ? 1 : 3;
        return new Point(ox + (baseCol + dx) * cs, oy + (baseRow + dy) * cs);
    }

    private Color getAwtColor(PlayerColor color) {
        switch (color) {
            case RED:    return COLOR_RED;
            case BLUE:   return COLOR_BLUE;
            case GREEN:  return COLOR_GREEN;
            case YELLOW: return COLOR_YELLOW;
            default:     return Color.GRAY;
        }
    }
}