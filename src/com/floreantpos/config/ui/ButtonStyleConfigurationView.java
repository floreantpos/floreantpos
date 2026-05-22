package com.floreantpos.config.ui;

import java.awt.BasicStroke;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.floreantpos.swing.ButtonStyleConfig;
import com.floreantpos.swing.MaterialIconPainter;

import net.miginfocom.swing.MigLayout;

public class ButtonStyleConfigurationView extends ConfigurationView {

    // ── Fixed fonts — no FlatLAF scaling ──────────────────────────────────
    private static final Font F_SECTION = new Font(Font.DIALOG, Font.BOLD,  13);
    private static final Font F_LABEL   = new Font(Font.DIALOG, Font.PLAIN, 11);
    private static final Font F_CARD    = new Font(Font.DIALOG, Font.BOLD,  12);
    private static final Font F_CARD_S  = new Font(Font.DIALOG, Font.PLAIN, 10);

    // ── Palette ────────────────────────────────────────────────────────────
    private static final Color BG       = new Color(0xF8, 0xFA, 0xFC);
    private static final Color SEL_RING = new Color(0x1A, 0x6E, 0xBD);
    private static final Color SECT_FG  = new Color(0x22, 0x2E, 0x3D);
    private static final Color MUTED    = new Color(0x64, 0x74, 0x8B);

    // ── State ──────────────────────────────────────────────────────────────
    private String selStyle;
    private String selAccent;
    private String selIcon;

    // ── UI refs ────────────────────────────────────────────────────────────
    private StyleCard  classicCard, accentCard;
    private JPanel     accentSection;

    public ButtonStyleConfigurationView() {
        setLayout(new MigLayout("insets 16, fill", "[grow]", "")); //$NON-NLS-1$
        setBackground(BG);
    }

    // ── ConfigurationView ─────────────────────────────────────────────────

    @Override public String getName() { return "Button Style"; } //$NON-NLS-1$

    @Override
    public void initialize() throws Exception {
        selStyle  = ButtonStyleConfig.getStyle();
        selAccent = ButtonStyleConfig.getDefaultAccentHex();
        selIcon   = ButtonStyleConfig.getDefaultIconKey();
        build();
        setInitialized(true);
    }

    @Override
    public boolean save() throws Exception {
        ButtonStyleConfig.setStyle(selStyle);
        if (ButtonStyleConfig.STYLE_ACCENT.equals(selStyle)) {
            ButtonStyleConfig.setDefaultAccentHex(selAccent);
            ButtonStyleConfig.setDefaultIconKey(selIcon);
        }
        return true;
    }

    // ── Build UI ──────────────────────────────────────────────────────────

    private void build() {
        removeAll();

        // ── Style cards ───────────────────────────────────────────────────
        add(sectionLabel("Button Style"), "wrap, gapy 0 8"); //$NON-NLS-1$

        JPanel cards = new JPanel(new MigLayout("insets 0", "[grow][16px][grow]", "[120px]")); //$NON-NLS-1$
        cards.setOpaque(false);
        classicCard = new StyleCard(ButtonStyleConfig.STYLE_CLASSIC);
        accentCard  = new StyleCard(ButtonStyleConfig.STYLE_ACCENT);
        cards.add(classicCard, "grow, h 120!"); //$NON-NLS-1$
        cards.add(new JPanel(){{ setOpaque(false); }}, ""); //$NON-NLS-1$
        cards.add(accentCard,  "grow, h 120!"); //$NON-NLS-1$
        add(cards, "growx, wrap, gapy 0 18"); //$NON-NLS-1$

        // ── Accent section (color + icon) ─────────────────────────────────
        accentSection = new JPanel(new MigLayout("insets 0", "[grow]", "[][grow][][grow]")); //$NON-NLS-1$
        accentSection.setOpaque(false);

        // Color swatches — each one IS the preview
        accentSection.add(sectionLabel("Accent Color  (select to preview)"), "wrap, gapy 0 8"); //$NON-NLS-1$
        JPanel colorGrid = new JPanel(new GridLayout(2, 6, 10, 10));
        colorGrid.setOpaque(false);
        for (String[] e : ButtonStyleConfig.ACCENT_COLORS)
            colorGrid.add(new ColorPreviewSwatch(e[0], e[1]));
        accentSection.add(colorGrid, "growx, wrap, gapy 0 16"); //$NON-NLS-1$

        // Icon swatches
        accentSection.add(sectionLabel("Category Icon"), "wrap, gapy 0 8"); //$NON-NLS-1$
        JPanel iconGrid = new JPanel(new GridLayout(2, 6, 10, 10));
        iconGrid.setOpaque(false);
        for (String[] ic : ButtonStyleConfig.CONFIG_ICONS)
            iconGrid.add(new IconSwatch(ic[0], ic[1]));
        accentSection.add(iconGrid, "growx"); //$NON-NLS-1$

        add(accentSection, "growx"); //$NON-NLS-1$
        updateAccentSection();
        revalidate(); repaint();
    }

