/**
 * ************************************************************************
 * * The contents of this file are subject to the MRPL 1.2
 * * (the  "License"),  being   the  Mozilla   Public  License
 * * Version 1.1  with a permitted attribution clause; you may not  use this
 * * file except in compliance with the License. You  may  obtain  a copy of
 * * the License at http://www.floreantpos.org/license.html
 * * Software distributed under the License  is  distributed  on  an "AS IS"
 * * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * * License for the specific  language  governing  rights  and  limitations
 * * under the License.
 * * The Original Code is FLOREANT POS.
 * * The Initial Developer of the Original Code is OROCUBE LLC
 * * All portions are Copyright (C) 2015 OROCUBE LLC
 * * All Rights Reserved.
 * ************************************************************************
 */
/*
 * GroupView.java
 *
 * Created on August 5, 2006, 9:29 PM
 */

package com.floreantpos.ui.views.order;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractButton;

import com.floreantpos.IconFactory;
import com.floreantpos.swing.ButtonStyleConfig;
import com.floreantpos.swing.MaterialIconPainter;
import com.floreantpos.PosException;
import com.floreantpos.PosLog;
import com.floreantpos.bo.ui.explorer.QuickMaintenanceExplorer;
import com.floreantpos.main.Application;
import com.floreantpos.model.MenuGroup;
import com.floreantpos.model.MenuItem;
import com.floreantpos.model.OrderType;
import com.floreantpos.model.dao.MenuItemDAO;
import com.floreantpos.swing.PosButton;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.ui.views.order.actions.ItemSelectionListener;
import com.floreantpos.util.CurrencyUtil;

/**
 * 
 * @author MShahriar
 */
public class MenuItemView extends SelectionView {
	public final static String VIEW_NAME = "ITEM_VIEW"; //$NON-NLS-1$

	private Vector<ItemSelectionListener> listenerList = new Vector<ItemSelectionListener>();

	private static final int GRID_GAP   = 10;
	private static final int MIN_COLS   = 5;
	private static final int MAX_COLS   = 9;
	private static final int TARGET_PX  = 165; // target tile size used to pick column count

	private int gridCols  = MIN_COLS;
	private int gridRows  = 2;
	private int tileSize  = TARGET_PX;

	private MenuGroup menuGroup;
	private Map<Integer, ItemButton> menuItemButtonMap = new HashMap<Integer, MenuItemView.ItemButton>();
	private boolean showPrice;
	private boolean showStockCount;

	/** Creates new form GroupView */
	public MenuItemView() {
		super(com.floreantpos.POSConstants.ITEMS, TARGET_PX, TARGET_PX);
		// Remove titled border — we want a clean full-width grid
		setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
		remove(actionButtonPanel);

		btnPrev.setText("<");
		btnNext.setText(">");

		add(btnPrev, BorderLayout.WEST);
		add(btnNext, BorderLayout.EAST);
	}

	/** Compute grid dimensions from container size, then delegate to base class. */
	@Override
	protected void renderItems() {
		int cw = buttonPanelContainer.getWidth();
		int ch = buttonPanelContainer.getHeight();
		if (cw < 100) cw = Math.max(MIN_COLS * TARGET_PX, getWidth() - 80);
		if (ch < 100) ch = Math.max(2 * TARGET_PX, getHeight() - 60);

		gridCols = Math.max(MIN_COLS, Math.min(MAX_COLS, cw / TARGET_PX));
		tileSize = Math.max(80, (cw - GRID_GAP * (gridCols - 1)) / gridCols);
		gridRows = Math.max(2, ch / (tileSize + GRID_GAP));

		setButtonSize(new Dimension(tileSize, tileSize));
		super.renderItems();
	}

	@Override
	protected LayoutManager createButtonPanelLayout() {
		return new SquareGridLayout(gridCols, tileSize, GRID_GAP);
	}

	@Override
	protected int getFitableButtonCount() {
		int cw = buttonPanelContainer.getWidth();
		int ch = buttonPanelContainer.getHeight();
		if (cw < 100) cw = Math.max(MIN_COLS * TARGET_PX, getWidth() - 80);
		if (ch < 100) ch = Math.max(2 * TARGET_PX, getHeight() - 60);

		int cols = Math.max(MIN_COLS, Math.min(MAX_COLS, cw / TARGET_PX));
		int tile = Math.max(80, (cw - GRID_GAP * (cols - 1)) / cols);
		int rows = Math.max(2, ch / (tile + GRID_GAP));
		return cols * rows;
	}

