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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import com.floreantpos.POSConstants;
import com.floreantpos.bo.ui.BOMessageDialog;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.UserType;
import com.floreantpos.model.dao.UserTypeDAO;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.PosTableRenderer;
import com.floreantpos.ui.dialog.ConfirmDeleteDialog;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.util.POSUtil;

import net.miginfocom.swing.MigLayout;

/**
 * User Type management — master-detail layout.
 *   Top:    grid of all user types with Add/Delete.
 *   Bottom: inline edit form (name + permissions checklist) with
 *           Save/Reset/New buttons.
 */
public class UserTypeExplorer extends TransparentPanel {

	private static final Color SECTION_TITLE_FG = new Color(0x1E, 0x2D, 0x3D);
	private static final Color HEADER_BG        = new Color(0xEE, 0xF1, 0xF6);
	private static final Color SECTION_BORDER   = new Color(0xDD, 0xE3, 0xEC);

	private List<UserType> typeList;

	private JTable table;
	private UserTypeExplorerTableModel tableModel;

	private JLabel              lblEditingTarget;
	private JTextField          tfTypeName;
	private Map<UserPermission, JCheckBox> permCheckBoxes;
	private JButton             btnSelectAll;
	private JButton             btnSelectNone;

	private JButton             btnSave;
	private JButton             btnReset;
	private JButton             btnNew;

	private UserType            currentEditing; // null = New

