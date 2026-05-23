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
package com.floreantpos.config.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;

import com.floreantpos.Messages;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.demo.KitchenDisplayView;
import com.floreantpos.main.Application;
import com.floreantpos.main.Main;
import com.floreantpos.model.OrderType;
import com.floreantpos.model.Restaurant;
import com.floreantpos.model.Terminal;
import com.floreantpos.model.User;
import com.floreantpos.model.dao.RestaurantDAO;
import com.floreantpos.model.dao.TerminalDAO;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.swing.DoubleTextField;
import com.floreantpos.swing.IntegerTextField;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.views.SwitchboardOtherFunctionsView;
import com.floreantpos.ui.views.SwitchboardView;
import com.floreantpos.util.POSUtil;

import net.miginfocom.swing.MigLayout;

/**
 * Terminal Configuration — two-column grouped layout.
 *
 * Left column:  Setup-oriented sections (identity, display, session/login).
 * Right column: Behavior-oriented sections (receipts/kitchen, ordering,
 *               cash drawer, maintenance).
 */
public class TerminalConfigurationView extends ConfigurationView {

	private static final Color SECTION_TITLE_FG = new Color(0x1E, 0x2D, 0x3D);
	private static final Color SECTION_BORDER   = new Color(0xDD, 0xE3, 0xEC);

	private IntegerTextField tfTerminalNumber;
	private IntegerTextField tfSecretKeyLength;
	private JTextArea        taTerminalLocation;

	private JCheckBox cbTranslatedName              = new JCheckBox(Messages.getString("TerminalConfigurationView.2"));  //$NON-NLS-1$
	private JCheckBox cbFullscreenMode              = new JCheckBox(Messages.getString("TerminalConfigurationView.3"));  //$NON-NLS-1$
	private JCheckBox cbUseSettlementPrompt         = new JCheckBox(Messages.getString("TerminalConfigurationView.4"));  //$NON-NLS-1$
	private JCheckBox cbShowDbConfiguration         = new JCheckBox(Messages.getString("TerminalConfigurationView.5"));  //$NON-NLS-1$
	private JCheckBox cbShowBarCodeOnReceipt        = new JCheckBox(Messages.getString("TerminalConfigurationView.21")); //$NON-NLS-1$
	private JCheckBox cbGroupKitchenReceiptItems    = new JCheckBox(Messages.getString("TerminalConfigurationView.7"));  //$NON-NLS-1$
	private JCheckBox chkEnabledMultiCurrency       = new JCheckBox(Messages.getString("TerminalConfigurationView.29")); //$NON-NLS-1$
	private JCheckBox chkAllowToDelPrintedItem      = new JCheckBox(Messages.getString("TerminalConfigurationView.33")); //$NON-NLS-1$
	private JCheckBox chkAllowQuickMaintenance      = new JCheckBox(Messages.getString("TerminalConfigurationView.35")); //$NON-NLS-1$
	private JCheckBox chkModifierCannotExceedMaxLimit = new JCheckBox(Messages.getString("TerminalConfigurationView.36"));//$NON-NLS-1$
	private JCheckBox chkAutoLoginConfig            = new JCheckBox(Messages.getString("TerminalConfigurationView.37")); //$NON-NLS-1$
	private JCheckBox cbAutoLogoff                  = new JCheckBox(Messages.getString("TerminalConfigurationView.16")); //$NON-NLS-1$

	private JComboBox        cbUsers       = new JComboBox<>();
	private JComboBox<String> cbFonts      = new JComboBox<String>();
	private JComboBox<String> cbDefaultView;

	// These are kept as fields so initialize() can still set them without NPE,
	// but they are not laid out in the UI (the effective values come from
	// the screen-scale slider, which derives button height and font size).
	private IntegerTextField tfButtonHeight = new IntegerTextField(5);
	private IntegerTextField tfFontSize     = new IntegerTextField(5);

