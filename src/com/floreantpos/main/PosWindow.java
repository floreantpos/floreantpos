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
package com.floreantpos.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.Timer;

import com.floreantpos.IconFactory;
import com.floreantpos.actions.ShutDownAction;
import com.floreantpos.config.AppConfig;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.main.TrainingMode;
import com.floreantpos.model.User;
import com.floreantpos.swing.GlassPane;
import com.floreantpos.swing.PosUIManager;

import net.miginfocom.swing.MigLayout;

public class PosWindow extends JFrame implements WindowListener {
	private static final String EXTENDEDSTATE = "extendedstate"; //$NON-NLS-1$
	private static final String WLOCY = "wlocy"; //$NON-NLS-1$
	private static final String WLOCX = "wlocx"; //$NON-NLS-1$
	private static final String WHEIGHT = "wheight"; //$NON-NLS-1$
	private static final String WWIDTH = "wwidth"; //$NON-NLS-1$

	private GlassPane glassPane;
	private JLabel statusLabel;
	private JLabel lblUser;
	private JLabel lblTerminal;
	private JLabel lblTaxInculed;
	private JLabel lblTime;
	private JPanel welcomeHeaderPanel;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss aaa");
	private Timer clockTimer = new Timer(1000, new ClockTimerHandler());
	private Timer autoLogoffTimer;
	private JPanel statusBar;
	private JPanel infoPanel;
	private static final Color TRAINING_BAR_COLOR = new Color(255, 140, 0);

