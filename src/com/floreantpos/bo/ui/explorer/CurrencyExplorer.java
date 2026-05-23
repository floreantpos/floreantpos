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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jdesktop.swingx.JXTable;

import com.floreantpos.POSConstants;
import com.floreantpos.bo.ui.BOMessageDialog;
import com.floreantpos.model.Currency;
import com.floreantpos.model.dao.CurrencyDAO;
import com.floreantpos.swing.BeanTableModel;
import com.floreantpos.swing.DoubleTextField;
import com.floreantpos.swing.FixedLengthTextField;
import com.floreantpos.swing.IntegerTextField;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.PosTableRenderer;
import com.floreantpos.ui.dialog.ConfirmDeleteDialog;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.util.CurrencyUtil;
import com.floreantpos.util.POSUtil;

import net.miginfocom.swing.MigLayout;

/**
 * Currency management — master-detail layout.
 *   Top:    JXTable grid of currencies + Add/Delete.
 *   Bottom: inline edit form populated by selection, with Save/Reset/New.
 */
public class CurrencyExplorer extends TransparentPanel {

	private static final Color SECTION_TITLE_FG = new Color(0x1E, 0x2D, 0x3D);
	private static final Color HEADER_BG        = new Color(0xEE, 0xF1, 0xF6);
	private static final Color SECTION_BORDER   = new Color(0xDD, 0xE3, 0xEC);

	private JXTable                table;
	private BeanTableModel<Currency> tableModel;

	private JLabel              lblEditingTarget;
	private FixedLengthTextField tfCode;
	private FixedLengthTextField tfName;
	private JTextField           tfSymbol;
	private DoubleTextField      tfExchangeRate;
	private DoubleTextField      tfTolerance;
	private IntegerTextField     tfDecimalPlaces;
	private JCheckBox            chkMain;

	private JButton              btnSave;
	private JButton              btnReset;
	private JButton              btnNew;

	private Currency             currentEditing; // null = New

	public CurrencyExplorer() {
		setLayout(new BorderLayout());

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopPanel(), buildBottomPanel());
		split.setResizeWeight(0.5);
		split.setContinuousLayout(true);
		split.setOneTouchExpandable(true);
		split.setBorder(null);
		add(split, BorderLayout.CENTER);

		clearFormForNew();
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Top half — grid + Add/Delete
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildTopPanel() {
		tableModel = new BeanTableModel<Currency>(Currency.class);
		tableModel.addColumn(POSConstants.ID.toUpperCase(),   "id");           //$NON-NLS-1$
		tableModel.addColumn(POSConstants.NAME.toUpperCase(), "name");         //$NON-NLS-1$
		tableModel.addColumn("CODE",      "code");           //$NON-NLS-1$
		tableModel.addColumn("SYMBOL",    "symbol");         //$NON-NLS-1$
		tableModel.addColumn("RATE",      "exchangeRate");   //$NON-NLS-1$
		tableModel.addColumn("MAIN",      "main");           //$NON-NLS-1$
		tableModel.addColumn("TOLERANCE", "tolerance");      //$NON-NLS-1$
		tableModel.addRows(CurrencyDAO.getInstance().findAll());

		table = new JXTable(tableModel);
		table.setDefaultRenderer(Object.class, new PosTableRenderer());
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				int viewRow = table.getSelectedRow();
				if (viewRow < 0) return;
				int modelRow = table.convertRowIndexToModel(viewRow);
				loadFormFromCurrency(tableModel.getRow(modelRow));
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
		Currency c = tableModel.getRow(modelRow);

		if (ConfirmDeleteDialog.showMessage(POSUtil.getBackOfficeWindow(),
				POSConstants.CONFIRM_DELETE, POSConstants.DELETE) != ConfirmDeleteDialog.YES) {
			return;
		}
		try {
			CurrencyDAO.getInstance().delete(c);
			tableModel.removeRow(modelRow);
			clearFormForNew();
		} catch (Exception ex) {
			BOMessageDialog.showError(POSConstants.ERROR_MESSAGE, ex);
		}
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Bottom half — inline form
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildBottomPanel() {
		// Header bar
		JPanel header = new JPanel(new MigLayout("ins 6 10 6 10, fillx", "[grow][][][]", ""));
		header.setBackground(HEADER_BG);
		header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, SECTION_BORDER));

