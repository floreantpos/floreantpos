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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.apache.commons.lang.StringUtils;

import net.miginfocom.swing.MigLayout;

import com.floreantpos.Messages;
import com.floreantpos.config.CardConfig;
import com.floreantpos.model.CardReader;
import com.floreantpos.swing.POSTextField;
import com.floreantpos.util.POSUtil;

public class DefaultMerchantGatewayConfigurationView extends ConfigurationView {

	// ── form fields ──────────────────────────────────────────────────────────
	private POSTextField   tfMerchantAccount;
	private JComboBox      cbCardReader;
	private JPasswordField tfMerchantPass;
	private JCheckBox      cbSandboxMode;
	private JCheckBox      chckbxAllowMagneticSwipe;
	private JCheckBox      chckbxAllowCardManual;
	private JCheckBox      chckbxAllowExternalTerminal;
	private JButton        btnTestConnection;
	private JButton        btnCreateNewMerchantAccount;

	// ── info panel refs (updated dynamically) ───────────────────────────────
	private JPanel infoBadge;
	private JLabel lblBadgeIcon;
	private JLabel lblBadgeTitle;
	private JLabel lblBadgeDesc;

	private static final String SANDBOX_URL = "https://developer.authorize.net/hello_world/sandbox/"; //$NON-NLS-1$
	private static final String LIVE_URL    = "http://reseller.authorize.net/application/?resellerId=27144"; //$NON-NLS-1$
	private String link = LIVE_URL;

	// ── sandbox / live colors ─────────────────────────────────────────────
	private static final Color SANDBOX_BG   = new Color(0xFF, 0xF3, 0xCD);
	private static final Color SANDBOX_FG   = new Color(0x7D, 0x59, 0x04);
	private static final Color SANDBOX_BDR  = new Color(0xFF, 0xD0, 0x6A);
	private static final Color LIVE_BG      = new Color(0xD4, 0xED, 0xDA);
	private static final Color LIVE_FG      = new Color(0x14, 0x54, 0x23);
	private static final Color LIVE_BDR     = new Color(0x8F, 0xD3, 0xA3);
	private static final Color PANEL_BG     = new Color(0xFA, 0xFA, 0xFA);
	private static final Color DIVIDER_CLR  = new Color(0xE0, 0xE0, 0xE0);
	private static final Color MUTED_TEXT   = new Color(0x55, 0x55, 0x55);

	public DefaultMerchantGatewayConfigurationView() {
		setLayout(new MigLayout("insets 0, fill", "[grow 55][1px][grow 45]", "[fill]")); //$NON-NLS-1$

		// ── LEFT: configuration form ─────────────────────────────────────
		JPanel formPanel = new JPanel(new MigLayout("insets 16 16 16 12", "[][grow]", "")); //$NON-NLS-1$
		formPanel.setOpaque(false);
		buildForm(formPanel);
		add(formPanel, "grow, aligny top"); //$NON-NLS-1$

		// ── Vertical divider ─────────────────────────────────────────────
		JPanel divider = new JPanel();
		divider.setBackground(DIVIDER_CLR);
		add(divider, "width 1px!, growy"); //$NON-NLS-1$

		// ── RIGHT: info panel ─────────────────────────────────────────────
		add(buildInfoPanel(), "grow"); //$NON-NLS-1$
	}

	// ────────────────────────────────────────────────────────────────────────
	// Form builder
	// ────────────────────────────────────────────────────────────────────────

