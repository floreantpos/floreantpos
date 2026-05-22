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
/**
 * 
 */
package com.floreantpos.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.UIManager;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.floreantpos.IconFactory;
import com.floreantpos.model.Ticket;
import com.floreantpos.swing.ListTableModel;
import com.floreantpos.util.NumberUtil;

public class PosTableRenderer extends DefaultTableCellRenderer {
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, h:m a"); //$NON-NLS-1$
	/**
	 * 
	 */
	private JCheckBox checkBox = new JCheckBox();
	private JLabel lblColor = new JLabel();
	private boolean showOnlineIcon;

	public PosTableRenderer() {
		this(false);
	}

	public PosTableRenderer(boolean showOnlineIcon) {
		this.showOnlineIcon = showOnlineIcon;
		checkBox.setHorizontalAlignment(SwingConstants.CENTER);
		lblColor.setOpaque(true);
	}

	private static final int    ROW_HEIGHT = 34;
	private static final String CONFIGURED = "posTableConfigured"; //$NON-NLS-1$

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		// One-time table setup via client property
		if (table.getClientProperty(CONFIGURED) == null) {
			table.putClientProperty(CONFIGURED, Boolean.TRUE);
			table.setShowHorizontalLines(true);
			table.setShowVerticalLines(false);
			table.setGridColor(com.floreantpos.bo.ui.CustomCellRenderer.GRID_COLOR);
			if (table.getRowHeight() < ROW_HEIGHT) table.setRowHeight(ROW_HEIGHT);
			table.setDefaultRenderer(Boolean.class, new com.floreantpos.bo.ui.CustomCellRenderer.BooleanCellRenderer());
		}

		JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
		if (showOnlineIcon) label.setIcon(null);
		label.setIcon(null);

		// Alternating row background for text cells
		if (!isSelected) {
			label.setBackground(com.floreantpos.bo.ui.CustomCellRenderer.rowBg(table, row));
		}

		if (value instanceof Boolean) {
			checkBox.setSelected(((Boolean) value).booleanValue());
			checkBox.setBackground(isSelected ? table.getSelectionBackground()
					: com.floreantpos.bo.ui.CustomCellRenderer.rowBg(table, row));
			return checkBox;
		}
		if (value instanceof Color) {
			Color color = (Color) value;
			String rgb = Integer.toHexString(color.getRGB()).toUpperCase();
			rgb = rgb.substring(2, rgb.length());

			lblColor.setText(rgb);
			lblColor.setBackground(color);

			return lblColor;
		}

		if (showOnlineIcon && column == 0) {
			Ticket ticket = null;
			TableModel tableModel = table.getModel();
			if (tableModel instanceof ListTableModel) {
				ListTableModel listTableModel = (ListTableModel) tableModel;
				Object rowData = listTableModel.getRowData(table.convertRowIndexToModel(row));
				if (rowData instanceof Ticket) {
					ticket = (Ticket) rowData;
				}
			}
			if (ticket == null) {
				return label;
			}

			if (ticket.isSourceWoocomerce()) {
				label.setIcon(IconFactory.getIcon("/ui_icons/", "woocom-logo.png")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else if (ticket.isSourceOnline()) {
				//label.setIcon(IconFactory.getIcon("/ui_icons/", "cloud.png")); //$NON-NLS-1$ //$NON-NLS-2$
				label.setIcon(IconFactory.getIcon("/ui_icons/", "menugreat.png")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return label;
	}

	@Override
	protected void setValue(Object value) {
		if (value == null) {
			setText(""); //$NON-NLS-1$
			return;
		}

		String text = value.toString();

		if (value instanceof Double || value instanceof Float) {
			text = NumberUtil.formatNumber(((java.lang.Number) value).doubleValue());
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
		else if (value instanceof Integer) {
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
		else if (value instanceof Date) {
			text = dateFormat.format(value);
			setHorizontalAlignment(SwingConstants.LEFT);
		}
		else {
			setHorizontalAlignment(SwingConstants.LEFT);
		}

		setText(" " + text + " "); //$NON-NLS-1$ //$NON-NLS-2$
	}
}