	public PosWindow() {
		setIconImage(Application.getApplicationIcon().getImage());
		addWindowListener(this);

		glassPane = new GlassPane();
		glassPane.setOpacity(0.6f);
		setGlassPane(glassPane);

		infoPanel = new JPanel(new MigLayout("fillx", "[]", ""));
		infoPanel.setBackground(Color.WHITE);

		statusLabel = new JLabel(""); //$NON-NLS-1$
		lblUser = new JLabel();
		lblTaxInculed = new JLabel();
		lblTerminal = new JLabel();
		lblTime = new JLabel("");
		Font f = statusLabel.getFont().deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(10));
		statusLabel.setFont(f.deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(12)));
		lblTerminal.setFont(f);
		lblUser.setFont(f);
		lblTaxInculed.setFont(f);
		lblTime.setFont(f);

		infoPanel.add(statusLabel, "split 4");
		infoPanel.add(lblTerminal, "gapleft 20");
		infoPanel.add(lblUser, "gapleft 15");
		infoPanel.add(lblTaxInculed, "gapleft 15");
		infoPanel.add(lblTime, "right");

		JPanel statusBarContainer = new JPanel(new BorderLayout());
		statusBarContainer.setBackground(Color.WHITE);
		statusBarContainer.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.NORTH);
		//		ImageIcon icon = IconFactory.getIcon("/ui_icons/", "footer-logo.png");
		//		statusLabel.setIcon(icon);

		statusLabel.setText("A Product by OROCUBE LLC.");
		statusLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {}
			
			@Override
			public void mousePressed(MouseEvent e) {}
			
			@Override
			public void mouseExited(MouseEvent e) {}
			
			@Override
			public void mouseEntered(MouseEvent e) {}
			

			@Override
			public void mouseClicked(MouseEvent e) {
				Application.getInstance().openWebpage("https://www.orocube.com/"); //$NON-NLS-1$
			}
		});
		statusBar = new JPanel();
		statusBar.setVisible(false);
		statusBar.setOpaque(false);
		statusBarContainer.add(infoPanel, BorderLayout.CENTER);
		statusBarContainer.add(statusBar, BorderLayout.EAST);
		getContentPane().add(statusBarContainer, BorderLayout.SOUTH);
		clockTimer.start();
	}

	public JPanel getStatusBar() {
		return statusBar;
	}

	public void setVisibleWelcomeHeader(boolean visible) {
		if (!visible) {
			getContentPane().remove(welcomeHeaderPanel);
			welcomeHeaderPanel = null;
			return;
		}
		JLabel titleLabel = new JLabel(IconFactory.getIcon("/ui_icons/", "title.png")); //$NON-NLS-1$ //$NON-NLS-2$
		titleLabel.setOpaque(true);
		titleLabel.setBackground(Color.WHITE);

		welcomeHeaderPanel = new JPanel(new BorderLayout());
		welcomeHeaderPanel.add(titleLabel, BorderLayout.CENTER);
		welcomeHeaderPanel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
		add(welcomeHeaderPanel, BorderLayout.NORTH);
	}

	public void setStatus(String status) {
	}

	public void updateView() {
		updateView(TrainingMode.isEnabled());
	}

	public void updateView(boolean showAsTraining) {
		lblTerminal.setText("Terminal: " + TerminalConfig.getTerminalId());

		User currentUser = Application.getCurrentUser();
		if (currentUser != null) {
			lblUser.setText("User: " + currentUser.getFullName() + " (" + currentUser.getType().getName() + ")");
		}
		else {
			lblUser.setText("User: Not Logged In"); //$NON-NLS-1$
		}

		if(Application.getInstance().isPriceIncludesTax()) {
			lblTaxInculed.setText("Tax included: YES");
		}else {
			lblTaxInculed.setText("Tax included: NO");
		}

		if (showAsTraining) {
			infoPanel.setBackground(TRAINING_BAR_COLOR);
			statusLabel.setText("\u26A0 TRAINING MODE"); //$NON-NLS-1$
			statusLabel.setForeground(Color.WHITE);
			statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(13)));
			lblTerminal.setForeground(Color.WHITE);
			lblUser.setForeground(Color.WHITE);
			lblTaxInculed.setForeground(Color.WHITE);
			lblTime.setForeground(Color.WHITE);
		} else {
			infoPanel.setBackground(Color.WHITE);
			statusLabel.setText("A Product by OROCUBE LLC."); //$NON-NLS-1$
			statusLabel.setForeground(Color.BLACK);
			statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(12)));
			lblTerminal.setForeground(Color.BLACK);
			lblUser.setForeground(Color.BLACK);
			lblTaxInculed.setForeground(Color.BLACK);
			lblTime.setForeground(Color.BLACK);
		}
		infoPanel.repaint();
	}

	public void setupSizeAndLocation() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(AppConfig.getInt(WWIDTH, (int) screenSize.getWidth()), AppConfig.getInt(WHEIGHT, (int) screenSize.getHeight()));

		setLocation(AppConfig.getInt(WLOCX, ((screenSize.width - getWidth()) >> 1)), AppConfig.getInt(WLOCY, ((screenSize.height - getHeight()) >> 1)));
		setMinimumSize(new Dimension(1024, 724));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		int extendedState = AppConfig.getInt(EXTENDEDSTATE, -1);
		if (extendedState != -1) {
			setExtendedState(extendedState);
		}
	}

	public void enterFullScreenMode() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screenSize);
		setExtendedState(MAXIMIZED_BOTH);
		setUndecorated(true);
		setLocation(0, 0);
		//GraphicsDevice window = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
		//window.setFullScreenWindow(this);
	}

	public void leaveFullScreenMode() {
		GraphicsDevice window = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
		setUndecorated(false);
		window.setFullScreenWindow(null);
	}

	public void saveSizeAndLocation() {
		int width = getWidth();
		int height = getHeight();
		AppConfig.putInt(WWIDTH, width);
		AppConfig.putInt(WHEIGHT, height);

		Point locationOnScreen = getLocationOnScreen();
		AppConfig.putInt(WLOCX, locationOnScreen.x);
		AppConfig.putInt(WLOCY, locationOnScreen.y);

		AppConfig.putInt(EXTENDEDSTATE, getExtendedState());
	}

	public void setGlassPaneVisible(boolean b) {
		glassPane.setVisible(b);
	}

	/*public void setGlassPaneMessage(String message) {
		glassPane.setMessage(message);
	}*/

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		if (Application.getCurrentUser() != null) {
			new ShutDownAction().actionPerformed(null);
		}
		else {
			Application.getInstance().shutdownPOS();
		}
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);

		if (aFlag) {
			startTimer();
		}
		else {
			stopTimer();
		}
	}

	private void startTimer() {
		clockTimer.start();

		if (autoLogoffTimer != null) {
			autoLogoffTimer.start();
		}
	}

	private void stopTimer() {
		clockTimer.stop();

		if (autoLogoffTimer != null) {
			autoLogoffTimer.stop();
		}
	}

	private class ClockTimerHandler implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!isShowing()) {
				clockTimer.stop();
				return;
			}

			showFooterTimer();
		}

		private void showFooterTimer() {
			StringBuilder sb = new StringBuilder();
			sb.append(dateFormat.format(Calendar.getInstance().getTime()));
			lblTime.setText("Time: " + sb.toString());
		}
	}
}