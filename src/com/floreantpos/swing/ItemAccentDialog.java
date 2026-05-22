package com.floreantpos.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.floreantpos.model.MenuItem;
import com.floreantpos.model.dao.MenuItemDAO;

import net.miginfocom.swing.MigLayout;

public class ItemAccentDialog extends JDialog {

    private static final Color PANEL_BG  = new Color(0xF8, 0xFA, 0xFC);
    private static final Color SEL_RING  = new Color(0x1A, 0x6E, 0xBD);
    private static final Color SECT_FG   = new Color(0x22, 0x2E, 0x3D);
    private static final Color MUTED     = new Color(0x64, 0x74, 0x8B);
    private static final Font  HDR_FONT  = new Font(Font.DIALOG, Font.BOLD,  15);
    private static final Font  LBL_FONT  = new Font(Font.DIALOG, Font.PLAIN, 13);
    private static final Font  BTN_FONT  = new Font(Font.DIALOG, Font.BOLD,  13);

    private List<MenuItem> items;
    private String selectedIconKey;
    private String selectedHex;

    public ItemAccentDialog(Window owner, MenuItem item) {
        this(owner, Collections.singletonList(item));
    }

    public ItemAccentDialog(Window owner, List<MenuItem> items) {
        super(owner, buildTitle(items), ModalityType.APPLICATION_MODAL);
        this.items = items;

        // Pre-select existing color if it matches one of the 12 accent colors
        MenuItem first = items.get(0);
        Integer code = first.getButtonColorCode();
        if (code != null && code != 0) {
            Color c = new Color(code);
            String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()); //$NON-NLS-1$
            // Only pre-select if it's one of our 12 defined accent colors
            for (String[] entry : ButtonStyleConfig.ACCENT_COLORS) {
                if (entry[1].equalsIgnoreCase(hex)) { selectedHex = entry[1]; break; }
            }
        }
        // Pre-select existing icon
        if (first.getProperties() != null)
            selectedIconKey = first.getProperties().get("icon"); //$NON-NLS-1$

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        buildUI();
        pack();
        setMinimumSize(new Dimension(980, 700));
        if (getWidth()  < 980) setSize(980, getHeight());
        if (getHeight() < 700) setSize(getWidth(), 700);
        setLocationRelativeTo(owner);
    }

    private static String buildTitle(List<MenuItem> items) {
        return items.size() == 1
            ? "Button Style — " + items.get(0).getDisplayName() //$NON-NLS-1$
            : "Button Style — " + items.size() + " items"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void buildUI() {
        JPanel root = new JPanel(new MigLayout("insets 20 24 20 24, fill", "[grow]", "")); //$NON-NLS-1$
        root.setBackground(PANEL_BG);

        // ── Icons (3 rows × 4 cols — 80% bigger than 2×6) ────────────────
        root.add(sectionLabel("Category Icon"), "wrap, gapy 0 10"); //$NON-NLS-1$
        JPanel iconGrid = new JPanel(new GridLayout(3, 4, 14, 14));
        iconGrid.setOpaque(false);
        for (String[] ic : ButtonStyleConfig.CONFIG_ICONS)
            iconGrid.add(new IconSwatch(ic[0], ic[1]));
        root.add(iconGrid, "growx, wrap, gapy 0 10"); //$NON-NLS-1$

        // ── Apply Icon Only button ────────────────────────────────────────
        JButton applyIconBtn = new JButton("Apply Icon Only (no color change)"); //$NON-NLS-1$
        applyIconBtn.setFont(BTN_FONT);
        applyIconBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyIconBtn.addActionListener(e -> saveIconOnly());
        root.add(applyIconBtn, "wrap, gapy 0 18"); //$NON-NLS-1$

        // ── Colors ────────────────────────────────────────────────────────
        String countStr = items.size() == 1 ? "this item" : items.size() + " items"; //$NON-NLS-1$ //$NON-NLS-2$
        root.add(sectionLabel("Accent Color  —  tap to apply to " + countStr), "wrap, gapy 0 10"); //$NON-NLS-1$
        JPanel colorGrid = new JPanel(new GridLayout(2, 6, 14, 14));
        colorGrid.setOpaque(false);
        for (String[] e : ButtonStyleConfig.ACCENT_COLORS)
            colorGrid.add(new ColorSwatch(e[0], e[1]));
        root.add(colorGrid, "growx, wrap, gapy 0 14"); //$NON-NLS-1$

        // ── Footer ────────────────────────────────────────────────────────
        JPanel foot = new JPanel(new MigLayout("insets 0", "[grow][]16[]", "")); //$NON-NLS-1$
        foot.setOpaque(false);

        JLabel resetLbl = link("Reset to auto"); //$NON-NLS-1$
        resetLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { selectedHex = null; save(); }
        });
        JLabel cancelLbl = link("Cancel"); //$NON-NLS-1$
        cancelLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dispose(); }
        });

        foot.add(resetLbl, "growx"); //$NON-NLS-1$
        foot.add(cancelLbl);
        root.add(foot, "growx"); //$NON-NLS-1$

        setContentPane(root);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HDR_FONT);
        l.setForeground(SECT_FG);
        return l;
    }

    private JLabel link(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LBL_FONT);
        l.setForeground(MUTED);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    // ── Save both color + icon ────────────────────────────────────────────

    private void save() {
        try {
            int colorCode = selectedHex == null ? 0
                    : ButtonStyleConfig.parseHex(selectedHex).getRGB();
            for (MenuItem item : items) {
                MenuItem fresh = MenuItemDAO.getInstance().get(item.getId());
                if (fresh != null) {
                    fresh.setButtonColorCode(colorCode);
                    applyIcon(fresh);
                    MenuItemDAO.getInstance().saveOrUpdate(fresh);
                }
                item.setButtonColorCode(colorCode);
                // Sync icon to local reference too
                if (selectedIconKey != null) {
                    if (item.getProperties() == null)
                        item.setProperties(new java.util.HashMap<String, String>());
                    item.getProperties().put("icon", selectedIconKey); //$NON-NLS-1$
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        dispose();
    }

    // ── Save icon only (color unchanged) ─────────────────────────────────

    private void saveIconOnly() {
        if (selectedIconKey == null) { dispose(); return; }
        try {
            for (MenuItem item : items) {
                MenuItem fresh = MenuItemDAO.getInstance().get(item.getId());
                if (fresh != null) {
                    applyIcon(fresh);
                    MenuItemDAO.getInstance().saveOrUpdate(fresh);
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        dispose();
    }

    private void applyIcon(MenuItem fresh) {
        if (selectedIconKey == null) return;
        if (fresh.getProperties() == null)
            fresh.setProperties(new HashMap<String, String>());
        fresh.getProperties().put("icon", selectedIconKey); //$NON-NLS-1$
    }

    // ── Icon swatch (3×4 grid, ~80% bigger than original 2×6) ─────────────

    private class IconSwatch extends JPanel {
        final String key; boolean hov;
        IconSwatch(String key, String label) {
            this.key = key;
            setOpaque(false);
            setPreferredSize(new Dimension(210, 190)); // ~75% bigger than original 120×114
            setToolTipText(label);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectedIconKey = key;
                    getParent().repaint();
                }
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            boolean sel = key.equals(selectedIconKey);
            Color accent = selectedHex != null ? ButtonStyleConfig.parseHex(selectedHex)
                         : new Color(0x5A, 0x72, 0x94);

            Color bg = sel ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45)
                           : (hov ? new Color(0xEB, 0xF2, 0xFB) : new Color(0xF3, 0xF5, 0xF9));
            g2.setColor(bg);
            g2.fillRoundRect(3, 3, w-6, h-6, 14, 14);
            if (sel) {
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(3, 3, w-6, h-6, 14, 14);
            }

            Color iconClr = sel ? accent.darker() : new Color(0x2D, 0x3B, 0x52);
            int iconSize = (int)(Math.min(w, h) * 0.62);
            MaterialIconPainter.paint(g2, key, w/2, h/2, iconSize, iconClr);
            g2.dispose();
        }
    }

    // ── Color swatch ───────────────────────────────────────────────────────

    private class ColorSwatch extends JPanel {
        final String name, hex; boolean hov;
        ColorSwatch(String name, String hex) {
            this.name = name; this.hex = hex;
            setOpaque(false);
            setPreferredSize(new Dimension(120, 110));
            setToolTipText(name);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selectedHex = hex; save(); }
                @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            boolean sel = hex.equalsIgnoreCase(selectedHex);
            Color c = ButtonStyleConfig.parseHex(hex);
            int inset = 6;
            int r = Math.min(w, h) / 2 - inset - (sel ? 4 : 0);
            int cx = w/2, cy = h/2;

            if (sel) {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 65));
                g2.fillOval(cx-r-6, cy-r-6, (r+6)*2, (r+6)*2);
                g2.setColor(c.darker());
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(cx-r-5, cy-r-5, (r+5)*2, (r+5)*2);
            }

            g2.setColor(hov ? c.brighter() : c);
            g2.fillOval(cx-r, cy-r, r*2, r*2);

            if (sel) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx-6, cy, cx-2, cy+5);
                g2.drawLine(cx-2, cy+5, cx+7, cy-5);
            }
            g2.dispose();
        }
    }
}
