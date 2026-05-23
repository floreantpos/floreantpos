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
package com.floreantpos.ui.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.floreantpos.actions.DrawerBleedAction;
import com.floreantpos.actions.PayoutAction;
import com.floreantpos.main.Application;
import com.floreantpos.model.DrawerPullReport;
import com.floreantpos.model.Terminal;
import com.floreantpos.model.User;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.dao.DrawerPullReportDAO;
import com.floreantpos.print.DrawerpullReportService;
import com.floreantpos.util.CurrencyUtil;

import net.miginfocom.swing.MigLayout;

/**
 * Cash & Card Management — sidebar-tabbed dialog. Uses the platform L&F's
 * default colours and fonts (no overrides) so it matches the rest of the
 * application as themes change.
 *
 * Tabs:
 *   • Dashboard            (placeholder)
 *   • Shifts &amp; Cash Registers (placeholder)
 *   • Drawer Status        (live — summary + Close Drawer / Drawer Bleed / Payout)
 *   • Card Transactions    (placeholder)
 *   • Tips Payment         (placeholder)
 */
public class CashCardManagementDialog extends POSDialog {

	private static final String TAB_DASHBOARD       = "dashboard";
	private static final String TAB_SHIFTS          = "shifts";
	private static final String TAB_DRAWER_STATUS   = "drawer_status";
	private static final String TAB_CARD_TXN        = "card_txn";
	private static final String TAB_TIPS            = "tips";

	private CardLayout  cardLayout;
	private JPanel      cardArea;
	private JEditorPane drawerReportPane;
	private JLabel      drawerHeaderShift;
	private Timer       autoRefreshTimer;

	public CashCardManagementDialog() {
		super(Application.getPosWindow(), true);
		setTitle("Cash & Card Management");
		setLayout(new BorderLayout());

		add(buildTopBar(),    BorderLayout.NORTH);
		add(buildSidebar(),   BorderLayout.WEST);
		add(buildCardArea(),  BorderLayout.CENTER);

		setMinimumSize(new Dimension(700, 500));

		installEscClose();
	}

