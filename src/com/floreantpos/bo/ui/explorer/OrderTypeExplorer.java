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
package com.floreantpos.bo.ui.explorer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXTable;

import com.floreantpos.Messages;
import com.floreantpos.POSConstants;
import com.floreantpos.bo.ui.BOMessageDialog;
import com.floreantpos.bo.ui.CustomCellRenderer;
import com.floreantpos.extension.ExtensionManager;
import com.floreantpos.extension.OrderServiceExtension;
import com.floreantpos.model.OrderType;
import com.floreantpos.model.dao.OrderTypeDAO;
import com.floreantpos.swing.BeanTableModel;
import com.floreantpos.swing.MessageDialog;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.util.POSUtil;

import net.miginfocom.swing.MigLayout;

/**
 * Order Type back-office screen.
 *
 * Layout: vertical split pane.
 *   Top half  — JXTable grid of all order types, with Add/Delete buttons.
 *   Bottom half — inline configuration form populated by the row selection,
 *                 with Save/Reset/New buttons.
 */
public class OrderTypeExplorer extends TransparentPanel {

	private static final Color SECTION_TITLE_FG = new Color(0x1E, 0x2D, 0x3D);
	private static final Color SECTION_BG       = new Color(0xF7, 0xF9, 0xFC);
	private static final Color SECTION_BORDER   = new Color(0xDD, 0xE3, 0xEC);
	private static final Color HEADER_BG        = new Color(0xEE, 0xF1, 0xF6);

	private JXTable table;
	private BeanTableModel<OrderType> tableModel;

	// Bottom-pane form fields
	private JLabel       lblEditingTarget;
	private JTextField   tfName;
	private JCheckBox    chkEnabled;
	private JCheckBox    chkShowTableSelection;
	private JCheckBox    chkShowGuestSelection;
	private JCheckBox    chkShouldPrintToKitchen;
	private JCheckBox    chkCloseOnPaid;
	private JCheckBox    chkPrepaid;
	private JCheckBox    chkDelivery;
	private JCheckBox    chkRequiredCustomerData;
	private JCheckBox    chkShowItemBarcode;
	private JCheckBox    chkShowInLoginScreen;
	private JCheckBox    chkConsolidateItemsInReceipt;
	private JCheckBox    chkAllowSeatBasedOrder;
	private JCheckBox    chkHideItemWithEmptyInventory;
	private JCheckBox    chkHasForHereAndToGo;
	private JCheckBox    chkBarTab;
	private JCheckBox    chkPreAuthCreditCard;
	private JCheckBox    chkShowPriceOnButton;
	private JCheckBox    chkShowStockCountOnButton;
	private JCheckBox    chkShowUnitPriceInTicketGrid;
	private JCheckBox    chkRetailOrder;
	private JCheckBox    chkAllowToAddTipsLater;

	private JButton      btnSave;
	private JButton      btnReset;
	private JButton      btnNew;

	private OrderType    currentEditing; // null = "New Order Type"

	public OrderTypeExplorer() {
		setLayout(new BorderLayout());

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopPanel(), buildBottomPanel());
		split.setResizeWeight(0.5);
		split.setContinuousLayout(true);
		split.setOneTouchExpandable(true);
		split.setBorder(null);

		add(split, BorderLayout.CENTER);

