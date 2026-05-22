package com.floreantpos.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.floreantpos.Messages;
import com.floreantpos.PosLog;
import com.floreantpos.config.AppConfig;
import com.floreantpos.config.AppProperties;
import com.floreantpos.main.Application;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.TitlePanel;
import com.floreantpos.util.POSUtil;

import net.miginfocom.swing.MigLayout;

public class LicenseDialog extends POSDialog implements WindowListener {
	public static final String PROP_DO_NOT_SHOW_LICENSE = "license.do.not.show"; //$NON-NLS-1$
	public static final String PROP_LICENSE_AGREED = "license.agreed"; //$NON-NLS-1$

	public LicenseDialog() {
		super(POSUtil.getBackOfficeWindow(), Messages.getString("LicenseDialog.0")); //$NON-NLS-1$
		setIconImage(Application.getApplicationIcon().getImage());
	}

	@Override
	protected void initUI() {
		TransparentPanel container = new TransparentPanel(new BorderLayout(0, 8));
		container.setBorder(new EmptyBorder(5, 10, 10, 10));

		TitlePanel titlePanel = new TitlePanel();
		titlePanel.setTitle(Messages.getString("LicenseDialog.1")); //$NON-NLS-1$
		titlePanel.setSeparatorVisible(false);
		container.add(titlePanel, BorderLayout.NORTH);

		// License scroll pane with subtle border
		JEditorPane jEditorPane = new JEditorPane();
		jEditorPane.setBorder(new EmptyBorder(5, 10, 5, 10));
		jEditorPane.setEditable(false);
		jEditorPane.setBackground(Color.WHITE);
		JScrollPane scrollPane = new JScrollPane(jEditorPane);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xDDE3EC)));
		try {
			jEditorPane.setPage(getClass().getResource("/FloreantLicense.html")); //$NON-NLS-1$
		} catch (Exception e) {
			PosLog.error(getClass(), e);
		}
		container.add(scrollPane, BorderLayout.CENTER);

		// Checkboxes
		JCheckBox cbAgreement = new JCheckBox(Messages.getString("LicenseDialog.7")); //$NON-NLS-1$
		cbAgreement.setSelected(AppConfig.getBoolean(LicenseDialog.PROP_LICENSE_AGREED, Boolean.FALSE));
		JCheckBox cbNoPrompt = new JCheckBox(Messages.getString("LicenseDialog.6")); //$NON-NLS-1$
		cbNoPrompt.setEnabled(cbAgreement.isSelected());

		JPanel checkPanel = new JPanel(new MigLayout("ins 0")); //$NON-NLS-1$
		checkPanel.add(cbAgreement, "wrap"); //$NON-NLS-1$
		checkPanel.add(cbNoPrompt);

		// Marketing text
		JLabel lblMarketing = new JLabel(
				"<html><b>Welcome to Floreant POS Community Edition</b><br>" //$NON-NLS-1$
				+ "You're currently using the free edition. Upgrade to ORO POS<br>" //$NON-NLS-1$
				+ "to unlock advanced features and business tools.</html>"); //$NON-NLS-1$

		// Action buttons
		JButton btnAbout   = makeCardButton("About Us"); //$NON-NLS-1$
		JButton btnKB      = makeCardButton("Knowledge Base"); //$NON-NLS-1$
		JButton btnUpgrade = makeCardButton("Upgrade"); //$NON-NLS-1$
		JButton btnCard    = makeCardButton("Card Processing"); //$NON-NLS-1$
		JButton btnPlugins = makeCardButton("Plugins"); //$NON-NLS-1$
		JButton btnContinue = makeCardButton("Skip & Continue  ›"); //$NON-NLS-1$
		btnContinue.setBackground(new Color(0x1A, 0x6E, 0xBD));
		btnContinue.setForeground(Color.WHITE);
		btnContinue.setEnabled(cbAgreement.isSelected());

		btnAbout  .addActionListener(e -> Application.getInstance().openWebpage("https://floreantpos.org")); //$NON-NLS-1$
		btnKB     .addActionListener(e -> Application.getInstance().openWebpage("https://guide.orocube.com/floreant-pos-installation-guide3/")); //$NON-NLS-1$
		btnUpgrade.addActionListener(e -> Application.getInstance().openWebpage("https://shop.orocube.com")); //$NON-NLS-1$
		btnCard   .addActionListener(e -> Application.getInstance().openWebpage("https://shop.orocube.com/cardquote/?source=floreantpos")); //$NON-NLS-1$
		btnPlugins.addActionListener(e -> Application.getInstance().openWebpage("http://shop.orocube.com")); //$NON-NLS-1$
		btnContinue.addActionListener(e -> dispose());

		// Checkbox listeners
		cbAgreement.addItemListener(e -> {
			boolean selected = ItemEvent.SELECTED == e.getStateChange();
			AppConfig.put(LicenseDialog.PROP_LICENSE_AGREED, selected);
			cbNoPrompt.setEnabled(selected);
			btnContinue.setEnabled(selected);
			AppConfig.put(LicenseDialog.PROP_DO_NOT_SHOW_LICENSE, selected && cbNoPrompt.isSelected());
		});
		cbNoPrompt.addItemListener(e -> AppConfig.put(LicenseDialog.PROP_DO_NOT_SHOW_LICENSE,
				ItemEvent.SELECTED == e.getStateChange() && cbAgreement.isSelected()));

		// Top row: checkboxes left, marketing text right
		JPanel topRow = new JPanel(new MigLayout("ins 0, fill")); //$NON-NLS-1$
		topRow.add(checkPanel);
		topRow.add(lblMarketing, "gapx 15, grow"); //$NON-NLS-1$

		// Bottom row: 5 link buttons + skip/continue
		JPanel bottomRow = new JPanel(new MigLayout("ins 0, fill", "sg btn, fill")); //$NON-NLS-1$ //$NON-NLS-2$
		bottomRow.add(btnAbout,    "grow"); //$NON-NLS-1$
		bottomRow.add(btnKB,       "grow"); //$NON-NLS-1$
		bottomRow.add(btnUpgrade,  "grow"); //$NON-NLS-1$
		bottomRow.add(btnCard,     "grow"); //$NON-NLS-1$
		bottomRow.add(btnPlugins,  "grow"); //$NON-NLS-1$
		bottomRow.add(btnContinue, "grow"); //$NON-NLS-1$

		JPanel footerPanel = new JPanel(new MigLayout("ins 10 0 0 0, fill")); //$NON-NLS-1$
		footerPanel.add(topRow,    "growx, wrap 8"); //$NON-NLS-1$
		footerPanel.add(bottomRow, "growx, hmin 50, wrap 4"); //$NON-NLS-1$

		// Copyright footer
		JLabel lblCopyright = new JLabel(
			"<html><center><font color='#6B7280'>" //$NON-NLS-1$
			+ "Copyright &copy; 2026 OROCUBE LLC &nbsp;|&nbsp; " //$NON-NLS-1$
			+ "1509 Johnson Ferry Rd, Marietta, GA 30062, USA" //$NON-NLS-1$
			+ "</font></center></html>"); //$NON-NLS-1$
		lblCopyright.setHorizontalAlignment(JLabel.CENTER);
		lblCopyright.setFont(new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN, 11));
		lblCopyright.setBorder(new EmptyBorder(6, 0, 2, 0));
		footerPanel.add(lblCopyright, "growx"); //$NON-NLS-1$

		container.add(footerPanel, BorderLayout.SOUTH);

		add(container);
		addWindowListener(this);
	}

	private JButton makeCardButton(String text) {
		JButton btn = new JButton(text);
		btn.setUI(new com.floreantpos.swing.CardPosButtonUI());
		btn.setOpaque(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		btn.setFocusPainted(false);
		return btn;
	}
	
	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (AppConfig.getBoolean(LicenseDialog.PROP_LICENSE_AGREED, Boolean.FALSE)) {
			dispose();
			return;
		}
		System.exit(0);
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

}
