package com.floreantpos.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;

import org.apache.commons.lang.StringUtils;

import com.floreantpos.Messages;
import com.floreantpos.main.Application;
import com.floreantpos.model.ShopTable;
import com.floreantpos.model.Ticket;
import com.floreantpos.model.User;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.model.util.DateUtil;
import com.floreantpos.services.TicketService;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.dialog.PasswordEntryDialog;

public class ShopTableButton extends PosButton {

    // ── layout ─────────────────────────────────────────────────────────────
    private static final int PAD    = 5;
    private static final int SHADOW = 3;
    private static final int ARC    = 10;   // 5px radius

    // ── Floreant POS blue + state palette ──────────────────────────────────
    private static final Color BLUE           = new Color(0x1A, 0x6E, 0xBD);
    private static final Color CLR_AVAILABLE  = new Color(0xFF, 0xFF, 0xFF);
    private static final Color CLR_AVAIL_FG   = new Color(0x21, 0x21, 0x21);
    private static final Color CLR_AVAIL_BDR  = new Color(0xDD, 0xDD, 0xDD);
    private static final Color CLR_SERVING_BG = new Color(0xC6, 0x28, 0x28);
    private static final Color CLR_BOOKED_BG  = new Color(0xEF, 0x6C, 0x00);
    private static final Color CLR_OTHER_BG   = new Color(0x6A, 0x1B, 0x9A);
    private static final Color WHITE           = Color.WHITE;

    // ── dine_in icon (Google Material Symbols, viewBox 0 -960 960 960) ─────
    // SVG path: https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsoutlined/dine_in/default/24px.svg
    // Icon when occupied: person sitting at table (dine_in)
    private static final String DINE_IN_SVG =
        "M160-160q-33 0-56.5-23.5T80-240v-440h80v440h280v80H160Z" +
        "m120-560q-33 0-56.5-23.5T200-800q0-33 23.5-56.5T280-880q33 0 56.5 23.5" +
        "T360-800q0 33-23.5 56.5T280-720Z" +
        "M480-80v-200H280q-33 0-56.5-23.5T200-360v-236q0-35 24-59.5t58-24.5" +
        "q19 0 35.5 8t28.5 22q45 49 96.5 89.5T560-520h54q-25-17-39.5-42.5T560-620" +
        "h241q0 32-14.5 57.5T747-520h133v80H720v360h-80v-360h-80q-53 0-107-23" +
        "t-93-55v138h120q33 0 56.5 23.5T560-300v220h-80Z";

    // Icon when available: empty table (round table top-down view)
    private static final String TABLE_SVG =
        "m240-160 60-150q9-23 29-36.5t45-13.5h66v-161q-153-5-256.5-45T80-660" +
        "q0-58 117-99t283-41q167 0 283.5 41T880-660q0 54-103.5 94T520-521v161h66" +
        "q24 0 44.5 13.5T660-310l60 150h-80l-48-120H368l-48 120h-80Z" +
        "m240-440q97 0 183-17t126-43q-40-26-126-43t-183-17q-97 0-183 17t-126 43" +
        "q40 26 126 43t183 17Zm0-60Z";

    private static volatile Path2D.Double DINE_IN_PATH = null;
    private static volatile Path2D.Double TABLE_PATH   = null;

    // ── display state ──────────────────────────────────────────────────────
    private ShopTable shopTable;
    private String    tableNum = "";
    private String    subLine1 = "";   // username
    private String    subLine2 = "";   // elapsed time / check#

    private User   user;
    private Ticket ticket;

    // ─────────────────────────────────────────────────────────────────────
    public ShopTableButton(ShopTable shopTable) {
        this.shopTable = shopTable;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setText("");
        update();
    }

    // ── Custom painting ───────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth(), h = getHeight();
        int bx = PAD, by = PAD;
        int bw = w - PAD - SHADOW - 1;
        int bh = h - PAD - SHADOW - 1;

        // ── Shadow ────────────────────────────────────────────────────────
        for (int s = SHADOW; s >= 1; s--) {
            g2.setColor(new Color(0, 0, 0, 8 + (SHADOW - s) * 8));
            g2.fillRoundRect(bx + s, by + s, bw, bh, ARC, ARC);
        }

