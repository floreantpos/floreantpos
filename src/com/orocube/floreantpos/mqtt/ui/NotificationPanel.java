package com.orocube.floreantpos.mqtt.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.floreantpos.ITicketList;
import com.floreantpos.IconFactory;
import com.floreantpos.PosLog;
import com.floreantpos.main.Application;
import com.floreantpos.model.Ticket;
import com.floreantpos.model.TicketStatus;
import com.floreantpos.model.User;
import com.floreantpos.swing.PosBlinkButton;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.ui.RefreshableView;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.views.IView;
import com.floreantpos.ui.views.LoginView;
import com.floreantpos.ui.views.OrderInfoDialog;
import com.floreantpos.ui.views.OrderInfoView;
import com.floreantpos.ui.views.SwitchboardView;
import com.floreantpos.ui.views.order.RootView;
import com.floreantpos.util.POSUtil;

import net.miginfocom.swing.MigLayout;

public class NotificationPanel extends JPanel {

	private JPanel rightPanel;
	private JPanel updateMsgPanel;
	private JPanel infoPanel;
	private JLabel lblMqttIcon;
	private final CircularFifoQueue<Ticket> mqttReceivedTickets = new CircularFifoQueue<>(20);
	private PosBlinkButton btnNewOrderArrived;
	private JSeparator separator;
	private static NotificationPanel instance;

	public NotificationPanel() {
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		infoPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		infoPanel.setOpaque(true);
		infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		rightPanel = new JPanel(new MigLayout("ins 0, hidemode 3")); //$NON-NLS-1$

		updateMsgPanel = new JPanel(new MigLayout("right, filly,hidemode 3, ins 0 0 0 0")); //$NON-NLS-1$
		lblMqttIcon = new JLabel();
		Font f = lblMqttIcon.getFont().deriveFont(Font.BOLD, (float) PosUIManager.getFontSize(10));

		btnNewOrderArrived = new PosBlinkButton("New order received");
		btnNewOrderArrived.setPreferredSize(PosUIManager.getSize(0, 20));
		btnNewOrderArrived.setFont(f);
		btnNewOrderArrived.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//doDownloadMenugreatTickets();
				doShowNewOrders();
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				btnNewOrderArrived.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				btnNewOrderArrived.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});

		separator = new JSeparator(SwingConstants.HORIZONTAL);

		rightPanel.add(updateMsgPanel);
		rightPanel.add(lblMqttIcon);
		setOpaque(false);
		infoPanel.setOpaque(false);
		rightPanel.setOpaque(false);

		add(infoPanel);
		add(rightPanel, BorderLayout.EAST);
	}

	private boolean doCheckAppLogin() {
		RootView rootView = RootView.getInstance();
		IView currentView2 = rootView.getCurrentView();
		User currentUser = Application.getCurrentUser();
		if (currentUser == null || (currentView2 == null || currentView2 instanceof LoginView)) {
			LoginView.getInstance().doLogin();
			if (rootView.getCurrentView() != null && !SwitchboardView.VIEW_NAME.equals(rootView.getCurrentView().getViewName())) {
				rootView.showView(SwitchboardView.getInstance());
			}
		}
		currentView2 = rootView.getCurrentView();
		currentUser = Application.getCurrentUser();
		if (currentUser == null || (currentView2 == null || currentView2 instanceof LoginView)) {
			return false;
		}
		return true;
	}

	private void doShowNewOrders() {
		try {
			if (!doCheckAppLogin()) {
				return;
			}

			int count = 1;
			for (Object object : mqttReceivedTickets) {
				if (!(object instanceof Ticket)) {
					return;
				}

				Ticket ticket = (Ticket) object;

				List<Ticket> ticketsToShow = new ArrayList<Ticket>();
				ticketsToShow.add(ticket);
				OrderInfoView view = new OrderInfoView(ticketsToShow);
				OrderInfoDialog dialog = new OrderInfoDialog(view, new ITicketList() {

					@Override
					public void updateTicketList() {
					}

					@Override
					public void updateCustomerTicketList(Integer customerId) {
					}

					@Override
					public Ticket getSelectedTicket() {
						return ticket;
					}
				});

				String title = "New order ";
				if (ticket.isSourceWoocomerce()) {
					title = "New woocomerce order ";
				}
				else if (ticket.isSourceOnline()) {
					title = "New menugreat order ";
				}
				dialog.setTitle(title + count++);
				dialog.updateView();
				dialog.pack();
				dialog.setSize(dialog.getSize().width + 50, PosUIManager.getSize(650));
				dialog.setLocationRelativeTo(Application.getPosWindow());
				dialog.setVisible(true);
			}
			IView currentView = RootView.getInstance().getCurrentView();
			if (currentView instanceof RefreshableView) {
				((RefreshableView) currentView).refresh();
			}
			if (currentView instanceof SwitchboardView) {
				((SwitchboardView) currentView).updateView();
			}

			mqttReceivedTickets.clear();
			btnNewOrderArrived.setVisible(false);
		} catch (Exception e) {
			PosLog.error(NotificationPanel.class, e);
			POSMessageDialog.showError(POSUtil.getFocusedWindow(), "Could not load orders. Please check your network connection and try again.");
		}
	}

	public JPanel getMessagePanel() {
		return updateMsgPanel;
	}

	public JPanel getRightPanel() {
		return rightPanel;
	}

	public void mqttConnectionLost() {
		lblMqttIcon.setIcon(IconFactory.getIcon("notification_off.png")); //$NON-NLS-1$
		lblMqttIcon.setToolTipText("Notification service off.");
	}

	public void mqttConnected() {
		lblMqttIcon.setIcon(IconFactory.getIcon("notification_on.png")); //$NON-NLS-1$
		lblMqttIcon.setToolTipText("Notification service on.");

	}

	public void ticketReceived(List<Object> tickets) {
		List<String> ticketIds = mqttReceivedTickets.stream().map(Ticket::getGlobalId).collect(Collectors.toList());

		boolean newTicket = false;
		for (Object object : tickets) {
			if (object instanceof Ticket) {
				Ticket ticket = (Ticket) object;
				if (ticket.getTicketStatus() == TicketStatus.Pending) {
					if (!ticketIds.contains(ticket.getGlobalId())) {
						mqttReceivedTickets.add(ticket);
						newTicket = true;
					}
				}
			}
		}
		if (!newTicket) {
			return;
		}
		updateMsgPanel.removeAll();
		updateMsgPanel.add(btnNewOrderArrived);
		updateMsgPanel.add(separator, "growy");//$NON-NLS-1$
		updateMsgPanel.revalidate();
		updateMsgPanel.repaint();
		btnNewOrderArrived.setText("New order received" + " (" + mqttReceivedTickets.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		btnNewOrderArrived.setBlinking(true);
		btnNewOrderArrived.setVisible(true);
		Application.getPosWindow().toFront();
		Toolkit.getDefaultToolkit().beep();
	}

	public static NotificationPanel getInstance() {
		if (instance == null) {
			instance = new NotificationPanel();
		}
		return instance;

	}
}
