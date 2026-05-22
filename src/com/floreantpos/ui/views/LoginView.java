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
 * LoginScreen.java
 *
 * Created on August 14, 2006, 10:57 PM
 */

package com.floreantpos.ui.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.apache.commons.logging.LogFactory;

import com.floreantpos.IconFactory;
import com.floreantpos.Messages;
import com.floreantpos.POSConstants;
import com.floreantpos.actions.ClockInOutAction;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.config.ui.DatabaseConfigurationDialog;
import com.floreantpos.demo.KitchenDisplayView;
import com.floreantpos.extension.ExtensionManager;
import com.floreantpos.extension.OrderServiceExtension;
import com.floreantpos.extension.OrderServiceFactory;
import com.floreantpos.main.Application;
import com.floreantpos.main.Main;
import com.floreantpos.main.TrainingMode;
import com.floreantpos.model.Terminal;
import com.floreantpos.model.User;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.swing.MessageDialog;
import com.floreantpos.swing.OrderTypeLoginButton;
import com.floreantpos.swing.PosButton;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.dialog.PasswordEntryDialog;
import com.floreantpos.ui.views.order.RootView;
import com.floreantpos.ui.views.order.ViewPanel;
import com.floreantpos.util.ShiftException;
import com.floreantpos.util.UserNotFoundException;

import net.miginfocom.swing.MigLayout;

/**
 *
 * @author  MShahriar
 */
public class LoginView extends ViewPanel {
	public final static String VIEW_NAME = "LOGIN_VIEW"; //$NON-NLS-1$
	private boolean backOfficeLogin;
	private com.floreantpos.swing.PosButton btnSwitchBoard;
	private com.floreantpos.swing.PosButton btnKitchenDisplay;
	private com.floreantpos.swing.PosButton btnDriverView;