	/** Fixed-size square grid — tiles are never stretched by the container. */
	private static class SquareGridLayout implements LayoutManager {
		private final int cols, size, gap;

		SquareGridLayout(int cols, int size, int gap) {
			this.cols = cols; this.size = size; this.gap = gap;
		}

		@Override
		public void layoutContainer(java.awt.Container p) {
			int n = p.getComponentCount();
			for (int i = 0; i < n; i++) {
				int col = i % cols;
				int row = i / cols;
				p.getComponent(i).setBounds(col * (size + gap), row * (size + gap), size, size);
			}
		}

		@Override
		public Dimension preferredLayoutSize(java.awt.Container p) {
			int n = p.getComponentCount();
			int rows = Math.max(1, (n + cols - 1) / cols);
			return new Dimension(cols * size + (cols - 1) * gap, rows * size + (rows - 1) * gap);
		}

		@Override public Dimension minimumLayoutSize(java.awt.Container p) { return preferredLayoutSize(p); }
		@Override public void addLayoutComponent(String name, java.awt.Component c) {}
		@Override public void removeLayoutComponent(java.awt.Component c) {}
	}

	public MenuGroup getMenuGroup() {
		return menuGroup;
	}

	public void setMenuGroup(MenuGroup menuGroup) {
		this.menuGroup = menuGroup;

		menuItemButtonMap.clear();

		if (menuGroup == null) {
			setItems(null);
			return;
		}
		OrderType orderType = OrderView.getInstance().getCurrentTicket().getOrderType();
		showPrice = orderType.isShowPriceOnButton();
		showStockCount = orderType.isShowStockCountOnButton();
		MenuItemDAO dao = new MenuItemDAO();
		try {
			List<MenuItem> items = new ArrayList<>();
			if (menuGroup.getId() != null) {
				items = dao.findByParent(Application.getInstance().getTerminal(), menuGroup, orderType, false);
			}
			if (RootView.getInstance().isMaintenanceMode()) {
				MenuItem newMenuItem = new MenuItem(null, "", 0.0, 0.0);
				newMenuItem.setParent(menuGroup);
				items.add(newMenuItem);
			}
			// filterItemsByOrderType(items);
			setItems(items);
		} catch (PosException e) {
			PosLog.error(getClass(), e);
		}
	}

	@Override
	protected AbstractButton createItemButton(Object item) {
		MenuItem menuItem = (MenuItem) item;

		ItemButton itemButton = new ItemButton(menuItem);
		menuItemButtonMap.put(menuItem.getId(), itemButton);

		filterByStockAmount(menuItem, itemButton);
		setInitialized(true);
		return itemButton;
	}

	public void updateView(MenuItem menuItem) {
		setMenuGroup(menuItem.getParent());
	}

	public void addItemSelectionListener(ItemSelectionListener listener) {
		listenerList.add(listener);
	}

	public void removeItemSelectionListener(ItemSelectionListener listener) {
		listenerList.remove(listener);
	}

	private void fireItemSelected(MenuItem foodItem) {
		for (ItemSelectionListener listener : listenerList) {
			listener.itemSelected(foodItem);
		}
	}

	public void selectItem(MenuItem menuItem) {
		// ItemButton button = menuItemButtonMap.get(menuItem.getId());
		/*
		 * if(button != null) { button.requestFocus(); }
		 */
	}