		lblEditingTarget = new JLabel("New Currency");
		lblEditingTarget.setFont(lblEditingTarget.getFont().deriveFont(Font.BOLD, 13f));
		lblEditingTarget.setForeground(SECTION_TITLE_FG);

		btnNew   = new JButton("New");
		btnReset = new JButton("Reset");
		btnSave  = new JButton("Save");
		btnNew.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				table.clearSelection();
				clearFormForNew();
				tfName.requestFocusInWindow();
			}
		});
		btnReset.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { onReset(); }
		});
		btnSave.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { onSave(); }
		});

		header.add(lblEditingTarget, "growx");
		header.add(btnNew);
		header.add(btnReset);
		header.add(btnSave);

		// Form body: 2-column grid with fixed-width inputs (text fields don't span the dialog)
		JPanel form = new JPanel(new MigLayout(
				"ins 18, wrap 5",
				"[140!,right][]30[140!,right][]push",
				"[]10[]10[]10[]"));
		form.setOpaque(false);

		tfName          = new FixedLengthTextField();
		tfCode          = new FixedLengthTextField();
		tfSymbol        = new JTextField();
		tfExchangeRate  = new DoubleTextField();
		tfTolerance     = new DoubleTextField();
		tfDecimalPlaces = new IntegerTextField(5);
		chkMain         = new JCheckBox("Main currency  (set as base; rate must be 1.0)");

		// Tight, semantically-sized inputs
		tfName.setPreferredSize(new Dimension(260, 30));
		tfCode.setPreferredSize(new Dimension(120, 30));
		tfSymbol.setPreferredSize(new Dimension(100, 30));
		tfExchangeRate.setPreferredSize(new Dimension(140, 30));
		tfTolerance.setPreferredSize(new Dimension(140, 30));
		tfDecimalPlaces.setPreferredSize(new Dimension(80, 30));

		form.add(labelFor("Name:"));
		form.add(tfName);
		form.add(labelFor("Code:"));
		form.add(tfCode);
		form.add(new JLabel("")); // spacer for push column

		form.add(labelFor("Symbol:"));
		form.add(tfSymbol);
		form.add(labelFor("Decimal Places:"));
		form.add(tfDecimalPlaces);
		form.add(new JLabel(""));

		form.add(labelFor("Exchange Rate:"));
		form.add(tfExchangeRate);
		form.add(labelFor("Tolerance:"));
		form.add(tfTolerance);
		form.add(new JLabel(""));

		form.add(new JLabel(""));
		form.add(chkMain, "span 4, gaptop 6, growx");

		// Hint row
		JLabel hint = new JLabel(
			"<html><font color='#6B7280'>" +
			"Tolerance is the rounding allowance applied to the main currency only. " +
			"Setting Main on this currency clears the Main flag on all others." +
			"</font></html>");
		hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
		form.add(new JLabel(""));
		form.add(hint, "span 4, gaptop 4, growx");

		JScrollPane scroll = new JScrollPane(form);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(header, BorderLayout.NORTH);
		bottom.add(scroll, BorderLayout.CENTER);
		bottom.setPreferredSize(new Dimension(0, 360));
		return bottom;
	}

	private JLabel labelFor(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		l.setForeground(SECTION_TITLE_FG);
		l.setHorizontalAlignment(SwingConstants.RIGHT);
		l.setBorder(new EmptyBorder(0, 0, 0, 10));
		return l;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Form ↔ model
	// ─────────────────────────────────────────────────────────────────────
	private void loadFormFromCurrency(Currency c) {
		currentEditing = c;
		lblEditingTarget.setText("Editing: " + (c.getName() == null ? "(unnamed)" : c.getName()));
		tfName.setText(safe(c.getName()));
		tfCode.setText(safe(c.getCode()));
		tfSymbol.setText(safe(c.getSymbol()));
		tfExchangeRate.setText(c.getExchangeRate() == null ? "1.0" : String.valueOf(c.getExchangeRate()));
		tfTolerance.setText(c.getTolerance() == null ? "0" : String.valueOf(c.getTolerance()));
		tfDecimalPlaces.setText(String.valueOf(c.getDecimalPlaces()));
		chkMain.setSelected(c.isMain());
	}

	private void clearFormForNew() {
		currentEditing = null;
		lblEditingTarget.setText("New Currency");
		tfName.setText("");
		tfCode.setText("");
		tfSymbol.setText("");
		tfExchangeRate.setText("1.0");
		tfTolerance.setText("0");
		tfDecimalPlaces.setText("2");
		chkMain.setSelected(false);
	}

	private void onReset() {
		if (currentEditing == null) {
			clearFormForNew();
		} else {
			loadFormFromCurrency(currentEditing);
		}
	}

	private void onSave() {
		String name = tfName.getText().trim();
		String code = tfCode.getText().trim();
		if (POSUtil.isBlankOrNull(code)) {
			POSMessageDialog.showError(POSUtil.getFocusedWindow(), "Code is required");
			return;
		}

		double exchangeRate = tfExchangeRate.getDouble();
		boolean isMain = chkMain.isSelected();
		if (isMain && exchangeRate != 1.0) {
			POSMessageDialog.showMessage(POSUtil.getFocusedWindow(), "Exchange rate must be 1.0 for main currency");
			return;
		}
		double tolerance = tfTolerance.getDoubleOrZero();
		if (tolerance > 0 && !isMain) {
			POSMessageDialog.showMessage(POSUtil.getFocusedWindow(), "Please check Main to set tolerance amount.");
			tfTolerance.setText("0");
			return;
		}

		Currency target = (currentEditing != null) ? currentEditing : new Currency();
		target.setName(name);
		target.setCode(code);
		target.setSymbol(tfSymbol.getText());
		target.setExchangeRate(exchangeRate);
		target.setTolerance(tolerance);
		target.setDecimalPlaces(tfDecimalPlaces.getInteger());
		target.setMain(isMain);

		try {
			CurrencyDAO.getInstance().saveOrUpdate(target);

			// If this row is now Main, clear Main on all others
			if (isMain) {
				List<Currency> all = CurrencyDAO.getInstance().findAll();
				Session session = null;
				Transaction tx = null;
				try {
					session = CurrencyDAO.getInstance().createNewSession();
					tx = session.beginTransaction();
					for (Currency c : all) {
						if (target.getId() != null && target.getId().equals(c.getId())) continue;
						if (c.isMain()) {
							c.setMain(false);
							session.saveOrUpdate(c);
						}
					}
					tx.commit();
				} catch (Exception ex) {
					if (tx != null) tx.rollback();
					BOMessageDialog.showError(POSConstants.ERROR_MESSAGE, ex);
				} finally {
					if (session != null) session.close();
				}
			}

			CurrencyUtil.populateCurrency();
		} catch (Exception ex) {
			BOMessageDialog.showError(POSConstants.ERROR_MESSAGE, ex);
			return;
		}

		// Reload grid so Main flips are reflected on other rows
		refreshGrid();
		// Reselect the saved row
		int saveRow = indexOfCurrency(target);
		if (saveRow >= 0) {
			int viewRow = table.convertRowIndexToView(saveRow);
			table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
		}
		lblEditingTarget.setText("Editing: " + target.getName());
		POSMessageDialog.showMessage(POSUtil.getFocusedWindow(), "Saved.");
	}

	private int indexOfCurrency(Currency c) {
		if (c.getId() == null) return -1;
		List<Currency> rows = tableModel.getRows();
		for (int i = 0; i < rows.size(); i++) {
			if (c.getId().equals(rows.get(i).getId())) return i;
		}
		return -1;
	}

	private void refreshGrid() {
		List<Currency> all = CurrencyDAO.getInstance().findAll();
		tableModel.getRows().clear();
		tableModel.addRows(all);
		table.repaint();
	}

	protected BeanTableModel<Currency> getModel() {
		return tableModel;
	}

	private static String safe(String s) { return s == null ? "" : s; }
}