        // ── Body ──────────────────────────────────────────────────────────
        Color bg = getBackground();
        if (getModel().isPressed()) bg = bg.darker();
        boolean avail = isAvailable();
        int splitY = by + bh * 63 / 100; // top 63% / bottom 37% split

        if (avail) {
            // Available: solid white body
            g2.setColor(bg);
            g2.fillRoundRect(bx, by, bw, bh, ARC, ARC);
        } else {
            // Occupied: white base first, then color only on top 63%
            g2.setColor(CLR_AVAILABLE);
            g2.fillRoundRect(bx, by, bw, bh, ARC, ARC);

            // Clip to top portion and fill with status color
            java.awt.Shape oldClip = g2.getClip();
            g2.setClip(bx, by, bw, splitY - by + ARC / 2);
            g2.setColor(bg);
            g2.fillRoundRect(bx, by, bw, bh, ARC, ARC);
            g2.setClip(oldClip);
        }

        if (getModel().isRollover() && !getModel().isPressed()) {
            g2.setColor(new Color(0, 0, 0, 12));
            g2.fillRoundRect(bx, by, bw, bh, ARC, ARC);
        }

        // ── Border ────────────────────────────────────────────────────────
        g2.setColor(avail ? CLR_AVAIL_BDR : bg.darker());
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawRoundRect(bx, by, bw, bh, ARC, ARC);

        // ── Layout split: top 63% text, bottom 37% icon ───────────────────
        int iconAreaTop = splitY;
        // Text color: white on colored top, dark on white available
        Color fg = avail ? CLR_AVAIL_FG : WHITE;