	private DoubleTextField  tfScaleFactor;
	private IntegerTextField tfLogoffTime  = new IntegerTextField(4);
	private JSlider          jsResize;

	private JTextField       tfDrawerName       = new JTextField(10);
	private JTextField       tfDrawerCodes      = new JTextField(15);
	private DoubleTextField  tfDrawerInitialBalance = new DoubleTextField(6);

	public TerminalConfigurationView() {
		super();
		initComponents();
	}

	private void initComponents() {
		setLayout(new BorderLayout());

		// Allocate inputs that are referenced by multiple sections
		tfTerminalNumber  = new IntegerTextField();   tfTerminalNumber.setColumns(10);
		tfSecretKeyLength = new IntegerTextField(3);
		taTerminalLocation = new JTextArea();
		taTerminalLocation.setLineWrap(true);
		taTerminalLocation.setRows(3);
		tfScaleFactor = new DoubleTextField(5);
		jsResize = new JSlider(JSlider.HORIZONTAL, 10, 50, 10);
		jsResize.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				JSlider src = (JSlider) e.getSource();
				if (!src.getValueIsAdjusting()) {
					tfScaleFactor.setText(String.valueOf(src.getValue() / 10.0));
				}
			}
		});

		cbAutoLogoff.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				tfLogoffTime.setEnabled(cbAutoLogoff.isSelected());
			}
		});

		Vector<String> defaultViewList = new Vector<String>();
		List<OrderType> orderTypes = Application.getInstance().getOrderTypes();
		if (orderTypes != null) {
			for (OrderType orderType : orderTypes) {
				defaultViewList.add(orderType.getName());
			}
		}
		defaultViewList.add(SwitchboardOtherFunctionsView.VIEW_NAME);
		defaultViewList.add(KitchenDisplayView.VIEW_NAME);
		defaultViewList.add(SwitchboardView.VIEW_NAME);
		cbDefaultView = new JComboBox<String>(defaultViewList);

		// ── Two-column container ──────────────────────────────────────────
		// GridLayout guarantees side-by-side; weights via panel preferred widths.
		JPanel columns = new JPanel(new java.awt.GridBagLayout());
		columns.setOpaque(false);

		// LEFT — Identity + Display
		JPanel leftCol = new JPanel(new MigLayout("ins 0, wrap 1, fillx", "[grow,fill]", ""));
		leftCol.setOpaque(false);
		leftCol.add(buildIdentitySection(),  "growx");
		leftCol.add(buildDisplaySection(),   "growx, gaptop 10");

		// RIGHT — Session + Ordering + Maintenance
		JPanel rightCol = new JPanel(new MigLayout("ins 0, wrap 1, fillx", "[grow,fill]", ""));
		rightCol.setOpaque(false);
		rightCol.add(buildSessionSection(),     "growx");
		rightCol.add(buildOrderingSection(),    "growx, gaptop 10");
		rightCol.add(buildMaintenanceSection(), "growx, gaptop 10");

		java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
		gc.fill    = java.awt.GridBagConstraints.BOTH;
		gc.anchor  = java.awt.GridBagConstraints.NORTHWEST;
		gc.weighty = 1.0;
		gc.insets  = new java.awt.Insets(14, 14, 14, 9);
		gc.gridx   = 0;
		gc.gridy   = 0;
		gc.weightx = 0.35; // left ~35% — just enough for a ~40-char input + label
		columns.add(leftCol, gc);
		gc.gridx   = 1;
		gc.insets  = new java.awt.Insets(14, 9, 14, 14);
		gc.weightx = 0.65; // right ~65% gets the remaining space
		columns.add(rightCol, gc);

		JScrollPane scrollPane = new JScrollPane(columns);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(18);
		add(scrollPane);
	}

	// ─────────────────────────────────────────────────────────────────────
	//  Section builders
	// ─────────────────────────────────────────────────────────────────────
	private JPanel buildIdentitySection() {
		JPanel s = section("Terminal Identity");
		s.add(label("Terminal Number:"));
		s.add(tfTerminalNumber, "wmin 100, growx");
		s.add(label("Default Password Length:"));
		s.add(tfSecretKeyLength, "wmin 80, growx");
		s.add(label("Terminal Location:"), "aligny top");
		s.add(new JScrollPane(taTerminalLocation), "growx, hmin 70");
		return s;
	}

	private JPanel buildDisplaySection() {
		JPanel s = section("Display & UI");
		s.add(label("UI Font:"));
		s.add(cbFonts, "growx");
		s.add(label("Default View on Login:"));
		s.add(cbDefaultView, "growx");

		// Scale slider: caption row, then slider row beneath spanning both columns
		JLabel scaleCaption = new JLabel("Change Text & Button size for high resolution display:");
		scaleCaption.setFont(scaleCaption.getFont().deriveFont(Font.PLAIN));
		scaleCaption.setForeground(SECTION_TITLE_FG);
		s.add(scaleCaption, "span 2, growx, gaptop 4");

		JPanel scaleRow = new JPanel(new MigLayout("ins 0, fillx", "[grow][80!]", ""));
		scaleRow.setOpaque(false);
		scaleRow.add(jsResize, "growx");
		scaleRow.add(tfScaleFactor, "growx");
		s.add(scaleRow, "span 2, growx");

		s.add(new JLabel(""));
		s.add(cbFullscreenMode, "growx");
		s.add(new JLabel(""));
		s.add(cbTranslatedName, "growx");
		return s;
	}

	private JPanel buildSessionSection() {
		JPanel s = section("Session & Login");
		s.add(new JLabel(""));
		JPanel logoffRow = new JPanel(new MigLayout("ins 0", "[][80!]", ""));
		logoffRow.setOpaque(false);
		logoffRow.add(cbAutoLogoff);
		logoffRow.add(tfLogoffTime, "wmin 60");
		s.add(logoffRow, "growx");
		s.add(new JLabel(""));
		s.add(chkAutoLoginConfig, "growx");
		s.add(label("Auto-login User:"));
		s.add(cbUsers, "growx");
		return s;
	}

	private JPanel buildOrderingSection() {
		JPanel s = section("Ordering Behavior");
		s.add(new JLabel(""));
		s.add(cbUseSettlementPrompt, "growx");
		s.add(new JLabel(""));
		s.add(chkEnabledMultiCurrency, "growx");
		s.add(new JLabel(""));
		s.add(chkAllowToDelPrintedItem, "growx");
		s.add(new JLabel(""));
		s.add(chkModifierCannotExceedMaxLimit, "growx");
		return s;
	}

	private JPanel buildMaintenanceSection() {
		JPanel s = section("Maintenance");
		s.add(new JLabel(""));
		s.add(chkAllowQuickMaintenance, "growx");
		s.add(new JLabel(""));
		s.add(cbShowDbConfiguration, "growx");
		return s;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  UI helpers
	// ─────────────────────────────────────────────────────────────────────
	private JPanel section(String title) {
		// Input cell: min 220, pref 320 (~40 chars), max 360 — caps input bloat
		// while still letting the section shrink when the window narrows.
		JPanel p = new JPanel(new MigLayout(
				"ins 14, wrap 2, fillx",
				"[160!,right][220:320:360,fill]",
				"[]6[]"));
		p.setOpaque(false);
		p.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(SECTION_BORDER),
				new EmptyBorder(2, 2, 8, 2)));

		JLabel lbl = new JLabel(title);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
		lbl.setForeground(SECTION_TITLE_FG);
		p.add(lbl, "span 2, growx, gapbottom 6");
		return p;
	}

	private JLabel label(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.PLAIN));
		l.setForeground(SECTION_TITLE_FG);
		l.setHorizontalAlignment(SwingConstants.RIGHT);
		l.setBorder(new EmptyBorder(0, 0, 0, 8));
		return l;
	}

	// ─────────────────────────────────────────────────────────────────────
	//  ConfigurationView API — unchanged
	// ─────────────────────────────────────────────────────────────────────
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new TerminalConfigurationView());
		frame.setSize(900, 700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public boolean canSave() {
		return true;
	}

	@Override
	public boolean save() {
		int terminalNumber = 0;
		double scaleFactor = tfScaleFactor.getDouble();
		int fontSize = (int) (scaleFactor * 12);
		int menuItemButtonWidth = (int) (scaleFactor * 80);
		int buttonHeight = (int) (scaleFactor * 80);

		if (scaleFactor > 5) {
			POSMessageDialog.showError(com.floreantpos.util.POSUtil.getFocusedWindow(), Messages.getString("TerminalConfigurationView.23")); //$NON-NLS-1$
			return false;
		}

		try {
			terminalNumber = Integer.parseInt(tfTerminalNumber.getText());
		} catch (Exception x) {
			POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("TerminalConfigurationView.14")); //$NON-NLS-1$
			return false;
		}

		Object selectedUser = cbUsers.getSelectedItem();
		if (chkAutoLoginConfig.isSelected() && selectedUser != null && "<select>".equals(selectedUser.toString())) { //$NON-NLS-1$
			POSMessageDialog.showError(POSUtil.getFocusedWindow(), Messages.getString("TerminalConfigurationView.42")); //$NON-NLS-1$
			return false;
		}

		int defaultPassLen = tfSecretKeyLength.getInteger();
		if (defaultPassLen == 0) defaultPassLen = 4;

		TerminalConfig.setTerminalId(terminalNumber);
		TerminalConfig.setDefaultPassLen(defaultPassLen);
		TerminalConfig.setFullscreenMode(cbFullscreenMode.isSelected());
		TerminalConfig.setShowDbConfigureButton(cbShowDbConfiguration.isSelected());
		TerminalConfig.setUseTranslatedName(cbTranslatedName.isSelected());

		TerminalConfig.setTouchScreenButtonHeight(buttonHeight);
		TerminalConfig.setMenuItemButtonWidth(menuItemButtonWidth);
		TerminalConfig.setMenuItemButtonHeight(buttonHeight);
		TerminalConfig.setTouchScreenFontSize(fontSize);
		TerminalConfig.setScreenScaleFactor(scaleFactor);

		TerminalConfig.setAutoLogoffEnable(cbAutoLogoff.isSelected());
		TerminalConfig.setAutoLogoffTime(tfLogoffTime.getInteger() <= 0 ? 10 : tfLogoffTime.getInteger());
		TerminalConfig.setUseSettlementPrompt(cbUseSettlementPrompt.isSelected());
		TerminalConfig.setShowBarcodeOnReceipt(cbShowBarCodeOnReceipt.isSelected());
		TerminalConfig.setGroupKitchenReceiptItems(cbGroupKitchenReceiptItems.isSelected());
		TerminalConfig.setEnabledMultiCurrency(chkEnabledMultiCurrency.isSelected());
		TerminalConfig.setAllowToDeletePrintedTicketItem(chkAllowToDelPrintedItem.isSelected());
		TerminalConfig.setAllowQuickMaintenance(chkAllowQuickMaintenance.isSelected());

		String selectedFont = (String) cbFonts.getSelectedItem();
		if ("<select>".equals(selectedFont)) selectedFont = null; //$NON-NLS-1$

		String selectedView = (String) cbDefaultView.getSelectedItem();

		TerminalConfig.setDefaultView(selectedView);
		TerminalConfig.setUiDefaultFont(selectedFont);
		TerminalConfig.setDrawerPortName(tfDrawerName.getText());
		TerminalConfig.setDrawerControlCodes(tfDrawerCodes.getText());

		TerminalDAO terminalDAO = TerminalDAO.getInstance();
		Terminal terminal = terminalDAO.get(terminalNumber);
		if (terminal == null) {
			terminal = new Terminal();
			terminal.setId(terminalNumber);
			terminal.setCurrentBalance(tfDrawerInitialBalance.getDouble());
			terminal.setName(String.valueOf(terminalNumber));
		}
		terminal.setLocation(taTerminalLocation.getText());
		terminal.setOpeningBalance(tfDrawerInitialBalance.getDouble());

		if (chkAutoLoginConfig.isSelected() && selectedUser != null && !"<select>".equals(selectedUser.toString())) { //$NON-NLS-1$
			terminal.putProperty(Terminal.PROP_AUTO_LOGIN_ENABLE, String.valueOf(chkAutoLoginConfig.isSelected()));
			terminal.putProperty(Terminal.PROP_AUTO_LOGIN_USER_AUTO_ID, ((User) selectedUser).getAutoId().toString());
		} else {
			terminal.putProperty(Terminal.PROP_AUTO_LOGIN_ENABLE, String.valueOf(chkAutoLoginConfig.isSelected()));
		}
		terminalDAO.saveOrUpdate(terminal);

		Restaurant restaurant = RestaurantDAO.getRestaurant();
		restaurant.setAllowModifierMaxExceed(chkModifierCannotExceedMaxLimit.isSelected());
		RestaurantDAO.getInstance().saveOrUpdate(restaurant);

		return true;
	}

	@Override
	public void initialize() throws Exception {
		tfTerminalNumber.setText(String.valueOf(TerminalConfig.getTerminalId()));
		tfSecretKeyLength.setText(String.valueOf(TerminalConfig.getDefaultPassLen()));
		cbFullscreenMode.setSelected(TerminalConfig.isFullscreenMode());
		cbShowDbConfiguration.setSelected(TerminalConfig.isShowDbConfigureButton());
		cbUseSettlementPrompt.setSelected(TerminalConfig.isUseSettlementPrompt());
		cbShowBarCodeOnReceipt.setSelected(TerminalConfig.isShowBarcodeOnReceipt());
		cbGroupKitchenReceiptItems.setSelected(TerminalConfig.isGroupKitchenReceiptItems());
		chkEnabledMultiCurrency.setSelected(TerminalConfig.isEnabledMultiCurrency());
		chkAllowToDelPrintedItem.setSelected(TerminalConfig.isAllowedToDeletePrintedTicketItem());
		chkAllowQuickMaintenance.setSelected(TerminalConfig.isAllowedQuickMaintenance());

		tfButtonHeight.setText("" + TerminalConfig.getTouchScreenButtonHeight()); //$NON-NLS-1$
		tfScaleFactor.setText("" + TerminalConfig.getScreenScaleFactor());        //$NON-NLS-1$
		tfFontSize.setText("" + TerminalConfig.getTouchScreenFontSize());         //$NON-NLS-1$
		jsResize.setValue((int) (TerminalConfig.getScreenScaleFactor() * 10));

		cbTranslatedName.setSelected(TerminalConfig.isUseTranslatedName());
		cbAutoLogoff.setSelected(TerminalConfig.isAutoLogoffEnable());
		tfLogoffTime.setText("" + TerminalConfig.getAutoLogoffTime()); //$NON-NLS-1$
		tfLogoffTime.setEnabled(cbAutoLogoff.isSelected());

		initializeAutoLoginConfig();
		initializeFontConfig();

		cbDefaultView.setSelectedItem(TerminalConfig.getDefaultView());

		Terminal terminal = Application.getInstance().refreshAndGetTerminal();
		tfDrawerName.setText(TerminalConfig.getDrawerPortName());
		tfDrawerCodes.setText(TerminalConfig.getDrawerControlCodes());
		tfDrawerInitialBalance.setText("" + terminal.getOpeningBalance()); //$NON-NLS-1$

		taTerminalLocation.setText(terminal.getLocation());
		Restaurant restaurant = RestaurantDAO.getRestaurant();
		chkModifierCannotExceedMaxLimit.setSelected(restaurant.isAllowModifierMaxExceed());
		setInitialized(true);
	}

	private void initializeAutoLoginConfig() {
		Terminal terminal = Application.getInstance().refreshAndGetTerminal();
		boolean isAutoLoginEnable = terminal.hasProperty(Terminal.PROP_AUTO_LOGIN_ENABLE)
				&& Boolean.parseBoolean(terminal.getProperty(Terminal.PROP_AUTO_LOGIN_ENABLE));
		chkAutoLoginConfig.setSelected(isAutoLoginEnable);
		cbUsers.setEnabled(chkAutoLoginConfig.isSelected());

		chkAutoLoginConfig.addItemListener(e -> cbUsers.setEnabled(e.getStateChange() == ItemEvent.SELECTED));

		DefaultComboBoxModel model = (DefaultComboBoxModel) cbUsers.getModel();
		model.removeAllElements();
		model.addElement("<select>"); //$NON-NLS-1$
		UserDAO userDao = UserDAO.getInstance();
		List<User> allUser = userDao.findAll();
		allUser.forEach(user -> model.addElement(user));

		if (terminal.hasProperty(Terminal.PROP_AUTO_LOGIN_USER_AUTO_ID)) {
			int userId = Integer.parseInt(terminal.getProperty(Terminal.PROP_AUTO_LOGIN_USER_AUTO_ID));
			allUser.forEach(user -> {
				if (user.getAutoId().equals(userId)) {
					cbUsers.setSelectedItem(user);
				}
			});
		}
	}

	private void initializeFontConfig() {
		GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Font[] fonts = e.getAllFonts();
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cbFonts.getModel();
		model.removeAllElements();
		model.addElement("<select>"); //$NON-NLS-1$
		for (Font f : fonts) model.addElement(f.getFontName());
		String uiDefaultFont = TerminalConfig.getUiDefaultFont();
		if (StringUtils.isNotEmpty(uiDefaultFont)) cbFonts.setSelectedItem(uiDefaultFont);
	}

	@Override
	public String getName() {
		return Messages.getString("TerminalConfigurationView.47"); //$NON-NLS-1$
	}

	public static void restartPOS() {
		JOptionPane optionPane = new JOptionPane(Messages.getString("TerminalConfigurationView.26"), JOptionPane.QUESTION_MESSAGE, //$NON-NLS-1$
				JOptionPane.OK_CANCEL_OPTION, Application.getApplicationIcon(),
				new String[] { Messages.getString("TerminalConfigurationView.30") }); //$NON-NLS-1$

		Object[] optionValues = optionPane.getComponents();
		for (Object object : optionValues) {
			if (object instanceof JPanel) {
				Component[] components = ((JPanel) object).getComponents();
				for (Component component : components) {
					if (component instanceof JButton) {
						component.setPreferredSize(new Dimension(100, 80));
						((JButton) component).setPreferredSize(PosUIManager.getSize(100, 50));
					}
				}
			}
		}
		JDialog dialog = optionPane.createDialog(Application.getPosWindow(), Messages.getString("TerminalConfigurationView.31")); //$NON-NLS-1$
		dialog.setIconImage(Application.getApplicationIcon().getImage());
		dialog.setLocationRelativeTo(Application.getPosWindow());
		dialog.setVisible(true);
		Object selectedValue = optionPane.getValue();
		if (selectedValue != null && selectedValue.equals(Messages.getString("TerminalConfigurationView.28"))) { //$NON-NLS-1$
			try {
				Main.restart();
			} catch (IOException | InterruptedException | URISyntaxException e) {
			}
		}
	}
}
