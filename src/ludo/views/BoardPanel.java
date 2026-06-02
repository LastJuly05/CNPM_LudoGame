package ludo.view;

import ludo.controller.GameController;
import ludo.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardPanel extends JPanel {
    private Board board;
    private Player[] players;
    private Point[] pathCoords;
    private GameController controller;

    // Màu sắc UI đẹp
    private static final Color COLOR_RED    = new Color(220, 50,  50);
    private static final Color COLOR_BLUE   = new Color(50,  100, 220);
    private static final Color COLOR_GREEN  = new Color(50,  180, 80);
    private static final Color COLOR_YELLOW = new Color(220, 190, 0);
    private static final Color COLOR_BG     = new Color(240, 235, 220);
    private static final Color COLOR_PATH   = new Color(255, 255, 255);
    private static final Color COLOR_SAFE   = new Color(200, 230, 255);
    private static final Color COLOR_HIGHLIGHT = new Color(255, 215, 0);

    public BoardPanel() {
        initPathCoordinates();
        setBackground(COLOR_BG);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void updateData(Board board, Player[] players) {
        this.board = board;
        this.players = players;
        repaint();
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
                    px = basePos.x;
                    py = basePos.y;
                    baseCounters.put(p.getColor(), count + 1);
                } else if (h.getState() == HorseState.ON_PATH) {
                    int pos = h.getCurrentPosition();
                    if (pos >= 0 && pos < pathCoords.length) {
                        Point gridP = pathCoords[pos];
                        px = ox + gridP.x * cs;
                        py = oy + gridP.y * cs;
                    }
                } else if (h.getState() == HorseState.IN_HOME) {
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
        for (int i = 1; i <= 6;  i++) pathCoords[idx++] = new Point(i, 0);
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

        // Nền bàn cờ
        g2d.setColor(new Color(200, 190, 170));
        g2d.fillRoundRect(ox - 5, oy - 5, cs * 15 + 10, cs * 15 + 10, 12, 12);
        g2d.setColor(COLOR_BG);
        g2d.fillRect(ox, oy, cs * 15, cs * 15);

        // Vẽ 4 chuồng màu (base)
        drawBase(g2d, ox, oy, cs, 0, 0, COLOR_RED,    "ĐỎ");
        drawBase(g2d, ox, oy, cs, 9, 0, COLOR_BLUE,   "XANH");
        drawBase(g2d, ox, oy, cs, 9, 9, COLOR_GREEN,  "LÁ");
        drawBase(g2d, ox, oy, cs, 0, 9, COLOR_YELLOW, "VÀNG");

        // Vẽ 56 ô vòng ngoài
        for (int i = 0; i < 56; i++) {
            Point p = pathCoords[i];
            Color bg = COLOR_PATH;
            if (board != null && board.isSafeCell(i)) bg = COLOR_SAFE;
            // Ô xuất phát (màu đặc trưng)
            if (i == 0)  bg = new Color(255, 120, 120);
            if (i == 14) bg = new Color(100, 140, 255);
            if (i == 28) bg = new Color(100, 220, 120);
            if (i == 42) bg = new Color(255, 230, 60);
            drawCell(g2d, ox + p.x * cs, oy + p.y * cs, cs, bg);

            // Số thứ tự ô (nhỏ, ở góc)
            g2d.setColor(new Color(160, 160, 160));
            g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(7, cs / 5)));
            g2d.drawString(String.valueOf(i), ox + p.x * cs + 2, oy + p.y * cs + cs - 2);
        }

        // Đường lên chuồng đích (home stretch)
        for (int i = 1; i <= 6; i++) {
            drawHomeCell(g2d, ox + 7 * cs, oy + i * cs, cs, COLOR_RED,    i);   // Đỏ đi xuống
            drawHomeCell(g2d, ox + (14-i) * cs, oy + 7 * cs, cs, COLOR_BLUE,   i);
            drawHomeCell(g2d, ox + 7 * cs, oy + (14-i) * cs, cs, COLOR_GREEN,  i);
            drawHomeCell(g2d, ox + i * cs, oy + 7 * cs, cs, COLOR_YELLOW, i);
        }

        // Trung tâm (tam giác gặp nhau)
        drawCenter(g2d, ox + 7 * cs, oy + 7 * cs, cs);

        // Vẽ ngựa
        if (players != null) {
            List<Horse> highlighted = (controller != null) ? controller.getHighlightedHorses() : null;
            drawAllHorses(g2d, ox, oy, cs, highlighted);
        }
    }

    private void drawBase(Graphics2D g2d, int ox, int oy, int cs, int col, int row, Color color, String label) {
        int x = ox + col * cs;
        int y = oy + row * cs;
        int w = 6 * cs, h = 6 * cs;

        // Nền màu chuồng
        g2d.setColor(color);
        g2d.fillRect(x, y, w, h);
        g2d.setColor(color.darker());
        g2d.drawRect(x, y, w, h);

        // Vùng trắng bên trong (khu chứa ngựa)
        int inner = 4 * cs;
        int ix = x + cs, iy = y + cs;
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillRoundRect(ix, iy, inner, inner, 12, 12);
        g2d.setColor(color.darker());
        g2d.drawRoundRect(ix, iy, inner, inner, 12, 12);

        // Nhãn tên
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, cs / 2)));
        FontMetrics fm = g2d.getFontMetrics();
        int lw = fm.stringWidth(label);
        g2d.drawString(label, x + (w - lw) / 2, y + cs - 4);
    }

    private void drawCell(Graphics2D g2d, int x, int y, int size, Color bgColor) {
        g2d.setColor(bgColor);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawRect(x, y, size, size);
    }

    private void drawHomeCell(Graphics2D g2d, int x, int y, int size, Color color, int step) {
        // Màu nhạt dần về trung tâm
        float alpha = 0.3f + (step / 6f) * 0.5f;
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
        g2d.setColor(c);
        g2d.fillRect(x, y, size, size);
        g2d.setColor(color.darker());
        g2d.drawRect(x, y, size, size);
    }

    private void drawCenter(Graphics2D g2d, int x, int y, int cs) {
        // Hình thoi trung tâm
        int[] xp = {x + cs/2, x + cs, x + cs/2, x};
        int[] yp = {y, y + cs/2, y + cs, y + cs/2};
        GradientPaint gp = new GradientPaint(x, y, new Color(80, 80, 80), x + cs, y + cs, new Color(40, 40, 40));
        g2d.setPaint(gp);
        g2d.fillPolygon(xp, yp, 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, cs / 3)));
        g2d.drawString("★", x + cs/4, y + cs * 3/4);
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
                    px = basePos.x;
                    py = basePos.y;
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
                    boolean isFinished = h.getState() == HorseState.FINISHED;
                    drawHorseIcon(g2d, px, py, cs, guiColor, isHighlighted, isFinished);
                }
            }
        }
    }

    private void drawHorseIcon(Graphics2D g2d, int x, int y, int size, Color color,
                                boolean highlighted, boolean finished) {
        int pad = size / 6;
        int d = size - 2 * pad;

        // Viền highlight (nhấp nháy vàng)
        if (highlighted) {
            g2d.setColor(COLOR_HIGHLIGHT);
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawOval(x + pad - 3, y + pad - 3, d + 6, d + 6);
            g2d.setStroke(new BasicStroke(1f));
        }

        // Thân ngựa (hình tròn với gradient)
        GradientPaint gp = new GradientPaint(
            x + pad, y + pad, color.brighter(),
            x + pad + d, y + pad + d, color.darker()
        );
        g2d.setPaint(gp);
        g2d.fillOval(x + pad, y + pad, d, d);

        // Viền
        g2d.setColor(finished ? new Color(255, 215, 0) : color.darker().darker());
        g2d.setStroke(new BasicStroke(finished ? 2f : 1.2f));
        g2d.drawOval(x + pad, y + pad, d, d);
        g2d.setStroke(new BasicStroke(1f));

        // Điểm sáng
        g2d.setColor(new Color(255, 255, 255, 160));
        g2d.fillOval(x + pad + d / 4, y + pad + d / 5, d / 4, d / 4);

        // Dấu ★ nếu FINISHED
        if (finished) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, Math.max(7, size / 4)));
            g2d.drawString("★", x + size / 2 - size / 8, y + size / 2 + size / 8);
        }
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