    private void updateAccentSection() {
        boolean accent = ButtonStyleConfig.STYLE_ACCENT.equals(selStyle);
        accentSection.setVisible(accent);
    }

    private void refreshAll() {
        classicCard.repaint();
        accentCard.repaint();
        if (accentSection != null) accentSection.repaint();
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_SECTION);
        l.setForeground(SECT_FG);
        return l;
    }

    // ── Style card ─────────────────────────────────────────────────────────

    private class StyleCard extends JPanel {
        final String style; boolean hov;
        StyleCard(String style) {
            this.style = style; setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selStyle = style; refreshAll(); updateAccentSection();
                }
                @Override public void mouseEntered(MouseEvent e) { hov=true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov=false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            int w = getWidth(), h = getHeight();
            boolean sel = style.equals(selStyle);
            Color accent = ButtonStyleConfig.parseHex(selAccent);

            // Container
            g2.setColor(sel ? new Color(0xEF,0xF6,0xFF) : (hov ? new Color(0xF8,0xFA,0xFC) : Color.WHITE));
            g2.fillRoundRect(0,0,w-1,h-1,14,14);
            g2.setColor(sel ? SEL_RING : new Color(0xE2,0xE8,0xF0));
            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.drawRoundRect(0,0,w-1,h-1,14,14);

            // Mini button sample
            int bw=w-28, bh=60, bx=14, by=8;
            if (ButtonStyleConfig.STYLE_CLASSIC.equals(style)) paintClassicMini(g2,bx,by,bw,bh,accent);
            else                                                paintAccentMini(g2,bx,by,bw,bh,accent);

            // Label
            String name = ButtonStyleConfig.STYLE_CLASSIC.equals(style) ? "Classic" : "Accent Color"; //$NON-NLS-1$ //$NON-NLS-2$
            g2.setFont(F_CARD); g2.setColor(sel ? SEL_RING : SECT_FG);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(name,(w-fm.stringWidth(name))/2, by+bh+fm.getAscent()+5);

            String desc = ButtonStyleConfig.STYLE_CLASSIC.equals(style) ? "Solid color fill" : "White + accent strip"; //$NON-NLS-1$ //$NON-NLS-2$
            g2.setFont(F_CARD_S); g2.setColor(MUTED);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(desc,(w-fm2.stringWidth(desc))/2,by+bh+fm.getAscent()+5+fm2.getHeight());

            if (sel) { g2.setFont(F_CARD); g2.setColor(SEL_RING); g2.drawString("✓",w-18,16); } //$NON-NLS-1$
            g2.dispose();
        }
        private void paintClassicMini(Graphics2D g2,int x,int y,int w,int h,Color a){
            g2.setPaint(new GradientPaint(x,y,a.brighter(),x+w,y+h,a));
            g2.fillRoundRect(x,y,w,h,10,10);
            g2.setColor(a.darker()); g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x,y,w,h,10,10);
            drawText(g2,x,y,w,h,Color.WHITE);
        }
        private void paintAccentMini(Graphics2D g2,int x,int y,int w,int h,Color a){
            g2.setColor(Color.WHITE); g2.fillRoundRect(x,y,w,h,10,10);
            java.awt.Shape sc=g2.getClip();
            g2.setClip(new java.awt.Rectangle(x,y,w,7));
            g2.setColor(a); g2.fillRoundRect(x,y,w,20,10,10);
            g2.setClip(sc);
            g2.setColor(new Color(a.getRed(),a.getGreen(),a.getBlue(),36));
            g2.fillOval(x-14,y+h-42,80,80);
            g2.setColor(new Color(0xDF,0xE4,0xEA)); g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x,y,w,h,10,10);
            drawText(g2,x,y,w,h,new Color(0x11,0x18,0x27));
        }
        private void drawText(Graphics2D g2,int x,int y,int w,int h,Color fg){
            g2.setFont(F_CARD_S); g2.setColor(fg);
            FontMetrics fm=g2.getFontMetrics();
            g2.drawString("Item Name",x+7,y+h-16); //$NON-NLS-1$
            g2.setFont(new Font(Font.DIALOG,Font.BOLD,11));
            g2.drawString("$9.99",x+7,y+h-4); //$NON-NLS-1$
        }
    }

    // ── Color preview swatch — color dot + mini button preview ─────────────

    private class ColorPreviewSwatch extends JPanel {
        final String name, hex; boolean hov;
        ColorPreviewSwatch(String name, String hex) {
            this.name=name; this.hex=hex; setOpaque(false);
            setPreferredSize(new Dimension(90,80));
            setToolTipText(name);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selAccent=hex; refreshAll(); }
                @Override public void mouseEntered(MouseEvent e) { hov=true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov=false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            int w=getWidth(), h=getHeight();
            boolean sel = hex.equals(selAccent);
            Color c = ButtonStyleConfig.parseHex(hex);

            // Outer card
            Color cardBg = sel ? new Color(c.getRed(),c.getGreen(),c.getBlue(),25) : (hov ? new Color(0xF0,0xF4,0xF9) : new Color(0xF8,0xFA,0xFC));
            g2.setColor(cardBg);
            g2.fillRoundRect(0,0,w-1,h-1,10,10);
            g2.setColor(sel ? c : new Color(0xE2,0xE8,0xF0));
            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.drawRoundRect(0,0,w-1,h-1,10,10);

            // Mini accent button preview (top area)
            int bx=6, by=6, bw=w-12, bh=h-26;
            g2.setColor(Color.WHITE); g2.fillRoundRect(bx,by,bw,bh,8,8);
            java.awt.Shape sc=g2.getClip();
            g2.setClip(new java.awt.Rectangle(bx,by,bw,6));
            g2.setColor(c); g2.fillRoundRect(bx,by,bw,16,8,8);
            g2.setClip(sc);
            g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),30));
            g2.fillOval(bx-10,by+bh-36,66,66);

            // Draw icon inside the mini button
            if (selIcon != null) {
                Color iconClr = c.darker();
                MaterialIconPainter.paint(g2, selIcon, bx+bw/2, by+bh/2+3, (int)(bh*0.45), iconClr);
            }

            g2.setColor(new Color(0xDF,0xE4,0xEA)); g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(bx,by,bw,bh,8,8);

            // Color name label at bottom
            g2.setFont(F_LABEL);
            g2.setColor(sel ? c.darker() : MUTED);
            FontMetrics fm=g2.getFontMetrics();
            String lbl = name.length()>8 ? name.substring(0,7)+"…" : name; //$NON-NLS-1$
            g2.drawString(lbl,(w-fm.stringWidth(lbl))/2, h-5);

            g2.dispose();
        }
    }

    // ── Icon swatch ────────────────────────────────────────────────────────

    private class IconSwatch extends JPanel {
        final String key, label; boolean hov;
        IconSwatch(String key, String label) {
            this.key=key; this.label=label; setOpaque(false);
            setPreferredSize(new Dimension(68,64));
            setToolTipText(label);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selIcon=key; refreshAll(); }
                @Override public void mouseEntered(MouseEvent e) { hov=true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hov=false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            boolean sel=key.equals(selIcon);
            Color accent=ButtonStyleConfig.parseHex(selAccent);
            int w=getWidth(), h=getHeight();
            int iconH=h-18;

            Color bg=sel ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),35)
                         : (hov ? new Color(0xEE,0xF2,0xF8) : new Color(0xF5,0xF7,0xFA));
            g2.setColor(bg); g2.fillRoundRect(2,2,w-4,iconH-2,10,10);
            if(sel){ g2.setColor(accent); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(2,2,w-4,iconH-2,10,10); }

            Color iconClr=sel ? accent.darker() : new Color(0x33,0x41,0x55);
            MaterialIconPainter.paint(g2,key,w/2,iconH/2+2,(int)(iconH*0.55),iconClr);

            g2.setFont(F_LABEL); g2.setColor(sel ? accent.darker() : MUTED);
            FontMetrics fm=g2.getFontMetrics();
            String lbl=label.length()>8 ? label.substring(0,7)+"…" : label; //$NON-NLS-1$
            g2.drawString(lbl,(w-fm.stringWidth(lbl))/2,h-3);
            g2.dispose();
        }
    }
}