	public UserTypeExplorer() {
		UserTypeDAO dao = new UserTypeDAO();
		typeList = dao.findAll();

		setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopPanel(), buildBottomPanel());
		split.setResizeWeight(0.4);
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
		tableModel = new UserTypeExplorerTableModel();
		table = new JTable(tableModel);
		table.setRowHeight(PosUIManager.getSize(24));
		table.setDefaultRenderer(Object.class, new PosTableRenderer());
		table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				int row = table.getSelectedRow();
				if (row < 0 || row >= typeList.size()) return;
				loadFormFromUserType(typeList.get(row));
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
				tfTypeName.requestFocusInWindow();
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
		int row = table.getSelectedRow();
		if (row < 0) return;
		UserType ut = typeList.get(row);
		if (ConfirmDeleteDialog.showMessage(UserTypeExplorer.this,
				POSConstants.CONFIRM_DELETE, POSConstants.DELETE) != ConfirmDeleteDialog.YES) {
			return;
		}
		try {
			new UserTypeDAO().delete(ut);
			tableModel.deleteType(ut, row);
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

		lblEditingTarget = new JLabel("New User Type");
		lblEditingTarget.setFont(lblEditingTarget.getFont().deriveFont(Font.BOLD, 13f));
		lblEditingTarget.setForeground(SECTION_TITLE_FG);

		btnNew   = new JButton("New");
		btnReset = new JButton("Reset");
		btnSave  = new JButton("Save");
		btnNew.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				table.clearSelection();
				clearFormForNew();
				tfTypeName.requestFocusInWindow();
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

		// Form body
		JPanel form = new JPanel(new MigLayout(
				"ins 14, fillx, wrap 2",
				"[140!,right][grow,fill]",
				"[]10[]4[grow,fill]"));
		form.setOpaque(false);

		tfTypeName = new JTextField();
		tfTypeName.setPreferredSize(new Dimension(320, 30));

		form.add(labelFor("Type Name:"));
		form.add(tfTypeName, "growx, wmin 240");

		// Permissions header row with quick-select buttons
		JPanel permHeader = new JPanel(new MigLayout("ins 0", "[grow][][]", ""));
		permHeader.setOpaque(false);
		JLabel lblPerm = new JLabel("Permissions");
		lblPerm.setFont(lblPerm.getFont().deriveFont(Font.BOLD));
		lblPerm.setForeground(SECTION_TITLE_FG);
		btnSelectAll  = new JButton("Select all");
		btnSelectNone = new JButton("Select none");
		btnSelectAll.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { setAllPermissions(true); }
		});
		btnSelectNone.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { setAllPermissions(false); }
		});
		permHeader.add(lblPerm, "growx");
		permHeader.add(btnSelectAll);
		permHeader.add(btnSelectNone);

		// Permissions grouped into 3 categorical columns
		permCheckBoxes = new LinkedHashMap<UserPermission, JCheckBox>();
		for (UserPermission p : UserPermission.permissions) {
			JCheckBox cb = new JCheckBox(p.toString());
			cb.setOpaque(false);
			permCheckBoxes.put(p, cb);
		}

		JPanel permGrid = new JPanel(new MigLayout("ins 8, fillx",
				"[grow,sg c]10[grow,sg c]10[grow,sg c]", "[top]"));
		permGrid.setOpaque(false);
		permGrid.add(buildPermissionColumn("Ticket Permissions", new UserPermission[]{
				UserPermission.CREATE_TICKET,
				UserPermission.VIEW_ALL_OPEN_TICKETS,
				UserPermission.VIEW_ALL_CLOSE_TICKETS,
				UserPermission.HOLD_TICKET,
				UserPermission.SPLIT_TICKET,
				UserPermission.TRANSFER_TICKET,
				UserPermission.SETTLE_TICKET,
				UserPermission.REOPEN_TICKET,
				UserPermission.VOID_TICKET,
				UserPermission.AUTHORIZE_TICKETS,
				UserPermission.MODIFY_PRINTED_TICKET,
				UserPermission.ADD_DISCOUNT,
				UserPermission.REFUND,
		}), "grow");
		permGrid.add(buildPermissionColumn("Admin & Manager Permissions", new UserPermission[]{
				UserPermission.PERFORM_ADMINISTRATIVE_TASK,
				UserPermission.PERFORM_MANAGER_TASK,
				UserPermission.VIEW_BACK_OFFICE,
				UserPermission.VIEW_EXPLORERS,
				UserPermission.VIEW_REPORTS,
				UserPermission.MANAGE_TABLE_LAYOUT,
				UserPermission.ALL_FUNCTIONS,
				UserPermission.QUICK_MAINTENANCE,
				UserPermission.SHUT_DOWN,
		}), "grow");
		permGrid.add(buildPermissionColumn("Other Permissions", new UserPermission[]{
				UserPermission.DRAWER_ASSIGNMENT,
				UserPermission.DRAWER_PULL,
				UserPermission.PAY_OUT,
				UserPermission.TABLE_BOOKING,
				UserPermission.KITCHEN_DISPLAY,
		}), "grow");

		JScrollPane permScroll = new JScrollPane(permGrid);
		permScroll.setBorder(BorderFactory.createLineBorder(SECTION_BORDER));
		permScroll.getVerticalScrollBar().setUnitIncrement(16);

		form.add(new JLabel(""));
		form.add(permHeader, "growx");

		form.add(new JLabel(""));
		form.add(permScroll, "grow, hmin 180");

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(header, BorderLayout.NORTH);
		bottom.add(form, BorderLayout.CENTER);
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

	private JPanel buildPermissionColumn(String title, UserPermission[] perms) {
		JPanel col = new JPanel(new MigLayout("ins 10, wrap 1, fillx", "[grow]", ""));
		col.setBackground(new Color(0xF7, 0xF9, 0xFC));
		col.setBorder(BorderFactory.createLineBorder(SECTION_BORDER));

		JLabel lbl = new JLabel(title);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
		lbl.setForeground(SECTION_TITLE_FG);
		col.add(lbl, "growx, gapbottom 6");

		for (UserPermission p : perms) {
			JCheckBox cb = permCheckBoxes.get(p);
			if (cb != null) col.add(cb, "growx");
		}
		return col;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Form ↔ model
	// ─────────────────────────────────────────────────────────────────────
	private void loadFormFromUserType(UserType u) {
		currentEditing = u;
		lblEditingTarget.setText("Editing: " + (u.getName() == null ? "(unnamed)" : u.getName()));
		tfTypeName.setText(u.getName() == null ? "" : u.getName());

		setAllPermissions(false);
		Set<UserPermission> permissions = u.getPermissions();
		if (permissions != null && !permissions.isEmpty()) {
			for (Map.Entry<UserPermission, JCheckBox> e : permCheckBoxes.entrySet()) {
				if (permissions.contains(e.getKey())) {
					e.getValue().setSelected(true);
				}
			}
		}
	}

	private void clearFormForNew() {
		currentEditing = null;
		lblEditingTarget.setText("New User Type");
		tfTypeName.setText("");
		if (permCheckBoxes != null) setAllPermissions(false);
	}

	private void setAllPermissions(boolean checked) {
		if (permCheckBoxes == null) return;
		for (JCheckBox cb : permCheckBoxes.values()) {
			cb.setSelected(checked);
		}
	}

	private void onReset() {
		if (currentEditing == null) clearFormForNew();
		else                        loadFormFromUserType(currentEditing);
	}

	private void onSave() {
		String name = tfTypeName.getText().trim();
		if (StringUtils.isEmpty(name)) {
			POSMessageDialog.showError(POSUtil.getFocusedWindow(),
					POSConstants.TYPE_NAME_CANNOT_BE_EMPTY);
			return;
		}

		UserType target = (currentEditing != null) ? currentEditing : new UserType();
		target.setName(name);
		target.clearPermissions();
		for (Map.Entry<UserPermission, JCheckBox> e : permCheckBoxes.entrySet()) {
			if (e.getValue().isSelected()) {
				target.addTopermissions(e.getKey());
			}
		}

		try {
			new UserTypeDAO().saveOrUpdate(target);
		} catch (Exception ex) {
			BOMessageDialog.showError(POSConstants.ERROR_MESSAGE, ex);
			return;
		}

		if (currentEditing == null) {
			tableModel.addType(target);
			currentEditing = target;
			int newRow = typeList.size() - 1;
			table.getSelectionModel().setSelectionInterval(newRow, newRow);
		} else {
			tableModel.fireTableDataChanged();
		}
		lblEditingTarget.setText("Editing: " + target.getName());
		POSMessageDialog.showMessage(POSUtil.getFocusedWindow(), "Saved.");
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Table model
	// ─────────────────────────────────────────────────────────────────────
	class UserTypeExplorerTableModel extends AbstractTableModel {
		String[] columnNames = { POSConstants.TYPE_NAME, "Permission Count" };

		@Override public int getRowCount()    { return typeList == null ? 0 : typeList.size(); }
		@Override public int getColumnCount() { return columnNames.length; }
		@Override public String getColumnName(int column) { return columnNames[column]; }
		@Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }

		@Override public Object getValueAt(int rowIndex, int columnIndex) {
			if (typeList == null) return ""; //$NON-NLS-1$
			UserType u = typeList.get(rowIndex);
			switch (columnIndex) {
				case 0: return u.getName();
				case 1: return (u.getPermissions() == null) ? "0" : String.valueOf(u.getPermissions().size());
			}
			return null;
		}

		public void addType(UserType type) {
			if (typeList == null) typeList = new ArrayList<UserType>();
			int size = typeList.size();
			typeList.add(type);
			fireTableRowsInserted(size, size);
		}

		public void deleteType(UserType type, int index) {
			typeList.remove(type);
			fireTableRowsDeleted(index, index);
		}
	}
}
