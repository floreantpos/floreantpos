package com.floreantpos.swing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

public class ButtonStyleDialog extends JDialog {

    // ── Selection state ───────────────────────────────────────────────────
    private String selectedStyle    = ButtonStyleConfig.getStyle();
    private String selectedAccent   = ButtonStyleConfig.getDefaultAccentHex();
    private String selectedIconKey  = ButtonStyleConfig.getDefaultIconKey();

    // ── UI refs ───────────────────────────────────────────────────────────
    private StyleCard classicCard;
    private StyleCard accentCard;
    private JPanel    colorSection;
    private JPanel    iconSection;

    // ── Colors ────────────────────────────────────────────────────────────
    private static final Color SELECTED_RING = new Color(0x1A, 0x6E, 0xBD);
    private static final Color PANEL_BG      = new Color(0xF8, 0xFA, 0xFC);
    private static final Color SECTION_FG    = new Color(0x33, 0x41, 0x55);
    private static final Color MUTED         = new Color(0x64, 0x74, 0x8B);

    public ButtonStyleDialog(Window owner) {
        super(owner, "Menu Button Style", ModalityType.APPLICATION_MODAL); //$NON-NLS-1$
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    // ── UI builder ────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new MigLayout("insets 24 24 16 24, fill", "[grow]", "[][][][]")); //$NON-NLS-1$
        root.setBackground(PANEL_BG);

        // ── Style cards ───────────────────────────────────────────────────
        JLabel styleLabel = sectionLabel("Choose a button style"); //$NON-NLS-1$
        root.add(styleLabel, "wrap, gapy 0 10"); //$NON-NLS-1$

        JPanel cards = new JPanel(new MigLayout("insets 0", "[grow][24px][grow]", "[180px]")); //$NON-NLS-1$
        cards.setOpaque(false);

        classicCard = new StyleCard(ButtonStyleConfig.STYLE_CLASSIC);
        accentCard  = new StyleCard(ButtonStyleConfig.STYLE_ACCENT);

        cards.add(classicCard, "grow, h 180!"); //$NON-NLS-1$
        cards.add(new JPanel() {{ setOpaque(false); }}, ""); //$NON-NLS-1$
        cards.add(accentCard,  "grow, h 180!"); //$NON-NLS-1$
        root.add(cards, "growx, wrap, gapy 0 18"); //$NON-NLS-1$

        refreshCards();

        // ── Accent color section ──────────────────────────────────────────
        colorSection = new JPanel(new MigLayout("insets 0", "[grow]", "[][grow]")); //$NON-NLS-1$
        colorSection.setOpaque(false);
        colorSection.add(sectionLabel("Accent Color"), "wrap, gapy 0 8"); //$NON-NLS-1$

        JPanel swatches = new JPanel(new GridLayout(2, 6, 10, 10));
        swatches.setOpaque(false);
        for (String[] entry : ButtonStyleConfig.ACCENT_COLORS) {
            swatches.add(new ColorSwatch(entry[0], entry[1]));
        }
        colorSection.add(swatches, "growx, wrap"); //$NON-NLS-1$
        root.add(colorSection, "growx, wrap, gapy 0 14"); //$NON-NLS-1$

        // ── Icon section ──────────────────────────────────────────────────
        iconSection = new JPanel(new MigLayout("insets 0", "[grow]", "[][grow]")); //$NON-NLS-1$
        iconSection.setOpaque(false);
        iconSection.add(sectionLabel("Category Icon"), "wrap, gapy 0 8"); //$NON-NLS-1$

        JPanel icons = new JPanel(new GridLayout(4, 7, 8, 8));
        icons.setOpaque(false);
        for (String[] ic : ButtonStyleConfig.FOOD_ICONS) {
            icons.add(new IconSwatch(ic[0], ic[1], ic[2]));
        }
        iconSection.add(icons, "growx, wrap"); //$NON-NLS-1$
        root.add(iconSection, "growx, wrap, gapy 0 20"); //$NON-NLS-1$

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel footer = new JPanel(new MigLayout("insets 0, al right", "[]8[]", "")); //$NON-NLS-1$
        footer.setOpaque(false);

        JButton cancel = new JButton("Cancel"); //$NON-NLS-1$
        cancel.addActionListener(e -> dispose());

        JButton save = new JButton("Save"); //$NON-NLS-1$
        save.setBackground(SELECTED_RING);
        save.setForeground(Color.WHITE);
        save.setOpaque(true);
        save.setBorderPainted(false);
        save.addActionListener(e -> {
            ButtonStyleConfig.setStyle(selectedStyle);
            ButtonStyleConfig.setDefaultAccentHex(selectedAccent);
            ButtonStyleConfig.setDefaultIconKey(selectedIconKey);
            dispose();
        });

        footer.add(cancel);
        footer.add(save);
        root.add(footer, "growx"); //$NON-NLS-1$

        setContentPane(root);
        updateSectionVisibility();
    }