	private void filterItemsByOrderType(List<MenuItem> items) {
		String orderType = OrderView.getInstance().getTicketView().getTicket().getOrderType().toString();
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			MenuItem menuItem = (MenuItem) iterator.next();
			List<OrderType> orderTypeList = menuItem.getOrderTypeList();

			if (orderTypeList == null || orderTypeList.size() == 0) {
				continue;
			}

			if (!orderTypeList.contains(orderType)) {
				iterator.remove();
			}
		}
	}

	private void filterByStockAmount(MenuItem menuItem, ItemButton itemButton) {
		if (menuItem.isDisableWhenStockAmountIsZero() && menuItem.getStockAmount() <= 0) {
			itemButton.setEnabled(false);
		}
	}

	// Flat color palette for items without a configured buttonColor (Square POS style)
	private static final Color[] ITEM_PALETTE = {
		new Color(0x3D7EBF), new Color(0x2E9E6B), new Color(0xC0392B),
		new Color(0x8E44AD), new Color(0xD35400), new Color(0x16A085),
		new Color(0x2980B9), new Color(0x27AE60), new Color(0xE74C3C),
		new Color(0x9B59B6), new Color(0xE67E22), new Color(0x1ABC9C),
	};
	public class ItemButton extends PosButton implements ActionListener, MouseListener {
		private static final int ARC = 18;
		MenuItem foodItem;
		private Color cardColor;
		private boolean hovered = false;
		private boolean pressed = false;

		ItemButton(MenuItem menuItem) {
			// Force plain UI so FlatLaf doesn't paint its own background over ours
			setUI(new javax.swing.plaf.basic.BasicButtonUI());
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			updateView(menuItem);
			addActionListener(this);
			addMouseListener(this);
		}

		private void updateView(MenuItem menuItem) {
			this.foodItem = menuItem;
			Color cfg = menuItem.getButtonColor();
			if (cfg != null) {
				cardColor = cfg;
			} else if (menuItem.getId() == null) {
				cardColor = new Color(0x607D8B);
			} else {
				// Stable color per item ID so it doesn't shift on re-render
				int idx = Math.abs(menuItem.getId().hashCode()) % ITEM_PALETTE.length;
				cardColor = ITEM_PALETTE[idx];
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

				int w = getWidth();
				int h = getHeight();

				// ── Drop shadow — 3 translucent layers offset down-right ──────────
				int cw = w - 3;
				int ch = h - 3;
				g2.setColor(new Color(0, 0, 0, 20));
				g2.fillRoundRect(3, 4, cw, ch, ARC + 2, ARC + 2);
				g2.setColor(new Color(0, 0, 0, 12));
				g2.fillRoundRect(2, 3, cw, ch, ARC + 1, ARC + 1);
				g2.setColor(new Color(0, 0, 0, 6));
				g2.fillRoundRect(1, 2, cw, ch, ARC,     ARC);

				// ── ACCENT MODE ───────────────────────────────────────────────────
				if (ButtonStyleConfig.isAccentMode() && foodItem.getId() != null) {
					paintAccentMode(g2, cw, ch);
					return;
				}

				// ── Card background (Classic) ─────────────────────────────────────
				Color bg = pressed ? cardColor.darker().darker()
						  : hovered ? cardColor.brighter()
						  : cardColor;
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);

				// ── "Add new" placeholder ─────────────────────────────────────────
				if (foodItem.getId() == null) {
					javax.swing.Icon ic = IconFactory.getIcon("/ui_icons/", "add+user.png"); //$NON-NLS-1$ //$NON-NLS-2$
					if (ic != null) {
						ic.paintIcon(this, g2, (cw - ic.getIconWidth()) / 2, (ch - ic.getIconHeight()) / 2);
					}
					return;
				}

				// ── Food image: fills top ~64% of the card ────────────────────────
				javax.swing.ImageIcon imgIcon = foodItem.getImage();
				Image img = imgIcon != null ? imgIcon.getImage() : null;
				int textAreaH = ch * 36 / 100;   // bottom 36% for label band
				int imgAreaH  = ch - textAreaH;

				if (img != null) {
					g2.setClip(new RoundRectangle2D.Float(0, 0, cw, imgAreaH + ARC, ARC, ARC));
					int iw = img.getWidth(null);
					int ih = img.getHeight(null);
					if (iw > 0 && ih > 0) {
						double scale = Math.max((double) cw / iw, (double) imgAreaH / ih);
						int sw = (int) (iw * scale);
						int sh = (int) (ih * scale);
						g2.drawImage(img, (cw - sw) / 2, (imgAreaH - sh) / 2, sw, sh, null);
					}
					g2.setClip(null);
				}

				// ── White label band — taller when no price so name can wrap ─────
				int textAreaHFinal = showPrice ? textAreaH : ch * 44 / 100;
				int imgAreaHFinal  = ch - textAreaHFinal;

				g2.setColor(Color.WHITE);
				g2.fillRect(0, imgAreaHFinal, cw, ARC);                       // bridge
				g2.fillRoundRect(0, imgAreaHFinal, cw, textAreaHFinal, ARC, ARC);

				// ── Text (black name + grey price) ────────────────────────────────
				Font nameFont  = getFont().deriveFont(Font.BOLD,  (float) PosUIManager.getFontSize(11));
				Font priceFont = getFont().deriveFont(Font.PLAIN, (float) PosUIManager.getFontSize(10));
				g2.setFont(nameFont);
				FontMetrics nfm = g2.getFontMetrics();

				String name   = foodItem.getDisplayName();
				int maxLineW  = cw - 8;
				String line1, line2 = null;
				if (nfm.stringWidth(name) <= maxLineW) {
					line1 = name;
				} else {
					int mid = name.length() / 2;
					int brk = mid;
					for (int k = mid; k >= 1; k--) {
						if (name.charAt(k) == ' ') { brk = k; break; }
					}
					line1 = name.substring(0, brk).trim();
					line2 = name.substring(brk).trim();
					while (line2.length() > 1 && nfm.stringWidth(line2) > maxLineW)
						line2 = line2.substring(0, line2.length() - 1) + "\u2026"; //$NON-NLS-1$
				}

				int lineGap    = 1;
				int nameLines  = (line2 != null) ? 2 : 1;
				int nameTotalH = nameLines * nfm.getHeight() + (nameLines - 1) * lineGap;
				int priceH     = showPrice ? (lineGap + nfm.getHeight()) : 0;
				int textTop    = imgAreaHFinal + (textAreaHFinal - nameTotalH - priceH) / 2;

				g2.setColor(new Color(0x1E2D3D));
				int y = textTop + nfm.getAscent();
				g2.drawString(line1, (cw - nfm.stringWidth(line1)) / 2, y);
				if (line2 != null) {
					y += nfm.getHeight() + lineGap;
					g2.drawString(line2, (cw - nfm.stringWidth(line2)) / 2, y);
				}

				if (showPrice) {
					g2.setFont(priceFont);
					FontMetrics pfm = g2.getFontMetrics();
					g2.setColor(new Color(0x555555));
					String priceStr = CurrencyUtil.getCurrencySymbol() + foodItem.getPrice();
					g2.drawString(priceStr, (cw - pfm.stringWidth(priceStr)) / 2,
						textTop + nameTotalH + lineGap + pfm.getAscent());
				}

				// ── Stock badge (top-right corner) ────────────────────────────────
				if (showStockCount) {
					Double stockAmt = foodItem.getStockAmount();
					String stock = String.valueOf(stockAmt != null ? stockAmt.intValue() : 0);
					Font sf = getFont().deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(9));
					g2.setFont(sf);
					FontMetrics sfm = g2.getFontMetrics();
					int bw = sfm.stringWidth(stock) + 8;
					int bh = sfm.getHeight() + 2;
					g2.setColor(new Color(0xE74C3C));
					g2.fillRoundRect(cw - bw - 3, 3, bw, bh, 6, 6);
					g2.setColor(Color.WHITE);
					g2.drawString(stock, cw - sfm.stringWidth(stock) - 7, 3 + sfm.getAscent());
				}

			} finally {
				g2.dispose();
			}
		}

		private void paintAccentMode(Graphics2D g2, int cw, int ch) {
			Color accent = cardColor;

			// White base
			g2.setColor(pressed ? new Color(0xF0, 0xF4, 0xF8) : Color.WHITE);
			g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);

			if (hovered && !pressed) {
				g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 14));
				g2.fillRoundRect(0, 0, cw, ch, ARC, ARC);
			}

			// Top accent strip — 10px, rounded top corners only
			java.awt.Shape savedClip = g2.getClip();
			g2.setClip(new java.awt.Rectangle(0, 0, cw, 10));
			g2.setColor(accent);
			g2.fillRoundRect(0, 0, cw, ARC * 2, ARC, ARC);
			g2.setClip(savedClip);

			// Large soft bubble — bottom-left, proportional to button size
			int blobSize = (int)(cw * 0.88);
			int blobX    = (int)(-blobSize * 0.26);
			int blobY    = ch - (int)(blobSize * 0.54);
			g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
			g2.fillOval(blobX, blobY, blobSize, blobSize);

			// Border (accent-tinted)
			g2.setColor(new Color(
				accentMix(accent.getRed(), 0xDF),
				accentMix(accent.getGreen(), 0xE4),
				accentMix(accent.getBlue(), 0xEA)));
			g2.setStroke(new java.awt.BasicStroke(1f));
			g2.drawRoundRect(0, 0, cw - 1, ch - 1, ARC, ARC);

			// Icon in soft-tinted rounded square (top area)
			int iconAreaH = ch * 58 / 100;
			int squareSize = Math.min(44, iconAreaH - 10);
			int ix = (cw - squareSize) / 2;
			int iy = (iconAreaH - squareSize) / 2 + 6;
			Color iconBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 36);
			g2.setColor(iconBg);
			g2.fillRoundRect(ix, iy, squareSize, squareSize, 10, 10);

			// Use per-item icon from properties, fall back to global default
			String iconKey = null;
			if (foodItem.getProperties() != null)
				iconKey = foodItem.getProperties().get("icon"); //$NON-NLS-1$
			if (iconKey == null) iconKey = ButtonStyleConfig.getDefaultIconKey();
			int iconDrawSize = (int)(squareSize * 0.68);
			MaterialIconPainter.paint(g2, iconKey,
				ix + squareSize / 2, iy + squareSize / 2,
				iconDrawSize, accent.darker());

			// Text sits directly on the white button — no extra band
			int textAreaH = ch * 38 / 100;
			int imgAreaH  = ch - textAreaH;

			// Name + price — fixed sizes, no theme scaling
			boolean showP = showPrice;
			Font nameFont  = new Font(Font.DIALOG, Font.BOLD,  13);
			Font priceFont = new Font(Font.DIALOG, Font.BOLD,  14);
			g2.setFont(nameFont);
			FontMetrics nfm = g2.getFontMetrics();
			String itemName = foodItem.getDisplayName();
			int maxW = cw - 8;
			String l1 = itemName, l2 = null;
			if (nfm.stringWidth(itemName) > maxW) {
				int mid = itemName.length() / 2, brk = mid;
				for (int k = mid; k >= 1; k--) if (itemName.charAt(k) == ' ') { brk = k; break; }
				l1 = itemName.substring(0, brk).trim();
				l2 = itemName.substring(brk).trim();
				while (l2.length() > 1 && nfm.stringWidth(l2) > maxW)
					l2 = l2.substring(0, l2.length() - 1) + "…"; //$NON-NLS-1$
			}
			int lines = (l2 != null) ? 2 : 1;
			int nameTotalH = lines * nfm.getHeight();
			int priceH = showP ? nfm.getHeight() : 0;
			int textTop = imgAreaH + (textAreaH - nameTotalH - priceH) / 2;
			g2.setColor(new Color(0x1E, 0x2D, 0x3D));
			int ty = textTop + nfm.getAscent();
			g2.drawString(l1, (cw - nfm.stringWidth(l1)) / 2, ty);
			if (l2 != null) { ty += nfm.getHeight(); g2.drawString(l2, (cw - nfm.stringWidth(l2)) / 2, ty); }
			if (showP) {
				g2.setFont(priceFont);
				FontMetrics pfm = g2.getFontMetrics();
				g2.setColor(new Color(0x55, 0x55, 0x55));
				String ps = com.floreantpos.util.CurrencyUtil.getCurrencySymbol() + foodItem.getPrice();
				g2.drawString(ps, (cw - pfm.stringWidth(ps)) / 2, textTop + nameTotalH + pfm.getAscent());
			}

			// Stock badge
			if (showStockCount) {
				Double stockAmt = foodItem.getStockAmount();
				String stock = String.valueOf(stockAmt != null ? stockAmt.intValue() : 0);
				java.awt.Font sf = getFont().deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(9));
				g2.setFont(sf);
				FontMetrics sfm = g2.getFontMetrics();
				int bw = sfm.stringWidth(stock) + 8, bh = sfm.getHeight() + 2;
				g2.setColor(new Color(0xE7, 0x4C, 0x3C));
				g2.fillRoundRect(cw - bw - 3, 3, bw, bh, 6, 6);
				g2.setColor(Color.WHITE);
				g2.drawString(stock, cw - sfm.stringWidth(stock) - 7, 3 + sfm.getAscent());
			}
		}

		private int accentMix(int ch, int base) {
			return Math.min(255, Math.max(0, (int)(ch * 0.25f + base * 0.75f)));
		}

		public void actionPerformed(ActionEvent e) {
			if (OrderView.getInstance().isVisible() && RootView.getInstance().isMaintenanceMode()) {
				if (foodItem.getId() != null) {
					foodItem = MenuItemDAO.getInstance().loadInitialized(foodItem.getId());
				}
				QuickMaintenanceExplorer.quickMaintain(foodItem);
			}
			fireItemSelected(foodItem);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			pressed = true;
			repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			pressed = false;
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			hovered = true;
			repaint();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			hovered = false;
			pressed = false;
			repaint();
		}
	}

	public void disableItemButton(MenuItem item) {
		ItemButton itemButton = menuItemButtonMap.get(item.getId());
		itemButton.setEnabled(false);

	}
}