	private com.floreantpos.swing.PosButton btnConfigureDatabase;
	private com.floreantpos.swing.PosButton btnBackOffice;
	private com.floreantpos.swing.PosButton btnShutdown;
	private com.floreantpos.swing.PosButton btnClockOUt;
	private JLabel lblTerminalId;
	private JLabel lblRestaurantName;
	private JPanel centerPanel = new JPanel(new MigLayout("al center center", "sg", "100")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static LoginView instance;
	private JPanel mainPanel;
	private JCheckBox cbTrainingMode;

	private JPanel panel1 = new JPanel(new MigLayout("fill, ins 0, hidemode 3", "sg, fill", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private JPanel panel2 = new JPanel(new MigLayout("fill, ins 0, hidemode 3", "sg, fill", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private int width;
	private int height;

	private LoginView() {
		setLayout(new BorderLayout(5, 5));

		width = PosUIManager.getSize(600);
		height = PosUIManager.getSize(100);
		centerPanel.setLayout(new MigLayout("al center center", "sg fill", String.valueOf(height))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		JLabel titleLabel = new JLabel(IconFactory.getIcon("/ui_icons/", "title.png")); //$NON-NLS-1$ //$NON-NLS-2$
		titleLabel.setOpaque(true);
		titleLabel.setBackground(Color.WHITE);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(titleLabel, BorderLayout.CENTER);
		panel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);

		add(panel, BorderLayout.NORTH);
		add(createCenterPanel(), BorderLayout.CENTER);
	}

	private JPanel createCenterPanel() {

		//		lblTerminalId = new JLabel(Messages.getString("LoginView.0")); //$NON-NLS-1$
		//		lblTerminalId.setForeground(Color.BLACK);
		//		lblTerminalId.setFont(new Font("Dialog", Font.BOLD, PosUIManager.getFontSize(18))); //$NON-NLS-1$
		//		lblTerminalId.setHorizontalAlignment(SwingConstants.CENTER);

		lblRestaurantName = new JLabel(Application.getInstance().getRestaurant().getName());
		lblRestaurantName.setPreferredSize(new Dimension(1000, 100));
		lblRestaurantName.setForeground(Color.BLACK);
		lblRestaurantName.setFont(new Font("Dialog", Font.BOLD, PosUIManager.getFontSize(28)));
		lblRestaurantName.setHorizontalAlignment(SwingConstants.CENTER);

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(lblRestaurantName, BorderLayout.NORTH);

		btnSwitchBoard = new PosButton(POSConstants.ORDERS);
		btnKitchenDisplay = new PosButton(POSConstants.KITCHEN_DISPLAY_BUTTON_TEXT);
		btnDriverView = new PosButton("DRIVER VIEW");
		btnConfigureDatabase = new PosButton(POSConstants.CONFIGURE_DATABASE);
		btnBackOffice = new PosButton(POSConstants.BACK_OFFICE_BUTTON_TEXT);
		btnShutdown = new PosButton(POSConstants.SHUTDOWN);
		btnClockOUt = new PosButton(new ClockInOutAction(false, true));

		applyButtonIcon(btnSwitchBoard,      "btn_orders.png");
		applyButtonIcon(btnBackOffice,       "btn_backoffice.png");
		applyButtonIcon(btnKitchenDisplay,   "btn_kitchen.png");
		applyButtonIcon(btnDriverView,       "btn_driver.png");
		applyButtonIcon(btnClockOUt,         "btn_clockout.png");
		applyButtonIcon(btnConfigureDatabase,"btn_configure.png");
		applyButtonIcon(btnShutdown,         "btn_shutdown.png");

		btnBackOffice.setVisible(false);
		btnSwitchBoard.setVisible(false);
		btnKitchenDisplay.setVisible(false);
		btnClockOUt.setVisible(false);

		JPanel panel3 = new JPanel(new GridLayout(1, 0, 5, 5));
		JPanel panel4 = new JPanel(new MigLayout("fill, ins 0, hidemode 3", "sg, fill", ""));

		centerPanel.add(panel1, "cell 0 0, wrap, w " + width + "px, h " + height + "px, grow");

		panel3.add(btnSwitchBoard);
		panel3.add(btnBackOffice);
		if (TerminalConfig.isShowKitchenBtnOnLoginScreen()) {
			panel3.add(btnKitchenDisplay);
		}
		OrderServiceExtension orderService = (OrderServiceExtension) ExtensionManager.getPlugin(OrderServiceExtension.class);
		if (orderService != null) {
			panel3.add(btnDriverView);
			btnDriverView.setVisible(false);
		}
		centerPanel.add(panel3, "cell 0 2, wrap, w " + width + "px, h " + height + "px, grow");

		cbTrainingMode = new JCheckBox("Training Mode"); //$NON-NLS-1$
		cbTrainingMode.setSelected(TrainingMode.isEnabled());
		cbTrainingMode.setFont(cbTrainingMode.getFont().deriveFont(Font.BOLD, 14f));
		cbTrainingMode.setForeground(new Color(180, 80, 0));
		cbTrainingMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean requested = cbTrainingMode.isSelected();
				int choice = javax.swing.JOptionPane.showConfirmDialog(
						Application.getPosWindow(),
						"The application must restart to " + (requested ? "enable" : "disable") + " Training Mode.\nRestart now?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						"Restart Required", //$NON-NLS-1$
						javax.swing.JOptionPane.YES_NO_OPTION,
						javax.swing.JOptionPane.WARNING_MESSAGE);
				if (choice == javax.swing.JOptionPane.YES_OPTION) {
					TrainingMode.savePreference(requested);
					try {
						Main.restart();
					} catch (Exception ex) {
						com.floreantpos.swing.MessageDialog.showError("Restart failed: " + ex.getMessage(), ex); //$NON-NLS-1$
					}
				} else {
					// Revert checkbox — user cancelled
					cbTrainingMode.setSelected(!requested);
				}
			}
		});

		panel4.add(btnClockOUt, "grow"); //$NON-NLS-1$
		panel4.add(btnConfigureDatabase, "grow"); //$NON-NLS-1$
		panel4.add(btnShutdown, "grow"); //$NON-NLS-1$

		centerPanel.add(panel4, "cell 0 3, wrap, w " + width + "px, h " + height + "px, grow"); //$NON-NLS-1$

		JPanel panelTrainingMode = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
		panelTrainingMode.setOpaque(false);
		panelTrainingMode.add(cbTrainingMode);
		centerPanel.add(panelTrainingMode, "cell 0 4, wrap, w " + width + "px, h 40px, grow"); //$NON-NLS-1$

		if (TerminalConfig.isFullscreenMode()) {
			if (btnConfigureDatabase != null) {
				btnConfigureDatabase.setVisible(false);
			}
			if (btnShutdown != null) {
				btnShutdown.setVisible(false);
			}
		}
		else {
			if (!TerminalConfig.isShowDbConfigureButton()) {
				btnConfigureDatabase.setVisible(false);
			}
		}

		initActionHandlers();

		mainPanel.add(centerPanel, BorderLayout.CENTER);
		return mainPanel;
	}

	public void initializeOrderButtonPanel() {
		panel1.removeAll();
		panel2.removeAll();

		List<com.floreantpos.model.OrderType> orderTypes = Application.getInstance().getOrderTypes();
		int buttonCount = 0;

		for (com.floreantpos.model.OrderType orderType : orderTypes) {
			if (!orderType.isShowInLoginScreen()) {
				continue;
			}
			if (buttonCount < 3) {
				panel1.add(new OrderTypeLoginButton(orderType), "grow"); //$NON-NLS-1$
			}
			else {
				panel2.add(new OrderTypeLoginButton(orderType), "grow"); //$NON-NLS-1$
			}
			++buttonCount;
		}

		if (buttonCount > 3) {
			centerPanel.add(panel2, "cell 0 1, wrap,w " + width + "px, h " + height + "px, grow");
		}
		btnSwitchBoard.setVisible(true);
		btnKitchenDisplay.setVisible(true);
		btnBackOffice.setVisible(true);
		btnClockOUt.setVisible(true);
		btnDriverView.setVisible(true);

		centerPanel.repaint();
	}

	public void updateView() {
		mainPanel.repaint();
	}

	private void applyButtonIcon(PosButton btn, String iconFile) {
		ImageIcon icon = IconFactory.getIcon("/ui_icons/", iconFile);
		if (icon != null) {
			btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(36, 36, java.awt.Image.SCALE_SMOOTH)));
			btn.setIconTextGap(8);
			btn.setHorizontalAlignment(SwingConstants.CENTER);
			btn.setHorizontalTextPosition(SwingConstants.RIGHT);
		}
	}