    // ── Section helpers ───────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f)); //$NON-NLS-1$
        lbl.setForeground(SECTION_FG);
        return lbl;
    }

    private void updateSectionVisibility() {
        boolean accent = ButtonStyleConfig.STYLE_ACCENT.equals(selectedStyle);
        colorSection.setVisible(accent);
        iconSection.setVisible(accent);
        pack();
    }

    private void refreshCards() {
        classicCard.repaint();
        accentCard.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════
    // StyleCard — clickable preview card for Classic or Accent
    // ══════════════════════════════════════════════════════════════════════

    private class StyleCard extends JPanel {
        private final String style;
        private boolean hovered = false;

        StyleCard(String style) {
            this.style = style;
            setOpaque(false);
            setPreferredSize(new Dimension(200, 180));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectedStyle = style;
                    refreshCards();
                    updateSectionVisibility();
                }
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            boolean sel = style.equals(selectedStyle);
            Color accent = ButtonStyleConfig.parseHex(selectedAccent);

            // ── Card container ────────────────────────────────────────────
            Color cardBg  = sel ? new Color(0xEFF6FF) : (hovered ? new Color(0xF8FAFC) : Color.WHITE);
            Color cardBdr = sel ? SELECTED_RING : new Color(0xE2, 0xE8, 0xF0);
            g2.setColor(cardBg);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.setColor(cardBdr);
            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

            // ── Checkmark or label ────────────────────────────────────────
            int bw = w - 28, bh = 88;
            int bx = 14, by = 14;

            // Draw the sample button
            if (ButtonStyleConfig.STYLE_CLASSIC.equals(style)) {
                paintClassicSample(g2, bx, by, bw, bh, accent);
            } else {
                paintAccentSample(g2, bx, by, bw, bh, accent);
            }

            // Style name label
            String name = ButtonStyleConfig.STYLE_CLASSIC.equals(style) ? "Classic" : "Accent Color"; //$NON-NLS-1$ //$NON-NLS-2$
            Font lf = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f); //$NON-NLS-1$
            g2.setFont(lf);
            g2.setColor(sel ? SELECTED_RING : SECTION_FG);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, by + bh + fm.getAscent() + 8);

            // Description
            String desc = ButtonStyleConfig.STYLE_CLASSIC.equals(style)
                ? "Solid color fill" //$NON-NLS-1$
                : "White + color accent"; //$NON-NLS-1$
            g2.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 10f)); //$NON-NLS-1$
            g2.setColor(MUTED);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(desc, (w - fm2.stringWidth(desc)) / 2, by + bh + fm.getAscent() + 8 + fm2.getHeight());

            // Selected indicator
            if (sel) {
                g2.setColor(SELECTED_RING);
                g2.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f)); //$NON-NLS-1$
                g2.drawString("✓", w - 22, 20); //$NON-NLS-1$
            }

            g2.dispose();
        }

        private void paintClassicSample(Graphics2D g2, int x, int y, int w, int h, Color accent) {
            // Gradient full-color button
            GradientPaint gp = new GradientPaint(x, y, accent.brighter(), x + w, y + h, accent);
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(accent.darker());
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, w, h, 12, 12);
            drawSampleContent(g2, x, y, w, h, Color.WHITE);
        }

        private void paintAccentSample(Graphics2D g2, int x, int y, int w, int h, Color accent) {
            // White base
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, y, w, h, 12, 12);

            // Top strip (8px)
            java.awt.Shape savedClip = g2.getClip();
            g2.setClip(new java.awt.Rectangle(x, y, w, 8));
            g2.setColor(accent);
            g2.fillRoundRect(x, y, w, 12 * 2, 12, 12);
            g2.setClip(savedClip);

            // Soft blob bottom-left
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 36));
            g2.fillOval(x - 18, y + h - 52, 80, 80);

            // Icon background (soft tinted square)
            Color iconBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 36);
            g2.setColor(iconBg);
            g2.fillRoundRect(x + 8, y + 14, 28, 28, 8, 8);

            // Border
            g2.setColor(new Color(0xDF, 0xE4, 0xEA));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, w, h, 12, 12);

            // Text (dark on white)
            drawSampleContent(g2, x, y, w, h, new Color(0x11, 0x18, 0x27));
        }

        private void drawSampleContent(Graphics2D g2, int x, int y, int w, int h, Color fg) {
            // Emoji icon
            String emoji = ButtonStyleConfig.getIconEmoji(selectedIconKey);
            Font ef = ButtonStyleConfig.getEmojiFont(20f);
            g2.setFont(ef);
            g2.setColor(fg);
            FontMetrics efm = g2.getFontMetrics();
            g2.drawString(emoji, x + 12, y + 14 + efm.getAscent());

            // Item name
            Font nf = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11f); //$NON-NLS-1$
            g2.setFont(nf);
            g2.setColor(fg);
            FontMetrics nfm = g2.getFontMetrics();
            g2.drawString("Item Name", x + 8, y + h - 22); //$NON-NLS-1$

            // Price
            Font pf = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 13f); //$NON-NLS-1$
            g2.setFont(pf);
            g2.drawString("$9.99", x + 8, y + h - 7); //$NON-NLS-1$
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ColorSwatch — 38px circle for accent color selection
    // ══════════════════════════════════════════════════════════════════════

    private class ColorSwatch extends JPanel {
        private final String name;
        private final String hex;
        private boolean hovered = false;

        ColorSwatch(String name, String hex) {
            this.name = name;
            this.hex  = hex;
            setOpaque(false);
            setPreferredSize(new Dimension(42, 42));
            setToolTipText(name);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectedAccent = hex;
                    repaint();
                    refreshCards();
                    iconSection.repaint();
                }
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean sel = hex.equals(selectedAccent);
            Color c = ButtonStyleConfig.parseHex(hex);

            int r = sel ? 16 : (hovered ? 15 : 14);
            int cx = w / 2, cy = h / 2;

            // Outer ring for selected
            if (sel) {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                g2.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);
                g2.setColor(c.darker());
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - r - 3, cy - r - 3, (r + 3) * 2, (r + 3) * 2);
            }

            // Dot
            g2.setColor(c);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Check on selected
            if (sel) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
            }

            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // IconSwatch — emoji icon picker
    // ══════════════════════════════════════════════════════════════════════

    private class IconSwatch extends JPanel {
        private final String key;
        private final String emoji;
        private boolean hovered = false;

        IconSwatch(String key, String emoji, String label) {
            this.key   = key;
            this.emoji = emoji;
            setOpaque(false);
            setPreferredSize(new Dimension(52, 52));
            setToolTipText(label);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectedIconKey = key;
                    repaint();
                    refreshCards();
                }
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            boolean sel = key.equals(selectedIconKey);
            Color accent = ButtonStyleConfig.parseHex(selectedAccent);

            // Background rounded square
            Color bg = sel
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40)
                : (hovered ? new Color(0xF1, 0xF5, 0xF9) : new Color(0xF8, 0xFA, 0xFC));
            g2.setColor(bg);
            g2.fillRoundRect(2, 2, w - 4, h - 4, 10, 10);

            if (sel) {
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 10, 10);
            }

            // Emoji
            Font ef = ButtonStyleConfig.getEmojiFont(22f);
            g2.setFont(ef);
            FontMetrics fm = g2.getFontMetrics();
            int ew = fm.stringWidth(emoji);
            g2.drawString(emoji, (w - ew) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);

            g2.dispose();
        }
    }
}