		// Start with a clean "New Order Type" form
		clearFormForNew();
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Top half — grid + Add/Delete
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildTopPanel() {
		tableModel = new BeanTableModel<OrderType>(OrderType.class);
		tableModel.addColumn(POSConstants.ID.toUpperCase(),              "id");                   //$NON-NLS-1$
		tableModel.addColumn(POSConstants.NAME.toUpperCase(),            "name");                 //$NON-NLS-1$
		tableModel.addColumn(Messages.getString("OrderTypeExplorer.0"),  "showTableSelection");   //$NON-NLS-1$ //$NON-NLS-2$
		tableModel.addColumn(Messages.getString("OrderTypeExplorer.2"),  "showGuestSelection");   //$NON-NLS-1$ //$NON-NLS-2$
		tableModel.addColumn(POSConstants.PRINT_TO_KITCHEN,              "shouldPrintToKitchen"); //$NON-NLS-1$
		tableModel.addColumn(POSConstants.ENABLED.toUpperCase(),         "enabled");              //$NON-NLS-1$
		tableModel.addColumn(Messages.getString("OrderTypeExplorer.4"),  "preAuthCreditCard");    //$NON-NLS-1$ //$NON-NLS-2$
		tableModel.addRows(OrderTypeDAO.getInstance().findAll());

		table = new JXTable(tableModel);
		table.setDefaultRenderer(Object.class, new CustomCellRenderer());
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				int viewRow = table.getSelectedRow();
				if (viewRow < 0) return;
				int modelRow = table.convertRowIndexToModel(viewRow);
				OrderType selected = tableModel.getRow(modelRow);
				loadFormFromOrderType(selected);
			}
		});

		JPanel topPanel = new JPanel(new BorderLayout(0, 6));
		topPanel.setOpaque(false);
		topPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel buttonRow = new JPanel(new MigLayout("ins 4 4 14 4", "[]rel[]push", ""));
		buttonRow.setOpaque(false);

		JButton addButton = new JButton(POSConstants.ADD);
		addButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				table.clearSelection();
				clearFormForNew();
				tfName.requestFocusInWindow();
			}
		});

		JButton deleteButton = new JButton(POSConstants.DELETE);
		deleteButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				deleteSelected();
			}
		});

		buttonRow.add(addButton);
		buttonRow.add(deleteButton);
		topPanel.add(buttonRow, BorderLayout.SOUTH);

		return topPanel;
	}

	private void deleteSelected() {
		int viewRow = table.getSelectedRow();
		if (viewRow < 0) return;
		int modelRow = table.convertRowIndexToModel(viewRow);
		OrderType ot = tableModel.getRow(modelRow);

		if (POSMessageDialog.showYesNoQuestionDialog(OrderTypeExplorer.this,
				POSConstants.CONFIRM_DELETE, POSConstants.DELETE) != JOptionPane.YES_OPTION) {
			return;
		}

		try {
			new OrderTypeDAO().delete(ot);
			tableModel.removeRow(modelRow);
			POSMessageDialog.showMessage(POSUtil.getFocusedWindow(),
					Messages.getString("TerminalConfigurationView.40")); //$NON-NLS-1$
			clearFormForNew();
		} catch (Exception ex) {
			BOMessageDialog.showError(POSConstants.ERROR_MESSAGE, ex);
		}
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Bottom half — inline configuration form
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildBottomPanel() {
		// Header bar with editing target indicator + action buttons
		JPanel header = new JPanel(new MigLayout("ins 6 10 6 10, fillx", "[grow][][][]", ""));
		header.setBackground(HEADER_BG);
		header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, SECTION_BORDER));

		lblEditingTarget = new JLabel("New Order Type");
		lblEditingTarget.setFont(lblEditingTarget.getFont().deriveFont(Font.BOLD, 13f));
		lblEditingTarget.setForeground(SECTION_TITLE_FG);

		btnSave  = new JButton("Save");
		btnReset = new JButton("Reset");
		btnNew   = new JButton("New");
		btnSave.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { onSave(); }
		});
		btnReset.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { onReset(); }
		});
		btnNew.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				table.clearSelection();
				clearFormForNew();
				tfName.requestFocusInWindow();
			}
		});

		header.add(lblEditingTarget, "growx");
		header.add(btnNew);
		header.add(btnReset);
		header.add(btnSave);

		// Form body — three grouped sections side by side, scrollable as a whole
		JPanel form = new JPanel(new MigLayout("ins 10, fillx, wrap 1", "[grow]", "[]10[]"));
		form.setOpaque(false);

		// Name row spans full width above the three columns
		JPanel nameRow = new JPanel(new MigLayout("ins 0, fillx", "[100!][grow]", ""));
		nameRow.setOpaque(false);
		JLabel nameLbl = new JLabel("Name:");
		nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD));
		tfName = new JTextField();
		nameRow.add(nameLbl,  "alignx left");
		nameRow.add(tfName,   "growx");
		form.add(nameRow, "growx");

		// Three columns
		JPanel columns = new JPanel(new MigLayout("ins 0, fillx", "[grow,sg c][grow,sg c][grow,sg c]", ""));
		columns.setOpaque(false);

		columns.add(buildSection("Identity & Ticket Setup", new JCheckBox[]{
				chkEnabled                  = new JCheckBox(POSConstants.ENABLED),
				chkShowInLoginScreen        = new JCheckBox(Messages.getString("OrderTypeForm.10")), //$NON-NLS-1$
				chkShowTableSelection       = new JCheckBox(Messages.getString("OrderTypeForm.1")),  //$NON-NLS-1$
				chkShowGuestSelection       = new JCheckBox(Messages.getString("OrderTypeForm.2")),  //$NON-NLS-1$
				chkAllowSeatBasedOrder      = new JCheckBox("Allow seat based order"),
				chkHasForHereAndToGo        = new JCheckBox(Messages.getString("OrderTypeForm.13")), //$NON-NLS-1$
				chkBarTab                   = new JCheckBox(Messages.getString("OrderTypeForm.14")), //$NON-NLS-1$
				chkRetailOrder              = new JCheckBox("Retail"),
		}), "grow");

		columns.add(buildSection("Workflow & Payment", new JCheckBox[]{
				chkShouldPrintToKitchen     = new JCheckBox(Messages.getString("OrderTypeForm.3")), //$NON-NLS-1$
				chkPrepaid                  = new JCheckBox(Messages.getString("OrderTypeForm.5")), //$NON-NLS-1$
				chkCloseOnPaid              = new JCheckBox(Messages.getString("OrderTypeForm.4")), //$NON-NLS-1$
				chkPreAuthCreditCard        = new JCheckBox(Messages.getString("OrderTypeForm.0")), //$NON-NLS-1$
				chkAllowToAddTipsLater      = new JCheckBox("Allow to add tips later"),
				chkRequiredCustomerData     = new JCheckBox(Messages.getString("OrderTypeForm.6")), //$NON-NLS-1$
				chkDelivery                 = new JCheckBox("Delivery"),
		}), "grow");

		columns.add(buildSection("Display Options", new JCheckBox[]{
				chkShowPriceOnButton          = new JCheckBox("Show price on button"),
				chkShowStockCountOnButton     = new JCheckBox("Show stock count on button"),
				chkShowUnitPriceInTicketGrid  = new JCheckBox("Show unit price in ticket grid"),
				chkShowItemBarcode            = new JCheckBox(Messages.getString("OrderTypeForm.9")),  //$NON-NLS-1$
				chkConsolidateItemsInReceipt  = new JCheckBox(Messages.getString("OrderTypeForm.11")), //$NON-NLS-1$
				chkHideItemWithEmptyInventory = new JCheckBox(Messages.getString("OrderTypeForm.12")), //$NON-NLS-1$
		}), "grow");

		form.add(columns, "growx");

		// Hide Delivery if the extension is not installed (preserves old behaviour)
		OrderServiceExtension orderServiceExtension =
				(OrderServiceExtension) ExtensionManager.getPlugin(OrderServiceExtension.class);
		if (orderServiceExtension == null) {
			chkDelivery.setVisible(false);
		}

		// Delivery ⇒ force-check required-customer-data
		chkDelivery.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent e) {
				if (chkDelivery.isSelected()) {
					chkRequiredCustomerData.setSelected(true);
					chkRequiredCustomerData.setEnabled(false);
				} else {
					chkRequiredCustomerData.setEnabled(true);
				}
			}
		});

		JScrollPane scroll = new JScrollPane(form);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(header, BorderLayout.NORTH);
		bottom.add(scroll, BorderLayout.CENTER);
		bottom.setPreferredSize(new Dimension(0, 320));
		return bottom;
	}

	private JPanel buildSection(String title, JCheckBox[] items) {
		JPanel p = new JPanel(new MigLayout("ins 10, wrap 1, fillx", "[grow]", ""));
		p.setBackground(SECTION_BG);
		p.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(SECTION_BORDER),
				new EmptyBorder(2, 2, 2, 2)));

		JLabel lbl = new JLabel(title);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
		lbl.setForeground(SECTION_TITLE_FG);
		lbl.setHorizontalAlignment(SwingConstants.LEFT);
		p.add(lbl, "growx, gapbottom 4");

		for (JCheckBox cb : items) {
			cb.setOpaque(false);
			p.add(cb, "growx");
		}
		return p;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Form ↔ model
	// ─────────────────────────────────────────────────────────────────────
	private void loadFormFromOrderType(OrderType o) {
		currentEditing = o;
		lblEditingTarget.setText("Editing: " + (o.getName() == null ? "(unnamed)" : o.getName()));
		tfName.setText(safe(o.getName()));
		chkEnabled                  .setSelected(o.isEnabled());
		chkShowTableSelection       .setSelected(o.isShowTableSelection());
		chkShowGuestSelection       .setSelected(o.isShowGuestSelection());
		chkShouldPrintToKitchen     .setSelected(o.isShouldPrintToKitchen());
		chkPrepaid                  .setSelected(o.isPrepaid());
		chkCloseOnPaid              .setSelected(o.isCloseOnPaid());
		chkDelivery                 .setSelected(o.isDelivery());
		chkRequiredCustomerData     .setSelected(o.isRequiredCustomerData());
		chkShowItemBarcode          .setSelected(o.isShowItemBarcode());
		chkShowInLoginScreen        .setSelected(o.isShowInLoginScreen());
		chkConsolidateItemsInReceipt.setSelected(o.isConsolidateItemsInReceipt());
		chkAllowSeatBasedOrder      .setSelected(o.isAllowSeatBasedOrder());
		chkHideItemWithEmptyInventory.setSelected(o.isHideItemWithEmptyInventory());
		chkHasForHereAndToGo        .setSelected(o.isHasForHereAndToGo());
		chkBarTab                   .setSelected(o.isBarTab());
		chkPreAuthCreditCard        .setSelected(o.isPreAuthCreditCard());
		chkShowPriceOnButton        .setSelected(o.isShowPriceOnButton());
		chkShowStockCountOnButton   .setSelected(o.isShowStockCountOnButton());
		chkShowUnitPriceInTicketGrid.setSelected(o.isShowUnitPriceInTicketGrid());
		chkRetailOrder              .setSelected(o.isRetailOrder());
		chkAllowToAddTipsLater      .setSelected(o.isAllowToAddTipsLater());
	}

	private void clearFormForNew() {
		currentEditing = null;
		lblEditingTarget.setText("New Order Type");
		tfName.setText("");
		for (JCheckBox cb : new JCheckBox[]{
				chkEnabled, chkShowTableSelection, chkShowGuestSelection, chkShouldPrintToKitchen,
				chkCloseOnPaid, chkPrepaid, chkDelivery, chkRequiredCustomerData, chkShowItemBarcode,
				chkShowInLoginScreen, chkConsolidateItemsInReceipt, chkAllowSeatBasedOrder,
				chkHideItemWithEmptyInventory, chkHasForHereAndToGo, chkBarTab, chkPreAuthCreditCard,
				chkShowPriceOnButton, chkShowStockCountOnButton, chkShowUnitPriceInTicketGrid,
				chkRetailOrder, chkAllowToAddTipsLater}) {
			cb.setSelected(false);
		}
		chkEnabled.setSelected(true); // sensible default for new
	}

	private void onReset() {
		if (currentEditing == null) {
			clearFormForNew();
		} else {
			loadFormFromOrderType(currentEditing);
		}
	}

	private void onSave() {
		String name = tfName.getText().trim();
		if (POSUtil.isBlankOrNull(name)) {
			MessageDialog.showError(Messages.getString("MenuCategoryForm.26")); //$NON-NLS-1$
			return;
		}

		OrderType target = (currentEditing != null) ? currentEditing : new OrderType();
		target.setName(name);
		target.setEnabled(chkEnabled.isSelected());
		target.setShowTableSelection(chkShowTableSelection.isSelected());
		target.setShowGuestSelection(chkShowGuestSelection.isSelected());
		target.setShouldPrintToKitchen(chkShouldPrintToKitchen.isSelected());
		target.setPrepaid(chkPrepaid.isSelected());
		target.setCloseOnPaid(chkCloseOnPaid.isSelected());
		target.setDelivery(chkDelivery.isSelected());
		target.setRequiredCustomerData(chkRequiredCustomerData.isSelected());
		target.setShowItemBarcode(chkShowItemBarcode.isSelected());
		target.setShowInLoginScreen(chkShowInLoginScreen.isSelected());
		target.setConsolidateItemsInReceipt(chkConsolidateItemsInReceipt.isSelected());
		target.setAllowSeatBasedOrder(chkAllowSeatBasedOrder.isSelected());
		target.setHideItemWithEmptyInventory(chkHideItemWithEmptyInventory.isSelected());
		target.setHasForHereAndToGo(chkHasForHereAndToGo.isSelected());
		target.setBarTab(chkBarTab.isSelected());
		target.setPreAuthCreditCard(chkPreAuthCreditCard.isSelected());
		target.setShowPriceOnButton(chkShowPriceOnButton.isSelected());
		target.setShowStockCountOnButton(chkShowStockCountOnButton.isSelected());
		target.setShowUnitPriceInTicketGrid(chkShowUnitPriceInTicketGrid.isSelected());
		target.setRetailOrder(chkRetailOrder.isSelected());
		target.addProperty(OrderType.ALLOW_TO_ADD_TIPS_LATER, String.valueOf(chkAllowToAddTipsLater.isSelected()));

		try {
			OrderTypeDAO.getInstance().saveOrUpdate(target);
		} catch (Exception ex) {
			MessageDialog.showError(ex);
			return;
		}

		if (currentEditing == null) {
			tableModel.addRow(target);
			currentEditing = target;
			int newRow = tableModel.getRowCount() - 1;
			int viewRow = table.convertRowIndexToView(newRow);
			table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
		} else {
			table.repaint();
		}
		lblEditingTarget.setText("Editing: " + target.getName());
		POSMessageDialog.showMessage(POSUtil.getFocusedWindow(),
				Messages.getString("TerminalConfigurationView.40")); //$NON-NLS-1$
	}

	private static String safe(String s) { return s == null ? "" : s; }
}
