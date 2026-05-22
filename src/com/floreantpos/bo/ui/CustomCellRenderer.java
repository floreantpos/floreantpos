package com.floreantpos.bo.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class CustomCellRenderer extends DefaultTableCellRenderer {
	private Border unselectedBorder = null;
	private Border selectedBorder   = null;

	private static final int    ROW_HEIGHT   = 34;
	private static final Border CELL_PADDING = BorderFactory.createEmptyBorder(6, 7, 6, 7);
	public static final  Color  GRID_COLOR   = new Color(0xDD, 0xE3, 0xEC);
	private static final String CONFIGURED   = "posTableConfigured"; //$NON-NLS-1$

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		// ── One-time table setup ──────────────────────────────────────────
		if (table.getClientProperty(CONFIGURED) == null) {
			table.putClientProperty(CONFIGURED, Boolean.TRUE);
			table.setShowHorizontalLines(true);
			table.setShowVerticalLines(false);
			table.setGridColor(GRID_COLOR);
			if (table.getRowHeight() < ROW_HEIGHT) table.setRowHeight(ROW_HEIGHT);
			// Register a consistent Boolean renderer for checkbox columns
			table.setDefaultRenderer(Boolean.class, new BooleanCellRenderer());
		}

		if (selectedBorder == null)
			selectedBorder   = BorderFactory.createMatteBorder(0, 0, 0, 0, table.getSelectionBackground());
		if (unselectedBorder == null)
			unselectedBorder = BorderFactory.createMatteBorder(0, 0, 0, 0, table.getBackground());

		// ── Image byte[] ──────────────────────────────────────────────────
		if (value instanceof byte[]) {
			byte[] imageData = (byte[]) value;
			ImageIcon image = new ImageIcon(imageData);
			image = new ImageIcon(image.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH));
			if (imageData != null) table.setRowHeight(row, 120);
			JLabel l = new JLabel(image);
			l.setBorder(isSelected ? selectedBorder : unselectedBorder);
			return l;
		}

		// ── Color swatch ──────────────────────────────────────────────────
		if (value instanceof Color) {
			JLabel l = new JLabel();
			l.setOpaque(true);
			l.setBackground((Color) value);
			l.setBorder(isSelected ? selectedBorder : unselectedBorder);
			return l;
		}

		// ── Date ──────────────────────────────────────────────────────────
		if (value instanceof Date)
			value = new SimpleDateFormat("MM/dd hh:mm a").format((Date) value); //$NON-NLS-1$

		// ── String / fallthrough ──────────────────────────────────────────
		if (value instanceof String)
			value = "<html>" + value + "</html>"; //$NON-NLS-1$ //$NON-NLS-2$

		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		((JLabel) c).setBorder(CELL_PADDING);
		if (!isSelected) c.setBackground(rowBg(table, row));
		return c;
	}

	public static Color rowBg(JTable table, int row) {
		Color alt = UIManager.getColor("Table.alternateRowColor"); //$NON-NLS-1$
		return (alt != null && row % 2 == 1) ? alt : table.getBackground();
	}

	// ── Consistent Boolean/checkbox renderer ──────────────────────────────

	public static class BooleanCellRenderer implements TableCellRenderer {
		private final JCheckBox cb = new JCheckBox();

		public BooleanCellRenderer() {
			cb.setHorizontalAlignment(SwingConstants.CENTER);
			cb.setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			cb.setSelected(Boolean.TRUE.equals(value));
			cb.setBackground(isSelected ? table.getSelectionBackground() : rowBg(table, row));
			cb.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
			return cb;
		}
	}
}