	void initActionHandlers() {
		btnConfigureDatabase.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DatabaseConfigurationDialog.show(Application.getPosWindow());
			}
		});

		btnBackOffice.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setBackOfficeLogin(true);
				TerminalConfig.setDefaultView(SwitchboardView.VIEW_NAME);
				doLogin(true);
			}
		});

		btnKitchenDisplay.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				TerminalConfig.setDefaultView(KitchenDisplayView.VIEW_NAME);
				doLogin();
			}
		});

		btnDriverView.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				IView view = OrderServiceFactory.getOrderService().getDriverView();
				if (view == null) {
					return;
				}
				RootView.getInstance().setAndShowHomeScreen(view);
			}
		});

		btnShutdown.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Application.getInstance().shutdownPOS();
			}
		});

		btnSwitchBoard.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				TerminalConfig.setDefaultView(SwitchboardView.VIEW_NAME);
				doLogin();
			}
		});
	}

	public synchronized void doLogin() {
		doLogin(false);
	}
	public synchronized void doLogin(boolean isBackOffice) {
		try {
			Application application = Application.getInstance();

			Terminal terminal = application.refreshAndGetTerminal();
			boolean isAutoLoginEnable = terminal.hasProperty(Terminal.PROP_AUTO_LOGIN_ENABLE) && Boolean.parseBoolean(terminal.getProperty(Terminal.PROP_AUTO_LOGIN_ENABLE));
			User user = null;
			if (TrainingMode.isEnabled()) {
				java.util.List<User> users = UserDAO.getInstance().findAll();
				if (users == null || users.isEmpty()) {
					com.floreantpos.ui.dialog.POSMessageDialog.showMessage(Application.getPosWindow(),
							"Training database has no users. Please configure via Back Office first."); //$NON-NLS-1$
					return;
				}
				user = users.get(0);
			} else if (isAutoLoginEnable && terminal.hasProperty(Terminal.PROP_AUTO_LOGIN_USER_AUTO_ID) && !isBackOffice) {
				int userId = Integer.parseInt(terminal.getProperty(Terminal.PROP_AUTO_LOGIN_USER_AUTO_ID));
				user = UserDAO.getInstance().get(userId);
			} else {
				user = PasswordEntryDialog.getUser(Application.getPosWindow(), Messages.getString("LoginView.1"), Messages.getString("LoginView.2")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (user == null) {
				setBackOfficeLogin(false);
				return;
			}
			application.doLogin(user);

		} catch (UserNotFoundException e) {
			LogFactory.getLog(Application.class).error(e);
			POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("LoginView.3")); //$NON-NLS-1$
		} catch (ShiftException e) {
			LogFactory.getLog(Application.class).error(e);
			MessageDialog.showError(e.getMessage());
		} catch (Exception e1) {
			LogFactory.getLog(Application.class).error(e1);
			String message = e1.getMessage();

			if (message != null && message.contains("Cannot open connection")) { //$NON-NLS-1$
				MessageDialog.showError(Messages.getString("LoginView.4"), e1); //$NON-NLS-1$
				DatabaseConfigurationDialog.show(Application.getPosWindow());
			}
			else {
				MessageDialog.showError(Messages.getString("LoginView.5"), e1); //$NON-NLS-1$
			}
		}
	}

	public void setTerminalId(int terminalId) {
		//		lblTerminalId.setText(Messages.getString("LoginView.6") + terminalId); //$NON-NLS-1$
	}

	@Override
	public String getViewName() {
		return VIEW_NAME;
	}

	public static LoginView getInstance() {
		if (instance == null) {
			instance = new LoginView();
		}

		return instance;
	}

	public JPanel getCenterPanel() {
		return centerPanel;
	}

	public JPanel getMainPanel() {
		return mainPanel;
	}

	public boolean isBackOfficeLogin() {
		return backOfficeLogin;
	}

	public void setBackOfficeLogin(boolean backOfficeLogin) {
		this.backOfficeLogin = backOfficeLogin;
	}
}