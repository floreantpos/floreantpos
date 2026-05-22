package com.floreantpos.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Card-style button UI: white rounded-corner card, icon above text, subtle drop shadow.
 * Used for the order-view action bar.
 */
public class CardPosButtonUI extends BasicButtonUI {

	/* ── Visual constants ─────────────────────────────────────────────────── */
	private static final Color CARD_BG       = Color.WHITE;
	private static final Color CARD_HOVER    = new Color(0xEEF4FF);
	private static final Color CARD_PRESS    = new Color(0xD5E5F8);
	private static final Color CARD_BORDER   = new Color(0xDDE3EC);
	// shadow: three semi-transparent layers for a soft glow
	private static final Color SHADOW_1      = new Color(0, 0, 0, 14);
	private static final Color SHADOW_2      = new Color(0, 0, 0,  8);
	private static final Color SHADOW_3      = new Color(0, 0, 0,  4);
	private static final int   ARC           = 14;   // corner radius

	public static ComponentUI createUI(JComponent c) {
		return new CardPosButtonUI();
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		AbstractButton b = (AbstractButton) c;
		b.setOpaque(false);
		b.setContentAreaFilled(false);
		b.setBorderPainted(false);
		b.setFocusPainted(false);
		// icon on top, label below — card layout
		b.setVerticalTextPosition(SwingConstants.BOTTOM);
		b.setHorizontalTextPosition(SwingConstants.CENTER);
		b.setHorizontalAlignment(SwingConstants.CENTER);
		b.setVerticalAlignment(SwingConstants.CENTER);
		b.setIconTextGap(PosUIManager.getSize(5));
		// inner padding; extra bottom/right for shadow
		b.setBorder(BorderFactory.createEmptyBorder(
				PosUIManager.getSize(6),
				PosUIManager.getSize(8),
				PosUIManager.getSize(8),
				PosUIManager.getSize(8)));
	}

	@Override
	public void update(Graphics g, JComponent c) {
		AbstractButton b     = (AbstractButton) c;
		ButtonModel    model = b.getModel();
		Graphics2D     g2    = (Graphics2D) g.create();

		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

			int w  = c.getWidth();
			int h  = c.getHeight();
			int cw = w - 3;
			int ch = h - 4;

				// Shadow
			g2.setColor(SHADOW_3); g2.fillRoundRect(3, 4, cw, ch, ARC + 4, ARC + 4);
			g2.setColor(SHADOW_2); g2.fillRoundRect(2, 3, cw, ch, ARC + 3, ARC + 3);
			g2.setColor(SHADOW_1); g2.fillRoundRect(1, 2, cw, ch, ARC + 2, ARC + 2);

			// Accent corner — only for tagged order-screen buttons
			boolean taggedForAccent = Boolean.TRUE.equals(b.getClientProperty("orderScreenAccent")); //$NON-NLS-1$
			if (ButtonStyleConfig.isAccentMode() && taggedForAccent) {
				Color accentColor = getCustomBackground(b);
				if (accentColor == null) accentColor = ButtonStyleConfig.getDefaultAccentColor();
				paintAccentCorner(g2, b, model, cw, ch, accentColor);
			} else {
				paintClassic(g2, b, model, cw, ch);
			}

		} finally {
			g2.dispose();
		}

