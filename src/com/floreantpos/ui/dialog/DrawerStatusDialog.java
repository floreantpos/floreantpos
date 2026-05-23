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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.floreantpos.PosException;
import com.floreantpos.main.Application;
import com.floreantpos.model.DrawerAssignedHistory;
import com.floreantpos.model.DrawerPullReport;
import com.floreantpos.model.Terminal;
import com.floreantpos.model.User;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.dao.TerminalDAO;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.print.DrawerpullReportService;
import com.floreantpos.print.PosPrintService;
import com.floreantpos.swing.NumericKeypad;
import com.floreantpos.util.CurrencyUtil;
import com.floreantpos.util.POSUtil;
import com.floreantpos.util.ShiftUtil;
import com.floreantpos.model.Shift;

import net.miginfocom.swing.MigLayout;

/**
 * Drawer Status — large modal that replaces the old "Drawer not assigned" nag.
 *
 * When the drawer is closed:
 *   • A big "Cash Drawer is Closed" header.
 *   • Dropdown of clocked-in users with DRAWER_ASSIGNMENT permission.
 *   • Amount input + numeric keypad for opening balance.
 *   • Open Drawer button.
 *
 * When the drawer is already open:
 *   • Status line: "Drawer opened on <time> by <user>".
 *   • Close button.
 *
 * The dialog uses a dark semi-transparent backdrop over the POS window so
 * the cashier knows the rest of the UI is blocked until they act.
 */
public class DrawerStatusDialog extends POSDialog {

	private static final Color OK_GREEN     = new Color(0x16, 0xA3, 0x4A);
	private static final Color CLOSED_RED   = new Color(0xDC, 0x26, 0x26);
	private static final Color TEXT_PRIMARY = new Color(0x1E, 0x2D, 0x3D);
	private static final Color TEXT_MUTED   = new Color(0x6B, 0x72, 0x80);
	private static final Color CARD_BG      = Color.WHITE;
	private static final Color BACKDROP     = new Color(0, 0, 0, 160); // semi-transparent overlay

	private final Terminal terminal;
	private boolean drawerOpened;

	private JComboBox<User> cbUser;
	private JTextField      tfAmount;
	private JButton         btnOpen;