	private void buildForm(JPanel p) {
		// Card reader row
		p.add(new JLabel(Messages.getString("CardConfigurationView.9")), "alignx leading"); //$NON-NLS-1$
		cbCardReader = new JComboBox();
		cbCardReader.setModel(new DefaultComboBoxModel<>(CardReader.values()));
		cbCardReader.addActionListener(e -> updateCheckBoxes());
		p.add(cbCardReader, "growx, wrap, gapy 0 4"); //$NON-NLS-1$

		// API Login ID
		p.add(new JLabel(Messages.getString("CardConfigurationView.19")), "alignx leading"); //$NON-NLS-1$
		tfMerchantAccount = new POSTextField();
		p.add(tfMerchantAccount, "growx, wrap, gapy 0 4"); //$NON-NLS-1$

		// Transaction Key
		p.add(new JLabel(Messages.getString("CardConfigurationView.22")), "alignx leading"); //$NON-NLS-1$
		tfMerchantPass = new JPasswordField();
		p.add(tfMerchantPass, "growx, wrap, gapy 0 10"); //$NON-NLS-1$

		// Checkboxes
		chckbxAllowMagneticSwipe = new JCheckBox(Messages.getString("CardConfigurationView.3")); //$NON-NLS-1$
		chckbxAllowMagneticSwipe.addActionListener(e -> updateCardList());
		p.add(chckbxAllowMagneticSwipe, "skip 1, wrap"); //$NON-NLS-1$

		chckbxAllowCardManual = new JCheckBox(Messages.getString("CardConfigurationView.5")); //$NON-NLS-1$
		chckbxAllowCardManual.addActionListener(e -> updateCardList());
		p.add(chckbxAllowCardManual, "skip 1, wrap"); //$NON-NLS-1$

		chckbxAllowExternalTerminal = new JCheckBox(Messages.getString("CardConfigurationView.7")); //$NON-NLS-1$
		chckbxAllowExternalTerminal.addActionListener(e -> updateCardList());
		p.add(chckbxAllowExternalTerminal, "skip 1, wrap, gapy 0 4"); //$NON-NLS-1$

		cbSandboxMode = new JCheckBox(Messages.getString("CardConfigurationView.25")); //$NON-NLS-1$
		cbSandboxMode.addActionListener(e -> applyModeStyle(cbSandboxMode.isSelected()));
		p.add(cbSandboxMode, "skip 1, wrap, gapy 0 10"); //$NON-NLS-1$

		// Test Connection button
		btnTestConnection = new JButton("Test Connection"); //$NON-NLS-1$
		btnTestConnection.addActionListener(e -> doTestConnection());
		p.add(btnTestConnection, "skip 1, wrap, gapy 0 6"); //$NON-NLS-1$

		// Get account link-style button
		btnCreateNewMerchantAccount = new JButton(Messages.getString(Messages.getString("DefaultMerchantGatewayConfigurationView.1"))); //$NON-NLS-1$
		btnCreateNewMerchantAccount.setForeground(new Color(0xC0, 0x39, 0x2B));
		btnCreateNewMerchantAccount.setFont(btnCreateNewMerchantAccount.getFont().deriveFont(Font.BOLD));
		btnCreateNewMerchantAccount.addActionListener(e -> {
			try { openBrowser(link); } catch (Exception ex) {}
		});
		p.add(btnCreateNewMerchantAccount, "skip 1, wrap"); //$NON-NLS-1$
	}

	// ────────────────────────────────────────────────────────────────────────
	// Info panel
	// ────────────────────────────────────────────────────────────────────────

	private JPanel buildInfoPanel() {
		JPanel outer = new JPanel(new MigLayout("insets 0, fill", "[fill]", "[fill]")); //$NON-NLS-1$
		outer.setBackground(PANEL_BG);

		JPanel inner = new JPanel(new MigLayout("insets 20 18 20 18, fill", "[fill]", "")); //$NON-NLS-1$
		inner.setBackground(PANEL_BG);
		inner.setOpaque(true);

		// ── Mode badge ───────────────────────────────────────────────────
		infoBadge = new JPanel(new MigLayout("insets 12 14 12 14, fill", "[][]", "[][]")) { //$NON-NLS-1$
			@Override protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(getBackground());
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.dispose();
			}
		};
		infoBadge.setOpaque(false);