		paint(g, c);
	}

	private void paintAccentCorner(Graphics2D g2, AbstractButton b, ButtonModel model, int cw, int ch, Color accent) {
		boolean sel = model.isSelected();

		// Light tinted base — same whether selected or not
		g2.setColor(Color.WHITE);
		g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);
		g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
				model.isPressed() ? 45 : 28));
		g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);

		// Quarter-circle arc in top-right corner
		// Selected → fully solid; unselected → normal opacity
		int r = (int)(Math.min(cw, ch) * 0.42);
		Shape saved = g2.getClip();
		g2.clip(new RoundRectangle2D.Float(0, 0, cw, ch, ARC, ARC));
		g2.setColor(sel ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
		g2.fillOval(cw - r, -r, r * 2, r * 2);

		// Small white dot on the arc at ~45°
		int dotR = Math.max(3, r / 5);
		int offset = (int)(r * 0.42);
		g2.setColor(Color.WHITE);
		g2.fillOval(cw - offset - dotR * 2, offset - dotR, dotR * 2, dotR * 2);
		g2.setClip(saved);

		// Hover overlay (only when not selected)
		if (model.isRollover() && !model.isPressed() && !sel) {
			g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12));
			g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);
		}

		// Border — stronger when selected
		if (sel) {
			g2.setColor(accent);
			g2.setStroke(new BasicStroke(2f));
		} else {
			g2.setColor(new Color(
				blnd(accent.getRed(),   0xDD, 0.35f),
				blnd(accent.getGreen(), 0xE3, 0.35f),
				blnd(accent.getBlue(),  0xEC, 0.35f)));
			g2.setStroke(new BasicStroke(1f));
		}
		g2.drawRoundRect(0, 0, cw - 1, ch - 1, ARC, ARC);
	}

	private static int blnd(int c, int base, float f) {
		return Math.min(255, Math.max(0, (int)(c * f + base * (1 - f))));
	}

	private Color getCustomBackground(AbstractButton b) {
		Color comp = b.getBackground();
		Color def1 = UIManager.getColor("Button.background"); //$NON-NLS-1$
		Color def2 = UIManager.getColor("control"); //$NON-NLS-1$
		if (comp != null && !comp.equals(def1) && !comp.equals(def2)
				&& comp.getRed() + comp.getGreen() + comp.getBlue() < 700) // not near-white
			return comp;
		return null;
	}

	private void paintClassic(Graphics2D g2, AbstractButton b, ButtonModel model, int cw, int ch) {
		Color bg = resolveBackground(b, model);
		g2.setColor(bg);
		g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);
		g2.setColor(CARD_BORDER);
		g2.setStroke(new BasicStroke(1f));
		g2.drawRoundRect(0, 0, cw - 1, ch - 1, ARC, ARC);
	}


	private static final Color CARD_SELECTED      = new Color(0x1E5FAA); // deep blue when group is selected
	private static final Color CARD_SELECTED_HOVER = new Color(0x2570C8);

	private Color resolveBackground(AbstractButton b, ButtonModel model) {
		// Toggle-button selected state (e.g. active menu group)
		if (model.isSelected()) {
			return model.isRollover() ? CARD_SELECTED_HOVER : CARD_SELECTED;
		}

		// If an explicit custom background was set (e.g. yellow for Pay Total), use it.
		Color comp = b.getBackground();
		Color uiDefault = UIManager.getColor("Button.background"); //$NON-NLS-1$
		boolean hasCustomBg = comp != null && !comp.equals(uiDefault)
				&& !comp.equals(UIManager.getColor("control")); //$NON-NLS-1$

		if (hasCustomBg) {
			return model.isPressed() ? comp.darker() : (model.isRollover() ? comp.brighter() : comp);
		}
		if (model.isPressed())  return CARD_PRESS;
		if (model.isRollover()) return CARD_HOVER;
		return CARD_BG;
	}

	/** White text when card is dark (selected toggle), dark text otherwise. */
	private Color resolveTextColor(AbstractButton b) {
		ButtonModel model = b.getModel();
		if (model.isSelected()) return Color.WHITE;
		Color fg = b.getForeground();
		return (fg != null) ? fg : new Color(0x1E2D3D);
	}

	@Override
	protected void installDefaults(AbstractButton b) {
		super.installDefaults(b);
		// ensure foreground is legible on white card
		if (b.getForeground() == null || b.getForeground().equals(Color.WHITE)) {
			b.setForeground(new Color(0x1E2D3D));
		}
	}

}