	public DrawerStatusDialog() {
		super(Application.getPosWindow(), true);
		setUndecorated(true);
		try {
			setBackground(new Color(0, 0, 0, 0)); // window transparent; backdrop panel paints dim layer
		} catch (Exception ignored) {
			// Per-pixel translucency unsupported on this GraphicsConfiguration —
			// fall back to a solid backdrop colour (still works, just no see-through).
		}
		this.terminal = Application.getInstance().getTerminal();
		buildUI();
		installKeyBindings();
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			setSize(Application.getPosWindow().getSize());
			setLocationRelativeTo(Application.getPosWindow());
		}
		super.setVisible(b);
	}

	/** Single source of truth for closing this dialog — used by every button + ESC. */
	private void closeDialog() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setVisible(false);
				dispose();
				java.awt.Window owner = Application.getPosWindow();
				if (owner != null) owner.repaint();
			}
		});
	}

	private void installKeyBindings() {
		javax.swing.JComponent root = (javax.swing.JComponent) getContentPane();
		root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closeDialog");
		root.getActionMap().put("closeDialog", new javax.swing.AbstractAction() {
			@Override public void actionPerformed(java.awt.event.ActionEvent e) {
				setCanceled(true);
				closeDialog();
			}
		});
	}

	private void buildUI() {
		// Backdrop panel (full-window dim layer)
		JPanel backdrop = new JPanel(new java.awt.GridBagLayout()) {
			@Override protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(BACKDROP);
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.dispose();
			}
		};
		backdrop.setOpaque(false);

		JPanel card = buildCard();
		backdrop.add(card); // centred via default GridBagConstraints

		setContentPane(backdrop);
	}

	private JPanel buildCard() {
		JPanel card = new JPanel(new BorderLayout()) {
			@Override protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(CARD_BG);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
				g2.setColor(new Color(0xDD, 0xE3, 0xEC));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		card.setOpaque(false);
		card.setBorder(new EmptyBorder(24, 30, 24, 30));

		card.add(buildHeader(), BorderLayout.NORTH);
		if (terminal.isCashDrawerAssigned()) {
			card.setPreferredSize(new Dimension(640, 540));
			card.add(buildOpenStatusBody(),    BorderLayout.CENTER);
			card.add(buildOpenStatusButtons(), BorderLayout.SOUTH);
		} else {
			card.setPreferredSize(new Dimension(560, 640));
			card.add(buildOpenForm(),   BorderLayout.CENTER);
		}
		return card;
	}

	private JPanel buildHeader() {
		boolean closed = !terminal.isCashDrawerAssigned();
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 18, 0));

		JLabel title = new JLabel(closed ? "Cash Drawer is Closed" : "Cash Drawer is Open");
		title.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
		title.setForeground(closed ? CLOSED_RED : OK_GREEN);
		title.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel sub = new JLabel(closed
				? "Select a cashier and enter the opening balance to open the drawer."
				: "Drawer is currently open. See details below.");
		sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 13f));
		sub.setForeground(TEXT_MUTED);
		sub.setHorizontalAlignment(SwingConstants.CENTER);

		header.add(title, BorderLayout.NORTH);
		header.add(sub,   BorderLayout.SOUTH);
		return header;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  CLOSED state — pick user + amount + keypad
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildOpenForm() {
		JPanel body = new JPanel(new MigLayout("ins 0, fillx, wrap 1", "[grow,fill]", "[]10[]10[]20[]"));
		body.setOpaque(false);

		// User dropdown
		JLabel lblUser = fieldLabel("Open By");
		cbUser = new JComboBox<User>();
		cbUser.setFont(cbUser.getFont().deriveFont(Font.BOLD, 16f));
		cbUser.setPreferredSize(new Dimension(0, 44));
		populateUsers();
		body.add(lblUser, "growx");
		body.add(cbUser,  "growx");

		// Amount field
		JLabel lblAmount = fieldLabel("Opening Amount (" + CurrencyUtil.getCurrencySymbol() + ")");
		tfAmount = new JTextField();
		tfAmount.setHorizontalAlignment(SwingConstants.RIGHT);
		tfAmount.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
		tfAmount.setPreferredSize(new Dimension(0, 48));
		tfAmount.setBackground(new Color(0xF7, 0xF9, 0xFC));
		tfAmount.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(0xDD, 0xE3, 0xEC)),
				new EmptyBorder(6, 12, 6, 12)));
		body.add(lblAmount, "growx");
		body.add(tfAmount,  "growx");

		// Numeric keypad
		NumericKeypad keypad = new NumericKeypad();
		body.add(keypad, "growx, hmin 200");

		// Buttons
		JPanel btns = new JPanel(new MigLayout("ins 8 0 0 0, fillx", "[grow]rel[200!]", ""));
		btns.setOpaque(false);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.setPreferredSize(new Dimension(120, 48));
		btnCancel.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				setCanceled(true);
				closeDialog();
			}
		});

		btnOpen = new JButton("Open Drawer");
		btnOpen.setBackground(OK_GREEN);
		btnOpen.setForeground(Color.WHITE);
		btnOpen.setFont(btnOpen.getFont().deriveFont(Font.BOLD, 16f));
		btnOpen.setFocusPainted(false);
		btnOpen.setPreferredSize(new Dimension(200, 56));
		btnOpen.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { doOpenDrawer(); }
		});

		btns.add(btnCancel, "alignx left");
		btns.add(btnOpen,   "alignx right");

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.add(body, BorderLayout.CENTER);
		wrapper.add(btns, BorderLayout.SOUTH);

		// Put initial focus on the amount field so keypad targets it
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() { tfAmount.requestFocusInWindow(); }
		});
		return wrapper;
	}

	private void populateUsers() {
		DefaultComboBoxModel<User> model = new DefaultComboBoxModel<User>();
		List<User> eligible = new ArrayList<User>();
		try {
			List<User> all = UserDAO.getInstance().findAll();
			for (User u : all) {
				if (u.isClockedIn() && u.hasPermission(UserPermission.DRAWER_ASSIGNMENT)) {
					eligible.add(u);
				}
			}
		} catch (Exception ignored) {
		}
		for (User u : eligible) model.addElement(u);
		cbUser.setModel(model);
		cbUser.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override public java.awt.Component getListCellRendererComponent(
					javax.swing.JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof User) setText(((User) value).getFullName());
				return this;
			}
		});
		if (eligible.isEmpty()) {
			cbUser.setEnabled(false);
			JLabel placeholder = new JLabel("(no clocked-in user has Drawer Assignment permission)");
			placeholder.setForeground(TEXT_MUTED);
		}
	}

	private void doOpenDrawer() {
		User user = (User) cbUser.getSelectedItem();
		if (user == null) {
			POSMessageDialog.showError(this, "Please select a user with Drawer Assignment permission.");
			return;
		}
		if (!user.isClockedIn()) {
			POSMessageDialog.showError(this, "Selected user is not clocked in.");
			return;
		}
		double amount;
		try {
			amount = Double.parseDouble(tfAmount.getText().trim());
		} catch (Exception ex) {
			POSMessageDialog.showError(this, "Enter a valid opening amount.");
			return;
		}
		if (amount < 0) {
			POSMessageDialog.showError(this, "Opening amount cannot be negative.");
			return;
		}

		Session session = null;
		Transaction tx = null;
		try {
			terminal.setAssignedUser(user);
			terminal.setOpeningBalance(amount);
			terminal.setCurrentBalance(amount);

			// Stamp the "Drawer Shift" label = <shiftName> <serial> (e.g.
			// "Morning 03") so each open/close session has a unique tag even
			// when the same shift has multiple sessions in a day.
			try {
				Shift currentShift = ShiftUtil.getCurrentShift();
				String shiftName = (currentShift != null && currentShift.getName() != null)
						? currentShift.getName() : "";
				int serial = countTodayShiftSessions(shiftName) + 1; // 1-based serial
				String label = shiftName.isEmpty()
						? String.format("%02d", serial)
						: shiftName + " " + String.format("%02d", serial);
				terminal.putProperty(Terminal.PROP_DRAWER_SHIFT_NAME, label);
			} catch (Exception shiftEx) {
				terminal.putProperty(Terminal.PROP_DRAWER_SHIFT_NAME, "");
			}

			DrawerAssignedHistory history = new DrawerAssignedHistory();
			history.setTime(new Date());
			history.setOperation(DrawerAssignedHistory.ASSIGNMENT_OPERATION);
			history.setUser(user);

			session = TerminalDAO.getInstance().createNewSession();
			tx = session.beginTransaction();
			session.saveOrUpdate(terminal);
			session.save(history);
			tx.commit();

			drawerOpened = true;
			setCanceled(false);
			closeDialog();
		} catch (Exception ex) {
			if (tx != null) tx.rollback();
			POSMessageDialog.showError(this, "Failed to open drawer: " + ex.getMessage());
		} finally {
			if (session != null) session.close();
		}
	}

	// ─────────────────────────────────────────────────────────────────────
	//  OPEN state — informational body (the buttons live in card SOUTH)
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildOpenStatusBody() {
		JPanel body = new JPanel(new MigLayout("ins 16, fillx, wrap 1",
				"[grow,fill]",
				"[]8[]14[]8[]14[]8[]14[]8[]14[]8[]"));
		body.setOpaque(false);

		User user = terminal.getAssignedUser();
		Date openedAt = lookupLastAssignTime(user);
		double opening = terminal.getOpeningBalance() == null ? 0.0 : terminal.getOpeningBalance();
		double current = terminal.getCurrentBalance() == null ? 0.0 : terminal.getCurrentBalance();
		String sym = CurrencyUtil.getCurrencySymbol();
		String shiftName = terminal.getProperty(Terminal.PROP_DRAWER_SHIFT_NAME);

		body.add(captionLabel("Drawer Shift:"));
		body.add(valueLabel(shiftName == null || shiftName.isEmpty() ? "—" : shiftName));
		body.add(captionLabel("Drawer opened on:"));
		body.add(valueLabel(openedAt != null
				? DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(openedAt)
				: "—"));
		body.add(captionLabel("Assigned cashier:"));
		body.add(valueLabel(user != null ? user.getFullName() : "—"));
		body.add(captionLabel("Opening balance:"));
		body.add(valueLabel(sym + String.format("%.2f", opening)));

		// "Cash in drawer right now" — what the cashier actually wants to know
		body.add(captionLabel("Current balance (cash in drawer):"));
		JLabel currentLbl = valueLabel(sym + String.format("%.2f", current));
		currentLbl.setForeground(OK_GREEN);
		body.add(currentLbl);
		return body;
	}

	private JPanel buildOpenStatusButtons() {
		JPanel btns = new JPanel(new MigLayout("ins 12 0 0 0, fillx", "[140!][grow][260!]", ""));
		btns.setOpaque(false);
		btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xDD, 0xE3, 0xEC)));

		JButton btnDismiss = new JButton("Dismiss");
		btnDismiss.setPreferredSize(new Dimension(140, 54));
		btnDismiss.setFont(btnDismiss.getFont().deriveFont(Font.BOLD, 14f));
		btnDismiss.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				setCanceled(true);
				closeDialog();
			}
		});

		JButton btnCloseDrawer = new JButton("✕  Close Drawer");
		btnCloseDrawer.setBackground(CLOSED_RED);
		btnCloseDrawer.setForeground(Color.WHITE);
		btnCloseDrawer.setOpaque(true);
		btnCloseDrawer.setBorderPainted(false);
		btnCloseDrawer.setFont(btnCloseDrawer.getFont().deriveFont(Font.BOLD, 17f));
		btnCloseDrawer.setFocusPainted(false);
		btnCloseDrawer.setPreferredSize(new Dimension(260, 64));
		btnCloseDrawer.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { doCloseDrawer(); }
		});

		btns.add(btnDismiss,     "alignx left");
		btns.add(new JLabel(""), "growx");
		btns.add(btnCloseDrawer, "alignx right");
		return btns;
	}

	private JLabel captionLabel(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
		l.setForeground(TEXT_MUTED);
		return l;
	}

	private JLabel valueLabel(String text) {
		JLabel l = new JLabel(text);
		l.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
		l.setForeground(TEXT_PRIMARY);
		return l;
	}

	private void doCloseDrawer() {
		int confirm = POSMessageDialog.showYesNoQuestionDialog(this,
				"Close the drawer? A drawer-pull report will be generated, the cashier will be de-assigned, and the balance reset.",
				"Close Drawer");
		if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

		try {
			User user = terminal.getAssignedUser();

			// Build + persist the drawer pull report (same flow as the
			// classic DrawerAssignmentAction.performDrawerClose).
			DrawerPullReport report = DrawerpullReportService.buildDrawerPullReport();
			report.setAssignedUser(user);

			// Clear shift stamp; resetCashDrawer will save the terminal change.
			terminal.putProperty(Terminal.PROP_DRAWER_SHIFT_NAME, "");

			TerminalDAO dao = new TerminalDAO();
			dao.resetCashDrawer(report, terminal, user, 0);

			// Record the close in drawer-assignment history.
			DrawerAssignedHistory history = new DrawerAssignedHistory();
			history.setTime(new Date());
			history.setOperation(DrawerAssignedHistory.CLOSE_OPERATION);
			history.setUser(user);

			Session session = null;
			Transaction tx = null;
			try {
				session = TerminalDAO.getInstance().createNewSession();
				tx = session.beginTransaction();
				session.save(history);
				tx.commit();
			} catch (Exception ex) {
				if (tx != null) tx.rollback();
				// Non-fatal: report is already persisted; just log via dialog.
			} finally {
				if (session != null) session.close();
			}

			// Try to print the pull report. Non-fatal if a printer isn't configured.
			try {
				PosPrintService.printDrawerPullReport(report, terminal);
			} catch (PosException printEx) {
				POSMessageDialog.showError(POSUtil.getFocusedWindow(), printEx.getMessage());
			}

			drawerOpened = false;
			refreshCard();
		} catch (Exception ex) {
			POSMessageDialog.showError(this, "Failed to close drawer: " + ex.getMessage());
		}
	}

	/** Rebuild the inner card so it reflects the current drawer state. */
	private void refreshCard() {
		java.awt.Container content = getContentPane();
		// content is the backdrop JPanel; rebuild it from scratch
		content.removeAll();
		// Re-use the same logic as buildUI() but operate on the existing content pane
		((JPanel) content).setLayout(new java.awt.GridBagLayout());
		((JPanel) content).add(buildCard());
		content.revalidate();
		content.repaint();
	}

	/**
	 * Count how many drawer-assignment events occurred today (since 00:00).
	 * Used to compute the per-day session serial appended to the shift name
	 * ("Morning 01", "Morning 02", …). Returns 0 if the query fails — the
	 * caller will then just start at "01".
	 */
	private int countTodayShiftSessions(String shiftName) {
		try {
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.set(java.util.Calendar.HOUR_OF_DAY, 0);
			c.set(java.util.Calendar.MINUTE, 0);
			c.set(java.util.Calendar.SECOND, 0);
			c.set(java.util.Calendar.MILLISECOND, 0);
			Date startOfDay = c.getTime();

			Session s = TerminalDAO.getInstance().createNewSession();
			try {
				Number n = (Number) s.createCriteria(DrawerAssignedHistory.class)
						.add(org.hibernate.criterion.Restrictions.eq("operation", DrawerAssignedHistory.ASSIGNMENT_OPERATION))
						.add(org.hibernate.criterion.Restrictions.ge("time", startOfDay))
						.setProjection(org.hibernate.criterion.Projections.rowCount())
						.uniqueResult();
				return n == null ? 0 : n.intValue();
			} finally {
				s.close();
			}
		} catch (Exception ex) {
			return 0;
		}
	}

	private Date lookupLastAssignTime(User user) {
		// Best-effort: query DrawerAssignedHistory for the most recent ASSIGNMENT row for this user.
		try {
			Session s = TerminalDAO.getInstance().createNewSession();
			try {
				org.hibernate.Criteria c = s.createCriteria(DrawerAssignedHistory.class)
						.add(org.hibernate.criterion.Restrictions.eq("operation", DrawerAssignedHistory.ASSIGNMENT_OPERATION))
						.addOrder(org.hibernate.criterion.Order.desc("time"))
						.setMaxResults(1);
				if (user != null && user.getAutoId() != null) {
					c.createAlias("user", "u")
					 .add(org.hibernate.criterion.Restrictions.eq("u.autoId", user.getAutoId()));
				}
				Object row = c.uniqueResult();
				if (row instanceof DrawerAssignedHistory) {
					return ((DrawerAssignedHistory) row).getTime();
				}
			} finally {
				s.close();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public boolean isDrawerOpened() { return drawerOpened; }

	// ─────────────────────────────────────────────────────────────────────
	//  Helpers
	// ─────────────────────────────────────────────────────────────────────
	private JLabel fieldLabel(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
		l.setForeground(TEXT_MUTED);
		return l;
	}
}