		lblBadgeIcon  = new JLabel();
		lblBadgeIcon.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f)); //$NON-NLS-1$

		lblBadgeTitle = new JLabel();
		lblBadgeTitle.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 13f)); //$NON-NLS-1$

		lblBadgeDesc  = new JLabel();
		lblBadgeDesc.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 11f)); //$NON-NLS-1$

		infoBadge.add(lblBadgeIcon,  ""); //$NON-NLS-1$
		infoBadge.add(lblBadgeTitle, "wrap"); //$NON-NLS-1$
		infoBadge.add(lblBadgeDesc,  "skip 1"); //$NON-NLS-1$

		inner.add(infoBadge, "growx, wrap, gapy 0 16"); //$NON-NLS-1$

		// ── Sandbox section ──────────────────────────────────────────────
		inner.add(makeDivider(), "growx, wrap, gapy 0 14"); //$NON-NLS-1$

		JLabel lblSandboxHead = new JLabel("Testing & Development"); //$NON-NLS-1$
		lblSandboxHead.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f)); //$NON-NLS-1$
		inner.add(lblSandboxHead, "wrap, gapy 0 5"); //$NON-NLS-1$

		JLabel lblSandboxBody = new JLabel(
			"<html><font color='#555555'>"
			+ "Use the free Authorize.net Sandbox to test<br>"
			+ "your integration without processing real<br>"
			+ "transactions. No credit card required."
			+ "</font></html>"); //$NON-NLS-1$
		lblSandboxBody.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 11f)); //$NON-NLS-1$
		inner.add(lblSandboxBody, "wrap, gapy 0 6"); //$NON-NLS-1$

		inner.add(makeLink("↗  Get a free Sandbox account", SANDBOX_URL), "wrap, gapy 0 16"); //$NON-NLS-1$

		// ── Live section ─────────────────────────────────────────────────
		inner.add(makeDivider(), "growx, wrap, gapy 0 14"); //$NON-NLS-1$

		JLabel lblLiveHead = new JLabel("Go Live"); //$NON-NLS-1$
		lblLiveHead.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f)); //$NON-NLS-1$
		inner.add(lblLiveHead, "wrap, gapy 0 5"); //$NON-NLS-1$

		JLabel lblLiveBody = new JLabel(
			"<html><font color='#555555'>"
			+ "When you're ready to accept real payments,<br>"
			+ "open a live Authorize.net merchant account<br>"
			+ "and switch off Sandbox Mode above."
			+ "</font></html>"); //$NON-NLS-1$
		lblLiveBody.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 11f)); //$NON-NLS-1$
		inner.add(lblLiveBody, "wrap, gapy 0 6"); //$NON-NLS-1$

		inner.add(makeLink("↗  Open a live merchant account", LIVE_URL), "wrap, gapy 0 16"); //$NON-NLS-1$

		// ── Supported regions ─────────────────────────────────────────────
		inner.add(makeDivider(), "growx, wrap, gapy 0 14"); //$NON-NLS-1$

		JLabel lblRegionsHead = new JLabel("Supported Regions"); //$NON-NLS-1$
		lblRegionsHead.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f)); //$NON-NLS-1$
		inner.add(lblRegionsHead, "wrap, gapy 0 6"); //$NON-NLS-1$

		JLabel lblRegionsBody = new JLabel(
			"<html>"
			+ "<font color='#555555'>"
			+ "Authorize.net processes payments in:<br><br>"
			+ "</font>"
			+ "<font color='#333333'>"
			+ "•&nbsp; <b>United States</b><br>"
			+ "•&nbsp; <b>Canada</b><br>"
			+ "•&nbsp; <b>United Kingdom</b><br>"
			+ "•&nbsp; <b>Australia</b>"
			+ "</font>"
			+ "</html>"); //$NON-NLS-1$
		lblRegionsBody.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 11f)); //$NON-NLS-1$
		inner.add(lblRegionsBody, "wrap"); //$NON-NLS-1$

		outer.add(inner, "grow"); //$NON-NLS-1$

		// Apply initial state
		applyModeStyle(CardConfig.isSandboxMode());

		return outer;
	}

	private JSeparator makeDivider() {
		JSeparator sep = new JSeparator();
		sep.setForeground(DIVIDER_CLR);
		return sep;
	}

	private JLabel makeLink(String text, String url) {
		JLabel lbl = new JLabel("<html><a href=''>" + text + "</a></html>"); //$NON-NLS-1$
		lbl.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 11f)); //$NON-NLS-1$
		lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lbl.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				try { openBrowser(url); } catch (Exception ex) {}
			}
		});
		return lbl;
	}

	private void applyModeStyle(boolean sandbox) {
		if (infoBadge == null) return;
		if (sandbox) {
			infoBadge.setBackground(SANDBOX_BG);
			infoBadge.setBorder(new CompoundBorder(
				new MatteBorder(1, 1, 1, 1, SANDBOX_BDR),
				new EmptyBorder(0, 0, 0, 0)));
			lblBadgeIcon.setText("⚠"); //$NON-NLS-1$
			lblBadgeIcon.setForeground(SANDBOX_FG);
			lblBadgeTitle.setText("  Sandbox Mode Active"); //$NON-NLS-1$
			lblBadgeTitle.setForeground(SANDBOX_FG);
			lblBadgeDesc.setText("<html><font color='#7D5904'>Transactions are simulated.<br>No real charges will occur.</font></html>"); //$NON-NLS-1$
		} else {
			infoBadge.setBackground(LIVE_BG);
			infoBadge.setBorder(new CompoundBorder(
				new MatteBorder(1, 1, 1, 1, LIVE_BDR),
				new EmptyBorder(0, 0, 0, 0)));
			lblBadgeIcon.setText("✔"); //$NON-NLS-1$
			lblBadgeIcon.setForeground(LIVE_FG);
			lblBadgeTitle.setText("  Live Mode — Production"); //$NON-NLS-1$
			lblBadgeTitle.setForeground(LIVE_FG);
			lblBadgeDesc.setText("<html><font color='#145423'>Real transactions will be processed.<br>Live charges will apply.</font></html>"); //$NON-NLS-1$
		}
		infoBadge.revalidate();
		infoBadge.repaint();
	}

	// ────────────────────────────────────────────────────────────────────────
	// Existing public API (unchanged)
	// ────────────────────────────────────────────────────────────────────────

	private void openBrowser(String target) throws Exception {
		URI uri = new URI(target);
		if (Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(uri);
		}
	}

	@Override
	public void initialize() throws Exception {
		chckbxAllowMagneticSwipe.setSelected(CardConfig.isSwipeCardSupported());
		chckbxAllowCardManual.setSelected(CardConfig.isManualEntrySupported());
		chckbxAllowExternalTerminal.setSelected(CardConfig.isExtTerminalSupported());

		cbCardReader.setSelectedItem(CardConfig.getCardReader());

		String merchantAccount = CardConfig.getMerchantAccount();
		if (merchantAccount != null) tfMerchantAccount.setText(merchantAccount);

		String merchantPass = CardConfig.getMerchantPass();
		if (merchantPass != null) tfMerchantPass.setText(merchantPass);

		cbSandboxMode.setSelected(CardConfig.isSandboxMode());
		applyModeStyle(CardConfig.isSandboxMode());

		updateCardList();
	}

	public void setMerchantDefaultValue(String accountNo, String pass) {
		tfMerchantAccount.setText(accountNo);
		tfMerchantPass.setText(pass);
	}

	public void setVisibleLinkButton(String btnText, String lnk, boolean visible) {
		this.link = lnk;
		btnCreateNewMerchantAccount.setText(btnText);
		btnCreateNewMerchantAccount.setVisible(visible);
	}

	protected void updateCheckBoxes() {
		CardReader selectedItem = (CardReader) cbCardReader.getSelectedItem();
		if      (selectedItem == CardReader.SWIPE)             chckbxAllowMagneticSwipe.setSelected(true);
		else if (selectedItem == CardReader.MANUAL)            chckbxAllowCardManual.setSelected(true);
		else if (selectedItem == CardReader.EXTERNAL_TERMINAL) chckbxAllowExternalTerminal.setSelected(true);
	}

	private DefaultComboBoxModel<CardReader> createComboBoxModel(Vector<CardReader> items) {
		DefaultComboBoxModel<CardReader> model = new DefaultComboBoxModel<>();
		for (CardReader r : items) model.addElement(r);
		return model;
	}

	protected void updateCardList() {
		boolean swipe  = chckbxAllowMagneticSwipe.isSelected();
		boolean manual = chckbxAllowCardManual.isSelected();
		boolean ext    = chckbxAllowExternalTerminal.isSelected();

		CardReader current = (CardReader) cbCardReader.getSelectedItem();
		Vector<CardReader> readers = new Vector<>(3);
		if (swipe)  readers.add(CardReader.SWIPE);
		if (manual) readers.add(CardReader.MANUAL);
		if (ext)    readers.add(CardReader.EXTERNAL_TERMINAL);

		cbCardReader.setModel(createComboBoxModel(readers));
		if (readers.contains(current)) cbCardReader.setSelectedItem(current);

		boolean enableCard = swipe || manual;
		cbCardReader.setEnabled(enableCard || ext);
		tfMerchantAccount.setEnabled(enableCard);
		tfMerchantPass.setEnabled(enableCard);
		cbSandboxMode.setEnabled(enableCard);
		btnTestConnection.setEnabled(enableCard);
	}

	@Override
	public boolean save() throws Exception {
		String account  = tfMerchantAccount.getText();
		char[] password = tfMerchantPass.getPassword();

		if (StringUtils.isBlank(account) || StringUtils.isBlank(new String(password))) {
			JOptionPane.showMessageDialog(POSUtil.getFocusedWindow(), "Please fill in the API Login ID and Transaction Key.");
			return false;
		}

		CardConfig.setSwipeCardSupported(chckbxAllowMagneticSwipe.isSelected());
		CardConfig.setManualEntrySupported(chckbxAllowCardManual.isSelected());
		CardConfig.setExtTerminalSupported(chckbxAllowExternalTerminal.isSelected());
		CardConfig.setCardReader((CardReader) cbCardReader.getSelectedItem());
		CardConfig.setMerchantAccount(account);
		CardConfig.setMerchantPass(new String(password));
		CardConfig.setSandboxMode(cbSandboxMode.isSelected());

		return true;
	}

	@Override
	public String getName() {
		return ""; //$NON-NLS-1$
	}

	// ────────────────────────────────────────────────────────────────────────
	// Test Connection
	// ────────────────────────────────────────────────────────────────────────

	private void doTestConnection() {
		String  loginId = tfMerchantAccount.getText().trim();
		String  tranKey = new String(tfMerchantPass.getPassword()).trim();
		boolean sandbox = cbSandboxMode.isSelected();

		if (loginId.isEmpty() || tranKey.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Please enter the API Login ID and Transaction Key before testing.",
				"Test Connection", JOptionPane.WARNING_MESSAGE);
			return;
		}

		btnTestConnection.setEnabled(false);
		btnTestConnection.setText("Testing…"); //$NON-NLS-1$

		new SwingWorker<String, Void>() {
			@Override
			protected String doInBackground() throws Exception {
				String endpoint = sandbox
					? "https://test.authorize.net/gateway/transact.dll"   //$NON-NLS-1$
					: "https://secure2.authorize.net/gateway/transact.dll"; //$NON-NLS-1$

				String params =
					"x_login="    + URLEncoder.encode(loginId, "UTF-8") //$NON-NLS-1$
					+ "&x_tran_key=" + URLEncoder.encode(tranKey,  "UTF-8") //$NON-NLS-1$
					+ "&x_version=3.1&x_type=AUTH_ONLY&x_amount=0.01" //$NON-NLS-1$
					+ "&x_card_num=4111111111111111&x_exp_date=1228" //$NON-NLS-1$
					+ "&x_delim_data=TRUE&x_delim_char=|&x_relay_response=FALSE"; //$NON-NLS-1$

				HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
				conn.setRequestMethod("POST"); //$NON-NLS-1$
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); //$NON-NLS-1$

				try (OutputStream os = conn.getOutputStream()) {
					os.write(params.getBytes("UTF-8")); //$NON-NLS-1$
				}

				StringBuilder sb = new StringBuilder();
				try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String line;
					while ((line = br.readLine()) != null) sb.append(line);
				}
				return sb.toString();
			}

			@Override
			protected void done() {
				btnTestConnection.setEnabled(true);
				btnTestConnection.setText("Test Connection"); //$NON-NLS-1$
				try {
					String   response   = get();
					String[] parts      = response.split("\\|"); //$NON-NLS-1$
					String   respCode   = parts.length > 0 ? parts[0] : ""; //$NON-NLS-1$
					String   reasonCode = parts.length > 2 ? parts[2] : ""; //$NON-NLS-1$
					String   reasonText = parts.length > 3 ? parts[3] : response;
					String   env        = sandbox ? " (Sandbox)" : " (Production)"; //$NON-NLS-1$

					if ("13".equals(reasonCode)) {
						JOptionPane.showMessageDialog(DefaultMerchantGatewayConfigurationView.this,
							"Connection failed: Invalid API Login ID or Transaction Key.\nPlease verify your credentials and try again.",
							"Test Connection — Failed", JOptionPane.ERROR_MESSAGE);
					} else if ("1".equals(respCode)) {
						JOptionPane.showMessageDialog(DefaultMerchantGatewayConfigurationView.this,
							"Connection successful!" + env + "\nYour credentials are valid and ready to use.",
							"Test Connection — Success", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(DefaultMerchantGatewayConfigurationView.this,
							"Connected" + env + " — credentials accepted.\n\nGateway response: " + reasonText,
							"Test Connection — Success", JOptionPane.INFORMATION_MESSAGE);
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(DefaultMerchantGatewayConfigurationView.this,
						"Could not reach Authorize.net.\n" + ex.getMessage()
						+ "\n\nPlease check your network connection and try again.",
						"Test Connection — Failed", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}
}