	private void installEscClose() {
		javax.swing.JComponent root = (javax.swing.JComponent) getContentPane();
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closeDlg");
		root.getActionMap().put("closeDlg", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
				setCanceled(true);
				dispose();
			}
		});
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Top bar — User / title / Date / Go Back
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildTopBar() {
		JPanel top = new JPanel(new MigLayout("ins 10 14 10 14, fillx", "[]push[]push[][]", ""));

		User u = Application.getCurrentUser();
		JLabel lblUser  = new JLabel("User:  " + (u == null ? "—" : u.getFullName()));
		JLabel lblTitle = new JLabel("Cash Management");
		JLabel lblDate  = new JLabel("Date:  " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()));

		JButton btnGoBack = new JButton("Go Back");
		btnGoBack.setBackground(new java.awt.Color(0xDC, 0x26, 0x26));
		btnGoBack.setForeground(java.awt.Color.WHITE);
		btnGoBack.setFocusPainted(false);
		btnGoBack.setBorderPainted(false);
		btnGoBack.setOpaque(true);
		btnGoBack.setFont(btnGoBack.getFont().deriveFont(java.awt.Font.BOLD, 14f));
		btnGoBack.setPreferredSize(new Dimension(130, 40));
		btnGoBack.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				setCanceled(true);
				dispose();
			}
		});

		top.add(lblUser);
		top.add(lblTitle);
		top.add(lblDate, "gapright 20");
		top.add(btnGoBack);
		top.add(new JSeparator(), "newline, span, growx");
		return top;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Sidebar — vertical tab buttons
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildSidebar() {
		JPanel side = new JPanel(new MigLayout("ins 14 10 10 10, wrap 1, fillx", "[grow,fill]", ""));
		side.setPreferredSize(new Dimension(230, 0));

		ButtonGroup group = new ButtonGroup();
		JToggleButton btnDash   = makeSideTab("Dashboard",                TAB_DASHBOARD);
		JToggleButton btnShifts = makeSideTab("Shifts & Cash Registers",  TAB_SHIFTS);
		JToggleButton btnDrawer = makeSideTab("Drawer Status",            TAB_DRAWER_STATUS);
		JToggleButton btnCard   = makeSideTab("Card Transactions",        TAB_CARD_TXN);
		JToggleButton btnTips   = makeSideTab("Tips Payment",             TAB_TIPS);

		group.add(btnDash);
		group.add(btnShifts);
		group.add(btnDrawer);
		group.add(btnCard);
		group.add(btnTips);

		side.add(btnDash);
		side.add(btnShifts, "gaptop 4");
		side.add(btnDrawer, "gaptop 4");
		side.add(btnCard,   "gaptop 4");
		side.add(btnTips,   "gaptop 4");

		side.add(new JSeparator(SwingConstants.VERTICAL), "dock east");
		btnDash.setSelected(true);
		return side;
	}

	private JToggleButton makeSideTab(String label, final String cardName) {
		final JToggleButton b = new JToggleButton(label);
		b.setHorizontalAlignment(SwingConstants.LEFT);
		b.setFocusPainted(false);
		b.setMargin(new java.awt.Insets(10, 12, 10, 12));
		b.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				cardLayout.show(cardArea, cardName);
				if (TAB_DRAWER_STATUS.equals(cardName)) refreshDrawerStatus();
				if (TAB_SHIFTS.equals(cardName))        refreshShiftsGrid();
				if (TAB_DASHBOARD.equals(cardName))     refreshDashboard();
			}
		});
		return b;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Card area — CardLayout switches between tab panels
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildCardArea() {
		cardLayout = new CardLayout();
		cardArea   = new JPanel(cardLayout);
		cardArea.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

		cardArea.add(buildDashboardPanel(), TAB_DASHBOARD);
		cardArea.add(buildShiftsRegistersPanel(), TAB_SHIFTS);
		cardArea.add(buildDrawerStatusPanel(),    TAB_DRAWER_STATUS);
		cardArea.add(buildCardTransactionsPanel(), TAB_CARD_TXN);
		cardArea.add(buildTipsPaymentPanel(),      TAB_TIPS);

		// Default landing tab: Dashboard
		cardLayout.show(cardArea, TAB_DASHBOARD);
		return cardArea;
	}

	private JPanel placeholder(String title, String subtitle) {
		JPanel p = new JPanel(new MigLayout("ins 18, fillx, wrap 1", "[grow,fill]", "[]8[]"));
		p.add(new JLabel(title));
		p.add(new JLabel(subtitle));
		return p;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Drawer Status tab
	//
	//  • Open  → DRAWER STATUS header + live sales-balance report +
	//            [Close drawer] [Drawer Bleed] [Payout] buttons
	//  • Closed→ "Drawer is Closed" message (no report) +
	//            single [Open Drawer] button
	// ─────────────────────────────────────────────────────────────────────
	private JButton    btnDrawerPrimary;   // "Close drawer" when open, "Open Drawer" when closed
	private JButton    btnDrawerBleed;
	private JButton    btnDrawerPayout;
	private JButton    btnDrawerPrint;

	private JPanel buildDrawerStatusPanel() {
		JPanel root = new JPanel(new BorderLayout(0, 10));

		JPanel head = new JPanel(new MigLayout("ins 0, fillx", "[grow]push[]", ""));
		head.add(new JLabel("DRAWER STATUS"));
		drawerHeaderShift = new JLabel();
		head.add(drawerHeaderShift);
		root.add(head, BorderLayout.NORTH);

		drawerReportPane = new JEditorPane();
		drawerReportPane.setContentType("text/html");
		drawerReportPane.setEditable(false);
		JScrollPane scroll = new JScrollPane(drawerReportPane);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		root.add(scroll, BorderLayout.CENTER);

		JPanel btns = new JPanel(new MigLayout("ins 10 0 0 0, fillx", "push[150!]12[150!]12[150!]12[150!]push", ""));

		btnDrawerPrimary = new JButton("Open Drawer");
		btnDrawerPrimary.setPreferredSize(new Dimension(150, 50));
		btnDrawerPrimary.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				DrawerStatusDialog d = new DrawerStatusDialog();
				d.setVisible(true);
				refreshDrawerStatus();
			}
		});

		btnDrawerBleed = new JButton("Drawer Bleed");
		btnDrawerBleed.setPreferredSize(new Dimension(150, 50));
		btnDrawerBleed.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try { new DrawerBleedAction().execute(); } catch (Exception ex) {
					POSMessageDialog.showError(CashCardManagementDialog.this, ex.getMessage());
				}
				refreshDrawerStatus();
			}
		});

		btnDrawerPayout = new JButton("Payout");
		btnDrawerPayout.setPreferredSize(new Dimension(150, 50));
		btnDrawerPayout.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try { new PayoutAction().execute(); } catch (Exception ex) {
					POSMessageDialog.showError(CashCardManagementDialog.this, ex.getMessage());
				}
				refreshDrawerStatus();
			}
		});

		btnDrawerPrint = new JButton("Print");
		btnDrawerPrint.setPreferredSize(new Dimension(150, 50));
		btnDrawerPrint.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try {
					Terminal t = Application.getInstance().refreshAndGetTerminal();
					if (t == null || !t.isCashDrawerAssigned()) {
						POSMessageDialog.showMessage(CashCardManagementDialog.this,
								"Drawer is closed — nothing to print.");
						return;
					}
					DrawerPullReport r = DrawerpullReportService.buildDrawerPullReport();
					r.setAssignedUser(t.getAssignedUser());
					com.floreantpos.print.PosPrintService.printDrawerPullReport(r, t);
				} catch (Exception ex) {
					POSMessageDialog.showError(CashCardManagementDialog.this, ex.getMessage());
				}
			}
		});

		btns.add(btnDrawerPrimary);
		btns.add(btnDrawerBleed);
		btns.add(btnDrawerPayout);
		btns.add(btnDrawerPrint);
		root.add(btns, BorderLayout.SOUTH);

		refreshDrawerStatus();
		return root;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Shifts & Cash Registers tab — sessions grid (manager-only)
	//
	//  One row per closed drawer-pull session in the selected period:
	//    Status | Date | Shift | Opener | Tot. Accountable | Actual
	//  Manager can type the Actual amount inline; persisted to
	//  DrawerPullReport.cashToDeposit on commit.
	// ─────────────────────────────────────────────────────────────────────
	private SessionsTableModel shiftsModel;
	private JTable             shiftsTable;
	private JLabel             shiftsTotalAccountable;
	private JLabel             shiftsTotalActual;
	private JLabel             shiftsRangeLabel;
	private boolean            shiftsFilterToday = true;

	private JPanel buildShiftsRegistersPanel() {
		JPanel root = new JPanel(new BorderLayout(0, 10));

		User u = Application.getCurrentUser();
		if (u == null || !u.hasPermission(UserPermission.PERFORM_MANAGER_TASK)) {
			JPanel locked = new JPanel(new MigLayout("ins 30, fillx, wrap 1, align center", "[align center]", "[]14[]"));
			locked.add(new JLabel("Manager Permission Required"));
			locked.add(new JLabel("This view shows the store's drawer sessions and is restricted to users with the 'Perform Manager Task' permission."));
			root.add(locked, BorderLayout.CENTER);
			return root;
		}

		// Top: filter toggle + range label + manual refresh
		JPanel head = new JPanel(new MigLayout("ins 0, fillx", "[][][][grow][]", ""));
		head.add(new JLabel("Show:"));
		final JToggleButton btnToday  = new JToggleButton("Today", true);
		final JToggleButton btnMonth  = new JToggleButton("This Month");
		ButtonGroup grp = new ButtonGroup();
		grp.add(btnToday);
		grp.add(btnMonth);
		btnToday.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				shiftsFilterToday = true;
				refreshShiftsGrid();
			}
		});
		btnMonth.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				shiftsFilterToday = false;
				refreshShiftsGrid();
			}
		});
		JButton btnManualRefresh = new JButton("⟳ Refresh");
		btnManualRefresh.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { refreshShiftsGrid(); }
		});
		head.add(btnToday);
		head.add(btnMonth);
		head.add(btnManualRefresh);
		shiftsRangeLabel = new JLabel();
		head.add(shiftsRangeLabel, "alignx right");
		root.add(head, BorderLayout.NORTH);

		// Middle: sessions grid
		shiftsModel = new SessionsTableModel();
		shiftsTable = new JTable(shiftsModel);
		// Larger, bolder cell font + 1.5em row height
		java.awt.Font baseFont = shiftsTable.getFont();
		java.awt.Font cellFont = baseFont.deriveFont(java.awt.Font.BOLD, baseFont.getSize() + 4f);
		shiftsTable.setFont(cellFont);
		java.awt.FontMetrics fm = shiftsTable.getFontMetrics(cellFont);
		shiftsTable.setRowHeight(Math.round(fm.getHeight() * 1.5f));
		shiftsTable.getTableHeader().setFont(cellFont.deriveFont(java.awt.Font.BOLD));
		shiftsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		shiftsTable.getTableHeader().setReorderingAllowed(false);

		// Column widths roughly matching the mock
		shiftsTable.getColumnModel().getColumn(0).setMaxWidth(70);   // Status
		shiftsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Date (with time)
		shiftsTable.getColumnModel().getColumn(2).setPreferredWidth(160); // Shift
		shiftsTable.getColumnModel().getColumn(3).setPreferredWidth(180); // Opener
		shiftsTable.getColumnModel().getColumn(4).setPreferredWidth(180); // Accountable
		shiftsTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Actual

		// Right-align amount columns
		DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
		rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
		shiftsTable.getColumnModel().getColumn(4).setCellRenderer(rightAlign);
		shiftsTable.getColumnModel().getColumn(5).setCellRenderer(rightAlign);

		// Status column: centred, coloured icon
		DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
			@Override public java.awt.Component getTableCellRendererComponent(
					JTable t, Object v, boolean sel, boolean focus, int r, int c) {
				JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, focus, r, c);
				l.setHorizontalAlignment(SwingConstants.CENTER);
				if (!sel) l.setForeground(new java.awt.Color(0x0A, 0xA8, 0x46));
				return l;
			}
		};
		shiftsTable.getColumnModel().getColumn(0).setCellRenderer(statusRenderer);

		// Listen for edits on the Actual column
		shiftsModel.addTableModelListener(new TableModelListener() {
			@Override public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == SessionsTableModel.COL_ACTUAL) {
					recomputeShiftsTotals();
				}
			}
		});

		JScrollPane scroll = new JScrollPane(shiftsTable);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		root.add(scroll, BorderLayout.CENTER);

		// Bottom: totals row + action buttons
		JPanel bottom = new JPanel(new MigLayout("ins 10 0 0 0, fillx, wrap 1", "[grow,fill]", "[]10[]"));

		JPanel totals = new JPanel(new MigLayout("ins 6 14 6 14, fillx", "push[140!][140!]", ""));
		totals.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new java.awt.Color(0x16, 0xB3, 0x4F)));
		shiftsTotalAccountable = new JLabel("0.00", SwingConstants.RIGHT);
		shiftsTotalActual      = new JLabel("0.00", SwingConstants.RIGHT);
		totals.add(shiftsTotalAccountable);
		totals.add(shiftsTotalActual);
		bottom.add(totals, "growx");

		JPanel btns = new JPanel(new MigLayout("ins 0, fillx", "push[200!]14[180!]", ""));
		JButton btnEditActual = new JButton("Edit Actual Amount");
		btnEditActual.setPreferredSize(new Dimension(200, 50));
		btnEditActual.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { showEditActualDialog(); }
		});
		JButton btnDetail = new JButton("Show Detail");
		btnDetail.setPreferredSize(new Dimension(180, 50));
		btnDetail.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { showSelectedRowDetail(); }
		});
		btns.add(btnEditActual);
		btns.add(btnDetail);
		bottom.add(btns, "growx");

		root.add(bottom, BorderLayout.SOUTH);

		refreshShiftsGrid();
		return root;
	}

	private void refreshShiftsGrid() {
		if (shiftsModel == null) return;
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		Date end = new Date();
		Date start;
		String rangeLabel;
		if (shiftsFilterToday) {
			start = c.getTime();
			rangeLabel = "Today (" + DateFormat.getDateInstance(DateFormat.MEDIUM).format(start) + ")";
		} else {
			c.set(java.util.Calendar.DAY_OF_MONTH, 1);
			start = c.getTime();
			rangeLabel = "This Month (" +
					new java.text.SimpleDateFormat("MMM yyyy").format(start) + ")";
		}
		if (shiftsRangeLabel != null) shiftsRangeLabel.setText(rangeLabel);

		java.util.List<DrawerPullReport> rows       = new java.util.ArrayList<DrawerPullReport>();
		java.util.List<String>           shiftNames = new java.util.ArrayList<String>();
		java.util.Set<Integer>           openIdx    = new java.util.HashSet<Integer>();

		// Row 0: currently-open drawer (if any) — synthetic, not persisted
		Terminal terminal = Application.getInstance().getTerminal();
		if (terminal != null && terminal.isCashDrawerAssigned()) {
			try {
				DrawerPullReport live = DrawerpullReportService.buildDrawerPullReport();
				live.setAssignedUser(terminal.getAssignedUser());
				// Stamp report time with the actual drawer-open time so it shows in Date col
				if (live.getReportTime() == null) {
					live.setReportTime(new Date());
				}
				rows.add(live);
				String openShift = terminal.getProperty(Terminal.PROP_DRAWER_SHIFT_NAME);
				shiftNames.add(openShift == null ? "" : openShift);
				openIdx.add(0);
			} catch (Exception ignored) {}
		}

		// Closed reports in range — derive shift name from reportTime
		try {
			java.util.List<com.floreantpos.model.Shift> allShifts =
					com.floreantpos.model.dao.ShiftDAO.getInstance().findAll();
			java.util.List<DrawerPullReport> reports = new DrawerPullReportDAO().findReports(start, end);
			if (reports != null) {
				for (DrawerPullReport r : reports) {
					rows.add(r);
					shiftNames.add(deriveShiftName(r.getReportTime(), allShifts));
				}
			}
		} catch (Exception ignored) {}

		shiftsModel.setRows(rows, shiftNames, openIdx);
		recomputeShiftsTotals();
	}

	/**
	 * Match the time-of-day portion of `when` against each Shift's
	 * [startTime, endTime] window. Returns the matching shift name or "".
	 */
	private static String deriveShiftName(Date when, java.util.List<com.floreantpos.model.Shift> shifts) {
		if (when == null || shifts == null) return "";
		java.util.Calendar src = java.util.Calendar.getInstance();
		src.setTime(when);
		int hh = src.get(java.util.Calendar.HOUR_OF_DAY);
		int mm = src.get(java.util.Calendar.MINUTE);
		int minutesOfDay = hh * 60 + mm;

		for (com.floreantpos.model.Shift sh : shifts) {
			if (sh.getStartTime() == null || sh.getEndTime() == null) continue;
			int s = minutesOfDay(sh.getStartTime());
			int e = minutesOfDay(sh.getEndTime());
			// Handle shifts that span midnight (e.g. 22:00 → 02:00)
			boolean inWindow = (s <= e)
					? (minutesOfDay >= s && minutesOfDay <  e)
					: (minutesOfDay >= s || minutesOfDay <  e);
			if (inWindow) return sh.getName() == null ? "" : sh.getName();
		}
		return "";
	}

	private static int minutesOfDay(Date d) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(d);
		return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
	}

	private void showSelectedRowDetail() {
		if (shiftsTable == null) return;
		int row = shiftsTable.getSelectedRow();
		if (row < 0) {
			POSMessageDialog.showMessage(this, "Select a row first.");
			return;
		}
		DrawerPullReport p = shiftsModel.getRows().get(row);
		// Build a small detail dialog showing the full breakdown for this session
		JEditorPane pane = new JEditorPane();
		pane.setContentType("text/html");
		pane.setEditable(false);
		pane.setText(buildSessionDetailHtml(p, shiftsModel.isOpenRow(row)));
		pane.setCaretPosition(0);

		JDialog dlg = new JDialog(this, "Drawer Session Detail", true);
		dlg.setLayout(new BorderLayout());
		dlg.add(new JScrollPane(pane), BorderLayout.CENTER);
		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { dlg.dispose(); }
		});
		JPanel south = new JPanel();
		south.add(close);
		dlg.add(south, BorderLayout.SOUTH);
		dlg.setSize(560, 640);
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
	}

	private void showEditActualDialog() {
		if (shiftsTable == null) return;
		int row = shiftsTable.getSelectedRow();
		if (row < 0) {
			POSMessageDialog.showMessage(this, "Select a row first.");
			return;
		}
		if (shiftsModel.isOpenRow(row)) {
			POSMessageDialog.showMessage(this, "Cannot edit Actual on an OPEN drawer — close the drawer first.");
			return;
		}
		final DrawerPullReport p = shiftsModel.getRows().get(row);
		final double accountable = p.getDrawerAccountable() == null ? 0.0 : p.getDrawerAccountable();
		final double currentActual = p.getCashToDeposit() == null ? 0.0 : p.getCashToDeposit();
		final String sym = CurrencyUtil.getCurrencySymbol();
		final DecimalFormat fmt = new DecimalFormat("#,##0.00");

		final JDialog dlg = new JDialog(this, "Edit Actual Amount", true);
		dlg.setLayout(new BorderLayout(0, 10));

		// Info header
		JPanel info = new JPanel(new MigLayout("ins 14, fillx, wrap 2", "[180!,right][grow,fill]", ""));
		info.add(new JLabel("Date:"));            info.add(new JLabel(p.getReportTime() == null ? "—"
				: DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(p.getReportTime())));
		info.add(new JLabel("Cashier:"));         info.add(new JLabel(p.getAssignedUser() == null ? "—" : p.getAssignedUser().getFullName()));
		info.add(new JLabel("Accountable:"));     info.add(new JLabel(sym + fmt.format(accountable)));
		dlg.add(info, BorderLayout.NORTH);

		// Amount field + keypad
		JPanel center = new JPanel(new MigLayout("ins 14, fillx, wrap 1", "[grow,fill]", "[]10[]14[]"));
		JLabel label = new JLabel("New Actual Amount (" + sym + "):");
		center.add(label, "growx");
		final JTextField tfAmount = new JTextField(currentActual == 0.0 ? "" : fmt.format(currentActual));
		tfAmount.setHorizontalAlignment(SwingConstants.RIGHT);
		tfAmount.setFont(tfAmount.getFont().deriveFont(java.awt.Font.BOLD, 22f));
		tfAmount.setPreferredSize(new Dimension(0, 48));
		center.add(tfAmount, "growx");

		com.floreantpos.swing.NumericKeypad keypad = new com.floreantpos.swing.NumericKeypad();
		center.add(keypad, "growx, hmin 220");
		dlg.add(center, BorderLayout.CENTER);

		// Buttons
		JPanel south = new JPanel(new MigLayout("ins 10 14 14 14, fillx", "push[140!]10[140!]", ""));
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setPreferredSize(new Dimension(140, 48));
		btnCancel.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { dlg.dispose(); }
		});
		JButton btnSave = new JButton("Save");
		btnSave.setPreferredSize(new Dimension(140, 48));
		btnSave.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				String txt = tfAmount.getText().trim().replace(",", "");
				double newActual;
				try {
					newActual = txt.isEmpty() ? 0.0 : Double.parseDouble(txt);
				} catch (NumberFormatException ex) {
					POSMessageDialog.showError(dlg, "Enter a valid amount.");
					return;
				}
				if (newActual < 0) {
					POSMessageDialog.showError(dlg, "Actual amount cannot be negative.");
					return;
				}
				p.setCashToDeposit(newActual);
				try {
					new DrawerPullReportDAO().saveOrUpdate(p);
				} catch (Exception ex) {
					POSMessageDialog.showError(dlg, "Save failed: " + ex.getMessage());
					return;
				}
				shiftsModel.fireTableRowsUpdated(0, shiftsModel.getRowCount() - 1);
				recomputeShiftsTotals();
				dlg.dispose();
			}
		});
		south.add(btnCancel);
		south.add(btnSave);
		dlg.add(south, BorderLayout.SOUTH);

		dlg.setSize(520, 560);
		dlg.setLocationRelativeTo(this);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() { tfAmount.requestFocusInWindow(); }
		});
		dlg.setVisible(true);
	}

	private static String buildSessionDetailHtml(DrawerPullReport r, boolean open) {
		DecimalFormat fmt = new DecimalFormat("#,##0.00");
		String sym = CurrencyUtil.getCurrencySymbol();
		StringBuilder s = new StringBuilder(2048);
		s.append("<html><body>");
		s.append("<div align='center'><b>DRAWER ").append(open ? "(OPEN)" : "PULL").append("</b></div>");
		s.append("<br/>");
		s.append("<div>Time: ").append(r.getReportTime() == null ? "—"
				: DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(r.getReportTime())).append("</div>");
		s.append("<div>Cashier: ").append(r.getAssignedUser() == null ? "—" : r.getAssignedUser().getFullName()).append("</div>");
		s.append("<hr/>");
		s.append("<table width='100%' cellspacing='3'>");
		s.append(row("Net Sales",          sym + fmt.format(nullSafe(r.getNetSales()))));
		s.append(row("Sales Tax",          sym + fmt.format(nullSafe(r.getSalesTax()))));
		s.append(row("Delivery Charge",    sym + fmt.format(nullSafe(r.getSalesDeliveryCharge()))));
		s.append(row("Total Revenue",      sym + fmt.format(nullSafe(r.getTotalRevenue()))));
		s.append(row("Charged Tips",       sym + fmt.format(nullSafe(r.getChargedTips()))));
		s.append(divider());
		s.append(row("Gross Receipts",     sym + fmt.format(nullSafe(r.getGrossReceipts()))));
		s.append("<tr><td colspan='2'>&nbsp;</td></tr>");
		s.append(row("Cash (" + nullSafeI(r.getCashReceiptCount()) + ")", sym + fmt.format(nullSafe(r.getCashReceiptAmount()))));
		s.append(row("Credit Cards (" + nullSafeI(r.getCreditCardReceiptCount()) + ")", sym + fmt.format(nullSafe(r.getCreditCardReceiptAmount()))));
		s.append(row("Debit Cards (" + nullSafeI(r.getDebitCardReceiptCount()) + ")", sym + fmt.format(nullSafe(r.getDebitCardReceiptAmount()))));
		s.append(row("Refunds (" + nullSafeI(r.getRefundReceiptCount()) + ")", sym + fmt.format(nullSafe(r.getRefundAmount()))));
		s.append(row("Tips Paid",          sym + fmt.format(nullSafe(r.getTipsPaid()))));
		s.append(row("Pay Out (" + nullSafeI(r.getPayOutCount()) + ")", sym + fmt.format(nullSafe(r.getPayOutAmount()))));
		s.append(row("Drawer Bleed (" + nullSafeI(r.getDrawerBleedCount()) + ")", sym + fmt.format(nullSafe(r.getDrawerBleedAmount()))));
		s.append(divider());
		s.append(row("Accountable",        sym + fmt.format(nullSafe(r.getDrawerAccountable()))));
		s.append(row("Cash To Deposit (Actual)", sym + fmt.format(nullSafe(r.getCashToDeposit()))));
		s.append("</table>");
		s.append("</body></html>");
		return s.toString();
	}

	private static double nullSafe(Double d)   { return d == null ? 0.0 : d; }
	private static int    nullSafeI(Integer i) { return i == null ? 0   : i; }

	// ─────────────────────────────────────────────────────────────────────
	//  Dashboard tab — visual overview with charts
	// ─────────────────────────────────────────────────────────────────────
	private JLabel       dashboardRevenue;
	private JLabel       dashboardCashInDrawer;
	private JLabel       dashboardTicketsCount;
	private JLabel       dashboardChargedTips;
	private TrendChartPanel    chartTrend;
	private HourlySalesChart   chartHourly;
	private PaymentPieChart    chartPie;
	private SalesSummaryVisual salesVisual;
	private ItemBarsPanel      itemBars;

	// Fixed Arial fonts used by every dashboard chart so they don't pick up
	// theme-scaled fonts.
	private static final java.awt.Font CHART_BODY  = new java.awt.Font("Arial", java.awt.Font.PLAIN, 12);
	private static final java.awt.Font CHART_BOLD  = new java.awt.Font("Arial", java.awt.Font.BOLD,  12);
	private static final java.awt.Font CHART_TITLE = new java.awt.Font("Arial", java.awt.Font.BOLD,  14);
	private static final java.awt.Color INK        = new java.awt.Color(0x1E, 0x2D, 0x3D);
	private static final java.awt.Color MUTED      = new java.awt.Color(0x6B, 0x72, 0x80);
	private static final java.awt.Color GRID       = new java.awt.Color(0xF1, 0xF5, 0xF9);
	private static final java.awt.Color CARD_BG    = java.awt.Color.WHITE;

	private JPanel buildDashboardPanel() {
		JPanel root = new JPanel(new MigLayout("ins 8, wrap 1, fill",
				"[grow,fill]",
				"[]12[grow,fill]12[grow,fill]"));

		// Row 1: KPI cards
		JPanel kpis = new JPanel(new MigLayout("ins 0, fillx",
				"[grow,sg c,fill]10[grow,sg c,fill]10[grow,sg c,fill]10[grow,sg c,fill]", ""));
		dashboardRevenue      = new JLabel("—", SwingConstants.CENTER);
		dashboardCashInDrawer = new JLabel("—", SwingConstants.CENTER);
		dashboardTicketsCount = new JLabel("—", SwingConstants.CENTER);
		dashboardChargedTips  = new JLabel("—", SwingConstants.CENTER);
		kpis.add(kpiCard("Today's Gross Revenue", dashboardRevenue),       "grow");
		kpis.add(kpiCard("Cash in Drawer", dashboardCashInDrawer),         "grow");
		kpis.add(kpiCard("Tickets", dashboardTicketsCount),                "grow");
		kpis.add(kpiCard("Charged Tips", dashboardChargedTips),            "grow");
		root.add(kpis, "growx");

		// Row 2: Trend chart + Hourly sales chart (side by side)
		JPanel row2 = new JPanel(new MigLayout("ins 0, fill",
				"[grow,sg c,fill]10[grow,sg c,fill]", ""));
		chartTrend = new TrendChartPanel();
		chartTrend.setBackground(CARD_BG);
		chartTrend.setBorder(BorderFactory.createTitledBorder("Daily Trend — Accountable vs Actual (This Month)"));
		chartHourly = new HourlySalesChart();
		chartHourly.setBackground(CARD_BG);
		chartHourly.setBorder(BorderFactory.createTitledBorder("Hourly Sales — Today"));
		row2.add(chartTrend,  "grow");
		row2.add(chartHourly, "grow");
		root.add(row2, "grow, hmin 200");

		// Row 3: Pie chart | Sales summary visual | Latest items bars
		JPanel row3 = new JPanel(new MigLayout("ins 0, fill",
				"[grow,sg c,fill]10[grow,sg c,fill]10[grow,sg c,fill]", ""));
		chartPie    = new PaymentPieChart();
		chartPie.setBackground(CARD_BG);
		chartPie.setBorder(BorderFactory.createTitledBorder("Card vs Cash (Today)"));
		salesVisual = new SalesSummaryVisual();
		salesVisual.setBackground(CARD_BG);
		salesVisual.setBorder(BorderFactory.createTitledBorder("Sales Summary — Today"));
		itemBars    = new ItemBarsPanel();
		itemBars.setBackground(CARD_BG);
		itemBars.setBorder(BorderFactory.createTitledBorder("Latest Items Sold"));
		row3.add(chartPie,    "grow");
		row3.add(salesVisual, "grow");
		row3.add(itemBars,    "grow");
		root.add(row3, "grow");

		refreshDashboard();
		return root;
	}

	private JPanel kpiCard(String title, JLabel valueLabel) {
		JPanel card = new JPanel(new MigLayout("ins 14, wrap 1, fillx, align center", "[grow,fill]", "[]8[]"));
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new java.awt.Color(0xDD, 0xE3, 0xEC)),
				BorderFactory.createEmptyBorder(4, 4, 4, 4)));
		JLabel lbl = new JLabel(title, SwingConstants.CENTER);
		lbl.setForeground(new java.awt.Color(0x6B, 0x72, 0x80));
		card.add(lbl, "growx");
		valueLabel.setFont(valueLabel.getFont().deriveFont(java.awt.Font.BOLD, 26f));
		card.add(valueLabel, "growx");
		return card;
	}

	private void refreshDashboard() {
		if (dashboardRevenue == null) return; // not built yet
		try {
			Terminal terminal = Application.getInstance().refreshAndGetTerminal();
			// Aggregate ALL drawer sessions for today: every closed DrawerPullReport
			// since 00:00 PLUS the currently-open drawer's running totals.
			DrawerPullReport aggregate = aggregateTodayReports();

			String sym = CurrencyUtil.getCurrencySymbol();
			DecimalFormat fmt = new DecimalFormat("#,##0.00");
			double revenue   = nullSafe(aggregate.getGrossReceipts());
			double cash      = nullSafe(aggregate.getDrawerAccountable());
			double tips      = nullSafe(aggregate.getChargedTips());
			int    tickets   = nullSafeI(aggregate.getCashReceiptCount())
					+ nullSafeI(aggregate.getCreditCardReceiptCount())
					+ nullSafeI(aggregate.getDebitCardReceiptCount());

			dashboardRevenue.setText(sym + fmt.format(revenue));
			dashboardCashInDrawer.setText(sym + fmt.format(cash));
			dashboardTicketsCount.setText(String.valueOf(tickets));
			dashboardChargedTips.setText(sym + fmt.format(tips));

			chartTrend.setData(loadMonthTrend());
			chartHourly.setData(loadTodayHourlySales());
			chartPie.setData(
					nullSafe(aggregate.getCashReceiptAmount()),
					nullSafe(aggregate.getCreditCardReceiptAmount()) + nullSafe(aggregate.getDebitCardReceiptAmount()));
			salesVisual.setReport(terminal, aggregate);
			itemBars.setItems(loadLatestItems(15));
		} catch (Exception ignored) {}
	}

	/**
	 * Sum every DrawerPullReport closed today plus the currently-open drawer.
	 * Mirrors the per-period aggregation used by Shifts & Cash Registers, but
	 * scoped to "today" (00:00 → now).
	 */
	private DrawerPullReport aggregateTodayReports() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		Date startOfDay = c.getTime();
		Date now        = new Date();

		DrawerPullReport agg = new DrawerPullReport();
		zeroInit(agg);

		try {
			java.util.List<DrawerPullReport> reports = new DrawerPullReportDAO().findReports(startOfDay, now);
			if (reports != null) {
				for (DrawerPullReport r : reports) addTo(agg, r);
			}
		} catch (Exception ignored) {}

		// Plus the currently-open drawer (its live, not-yet-saved totals)
		try {
			Terminal t = Application.getInstance().getTerminal();
			if (t != null && t.isCashDrawerAssigned()) {
				DrawerPullReport live = DrawerpullReportService.buildDrawerPullReport();
				addTo(agg, live);
			}
		} catch (Exception ignored) {}

		return agg;
	}

	private static void zeroInit(DrawerPullReport a) {
		a.setNetSales(0.0); a.setSalesTax(0.0); a.setSalesDeliveryCharge(0.0);
		a.setTotalRevenue(0.0); a.setChargedTips(0.0); a.setGrossReceipts(0.0);
		a.setCashReceiptCount(0); a.setCashReceiptAmount(0.0);
		a.setCreditCardReceiptCount(0); a.setCreditCardReceiptAmount(0.0);
		a.setDebitCardReceiptCount(0); a.setDebitCardReceiptAmount(0.0);
		a.setRefundReceiptCount(0); a.setRefundAmount(0.0);
		a.setTipsPaid(0.0); a.setPayOutCount(0); a.setPayOutAmount(0.0);
		a.setCashBack(0.0); a.setDrawerBleedCount(0); a.setDrawerBleedAmount(0.0);
		a.setDrawerAccountable(0.0); a.setCashToDeposit(0.0);
	}

	private static void addTo(DrawerPullReport a, DrawerPullReport r) {
		a.setNetSales(           sumD(a.getNetSales(),           r.getNetSales()));
		a.setSalesTax(           sumD(a.getSalesTax(),           r.getSalesTax()));
		a.setSalesDeliveryCharge(sumD(a.getSalesDeliveryCharge(),r.getSalesDeliveryCharge()));
		a.setTotalRevenue(       sumD(a.getTotalRevenue(),       r.getTotalRevenue()));
		a.setChargedTips(        sumD(a.getChargedTips(),        r.getChargedTips()));
		a.setGrossReceipts(      sumD(a.getGrossReceipts(),      r.getGrossReceipts()));
		a.setCashReceiptCount(   sumI(a.getCashReceiptCount(),   r.getCashReceiptCount()));
		a.setCashReceiptAmount(  sumD(a.getCashReceiptAmount(),  r.getCashReceiptAmount()));
		a.setCreditCardReceiptCount(  sumI(a.getCreditCardReceiptCount(),  r.getCreditCardReceiptCount()));
		a.setCreditCardReceiptAmount( sumD(a.getCreditCardReceiptAmount(), r.getCreditCardReceiptAmount()));
		a.setDebitCardReceiptCount(   sumI(a.getDebitCardReceiptCount(),   r.getDebitCardReceiptCount()));
		a.setDebitCardReceiptAmount(  sumD(a.getDebitCardReceiptAmount(),  r.getDebitCardReceiptAmount()));
		a.setRefundReceiptCount( sumI(a.getRefundReceiptCount(), r.getRefundReceiptCount()));
		a.setRefundAmount(       sumD(a.getRefundAmount(),       r.getRefundAmount()));
		a.setTipsPaid(           sumD(a.getTipsPaid(),           r.getTipsPaid()));
		a.setPayOutCount(        sumI(a.getPayOutCount(),        r.getPayOutCount()));
		a.setPayOutAmount(       sumD(a.getPayOutAmount(),       r.getPayOutAmount()));
		a.setCashBack(           sumD(a.getCashBack(),           r.getCashBack()));
		a.setDrawerBleedCount(   sumI(a.getDrawerBleedCount(),   r.getDrawerBleedCount()));
		a.setDrawerBleedAmount(  sumD(a.getDrawerBleedAmount(),  r.getDrawerBleedAmount()));
		a.setDrawerAccountable(  sumD(a.getDrawerAccountable(),  r.getDrawerAccountable()));
		a.setCashToDeposit(      sumD(a.getCashToDeposit(),      r.getCashToDeposit()));
	}

	private static Double  sumD(Double a, Double b) { return (a == null ? 0.0 : a) + (b == null ? 0.0 : b); }
	private static Integer sumI(Integer a, Integer b) { return (a == null ? 0 : a) + (b == null ? 0 : b); }

	/** Returns a list of [day-of-month, accountable, actual] for this month. */
	private java.util.List<double[]> loadMonthTrend() {
		java.util.List<double[]> series = new java.util.ArrayList<double[]>();
		try {
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.set(java.util.Calendar.DAY_OF_MONTH, 1);
			c.set(java.util.Calendar.HOUR_OF_DAY, 0);
			c.set(java.util.Calendar.MINUTE, 0);
			c.set(java.util.Calendar.SECOND, 0);
			c.set(java.util.Calendar.MILLISECOND, 0);
			Date start = c.getTime();
			Date end   = new Date();

			java.util.Map<Integer, double[]> byDay = new java.util.TreeMap<Integer, double[]>();
			java.util.List<DrawerPullReport> reports = new DrawerPullReportDAO().findReports(start, end);
			if (reports != null) {
				for (DrawerPullReport r : reports) {
					if (r.getReportTime() == null) continue;
					java.util.Calendar rc = java.util.Calendar.getInstance();
					rc.setTime(r.getReportTime());
					int day = rc.get(java.util.Calendar.DAY_OF_MONTH);
					double[] agg = byDay.get(day);
					if (agg == null) { agg = new double[]{0, 0}; byDay.put(day, agg); }
					agg[0] += nullSafe(r.getDrawerAccountable());
					agg[1] += nullSafe(r.getCashToDeposit());
				}
			}
			for (java.util.Map.Entry<Integer, double[]> e : byDay.entrySet()) {
				series.add(new double[]{ e.getKey(), e.getValue()[0], e.getValue()[1] });
			}
		} catch (Exception ignored) {}
		return series;
	}

	private static class ItemRow {
		final String time, name;
		final double total;
		final int qty;
		ItemRow(String time, String name, int qty, double total) {
			this.time = time; this.name = name; this.qty = qty; this.total = total;
		}
	}

	/** Hourly sales totals for today (24-element array, indexed by hour-of-day). */
	private double[] loadTodayHourlySales() {
		double[] hours = new double[24];
		try {
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.set(java.util.Calendar.HOUR_OF_DAY, 0);
			c.set(java.util.Calendar.MINUTE, 0);
			c.set(java.util.Calendar.SECOND, 0);
			c.set(java.util.Calendar.MILLISECOND, 0);
			Date startOfDay = c.getTime();

			org.hibernate.Session s = com.floreantpos.model.dao.TicketDAO.getInstance().createNewSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<com.floreantpos.model.Ticket> tickets = s
						.createCriteria(com.floreantpos.model.Ticket.class)
						.add(org.hibernate.criterion.Restrictions.ge("createDate", startOfDay))
						.list();
				if (tickets != null) {
					java.util.Calendar tc = java.util.Calendar.getInstance();
					for (com.floreantpos.model.Ticket t : tickets) {
						if (t.getCreateDate() == null) continue;
						tc.setTime(t.getCreateDate());
						int hr = tc.get(java.util.Calendar.HOUR_OF_DAY);
						double amt = t.getTotalAmount() == null ? 0.0 : t.getTotalAmount();
						hours[hr] += amt;
					}
				}
			} finally { s.close(); }
		} catch (Exception ignored) {}
		return hours;
	}

	private java.util.List<ItemRow> loadLatestItems(int limit) {
		java.util.List<ItemRow> out = new java.util.ArrayList<ItemRow>();
		try {
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.set(java.util.Calendar.HOUR_OF_DAY, 0);
			c.set(java.util.Calendar.MINUTE, 0);
			c.set(java.util.Calendar.SECOND, 0);
			c.set(java.util.Calendar.MILLISECOND, 0);
			Date startOfDay = c.getTime();

			org.hibernate.Session s = com.floreantpos.model.dao.TicketDAO.getInstance().createNewSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<com.floreantpos.model.TicketItem> items = s
						.createCriteria(com.floreantpos.model.TicketItem.class)
						.createAlias("ticket", "tk")
						.add(org.hibernate.criterion.Restrictions.ge("tk.createDate", startOfDay))
						.addOrder(org.hibernate.criterion.Order.desc("id"))
						.setMaxResults(limit)
						.list();
				java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("hh:mma");
				for (com.floreantpos.model.TicketItem ti : items) {
					Date created = ti.getTicket() == null ? null : ti.getTicket().getCreateDate();
					out.add(new ItemRow(
							created == null ? "—" : tf.format(created),
							ti.getName() == null ? "—" : ti.getName(),
							ti.getItemCount() == null ? 1 : ti.getItemCount(),
							ti.getTotalAmount() == null ? 0.0 : ti.getTotalAmount()
					));
				}
			} finally { s.close(); }
		} catch (Exception ignored) {}
		return out;
	}

	// ── Charts (custom-painted, no external lib) ──────────────────────────

	private static class TrendChartPanel extends JPanel {
		private java.util.List<double[]> data = new java.util.ArrayList<double[]>();
		TrendChartPanel() { setOpaque(true); setBackground(CARD_BG); }
		void setData(java.util.List<double[]> d) { this.data = d == null ? new java.util.ArrayList<double[]>() : d; repaint(); }
		@Override protected void paintComponent(java.awt.Graphics g) {
			super.paintComponent(g);
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth(), h = getHeight();
			int padL = 50, padR = 16, padT = 16, padB = 32;
			int pw = w - padL - padR, ph = h - padT - padB;
			if (pw <= 0 || ph <= 0) { g2.dispose(); return; }

			g2.setFont(CHART_BODY);
			g2.setColor(new java.awt.Color(0xDD, 0xE3, 0xEC));
			g2.drawLine(padL, padT, padL, padT + ph);
			g2.drawLine(padL, padT + ph, padL + pw, padT + ph);

			if (data.isEmpty()) {
				g2.setColor(MUTED);
				g2.drawString("No data for this month yet.", padL + 12, padT + ph / 2);
				g2.dispose(); return;
			}

			double max = 1.0;
			for (double[] row : data) {
				if (row[1] > max) max = row[1];
				if (row[2] > max) max = row[2];
			}
			java.text.DecimalFormat axFmt = new java.text.DecimalFormat("#,##0");
			for (int i = 0; i <= 4; i++) {
				int y = padT + ph - (int)(ph * i / 4.0);
				double v = max * i / 4.0;
				g2.setColor(MUTED);
				g2.drawString(axFmt.format(v), 4, y + 4);
				g2.setColor(GRID);
				g2.drawLine(padL + 1, y, padL + pw, y);
			}

			int n = data.size();
			double stepX = n == 1 ? pw : (pw / (double)(n - 1));
			java.awt.Color accBlue = new java.awt.Color(0x1A, 0x6E, 0xBD);
			java.awt.Color actGrn  = new java.awt.Color(0x16, 0xA3, 0x4A);
			drawLineSeries(g2, data, 1, accBlue, padL, padT, ph, max, stepX);
			drawLineSeries(g2, data, 2, actGrn,  padL, padT, ph, max, stepX);

			int xStep = Math.max(1, n / 6);
			g2.setColor(MUTED);
			for (int i = 0; i < n; i += xStep) {
				int day = (int) data.get(i)[0];
				int x = padL + (int)(i * stepX);
				g2.drawString(String.valueOf(day), x - 4, padT + ph + 16);
			}

			int legY = padT + 4;
			g2.setColor(accBlue); g2.fillRect(padL + pw - 200, legY, 12, 12);
			g2.setColor(INK);     g2.drawString("Accountable", padL + pw - 184, legY + 11);
			g2.setColor(actGrn);  g2.fillRect(padL + pw - 100, legY, 12, 12);
			g2.setColor(INK);     g2.drawString("Actual", padL + pw - 84, legY + 11);
			g2.dispose();
		}

		private static void drawLineSeries(java.awt.Graphics2D g2, java.util.List<double[]> data,
		                                   int idx, java.awt.Color color, int padL, int padT, int ph,
		                                   double max, double stepX) {
			g2.setColor(color);
			g2.setStroke(new java.awt.BasicStroke(2.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
			int prevX = 0, prevY = 0;
			for (int i = 0; i < data.size(); i++) {
				double v = data.get(i)[idx];
				int x = padL + (int)(i * stepX);
				int y = padT + ph - (int)(ph * v / max);
				if (i > 0) g2.drawLine(prevX, prevY, x, y);
				g2.fillOval(x - 3, y - 3, 6, 6);
				prevX = x; prevY = y;
			}
		}
	}

	private static class HourlySalesChart extends JPanel {
		private double[] hours = new double[24];
		HourlySalesChart() { setOpaque(true); setBackground(CARD_BG); }
		void setData(double[] hrs) { this.hours = hrs == null ? new double[24] : hrs; repaint(); }
		@Override protected void paintComponent(java.awt.Graphics g) {
			super.paintComponent(g);
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setFont(CHART_BODY);
			int w = getWidth(), h = getHeight();
			int padL = 50, padR = 14, padT = 16, padB = 32;
			int pw = w - padL - padR, ph = h - padT - padB;
			if (pw <= 0 || ph <= 0) { g2.dispose(); return; }

			g2.setColor(new java.awt.Color(0xDD, 0xE3, 0xEC));
			g2.drawLine(padL, padT, padL, padT + ph);
			g2.drawLine(padL, padT + ph, padL + pw, padT + ph);

			double max = 1.0;
			for (double v : hours) if (v > max) max = v;
			java.text.DecimalFormat axFmt = new java.text.DecimalFormat("#,##0");
			for (int i = 0; i <= 4; i++) {
				int y = padT + ph - (int)(ph * i / 4.0);
				double v = max * i / 4.0;
				g2.setColor(MUTED); g2.drawString(axFmt.format(v), 4, y + 4);
				g2.setColor(GRID);  g2.drawLine(padL + 1, y, padL + pw, y);
			}

			int n = 24;
			double slot = pw / (double) n;
			java.awt.Color bar = new java.awt.Color(0x1A, 0x6E, 0xBD);
			for (int i = 0; i < n; i++) {
				int barW = (int) Math.max(2, slot * 0.7);
				int barX = padL + (int)(slot * i) + (int)((slot - barW) / 2);
				int barH = (int) Math.round(ph * hours[i] / max);
				int barY = padT + ph - barH;
				g2.setColor(bar);
				g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
			}

			g2.setColor(MUTED);
			for (int i = 0; i < n; i += 3) {
				int x = padL + (int)(slot * i + slot / 2);
				String label = (i == 0 ? "12a" : i < 12 ? i + "a" : i == 12 ? "12p" : (i - 12) + "p");
				g2.drawString(label, x - g2.getFontMetrics().stringWidth(label) / 2, padT + ph + 16);
			}

			g2.dispose();
		}
	}

	private static class PaymentPieChart extends JPanel {
		private double cash, card;
		PaymentPieChart() { setOpaque(true); setBackground(CARD_BG); }
		void setData(double cash, double card) { this.cash = cash; this.card = card; repaint(); }
		@Override protected void paintComponent(java.awt.Graphics g) {
			super.paintComponent(g);
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setFont(CHART_BODY);
			int w = getWidth(), h = getHeight();
			int sz = Math.min(w, h) - 70;
			if (sz < 20) sz = 20;
			int cx = (w - sz) / 2;
			int cy = 28;
			double total = cash + card;
			java.awt.Color cashColor = new java.awt.Color(0x16, 0xA3, 0x4A);
			java.awt.Color cardColor = new java.awt.Color(0x1A, 0x6E, 0xBD);
			if (total <= 0) {
				g2.setColor(new java.awt.Color(0xE5, 0xE7, 0xEB));
				g2.fillOval(cx, cy, sz, sz);
				g2.setColor(MUTED);
				g2.drawString("No payments yet", cx + sz/2 - 50, cy + sz/2);
			} else {
				int cashAng = (int) Math.round(360.0 * cash / total);
				g2.setColor(cashColor);
				g2.fillArc(cx, cy, sz, sz, 90, -cashAng);
				g2.setColor(cardColor);
				g2.fillArc(cx, cy, sz, sz, 90 - cashAng, -(360 - cashAng));
				g2.setColor(CARD_BG);
				g2.fillOval(cx + sz/4, cy + sz/4, sz/2, sz/2);
			}
			java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0.00");
			String sym = CurrencyUtil.getCurrencySymbol();
			int legY = cy + sz + 16;
			g2.setColor(cashColor); g2.fillRect(12, legY, 14, 14);
			g2.setColor(INK);       g2.drawString("Cash:  " + sym + fmt.format(cash), 32, legY + 12);
			g2.setColor(cardColor); g2.fillRect(12, legY + 22, 14, 14);
			g2.setColor(INK);       g2.drawString("Card:  " + sym + fmt.format(card), 32, legY + 34);
			g2.dispose();
		}
	}

	private static class SalesSummaryVisual extends JPanel {
		private Terminal terminal;
		private DrawerPullReport report;
		SalesSummaryVisual() { setOpaque(true); setBackground(CARD_BG); }
		void setReport(Terminal t, DrawerPullReport r) { this.terminal = t; this.report = r; repaint(); }
		@Override protected void paintComponent(java.awt.Graphics g) {
			super.paintComponent(g);
			if (report == null) return;
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			String sym = CurrencyUtil.getCurrencySymbol();
			java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0.00");
			double net    = nullSafe(report.getNetSales());
			double tax    = nullSafe(report.getSalesTax());
			double tip    = nullSafe(report.getChargedTips());
			double gross  = nullSafe(report.getGrossReceipts());
			double total  = Math.max(1.0, net + tax + tip);

			int x = 14, y = 30, w = getWidth() - 28;
			g2.setColor(INK);
			g2.setFont(CHART_TITLE);
			g2.drawString("Today: " + sym + fmt.format(gross), x, y);
			y += 24;

			g2.setFont(CHART_BODY);
			y = drawStackBar(g2, "Net Sales", net, total, x, y, w, new java.awt.Color(0x1A, 0x6E, 0xBD), sym, fmt);
			y = drawStackBar(g2, "Sales Tax", tax, total, x, y, w, new java.awt.Color(0xEA, 0xB3, 0x08), sym, fmt);
			y = drawStackBar(g2, "Tips",      tip, total, x, y, w, new java.awt.Color(0x16, 0xA3, 0x4A), sym, fmt);
			y += 8;
			g2.setColor(MUTED);
			g2.drawString("Drawer Accountable: " + sym + fmt.format(nullSafe(report.getDrawerAccountable())), x, y); y += 16;
			g2.drawString("Cash To Deposit:    " + sym + fmt.format(nullSafe(report.getCashToDeposit())), x, y);
			g2.dispose();
		}
		private static int drawStackBar(java.awt.Graphics2D g2, String label, double value, double total,
		                                int x, int y, int w, java.awt.Color color, String sym,
		                                java.text.DecimalFormat fmt) {
			int barH = 18;
			g2.setColor(GRID);
			g2.fillRoundRect(x, y, w, barH, 6, 6);
			int filled = (int) Math.round(w * Math.min(1.0, value / total));
			g2.setColor(color);
			g2.fillRoundRect(x, y, filled, barH, 6, 6);
			g2.setColor(INK);
			g2.drawString(label + ":  " + sym + fmt.format(value), x + 6, y + barH - 5);
			return y + barH + 6;
		}
	}

	private static class ItemBarsPanel extends JPanel {
		private java.util.List<ItemRow> items = new java.util.ArrayList<ItemRow>();
		private static final java.awt.Color[] PALETTE = {
				new java.awt.Color(0x1A, 0x6E, 0xBD), new java.awt.Color(0x16, 0xA3, 0x4A),
				new java.awt.Color(0xEA, 0xB3, 0x08), new java.awt.Color(0xEF, 0x44, 0x44),
				new java.awt.Color(0x8B, 0x5C, 0xF6), new java.awt.Color(0x06, 0xB6, 0xD4),
				new java.awt.Color(0xF9, 0x73, 0x16), new java.awt.Color(0xEC, 0x48, 0x99),
		};
		ItemBarsPanel() { setOpaque(true); setBackground(CARD_BG); }
		void setItems(java.util.List<ItemRow> rows) { this.items = rows == null ? new java.util.ArrayList<ItemRow>() : rows; revalidate(); repaint(); }
		@Override public Dimension getPreferredSize() {
			return new Dimension(260, Math.max(120, items.size() * 30 + 24));
		}
		@Override protected void paintComponent(java.awt.Graphics g) {
			super.paintComponent(g);
			java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
			g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setFont(CHART_BODY);
			int x = 14, y = 26, w = getWidth() - 28;
			if (items.isEmpty()) {
				g2.setColor(MUTED);
				g2.drawString("No items sold today yet.", x, y);
				g2.dispose(); return;
			}
			java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0.00");
			String sym = CurrencyUtil.getCurrencySymbol();
			for (int i = 0; i < items.size(); i++) {
				ItemRow it = items.get(i);
				java.awt.Color c = PALETTE[Math.abs(it.name.hashCode()) % PALETTE.length];
				g2.setColor(c);
				g2.fillRoundRect(x, y, w, 26, 8, 8);
				g2.setColor(java.awt.Color.WHITE);
				g2.setFont(CHART_BOLD);
				String label = it.time + "  " + (it.qty > 1 ? it.qty + "× " : "") + it.name;
				if (label.length() > 28) label = label.substring(0, 26) + "…";
				g2.drawString(label, x + 10, y + 18);
				String amt = sym + fmt.format(it.total);
				int amtW = g2.getFontMetrics().stringWidth(amt);
				g2.drawString(amt, x + w - amtW - 10, y + 18);
				y += 30;
			}
			g2.dispose();
		}
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Card Transactions tab — embeds the AuthorizableTicketBrowser content
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildCardTransactionsPanel() {
		JPanel root = new JPanel(new BorderLayout(0, 10));
		root.add(new JLabel("Card Transactions"), BorderLayout.NORTH);

		// Construct a hidden AuthorizableTicketBrowser solely to harvest its
		// content pane (the tabs + buttons). Reparent the content into our tab.
		try {
			com.floreantpos.ui.views.payment.AuthorizableTicketBrowser browser =
					new com.floreantpos.ui.views.payment.AuthorizableTicketBrowser(Application.getPosWindow());
			java.awt.Container content = browser.getContentPane();
			browser.setContentPane(new JPanel()); // detach so disposal doesn't drag UI
			browser.dispose();
			// Remove any "Close" button that the original browser added — it would
			// just close the dialog from within the embedded panel which is confusing.
			removeCloseButtonsDeep(content);
			if (content instanceof JComponent) {
				root.add((JComponent) content, BorderLayout.CENTER);
			} else {
				JPanel wrap = new JPanel(new BorderLayout());
				wrap.add(content, BorderLayout.CENTER);
				root.add(wrap, BorderLayout.CENTER);
			}
		} catch (Exception ex) {
			JLabel err = new JLabel("<html><div style='padding:20px;color:#b91c1c'>" +
					"Unable to load card transactions: " + ex.getMessage() + "</div></html>");
			root.add(err, BorderLayout.CENTER);
		}
		return root;
	}

	/** Recursively strip JButtons labelled "Close" / localised equivalent. */
	private static void removeCloseButtonsDeep(java.awt.Container c) {
		if (c == null) return;
		java.util.List<java.awt.Component> toRemove = new java.util.ArrayList<java.awt.Component>();
		for (java.awt.Component child : c.getComponents()) {
			if (child instanceof JButton) {
				String txt = ((JButton) child).getText();
				if (txt != null && (txt.equalsIgnoreCase("close") || txt.equalsIgnoreCase("close window"))) {
					toRemove.add(child);
				}
			} else if (child instanceof java.awt.Container) {
				removeCloseButtonsDeep((java.awt.Container) child);
			}
		}
		for (java.awt.Component child : toRemove) c.remove(child);
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Tips Payment tab — embedded criteria + inline report
	// ─────────────────────────────────────────────────────────────────────
	private org.jdesktop.swingx.JXDatePicker tipsFromPicker;
	private org.jdesktop.swingx.JXDatePicker tipsToPicker;
	private JComboBox tipsUserCombo;
	private JEditorPane tipsReportPane;
	private JTable      tipsReportTable;
	private JPanel      tipsReportArea;

	private JPanel buildTipsPaymentPanel() {
		JPanel root = new JPanel(new BorderLayout(0, 10));

		// Criteria row — all fields and buttons share the same fixed height
		final int CTRL_H = 34;
		JPanel criteria = new JPanel(new MigLayout(
				"ins 0, fillx, novisualpadding",
				"[60!,right][160!]20[60!,right][160!]20[80!,right][200!,grow]20[80!]10[120!]",
				""));
		tipsFromPicker = com.floreantpos.ui.util.UiUtil.getCurrentMonthStart();
		tipsToPicker   = com.floreantpos.ui.util.UiUtil.getCurrentMonthEnd();
		tipsFromPicker.setPreferredSize(new Dimension(160, CTRL_H));
		tipsToPicker.setPreferredSize(new Dimension(160, CTRL_H));

		tipsUserCombo  = new JComboBox();
		tipsUserCombo.setPreferredSize(new Dimension(200, CTRL_H));
		try {
			java.util.List<com.floreantpos.model.User> users =
					com.floreantpos.model.dao.UserDAO.getInstance().findAll();
			tipsUserCombo.setModel(new com.floreantpos.swing.ListComboBoxModel(users));
		} catch (Exception ignored) {}

		JButton btnLoad = new JButton("OK");
		btnLoad.setPreferredSize(new Dimension(80, CTRL_H));
		btnLoad.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { loadTipsReport(); }
		});

		JButton btnPayTips = new JButton("Pay Tips");
		btnPayTips.setPreferredSize(new Dimension(120, CTRL_H));
		btnPayTips.setBackground(new java.awt.Color(0x16, 0xA3, 0x4A));
		btnPayTips.setForeground(java.awt.Color.WHITE);
		btnPayTips.setOpaque(true);
		btnPayTips.setBorderPainted(false);
		btnPayTips.setFocusPainted(false);
		btnPayTips.setFont(btnPayTips.getFont().deriveFont(java.awt.Font.BOLD));
		btnPayTips.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { payTipsForSelectedServer(); }
		});

		String cell = "h " + CTRL_H + "!, growx";
		criteria.add(new JLabel("From:"),   "h " + CTRL_H + "!, alignx right");
		criteria.add(tipsFromPicker,        cell);
		criteria.add(new JLabel("To:"),     "h " + CTRL_H + "!, alignx right");
		criteria.add(tipsToPicker,          cell);
		criteria.add(new JLabel("Server:"), "h " + CTRL_H + "!, alignx right");
		criteria.add(tipsUserCombo,         cell);
		criteria.add(btnLoad,               "h " + CTRL_H + "!, growx");
		criteria.add(btnPayTips,            "h " + CTRL_H + "!, growx");

		root.add(criteria, BorderLayout.NORTH);

		// Report area — summary HTML at top, detail table below
		tipsReportArea = new JPanel(new BorderLayout(0, 10));
		tipsReportPane = new JEditorPane();
		tipsReportPane.setContentType("text/html");
		tipsReportPane.setEditable(false);
		tipsReportPane.setText("<html><body><div style='padding:30px;color:#6B7280'>" +
				"Pick a server and date range, then press <b>OK</b> to load the tips cashout report." +
				"</div></body></html>");

		JScrollPane summaryScroll = new JScrollPane(tipsReportPane);
		summaryScroll.setPreferredSize(new Dimension(0, 200));
		tipsReportArea.add(summaryScroll, BorderLayout.NORTH);

		tipsReportTable = new JTable();
		JScrollPane tableScroll = new JScrollPane(tipsReportTable);
		tipsReportArea.add(tableScroll, BorderLayout.CENTER);

		root.add(tipsReportArea, BorderLayout.CENTER);
		return root;
	}

	private void loadTipsReport() {
		try {
			Object selUser = tipsUserCombo == null ? null : tipsUserCombo.getSelectedItem();
			if (!(selUser instanceof com.floreantpos.model.User)) {
				POSMessageDialog.showError(this, "Please select a server.");
				return;
			}
			Date from = tipsFromPicker.getDate();
			Date to   = tipsToPicker.getDate();
			if (from == null || to == null) {
				POSMessageDialog.showError(this, "Please select both From and To dates.");
				return;
			}
			com.floreantpos.model.User user = (com.floreantpos.model.User) selUser;
			com.floreantpos.model.dao.GratuityDAO dao = new com.floreantpos.model.dao.GratuityDAO();
			com.floreantpos.model.TipsCashoutReport report = dao.createReport(from, to, user);

			tipsReportPane.setText(buildTipsReportHtml(report));
			tipsReportPane.setCaretPosition(0);
			tipsReportTable.setModel(new com.floreantpos.model.TipsCashoutReportTableModel(report.getDatas()));
		} catch (Exception ex) {
			POSMessageDialog.showError(this, ex.getMessage());
		}
	}

	private static String buildTipsReportHtml(com.floreantpos.model.TipsCashoutReport report) {
		String sym = CurrencyUtil.getCurrencySymbol();
		StringBuilder s = new StringBuilder(1024);
		s.append("<html><body>");
		s.append("<h2 style='margin:0 0 8px 0'>Server Tips Report</h2>");
		s.append("<table cellpadding='2' cellspacing='2'>");
		s.append(tipRow("Server",        report.getServer() == null ? "—" : report.getServer().toString()));
		s.append(tipRow("From",          report.getFromDate()   == null ? "—" : Application.formatDate(report.getFromDate())));
		s.append(tipRow("To",            report.getToDate()     == null ? "—" : Application.formatDate(report.getToDate())));
		s.append(tipRow("Report Time",   report.getReportTime() == null ? "—" : Application.formatDate(report.getReportTime())));
		s.append(tipRow("Orders",        report.getDatas() == null ? "0" : String.valueOf(report.getDatas().size())));
		s.append(tipRow("Cash Tips",     sym + com.floreantpos.util.NumberUtil.formatNumber(report.getCashTipsAmount())));
		s.append(tipRow("Charged Tips",  sym + com.floreantpos.util.NumberUtil.formatNumber(report.getChargedTipsAmount())));
		s.append(tipRow("Tips Due",      sym + com.floreantpos.util.NumberUtil.formatNumber(report.getTipsDue())));
		s.append("</table></body></html>");
		return s.toString();
	}

	private static String tipRow(String label, String value) {
		return "<tr><td><b>" + label + "</b></td><td>:&nbsp;" + value + "</td></tr>";
	}

	private void payTipsForSelectedServer() {
		Object selUser = tipsUserCombo == null ? null : tipsUserCombo.getSelectedItem();
		if (!(selUser instanceof com.floreantpos.model.User)) {
			POSMessageDialog.showError(this, "Please select a server.");
			return;
		}
		com.floreantpos.model.User user = (com.floreantpos.model.User) selUser;
		try {
			com.floreantpos.model.dao.GratuityDAO dao = new com.floreantpos.model.dao.GratuityDAO();
			java.util.List<com.floreantpos.model.Gratuity> unpaid = dao.findByUser(user);
			if (unpaid == null || unpaid.isEmpty()) {
				POSMessageDialog.showMessage(this, "No unpaid tips for " + user.getFullName() + ".");
				return;
			}
			double total = 0.0;
			for (com.floreantpos.model.Gratuity g : unpaid) total += g.getAmount();

			String sym = CurrencyUtil.getCurrencySymbol();
			int confirm = POSMessageDialog.showYesNoQuestionDialog(this,
					"Pay all unpaid tips for " + user.getFullName() + "?\n"
							+ "Tips count: " + unpaid.size() + "\n"
							+ "Total: " + sym + com.floreantpos.util.NumberUtil.formatNumber(total),
					"Pay Tips");
			if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

			dao.payGratuities(unpaid);
			POSMessageDialog.showMessage(this,
					"Paid " + unpaid.size() + " tip(s) totalling " + sym + com.floreantpos.util.NumberUtil.formatNumber(total)
					+ " to " + user.getFullName() + ".");
			loadTipsReport(); // refresh the displayed report
		} catch (Exception ex) {
			POSMessageDialog.showError(this, "Failed to pay tips: " + ex.getMessage());
		}
	}

	private void recomputeShiftsTotals() {
		if (shiftsTotalAccountable == null || shiftsTotalActual == null || shiftsModel == null) return;
		double accountable = 0.0;
		double actual      = 0.0;
		for (DrawerPullReport r : shiftsModel.getRows()) {
			accountable += r.getDrawerAccountable() == null ? 0.0 : r.getDrawerAccountable();
			actual      += r.getCashToDeposit()     == null ? 0.0 : r.getCashToDeposit();
		}
		DecimalFormat fmt = new DecimalFormat("#,##0.00");
		shiftsTotalAccountable.setText(fmt.format(accountable));
		shiftsTotalActual.setText(fmt.format(actual));
	}

	/**
	 * Table model backing the Shifts & Cash Registers grid. Each row wraps a
	 * single DrawerPullReport. The "Actual" column is editable on closed rows;
	 * commits write back to DrawerPullReport.cashToDeposit via the DAO.
	 *
	 * If the terminal currently has an open drawer, row 0 represents that
	 * live session (status = "Open", actual cell read-only).
	 */
	private static class SessionsTableModel extends AbstractTableModel {
		static final int COL_STATUS      = 0;
		static final int COL_DATE        = 1;
		static final int COL_SHIFT       = 2;
		static final int COL_OPENER      = 3;
		static final int COL_ACCOUNTABLE = 4;
		static final int COL_ACTUAL      = 5;

		private final String[] cols = { "S", "Date", "Shift", "Opener", "Tot. Accountable", "Actual" };
		private java.util.List<DrawerPullReport>  rows       = new java.util.ArrayList<DrawerPullReport>();
		private java.util.List<String>            shiftNames = new java.util.ArrayList<String>();
		private java.util.Set<Integer>            openIdx    = new java.util.HashSet<Integer>();
		private final DecimalFormat amtFmt  = new DecimalFormat("#,##0.00");
		private final java.text.SimpleDateFormat dateFmt = new java.text.SimpleDateFormat("MM/dd hh:mma");

		void setRows(java.util.List<DrawerPullReport> rows,
		             java.util.List<String> shifts,
		             java.util.Set<Integer> openIdx) {
			this.rows       = rows       == null ? new java.util.ArrayList<DrawerPullReport>() : rows;
			this.shiftNames = shifts     == null ? new java.util.ArrayList<String>() : shifts;
			this.openIdx    = openIdx    == null ? new java.util.HashSet<Integer>() : openIdx;
			fireTableDataChanged();
		}
		java.util.List<DrawerPullReport> getRows() { return rows; }
		boolean isOpenRow(int r) { return openIdx.contains(r); }

		@Override public int getRowCount()                { return rows.size(); }
		@Override public int getColumnCount()             { return cols.length; }
		@Override public String getColumnName(int c)      { return cols[c]; }
		@Override public boolean isCellEditable(int r, int c) {
			// Only the Actual column of CLOSED rows is editable
			return c == COL_ACTUAL && !isOpenRow(r);
		}

		@Override public Object getValueAt(int r, int c) {
			DrawerPullReport p = rows.get(r);
			boolean open = isOpenRow(r);
			double accountable = p.getDrawerAccountable() == null ? 0.0 : p.getDrawerAccountable();
			double actual      = p.getCashToDeposit()     == null ? 0.0 : p.getCashToDeposit();
			switch (c) {
				case COL_STATUS:
					if (open)                                   return "Open";
					if (accountable == 0.0)                     return "✓";
					if (Math.abs(actual - accountable) < 0.005) return "✓";
					return "–";
				case COL_DATE:
					return p.getReportTime() == null ? "—" : dateFmt.format(p.getReportTime());
				case COL_SHIFT:
					return (r < shiftNames.size() && shiftNames.get(r) != null) ? shiftNames.get(r) : "";
				case COL_OPENER:
					return p.getAssignedUser() == null ? "—" : p.getAssignedUser().getFullName();
				case COL_ACCOUNTABLE:
					return amtFmt.format(accountable);
				case COL_ACTUAL:
					if (open) return "—";
					return actual == 0.0 ? "" : amtFmt.format(actual);
			}
			return null;
		}

		@Override public void setValueAt(Object value, int r, int c) {
			if (c != COL_ACTUAL) return;
			if (isOpenRow(r)) return; // safety
			DrawerPullReport p = rows.get(r);
			Double newActual = parseAmount(value == null ? "" : value.toString());
			if (newActual == null) {
				JOptionPane.showMessageDialog(null, "Enter a valid amount.");
				return;
			}
			p.setCashToDeposit(newActual);
			try {
				new DrawerPullReportDAO().saveOrUpdate(p);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "Save failed: " + ex.getMessage());
				return;
			}
			fireTableRowsUpdated(r, r);
		}

		private static Double parseAmount(String s) {
			s = s.trim().replace(",", "");
			if (s.isEmpty()) return 0.0;
			try { return Double.valueOf(s); } catch (NumberFormatException ex) { return null; }
		}
	}

	private void refreshDrawerStatus() {
		try {
			Terminal terminal = Application.getInstance().refreshAndGetTerminal();
			boolean open = terminal != null && terminal.isCashDrawerAssigned();

			if (open) {
				DrawerPullReport r = DrawerpullReportService.buildDrawerPullReport();
				r.setAssignedUser(terminal.getAssignedUser());

				String shiftLabel = terminal.getProperty(Terminal.PROP_DRAWER_SHIFT_NAME);
				if (drawerHeaderShift != null) {
					drawerHeaderShift.setText(shiftLabel == null || shiftLabel.isEmpty()
							? "" : "Drawer Shift: " + shiftLabel);
				}
				drawerReportPane.setText(buildReportHtml(terminal, r));
				drawerReportPane.setCaretPosition(0);

				// Button state for OPEN drawer
				if (btnDrawerPrimary != null) {
					btnDrawerPrimary.setText("Close drawer");
				}
				if (btnDrawerBleed  != null) btnDrawerBleed.setVisible(true);
				if (btnDrawerPayout != null) btnDrawerPayout.setVisible(true);
				if (btnDrawerPrint  != null) btnDrawerPrint.setVisible(true);
			} else {
				if (drawerHeaderShift != null) drawerHeaderShift.setText("");
				drawerReportPane.setText(
						"<html><body>" +
						"<div style='text-align:center;padding:60px 20px;'>" +
						"<h2>Drawer is Closed</h2>" +
						"<p>Open the drawer to begin a session.</p>" +
						"</div></body></html>");
				drawerReportPane.setCaretPosition(0);

				// Button state for CLOSED drawer — only "Open Drawer" visible
				if (btnDrawerPrimary != null) {
					btnDrawerPrimary.setText("Open Drawer");
				}
				if (btnDrawerBleed  != null) btnDrawerBleed.setVisible(false);
				if (btnDrawerPayout != null) btnDrawerPayout.setVisible(false);
				if (btnDrawerPrint  != null) btnDrawerPrint.setVisible(false);
			}
		} catch (Exception ex) {
			if (drawerReportPane != null) {
				drawerReportPane.setText("Unable to load drawer status: " + ex.getMessage());
			}
		}
	}

	private static String buildReportHtml(Terminal terminal, DrawerPullReport r) {
		DecimalFormat fmt = new DecimalFormat("#,##0.00");
		String sym = CurrencyUtil.getCurrencySymbol();
		StringBuilder s = new StringBuilder(2048);
		s.append("<html><body>");
		s.append("<div align='center'>====================================<br/>");
		s.append("<b>DRAWER PULL</b><br/>");
		s.append("TERMINAL #: ").append(terminal == null ? "—" : terminal.getName()).append("<br/>");
		s.append("====================================");
		s.append("</div><br/>");
		s.append("<div>Time: ").append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date())).append("</div>");

		s.append(section("SALES BALANCE"));
		s.append(table(
				row("NET SALES",         sym + fmt.format(r.getNetSales())),
				row("+SALES TAX",        sym + fmt.format(r.getSalesTax())),
				row("+DELIVERY CHARGE",  sym + fmt.format(r.getSalesDeliveryCharge() == null ? 0.0 : r.getSalesDeliveryCharge())),
				row("=TOTAL REVENUES",   sym + fmt.format(r.getTotalRevenue())),
				row("+CHARGED TIPS",     sym + fmt.format(r.getChargedTips())),
				divider(),
				row("=GROSS RECEIPTS",   sym + fmt.format(r.getGrossReceipts()))
		));

		s.append("<br/>");
		s.append(table(
				row("-CASH RECEIPTS (" + r.getCashReceiptCount() + ")", sym + fmt.format(r.getCashReceiptAmount())),
				row("-CREDIT CARDS ("  + r.getCreditCardReceiptCount() + ")", sym + fmt.format(r.getCreditCardReceiptAmount())),
				row("-DEBIT CARDS ("   + r.getDebitCardReceiptCount()  + ")", sym + fmt.format(r.getDebitCardReceiptAmount())),
				row("-REFUND ("        + r.getRefundReceiptCount()     + ")", sym + fmt.format(r.getRefundAmount()))
		));

		s.append(section("CASH BREAKDOWN"));
		s.append(table(
				row("CASH SALES (" + r.getCashReceiptCount() + ")", sym + fmt.format(r.getCashReceiptAmount())),
				row("-TIPS PAID",        sym + fmt.format(r.getTipsPaid())),
				row("-PAY OUT (" + r.getPayOutCount() + ")",       sym + fmt.format(r.getPayOutAmount())),
				row("-CASH BACK",        sym + fmt.format(r.getCashBack())),
				row("+OPENING BALANCE",  sym + fmt.format(terminal == null || terminal.getOpeningBalance() == null ? 0.0 : terminal.getOpeningBalance())),
				row("-DRAWER BLEED (" + r.getDrawerBleedCount() + ")", sym + fmt.format(r.getDrawerBleedAmount())),
				divider(),
				row("=DRAWER ACCOUNTABLE", sym + fmt.format(r.getDrawerAccountable())),
				row(">CASH TO DEPOSIT",    sym + fmt.format(r.getCashToDeposit()))
		));

		s.append("</body></html>");
		return s.toString();
	}

	private static String section(String title) {
		return "<div align='center'><b>" + title + "</b></div><hr/>";
	}

	private static String table(String... rows) {
		StringBuilder sb = new StringBuilder("<table width='100%' cellspacing='2'>");
		for (String r : rows) sb.append(r);
		sb.append("</table>");
		return sb.toString();
	}

	private static String row(String left, String right) {
		return "<tr><td>" + left + "</td><td align='right'>" + right + "</td></tr>";
	}

	private static String divider() {
		return "<tr><td colspan='2'><hr/></td></tr>";
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			Dimension parent = Application.getPosWindow().getSize();
			int w = Math.max(700, (int) (parent.width  * 0.80));
			int h = Math.max(500, (int) (parent.height * 0.80));
			setSize(w, h);
			setLocationRelativeTo(Application.getPosWindow());
			startAutoRefresh();
			// Force an immediate refresh once the layout settles so the grid
			// is never blank on first open.
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					try { refreshShiftsGrid();   } catch (Exception ignored) {}
					try { refreshDrawerStatus(); } catch (Exception ignored) {}
					try { refreshDashboard();    } catch (Exception ignored) {}
				}
			});
		} else {
			stopAutoRefresh();
		}
		super.setVisible(b);
	}

	@Override
	public void dispose() {
		stopAutoRefresh();
		super.dispose();
	}

	/** Refresh the Shifts grid every 2 minutes while the dialog is up. */
	private void startAutoRefresh() {
		if (autoRefreshTimer != null) return;
		autoRefreshTimer = new Timer(120_000, new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try { refreshShiftsGrid(); } catch (Exception ignored) {}
				try { refreshDashboard();  } catch (Exception ignored) {}
			}
		});
		autoRefreshTimer.setRepeats(true);
		autoRefreshTimer.start();
	}

	private void stopAutoRefresh() {
		if (autoRefreshTimer != null) {
			autoRefreshTimer.stop();
			autoRefreshTimer = null;
		}
	}
}