        // ── Table number ──────────────────────────────────────────────────
        Font numFont = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 26f); //$NON-NLS-1$
        g2.setFont(numFont);
        g2.setColor(fg);
        FontMetrics numFm = g2.getFontMetrics();
        String num = tableNum.isEmpty() ? "?" : tableNum; //$NON-NLS-1$
        int numX = bx + (bw - numFm.stringWidth(num)) / 2;
        int numY = by + numFm.getAscent() + 6;
        g2.drawString(num, numX, numY);

        int textY = numY + numFm.getDescent() + 2;

        // ── Sub-line 1: username ──────────────────────────────────────────
        if (!subLine1.isEmpty()) {
            Font sf = UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 16f); //$NON-NLS-1$
            g2.setFont(sf);
            FontMetrics sfm = g2.getFontMetrics();
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 185));
            String clipped = clip(subLine1, sfm, bw - 8);
            g2.drawString(clipped, bx + (bw - sfm.stringWidth(clipped)) / 2, textY + sfm.getAscent());
            textY += sfm.getHeight() + 1;
        }

        // ── Sub-line 2: elapsed time / check# ────────────────────────────
        if (!subLine2.isEmpty()) {
            Font sf2 = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 16f); //$NON-NLS-1$
            g2.setFont(sf2);
            FontMetrics sfm2 = g2.getFontMetrics();
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 210));
            String clipped = clip(subLine2, sfm2, bw - 8);
            g2.drawString(clipped, bx + (bw - sfm2.stringWidth(clipped)) / 2, textY + sfm2.getAscent());
        }

        // ── Divider ───────────────────────────────────────────────────────
        g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 35));
        g2.setStroke(new BasicStroke(0.7f));
        g2.drawLine(bx + 8, iconAreaTop, bx + bw - 8, iconAreaTop);

        // ── Dine-in icon in lower 37% ─────────────────────────────────────
        int iconAreaH  = (by + bh) - iconAreaTop;
        int iconCx     = bx + bw / 2;
        int iconCy     = iconAreaTop + iconAreaH / 2;
        int iconSize   = Math.min(iconAreaH - 4, bw / 4);

        // Available → empty table icon; Occupied → person-at-table icon
        drawIcon(g2, iconCx, iconCy, iconSize, BLUE, avail);

        g2.dispose();
    }

    // ── Icon rendering ────────────────────────────────────────────────────

    private static void drawIcon(Graphics2D g2, int cx, int cy, int sz, Color color, boolean available) {
        if (sz < 4) return;
        Path2D.Double path = available ? getTablePath() : getDineInPath();
        AffineTransform save = g2.getTransform();

        // SVG viewBox: 0 -960 960 960 → sz×sz centred at (cx,cy)
        double scale = sz / 960.0;
        AffineTransform at = new AffineTransform();
        at.translate(cx - sz / 2.0, cy - sz / 2.0);
        at.scale(scale, scale);
        at.translate(0, 960); // flip y: SVG y is -960..0

        g2.transform(at);
        g2.setColor(color);
        g2.fill(path);
        g2.setTransform(save);
    }

    private static Path2D.Double getDineInPath() {
        if (DINE_IN_PATH == null) {
            synchronized (ShopTableButton.class) {
                if (DINE_IN_PATH == null) DINE_IN_PATH = parseSvgPath(DINE_IN_SVG);
            }
        }
        return DINE_IN_PATH;
    }

    private static Path2D.Double getTablePath() {
        if (TABLE_PATH == null) {
            synchronized (ShopTableButton.class) {
                if (TABLE_PATH == null) TABLE_PATH = parseSvgPath(TABLE_SVG);
            }
        }
        return TABLE_PATH;
    }

    // ── Lightweight SVG path parser (handles M m L l H h V v Q q T t Z) ──

    private static Path2D.Double parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double();
        Pattern tok = Pattern.compile(
            "[MmLlHhVvQqTtZz]|[-+]?(?:[0-9]*\\.)?[0-9]+(?:[eE][-+]?[0-9]+)?"); //$NON-NLS-1$
        Matcher m = tok.matcher(d);
        List<String> tokens = new ArrayList<String>();
        while (m.find()) tokens.add(m.group());

        int i = 0, sz = tokens.size();
        double cx = 0, cy = 0, mx = 0, my = 0, lqcx = 0, lqcy = 0;
        boolean lastQ = false;
        char cmd = 'M';

        while (i < sz) {
            String t = tokens.get(i);
            char c0 = t.charAt(0);
            if (Character.isLetter(c0)) { cmd = c0; i++; }
            if (i >= sz && cmd != 'Z' && cmd != 'z') break;

            switch (cmd) {
                case 'Z': case 'z':
                    path.closePath(); cx = mx; cy = my; lastQ = false; break;

                case 'M': { double x=g(tokens,i++),y=g(tokens,i++); path.moveTo(x,y); cx=mx=x; cy=my=y; lastQ=false; cmd='L'; break; }
                case 'm': { double x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.moveTo(x,y); cx=mx=x; cy=my=y; lastQ=false; cmd='l'; break; }
                case 'L': { double x=g(tokens,i++),y=g(tokens,i++); path.lineTo(x,y); cx=x; cy=y; lastQ=false; break; }
                case 'l': { double x=cx+g(tokens,i++),y=cy+g(tokens,i++); path.lineTo(x,y); cx=x; cy=y; lastQ=false; break; }
                case 'H': { double x=g(tokens,i++); path.lineTo(x,cy); cx=x; lastQ=false; break; }
                case 'h': { double x=cx+g(tokens,i++); path.lineTo(x,cy); cx=x; lastQ=false; break; }
                case 'V': { double y=g(tokens,i++); path.lineTo(cx,y); cy=y; lastQ=false; break; }
                case 'v': { double y=cy+g(tokens,i++); path.lineTo(cx,y); cy=y; lastQ=false; break; }

                case 'Q': {
                    double x1=g(tokens,i++),y1=g(tokens,i++),x=g(tokens,i++),y=g(tokens,i++);
                    path.quadTo(x1,y1,x,y); lqcx=x1; lqcy=y1; cx=x; cy=y; lastQ=true; break;
                }
                case 'q': {
                    double x1=cx+g(tokens,i++),y1=cy+g(tokens,i++),x=cx+g(tokens,i++),y=cy+g(tokens,i++);
                    path.quadTo(x1,y1,x,y); lqcx=x1; lqcy=y1; cx=x; cy=y; lastQ=true; break;
                }
                case 'T': {
                    double tcx=lastQ?2*cx-lqcx:cx, tcy=lastQ?2*cy-lqcy:cy;
                    double x=g(tokens,i++),y=g(tokens,i++);
                    path.quadTo(tcx,tcy,x,y); lqcx=tcx; lqcy=tcy; cx=x; cy=y; lastQ=true; break;
                }
                case 't': {
                    double tcx=lastQ?2*cx-lqcx:cx, tcy=lastQ?2*cy-lqcy:cy;
                    double x=cx+g(tokens,i++),y=cy+g(tokens,i++);
                    path.quadTo(tcx,tcy,x,y); lqcx=tcx; lqcy=tcy; cx=x; cy=y; lastQ=true; break;
                }
                default: i++; break; // skip unknown
            }
        }
        return path;
    }

    private static double g(List<String> tokens, int idx) {
        return Double.parseDouble(tokens.get(idx));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isAvailable() {
        Color bg = getBackground();
        return bg.getRed() > 230 && bg.getGreen() > 230 && bg.getBlue() > 230;
    }

    private String clip(String text, FontMetrics fm, int maxW) {
        if (fm.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && fm.stringWidth(text + "…") > maxW) //$NON-NLS-1$
            text = text.substring(0, text.length() - 1);
        return text + "…"; //$NON-NLS-1$
    }

    // ── State update ──────────────────────────────────────────────────────

    public void update() {
        if (shopTable == null) return;

        tableNum = shopTable.toString() != null ? shopTable.toString() : ""; //$NON-NLS-1$
        subLine1 = "";
        subLine2 = "";

        boolean serving     = shopTable.isServing();
        String  userName    = shopTable.getUserName();
        String  ticketIdStr = shopTable.getTicketIdAsString();
        Date    ticketTime  = shopTable.getTicketCreateTime();

        if (StringUtils.isNotEmpty(ticketIdStr) && ticketTime == null) {
            subLine2 = "Chk#" + ticketIdStr; //$NON-NLS-1$
        } else if (ticketTime != null) {
            subLine2 = DateUtil.getElapsedTime(ticketTime, new Date());
        } else {
            serving = false;
        }
        if (StringUtils.isNotEmpty(userName)) subLine1 = userName;

        if (shopTable.getUserId() != null &&
            shopTable.getUserId().intValue() != Application.getCurrentUser().getAutoId().intValue()) {
            setBackground(CLR_OTHER_BG); setForeground(WHITE);
        } else if (serving) {
            setBackground(CLR_SERVING_BG); setForeground(WHITE);
        } else if (shopTable.isBooked()) {
            setEnabled(false); setOpaque(false);
            setBackground(CLR_BOOKED_BG); setForeground(WHITE);
        } else {
            setEnabled(true);
            setBackground(CLR_AVAILABLE); setForeground(CLR_AVAIL_FG);
        }
        repaint();
    }

    // ── Public API ────────────────────────────────────────────────────────

    public int        getId()                  { return shopTable.getId(); }
    public void       setShopTable(ShopTable t){ this.shopTable = t; }
    public ShopTable  getShopTable()           { return shopTable; }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof ShopTableButton)) return false;
        return shopTable.equals(((ShopTableButton) obj).shopTable);
    }
    @Override public int    hashCode() { return shopTable.hashCode(); }
    @Override public String toString() { return shopTable.toString(); }

    public void setUser(User user)   { this.user = user; }
    public User getUser() {
        if (user == null && shopTable.getUserId() != null)
            user = UserDAO.getInstance().get(shopTable.getUserId());
        return user;
    }

    public boolean hasUserAccess() {
        User u = getUser();
        if (u == null) return false;
        User current = Application.getCurrentUser();
        if (current.getUserId() == u.getUserId()) return true;
        if (current.hasPermission(UserPermission.PERFORM_MANAGER_TASK) ||
            current.hasPermission(UserPermission.PERFORM_ADMINISTRATIVE_TASK)) return true;
        String pw = PasswordEntryDialog.show(Application.getPosWindow(), Messages.getString("PosAction.0")); //$NON-NLS-1$
        if (StringUtils.isEmpty(pw)) return false;
        if (UserDAO.getInstance().findUserBySecretKey(pw).getAutoId() != u.getAutoId()) {
            POSMessageDialog.showError(Application.getPosWindow(), "Incorrect password"); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    public void   setTicket(Ticket ticket) { this.ticket = ticket; }
    public Ticket getTicket() {
        if (ticket == null || ticket.getId() != shopTable.getTicketId())
            ticket = TicketService.getTicket(shopTable.getTicketId());
        return ticket;
    }
}
