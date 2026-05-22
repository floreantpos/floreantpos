package com.floreantpos.swing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JToggleButton;

/**
 * Pill-shaped toggle switch (iOS / Material style).
 * No text — pair with an external JLabel for a caption.
 */
public class ToggleSwitchButton extends JToggleButton {

    private static final Color ON_TRACK  = new Color(0x4C, 0xAF, 0x50); // green
    private static final Color OFF_TRACK = new Color(0x78, 0x90, 0xA8); // muted grey-blue
    private static final Color KNOB_CLR  = Color.WHITE;
    private static final Color SHADOW    = new Color(0, 0, 0, 50);

    private static final int W = 44;
    private static final int H = 22;
    private static final int KD = H - 4; // knob diameter

    public ToggleSwitchButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension d = new Dimension(W, H);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Track
        Color track = isSelected() ? ON_TRACK : OFF_TRACK;
        if (getModel().isRollover()) {
            float[] hsb = Color.RGBtoHSB(track.getRed(), track.getGreen(), track.getBlue(), null);
            track = Color.getHSBColor(hsb[0], hsb[1], Math.min(1f, hsb[2] + 0.12f));
        }
        g2.setColor(track);
        g2.fillRoundRect(0, 0, w, h, h, h);

        // Knob shadow
        int knobX = isSelected() ? (w - KD - 2) : 2;
        int knobY = (h - KD) / 2;
        g2.setColor(SHADOW);
        g2.fillOval(knobX + 1, knobY + 1, KD, KD);

        // Knob
        g2.setColor(KNOB_CLR);
        g2.fillOval(knobX, knobY, KD, KD);

        g2.dispose();
    }
}
