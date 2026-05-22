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
 * OrderView.java
 *
 * Created on August 4, 2006, 6:58 PM
 */

package com.floreantpos.ui.views.order;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleStateException;

import com.floreantpos.IconFactory;
import com.floreantpos.Messages;
import com.floreantpos.POSConstants;
import com.floreantpos.PosException;
import com.floreantpos.PosLog;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.customer.CustomerSelectorDialog;
import com.floreantpos.customer.CustomerSelectorFactory;
import com.floreantpos.extension.ExtensionManager;
import com.floreantpos.extension.OrderServiceExtension;
import com.floreantpos.extension.OrderServiceFactory;
import com.floreantpos.main.Application;
import com.floreantpos.model.Customer;
import com.floreantpos.model.ITicketItem;
import com.floreantpos.model.MenuCategory;
import com.floreantpos.model.MenuGroup;
import com.floreantpos.model.MenuItem;
import com.floreantpos.model.OrderType;
import com.floreantpos.model.PosPrinters;
import com.floreantpos.model.ShopTable;
import com.floreantpos.model.Ticket;
import com.floreantpos.model.TicketItem;
import com.floreantpos.model.TicketItemCookingInstruction;
import com.floreantpos.model.User;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.dao.CustomerDAO;
import com.floreantpos.model.dao.MenuItemDAO;
import com.floreantpos.model.dao.ShopTableDAO;
import com.floreantpos.model.dao.ShopTableStatusDAO;
import com.floreantpos.model.dao.TicketDAO;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.swing.PosButton;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.RefreshableView;
import com.floreantpos.ui.dialog.MiscTicketItemDialog;
import com.floreantpos.ui.dialog.NumberSelectionDialog2;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.dialog.PasswordEntryDialog;
import com.floreantpos.ui.dialog.SeatSelectionDialog;
import com.floreantpos.ui.tableselection.TableSelectorDialog;
import com.floreantpos.ui.tableselection.TableSelectorFactory;
import com.floreantpos.ui.views.CookingInstructionSelectionView;
import com.floreantpos.ui.views.order.actions.TicketEditListener;
import com.floreantpos.ui.views.payment.PaymentListener;
import com.floreantpos.ui.views.payment.PaymentView;
import com.floreantpos.ui.views.payment.SettleTicketProcessor;
import com.floreantpos.util.CurrencyUtil;
import com.floreantpos.util.DrawerUtil;
import com.floreantpos.util.NumberUtil;
import com.floreantpos.util.POSUtil;

/**
 *
 * @author  MShahriar
 */
public class OrderView extends ViewPanel implements PaymentListener, TicketEditListener, RefreshableView {
	private HashMap<String, JComponent> views = new HashMap<String, JComponent>();
	private SettleTicketProcessor ticketProcessor = new SettleTicketProcessor(Application.getCurrentUser(), this);
	public final static String VIEW_NAME = "ORDER_VIEW"; //$NON-NLS-1$
	private static OrderView instance;

	private Ticket currentTicket;

	private CategoryView categoryView = new CategoryView();
	private GroupView groupView = new GroupView();
	private MenuItemView itemView = new MenuItemView();
	private TicketView ticketView = new TicketView();

	private TransparentPanel midContainer = new TransparentPanel(new BorderLayout(5, 5));
	private JPanel ticketViewContainer = new JPanel(new BorderLayout());
	private JPanel bottomBar = new JPanel(new BorderLayout(0, 0));
	private JPanel ticketSummaryView = createTicketSummeryPanel();

	private OrderController orderController = new OrderController(this);

	// Yellow background for Pay Total button when total > 0
	private static final Color PAY_TOTAL_ACTIVE_BG  = new Color(0xFFB800);
	private static final Color PAY_TOTAL_ACTIVE_FG  = Color.BLACK;
	private Color btnTotalPlusBg; // saved lazily on first activation

	// All action buttons live in one single row.
	// hidemode 3 ensures hidden buttons (table/guest/seat) take no space.
	private JPanel actionButtonPanel = new JPanel(new MigLayout("fill, ins 3, gap 3, hidemode 3", "", "[fill, grow]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private com.floreantpos.swing.PosButton btnHold   = new com.floreantpos.swing.PosButton("Hold Order"); //$NON-NLS-1$
	private com.floreantpos.swing.PosButton btnTotalPlus = new com.floreantpos.swing.PosButton("Total Plus"); //$NON-NLS-1$
	private com.floreantpos.swing.PosButton btnSend   = new com.floreantpos.swing.PosButton("Send Kitchen"); //$NON-NLS-1$
	private com.floreantpos.swing.PosButton btnCancel = new com.floreantpos.swing.PosButton(POSConstants.CANCEL_BUTTON_TEXT);
	private com.floreantpos.swing.PosButton btnGuestNo     = new com.floreantpos.swing.PosButton(POSConstants.GUEST_NO_BUTTON_TEXT);
	private com.floreantpos.swing.PosButton btnSeatNo      = new com.floreantpos.swing.PosButton(Messages.getString("OrderView.1")); //$NON-NLS-1$
	private com.floreantpos.swing.PosButton btnMisc        = new com.floreantpos.swing.PosButton("Misc Item"); //$NON-NLS-1$
	private com.floreantpos.swing.PosButton btnOrderType   = new com.floreantpos.swing.PosButton(POSConstants.ORDER_TYPE_BUTTON_TEXT);
	private com.floreantpos.swing.PosButton btnTableNumber = new com.floreantpos.swing.PosButton(POSConstants.TABLE_NO_BUTTON_TEXT);
	private com.floreantpos.swing.PosButton btnCustomer    = new PosButton("Customer"); //$NON-NLS-1$
	private PosButton btnCookingInstruction = new PosButton("Cook. Inst."); //$NON-NLS-1$
	//	private PosButton btnAddOn = new PosButton(POSConstants.ADD_ON);
	private PosButton btnDiscount    = new PosButton(Messages.getString("TicketView.43")); //$NON-NLS-1$
	private PosButton btnDeliveryInfo = new PosButton(Messages.getString("OrderView.2")); //$NON-NLS-1$

	private JTextField tfSubtotal;
	private JTextField tfDiscount;
	private JTextField tfDeliveryCharge;
	private JTextField tfTax;
	private JTextField tfGratuity;
	private JTextField tfTotal;
	private PaymentView paymentView;

	/** Creates new form OrderView */
	private OrderView() {
		initComponents();
		this.orderController.addTicketUpdateListener(this);
		this.ticketView.getTicketViewerTable().addTicketUpdateListener(this);
		this.ticketProcessor.addPaymentListener(this);
	}

	public void addView(final String viewName, final JComponent view) {
		JComponent oldView = views.get(viewName);
		if (oldView != null) {
			return;
		}
		midContainer.add(view, viewName);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
	private void initComponents() {
		setOpaque(false);
		setLayout(new java.awt.BorderLayout(2, 1));

		JPanel ticketViewInnerCon = new JPanel(new BorderLayout());
		ticketViewInnerCon.add(ticketView);
		ticketViewContainer.add(ticketViewInnerCon);

		// bottomBar: btnTotalPlus on WEST (width synced to ticket panel), action buttons fill the rest
		bottomBar.add(btnTotalPlus, BorderLayout.WEST);
		bottomBar.add(actionButtonPanel, BorderLayout.CENTER);
		ticketViewContainer.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override public void componentResized(java.awt.event.ComponentEvent e) {
				int w = ticketViewContainer.getWidth();
				if (w > 0) {
					btnTotalPlus.setPreferredSize(new java.awt.Dimension(w, btnTotalPlus.getPreferredSize().height));
					bottomBar.revalidate();
				}
			}
		});


		midContainer.setOpaque(false);
		midContainer.setBorder(null);
		midContainer.add(groupView, BorderLayout.NORTH);
		midContainer.add(itemView);

		//		add(categoryView, java.awt.BorderLayout.EAST);
		//		add(ticketView, java.awt.BorderLayout.WEST);
		//		add(midContainer, java.awt.BorderLayout.CENTER);
		//		add(actionButtonPanel, java.awt.BorderLayout.SOUTH);

		//		addView(GroupView.VIEW_NAME, groupView);
		//		addView(MenuItemView.VIEW_NAME, itemView);
		//		addView("VIEW_EMPTY", new com.floreantpos.swing.TransparentPanel()); //$NON-NLS-1$

		// Light grey tray background makes white cards pop
		actionButtonPanel.setBackground(new Color(0xEEF1F6));
		addActionButtonPanel();
		updatePayTotalButton(0.0); // initialise Pay Total label with $0.00
		btnOrderType.setVisible(false);
		showView("VIEW_EMPTY"); //$NON-NLS-1$
	}// </editor-fold>//GEN-END:initComponents

	private void handleTicketItemSelection() {

		ITicketItem selectedItem = (ITicketItem) ticketView.getTicketViewerTable().getSelected();
		TicketItem selectedTicketItem = null;
		OrderView orderView = OrderView.getInstance();

		if (selectedItem instanceof TicketItem) {
			selectedTicketItem = (TicketItem) selectedItem;
			MenuItemDAO dao = new MenuItemDAO();
			MenuItem menuItem = dao.get(selectedTicketItem.getItemId());

			if (menuItem != null) {
				MenuGroup menuGroup = menuItem.getParent();
				MenuItemView itemView = OrderView.getInstance().getItemView();
				if (!menuGroup.equals(itemView.getMenuGroup())) {
					itemView.setMenuGroup(menuGroup);
				}

				orderView.showView(MenuItemView.VIEW_NAME);
				itemView.selectItem(menuItem);

				MenuCategory menuCategory = menuGroup.getParent();
				orderView.getCategoryView().setSelectedCategory(menuCategory);
			}
		}

		if (selectedItem == null) {
			btnCookingInstruction.setEnabled(false);
			btnDiscount.setEnabled(false);
			//			btnAddOn.setEnabled(false);
		}
		else {
			btnCookingInstruction.setEnabled(selectedItem.canAddCookingInstruction());
			btnDiscount.setEnabled(selectedItem.canAddDiscount());
			//			btnAddOn.setEnabled(selectedTicketItem != null && selectedTicketItem.isHasModifiers());
		}
		//			btnVoid.setEnabled(item.canAddAdOn());
		//			btnAddOn.setEnabled(item.canVoid());

		//		else if (selectedObject instanceof TicketItemModifier) {
		//			selectedTicketItem = ((TicketItemModifier) selectedObject).getParent().getParent();
		//			if (selectedTicketItem == null)
		//				return;
		//
		//			ModifierView modifierView = orderView.getModifierView();
		//
		//			if (selectedTicketItem.isHasModifiers()) {
		//				MenuItemDAO dao = new MenuItemDAO();
		//				MenuItem menuItem = dao.get(selectedTicketItem.getItemId());
		//				if (!menuItem.equals(modifierView.getMenuItem())) {
		//					menuItem = dao.initialize(menuItem);
		//					modifierView.setMenuItem(menuItem, selectedTicketItem);
		//				}
		//
		//				MenuCategory menuCategory = menuItem.getParent().getParent();
		//				orderView.getCategoryView().setSelectedCategory(menuCategory);
		//
		//				TicketItemModifier ticketItemModifier = (TicketItemModifier) selectedObject;
		//				ticketItemModifier.setSelected(true);
		//				modifierView.select(ticketItemModifier);
		//
		//				orderView.showView(ModifierView.VIEW_NAME);
		//			}
		//		}
	}

	private void addActionButtonPanel() {
		ticketView.getTicketViewerTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					handleTicketItemSelection();
				}
			}
		});

btnCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {

				if (ticketView.isCancelable()) {
					ticketView.doCancelOrder();
					return;
				}

				int result = POSMessageDialog.showYesNoQuestionDialog(null, Messages.getString("OrderView.3"), Messages.getString("OrderView.4")); //$NON-NLS-1$ //$NON-NLS-2$
				if (result != JOptionPane.YES_OPTION) {
					return;
				}

				ticketView.doCancelOrder();
				ticketView.setAllowToLogOut(true);
			}
		});

		btnSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					PosPrinters printers = PosPrinters.load();
					if (printers.getKitchenPrinters().size() == 0) {
						POSMessageDialog.showError(POSUtil.getFocusedWindow(), Messages.getString("OrderView.5")); //$NON-NLS-1$
						return;
					}
					
					ticketView.sendTicketToKitchen();
					ticketView.updateView();
					POSMessageDialog.showMessage(Messages.getString("OrderView.6")); //$NON-NLS-1$
				} catch (StaleStateException x) {
					POSMessageDialog.showMessageDialogWithReloadButton(POSUtil.getFocusedWindow(), getInstance());
				} catch (PosException x) {
					POSMessageDialog.showError(x.getMessage());
				} catch (Exception x) {
					POSMessageDialog.showError(Application.getPosWindow(), POSConstants.ERROR_MESSAGE, x);
				}

			}
		});

		btnOrderType.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				// doViewOrderInfo();
				//doChangeOrderType(); fix
			}
		});

		btnCustomer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAddEditCustomer();
			}
		});

		btnMisc.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				doInsertMisc(evt);
			}
		});

		btnGuestNo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCustomerNumberActionPerformed();
			}
		});

		btnSeatNo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				doAddSeatNumber();
			}
		});

		btnTableNumber.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				updateTableNumber();
			}
		});

		btnCookingInstruction.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAddCookingInstruction();
			}
		});

		btnHold.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					OrderType orderType = currentTicket.getOrderType();
					if (orderType.isShowTableSelection() && orderType.isRequiredCustomerData()//fix
							&& !Application.getCurrentUser().hasPermission(UserPermission.HOLD_TICKET)) {
						//

						String password = PasswordEntryDialog.show(Application.getPosWindow(), Messages.getString("OrderView.7")); //$NON-NLS-1$
						if (StringUtils.isEmpty(password)) {
							return;
						}

						User user2 = UserDAO.getInstance().findUserBySecretKey(password);
						if (user2 == null) {
							POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("OrderView.8")); //$NON-NLS-1$
							return;
						}
						else {
							if (!user2.hasPermission(UserPermission.HOLD_TICKET)) {
								POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("OrderView.9")); //$NON-NLS-1$
								return;
							}
						}
					}

					if (!currentTicket.isBarTab() && (ticketView.getTicket().getTicketItems() == null || ticketView.getTicket().getTicketItems().size() == 0)) {
						POSMessageDialog.showError(com.floreantpos.POSConstants.TICKET_IS_EMPTY_);
						return;
					}
					ticketView.doHoldOrder();
					ticketView.setAllowToLogOut(true);
				} catch (StaleStateException x) {
					POSMessageDialog.showMessageDialogWithReloadButton(POSUtil.getFocusedWindow(), getInstance());
				} catch (PosException x) {
					POSMessageDialog.showError(x.getMessage());
				} catch (Exception x) {
					POSMessageDialog.showError(Application.getPosWindow(), POSConstants.ERROR_MESSAGE, x);
				}
			}
		});

		//		btnAddOn.addActionListener(new ActionListener() {
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				doAddAddOn();
		//			}
		//		});

		btnDiscount.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addDiscount();
			}
		});

		btnDeliveryInfo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				doShowDeliveryDialog();
			}
		});

		// Icon and button size scale with screen DPI
		int iconSize  = PosUIManager.getSize(30);
		int btnHeight = PosUIManager.getSize(45);

		// Icons match the login-screen button style: icon LEFT, text RIGHT
		setupActionButton(btnOrderType,          "btn_orders.png",           "Order Type",   iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnTableNumber,        "order.png",                "Table",        iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnGuestNo,            "add_user.png",             "Guests",       iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnSeatNo,             "chair.png",                "Seat",         iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnDeliveryInfo,       "btn_driver.png",           "Delivery",     iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnCustomer,           "add_user.png",             "Customer",     iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnCookingInstruction, "cooking-instruction.png",  "Cook. Inst.",  iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnMisc,               "order.png",                "Misc Item",    iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnDiscount,           "minus_32.png",             "Discount",     iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnHold,               "notification_off.png",     "Hold Order",   iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnSend,               "btn_kitchen.png",          "Send Kitchen", iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnCancel,             "btn_clockout.png",         "Cancel Order", iconSize); //$NON-NLS-1$ //$NON-NLS-2$
		setupActionButton(btnTotalPlus,          "settle_ticket.png",        "Total",        iconSize); //$NON-NLS-1$ //$NON-NLS-2$

		// Single row — all buttons share space; hidemode 3 means hidden ones take no space
		String cell      = "grow, push, sg btn, hmin " + btnHeight; //$NON-NLS-1$
		String cellTotal = "grow 2, push, hmin " + btnHeight;       //$NON-NLS-1$

		// Context buttons (hidden until dine-in order opens)
		actionButtonPanel.add(btnOrderType,   cell);
		actionButtonPanel.add(btnTableNumber, cell);
		actionButtonPanel.add(btnGuestNo,     cell);
		actionButtonPanel.add(btnSeatNo,      cell);
		actionButtonPanel.add(btnDeliveryInfo, cell);

		// Wire btnTotalPlus — opens payment screen
		btnTotalPlus.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try {
					ticketView.doPayNow();
				} catch (Exception x) {
					POSMessageDialog.showError(Application.getPosWindow(), POSConstants.ERROR_MESSAGE, x);
				}
			}
		});

		actionButtonPanel.add(btnCustomer,           cell);
		actionButtonPanel.add(btnCookingInstruction, cell);
		actionButtonPanel.add(btnMisc,               cell);
		actionButtonPanel.add(btnHold,               cell);
		actionButtonPanel.add(btnSend,               cell);
		actionButtonPanel.add(btnCancel,             cell);

		btnCookingInstruction.setEnabled(false);
		btnDiscount.setVisible(false);
		btnDeliveryInfo.setVisible(false);
	}

	/**
	 * Apply the card-style UI, a DPI-aware icon and label text.
	 * The CardPosButtonUI handles icon-top / text-bottom layout and the white card rendering.
	 */
	private void setupActionButton(PosButton btn, String iconFile, String text, int iconSize) {
		// Switch to card-style renderer
		btn.setUI(new com.floreantpos.swing.CardPosButtonUI());

		try {
			ImageIcon raw = IconFactory.getIcon("/ui_icons/", iconFile); //$NON-NLS-1$
			if (raw != null) {
				btn.setIcon(new ImageIcon(raw.getImage().getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH)));
			}
		} catch (Exception ignored) {
		}
		btn.setText(text);
		btn.setFont(btn.getFont().deriveFont(Font.BOLD, PosUIManager.getFontSize(11)));
		btn.setForeground(new Color(0x1E2D3D));
	}

	private JPanel createTicketSummeryPanel() {
		JLabel lblSubtotal = new javax.swing.JLabel();
		lblSubtotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblSubtotal.setText(com.floreantpos.POSConstants.SUBTOTAL + ":" + " " + CurrencyUtil.getCurrencySymbol()); //$NON-NLS-1$ //$NON-NLS-2$

		tfSubtotal = new javax.swing.JTextField(10);
		tfSubtotal.setHorizontalAlignment(SwingConstants.TRAILING);
		tfSubtotal.setEditable(false);

		JLabel lblDiscount = new javax.swing.JLabel();
		lblDiscount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblDiscount.setText(Messages.getString("TicketView.9") + " " + CurrencyUtil.getCurrencySymbol()); //$NON-NLS-1$ //$NON-NLS-2$

		tfDiscount = new javax.swing.JTextField(10);
		//	tfDiscount.setFont(tfDiscount.getFont().deriveFont(Font.PLAIN, 16));
		tfDiscount.setHorizontalAlignment(SwingConstants.TRAILING);
		tfDiscount.setEditable(false);
		//		tfDiscount.setText(ticket.getDiscountAmount().toString());

		JLabel lblDeliveryCharge = new javax.swing.JLabel();
		lblDeliveryCharge.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblDeliveryCharge.setText("Delivery Charge:" + " " + CurrencyUtil.getCurrencySymbol()); //$NON-NLS-1$ //$NON-NLS-2$

		tfDeliveryCharge = new javax.swing.JTextField(10);
		tfDeliveryCharge.setHorizontalAlignment(SwingConstants.TRAILING);
		tfDeliveryCharge.setEditable(false);

		JLabel lblTax = new javax.swing.JLabel();
		lblTax.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblTax.setText(com.floreantpos.POSConstants.TAX + ":" + " " + CurrencyUtil.getCurrencySymbol()); //$NON-NLS-1$ //$NON-NLS-2$

		tfTax = new javax.swing.JTextField(10);
		//	tfTax.setFont(tfTax.getFont().deriveFont(Font.PLAIN, 16));
		tfTax.setEditable(false);
		tfTax.setHorizontalAlignment(SwingConstants.TRAILING);

		JLabel lblGratuity = new javax.swing.JLabel();
		lblGratuity.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblGratuity.setText(Messages.getString("SettleTicketDialog.5") + ":" + " " + CurrencyUtil.getCurrencySymbol()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

		tfGratuity = new javax.swing.JTextField(10);
		tfGratuity.setEditable(false);
		tfGratuity.setHorizontalAlignment(SwingConstants.TRAILING);

		JLabel lblTotal = new javax.swing.JLabel();
		lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, PosUIManager.getFontSize(18)));
		lblTotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		lblTotal.setText(com.floreantpos.POSConstants.TOTAL + ":" + " " + CurrencyUtil.getCurrencySymbol()); //$NON-NLS-1$ //$NON-NLS-2$

		tfTotal = new javax.swing.JTextField(10);
		tfTotal.setFont(tfTotal.getFont().deriveFont(Font.BOLD, PosUIManager.getFontSize(18)));
		tfTotal.setHorizontalAlignment(SwingConstants.TRAILING);
		tfTotal.setEditable(false);

		JPanel ticketAmountPanel = new com.floreantpos.swing.TransparentPanel(new MigLayout("hidemode 3,ins 2 2 3 2,alignx trailing,fill", "[grow]2[]", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		ticketAmountPanel.add(lblSubtotal, "growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(tfSubtotal, "growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(lblDiscount, "newline,growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(tfDiscount, "growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(lblTax, "newline,growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(tfTax, "growx,aligny center"); //$NON-NLS-1$
		//		if (ticket.getOrderType().isDelivery() && !ticket.isCustomerWillPickup()) {
		//			ticketAmountPanel.add(lblDeliveryCharge, "newline,growx,aligny center"); //$NON-NLS-1$
		//			ticketAmountPanel.add(tfDeliveryCharge, "growx,aligny center"); //$NON-NLS-1$
		//		}
		ticketAmountPanel.add(lblGratuity, "newline,growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(tfGratuity, "growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(lblTotal, "newline,growx,aligny center"); //$NON-NLS-1$
		ticketAmountPanel.add(tfTotal, "growx,aligny center"); //$NON-NLS-1$

		return ticketAmountPanel;
	}

	//	public void clearTicketSummeryView() {
	//		tfSubtotal.setText(""); //$NON-NLS-1$
	//		tfDiscount.setText(""); //$NON-NLS-1$
	//		tfDeliveryCharge.setText(""); //$NON-NLS-1$
	//		tfTax.setText(""); //$NON-NLS-1$
	//		tfTotal.setText(""); //$NON-NLS-1$
	//		tfGratuity.setText(""); //$NON-NLS-1$
	//	}

	public void updateTicketSummeryView() {
		Ticket ticket = ticketView.getTicket();
		if (ticket == null) {
			tfSubtotal.setText(""); //$NON-NLS-1$
			tfDiscount.setText(""); //$NON-NLS-1$
			tfDeliveryCharge.setText(""); //$NON-NLS-1$
			tfTax.setText(""); //$NON-NLS-1$
			tfTotal.setText(""); //$NON-NLS-1$
			tfGratuity.setText(""); //$NON-NLS-1$
			return;
		}
		tfSubtotal.setText(NumberUtil.formatNumber(ticket.getSubtotalAmount()));
		tfDiscount.setText(NumberUtil.formatNumber(ticket.getDiscountAmount()));
		tfDeliveryCharge.setText(NumberUtil.formatNumber(ticket.getDeliveryCharge()));

		if (Application.getInstance().isPriceIncludesTax()) {
			tfTax.setText(Messages.getString("TicketView.35")); //$NON-NLS-1$
		}
		else {
			tfTax.setText(NumberUtil.formatNumber(ticket.getTaxAmount()));
		}
		if (ticket.getGratuity() != null) {
			tfGratuity.setText(NumberUtil.formatNumber(ticket.getGratuity().getAmount()));
		}
		else {
			tfGratuity.setText("0.00"); //$NON-NLS-1$
		}
		double total = ticket.getTotalAmount();
		tfTotal.setText(NumberUtil.formatNumber(total));
		updatePayTotalButton(total);

		//		doShowTicketDiscountPanel();
	}

	/**
	 * Update the Pay Total button: yellow with the total amount when > 0,
	 * default style when the ticket is empty.
	 */
	private void updatePayTotalButton(double total) {
		String amount = CurrencyUtil.getCurrencySymbol() + NumberUtil.formatNumber(total);
		btnTotalPlus.setText("<html><center><b>TOTAL</b><br/>" + amount + "</center></html>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (total > 0.0) {
			if (btnTotalPlusBg == null) btnTotalPlusBg = btnTotalPlus.getBackground();
			btnTotalPlus.setBackground(PAY_TOTAL_ACTIVE_BG);
			btnTotalPlus.setForeground(PAY_TOTAL_ACTIVE_FG);
		} else {
			if (btnTotalPlusBg != null) btnTotalPlus.setBackground(btnTotalPlusBg);
			btnTotalPlus.setForeground(new Color(0x1E, 0x2D, 0x3D));
		}
	}

	protected void doShowDeliveryDialog() {
		Customer customer = CustomerDAO.getInstance().findById(currentTicket.getCustomerId());
		OrderServiceFactory.getOrderService().showDeliveryInfo(currentTicket.getOrderType(), customer);
	}

	public void updateTableNumber() {
		try {
			TableSelectorDialog dialog = TableSelectorFactory.createTableSelectorDialog(currentTicket.getOrderType());
			dialog.setCreateNewTicket(false);
			if (currentTicket != null) {
				dialog.setTicket(currentTicket);
			}
			dialog.openUndecoratedFullScreen();

			if (dialog.isCanceled()) {
				return;
			}
			List<ShopTable> tables = dialog.getSelectedTables();
			List<Integer> addedTables = new ArrayList<>();
			for (ShopTable shopTable : tables) {
				addedTables.add(shopTable.getId());
			}
			ShopTableStatusDAO.getInstance().removeTicketFromShopTableStatus(currentTicket, null);
			currentTicket.setTableNumbers(addedTables);
			ShopTableDAO.getInstance().occupyTables(currentTicket);
			actionUpdate();
		} catch (Exception e) {
			PosLog.error(getClass(), e);
		}
	}

	protected void btnCustomerNumberActionPerformed() {// GEN-FIRST:event_btnCustomerNumberActionPerformed
		Ticket thisTicket = currentTicket;
		int guestNumber = thisTicket.getNumberOfGuests();

		NumberSelectionDialog2 dialog = new NumberSelectionDialog2();
		dialog.setTitle(com.floreantpos.POSConstants.NUMBER_OF_GUESTS);
		dialog.setValue(guestNumber);
		dialog.pack();
		dialog.open();

		if (dialog.isCanceled()) {
			return;
		}

		guestNumber = (int) dialog.getValue();
		if (guestNumber == 0) {
			POSMessageDialog.showError(Application.getPosWindow(), com.floreantpos.POSConstants.GUEST_NUMBER_CANNOT_BE_0);
			return;
		}

		thisTicket.setNumberOfGuests(guestNumber);
		actionUpdate();
	}// GEN-LAST:event_btnCustomerNumberActionPerformed

	protected void doAddSeatNumber() {// GEN-FIRST:event_btnCustomerNumberActionPerformed
		SeatSelectionDialog seatDialog = new SeatSelectionDialog(currentTicket.getTableNumbers(), getSeatNumbers());
		seatDialog.setTitle(Messages.getString("OrderView.10")); //$NON-NLS-1$
		seatDialog.pack();
		seatDialog.open();

		if (seatDialog.isCanceled()) {
			return;
		}
		int seatNumber = seatDialog.getSeatNumber();
		if (seatNumber == -1) {
			NumberSelectionDialog2 dialog = new NumberSelectionDialog2();
			dialog.setTitle(Messages.getString("OrderView.11")); //$NON-NLS-1$
			dialog.pack();
			dialog.open();

			if (dialog.isCanceled()) {
				return;
			}
			seatNumber = (int) dialog.getValue();
		}

		btnSeatNo.setText(Messages.getString("OrderView.12") + seatNumber); //$NON-NLS-1$
		btnSeatNo.putClientProperty("SEAT_NO", seatNumber); //$NON-NLS-1$
		doAddSeatTreatTicketItem(seatNumber);
	}

	private void doAddSeatTreatTicketItem(Integer seatNumber) {
		TicketItem ticketItem = new TicketItem();
		if (seatNumber == 0)
			ticketItem.setName(Messages.getString("OrderView.13")); //$NON-NLS-1$
		else
			ticketItem.setName(Messages.getString("OrderView.14") + seatNumber); //$NON-NLS-1$

		ticketItem.setShouldPrintToKitchen(true);
		ticketItem.setTreatAsSeat(true);
		ticketItem.setSeatNumber(seatNumber);
		ticketItem.setTicket(currentTicket);
		ticketView.addTicketItem(ticketItem);
	}

	private int getLastSeatNumber() {
		int lastSeatNumber = 0;
		List<TicketItem> ticketItems = currentTicket.getTicketItems();
		if (ticketItems != null && !ticketItems.isEmpty()) {
			TicketItem lastTicketItem = ticketItems.get(ticketItems.size() - 1);
			lastSeatNumber = lastTicketItem.getSeatNumber();
		}
		return lastSeatNumber;
	}

	protected Integer getSelectedSeatNumber() {
		Object seatNumber = btnSeatNo.getClientProperty("SEAT_NO"); //$NON-NLS-1$
		if (seatNumber == null)
			return 0;

		Integer seatNo = (Integer) seatNumber;

		boolean sendToKitchen = false;
		for (TicketItem ticketItem : currentTicket.getTicketItems()) {
			if (!ticketItem.isTreatAsSeat())
				continue;
			int existingSeatNumber = ticketItem.getSeatNumber();
			if (existingSeatNumber == seatNo) {
				sendToKitchen = ticketItem.isPrintedToKitchen();
			}
		}
		if (sendToKitchen) {
			doAddSeatTreatTicketItem(seatNo);
		}

		return seatNo;
	}

	protected List<Integer> getSeatNumbers() {
		List<Integer> seatNumbers = new ArrayList<>();

		for (TicketItem ticketItem : currentTicket.getTicketItems()) {
			if (ticketItem.isTreatAsSeat() && !seatNumbers.contains(ticketItem.getSeatNumber())) {
				seatNumbers.add(ticketItem.getSeatNumber());
			}
		}
		return seatNumbers;
	}

	protected void doInsertMisc(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_doInsertMisc
		MiscTicketItemDialog dialog = new MiscTicketItemDialog();
		//dialog.setSize(900, 580);
		dialog.pack();
		dialog.open();

		if (!dialog.isCanceled()) {
			TicketItem ticketItem = dialog.getTicketItem();
			ticketItem.setTicket(currentTicket);
			ticketItem.calculatePrice();
			ticketView.addTicketItem(ticketItem);
		}
	}// GEN-LAST:event_doInsertMisc

	protected void doAddEditCustomer() {
		CustomerSelectorDialog dialog = CustomerSelectorFactory.createCustomerSelectorDialog(currentTicket.getOrderType());
		dialog.setCreateNewTicket(false);
		if (currentTicket != null) {
			dialog.setTicket(currentTicket);
		}
		dialog.openUndecoratedFullScreen();

		if (!dialog.isCanceled() && dialog.getSelectedCustomer() != null) {
			currentTicket.setCustomer(dialog.getSelectedCustomer());
			btnCustomer.setText(Messages.getString("OrderView.15") + dialog.getSelectedCustomer().getName() + Messages.getString("OrderView.16")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected void addDiscount() {
		Object selectedObject = (ITicketItem) ticketView.getTicketViewerTable().getSelected();

		if (!(selectedObject instanceof TicketItem)) {
			POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("TicketView.20")); //$NON-NLS-1$
			return;
		}

		/*TicketItem ticketItem = (TicketItem) selectedObject;
		
		double d = NumberSelectionDialog2.takeDoubleInput(
				Messages.getString("TicketView.39"), Messages.getString("TicketView.40"), ticketItem.getDiscountAmount()); //$NON-NLS-1$ //$NON-NLS-2$
		if (Double.isNaN(d)) {
			return;
		}
		
		ticketItem.setDiscountAmount(d);
		ticketView.getTicketViewerTable().repaint();
		ticketView.updateView();*/
	}

	protected void doAddCookingInstruction() {

		try {
			Object object = ticketView.getTicketViewerTable().getSelected();
			if (!(object instanceof TicketItem)) {
				POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("TicketView.20")); //$NON-NLS-1$
				return;
			}

			TicketItem ticketItem = (TicketItem) object;

			if (ticketItem.isPrintedToKitchen()) {
				POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("TicketView.21")); //$NON-NLS-1$
				return;
			}

			CookingInstructionSelectionView dialog = new CookingInstructionSelectionView();
			dialog.setSize(1000, 680);
			dialog.setLocationRelativeTo(Application.getPosWindow());
			dialog.setVisible(true);

			if (dialog.isCanceled()) {
				return;
			}

			List<TicketItemCookingInstruction> instructions = dialog.getTicketItemCookingInstructions();
			ticketItem.addCookingInstructions(instructions);

			ticketView.getTicketViewerTable().updateView();
		} catch (Exception e) {
			PosLog.error(getClass(), e);
			POSMessageDialog.showError(e.getMessage());
		}
	}

	//	private void doAddAddOn() {
	//		Object object = ticketView.getTicketViewerTable().getSelected();
	//		if (!(object instanceof TicketItem)) {
	//			POSMessageDialog.showError(Application.getPosWindow(), Messages.getString("TicketView.20")); //$NON-NLS-1$
	//			return;
	//		}
	//
	//		TicketItem ticketItem = (TicketItem) object;
	//
	//		if (!ticketItem.isHasModifiers()) {
	//			return;
	//		}
	//
	//		Integer itemId = ticketItem.getItemId();
	//		MenuItem menuItem = MenuItemDAO.getInstance().get(itemId);
	//		if (menuItem == null) {
	//			return;
	//		}
	//
	//		menuItem = MenuItemDAO.getInstance().initialize(menuItem);
	//		ModifierSelectionDialog dialog = new ModifierSelectionDialog(new ModifierSelectionModel(ticketItem, menuItem), true);
	//		dialog.open();
	//		ticketView.updateView();
	//	}

	public void actionUpdate() {

		if (currentTicket != null) {
			OrderType type = currentTicket.getOrderType();

			btnTotalPlus.setVisible(true);
			btnSend.setEnabled(type.isShouldPrintToKitchen());

			if (!type.isAllowSeatBasedOrder()) {
				btnSeatNo.setVisible(false);
			}
			else {
				btnSeatNo.setVisible(true);
				int lastSeatNumber = getLastSeatNumber();
				btnSeatNo.putClientProperty("SEAT_NO", lastSeatNumber); //$NON-NLS-1$
				if (lastSeatNumber > 0)
					btnSeatNo.setText(Messages.getString("OrderView.17") + lastSeatNumber); //$NON-NLS-1$
				else
					btnSeatNo.setText(Messages.getString("OrderView.18")); //$NON-NLS-1$
			}

			if (!type.isShowTableSelection()) {
				btnGuestNo.setVisible(false);
				btnTableNumber.setVisible(false);
			}
			else {
				btnGuestNo.setVisible(true);
				btnTableNumber.setVisible(true);

				List<Integer> tableNumbers = currentTicket.getTableNumbers();

				if (tableNumbers != null) {

					String tables = getTableNumbers(currentTicket.getTableNumbers());

					btnTableNumber.setText(Messages.getString("OrderView.19") + tables + Messages.getString("OrderView.20")); //$NON-NLS-1$ //$NON-NLS-2$

				}
				else {
					btnTableNumber.setText(Messages.getString("OrderView.21")); //$NON-NLS-1$
				}

				btnGuestNo.setText(Messages.getString("OrderView.22") + String.valueOf(currentTicket.getNumberOfGuests())); //$NON-NLS-1$
			}
			OrderServiceExtension orderService = (OrderServiceExtension) ExtensionManager.getPlugin(OrderServiceExtension.class);
			btnDeliveryInfo.setVisible(orderService != null && type.isDelivery() && type.isRequiredCustomerData());

			}
	}

	private String getTableNumbers(List<Integer> numbers) {

		String tableNumbers = ""; //$NON-NLS-1$

		if (numbers != null && !numbers.isEmpty()) {
			for (Iterator iterator = numbers.iterator(); iterator.hasNext();) {
				Integer n = (Integer) iterator.next();
				tableNumbers += n;

				if (iterator.hasNext()) {
					tableNumbers += ", "; //$NON-NLS-1$
				}
			}
			return tableNumbers;
		}
		return tableNumbers;
	}

	public void showView(final String viewName) {

		//scardLayout.show(midContainer, viewName);
		//getTicketView().txtSearchItem.requestFocus();
	}

	public com.floreantpos.ui.views.order.CategoryView getCategoryView() {
		return categoryView;
	}

	public void setCategoryView(com.floreantpos.ui.views.order.CategoryView categoryView) {
		this.categoryView = categoryView;
	}

	public GroupView getGroupView() {
		return groupView;
	}

	public void setGroupView(GroupView groupView) {
		this.groupView = groupView;
	}

	public MenuItemView getItemView() {
		return itemView;
	}

	public void setItemView(MenuItemView itemView) {
		this.itemView = itemView;
	}

	public com.floreantpos.ui.views.order.TicketView getTicketView() {
		return ticketView;
	}

	public void setTicketView(com.floreantpos.ui.views.order.TicketView ticketView) {
		this.ticketView = ticketView;
	}

	public OrderController getOrderController() {
		return orderController;
	}

	public Ticket getCurrentTicket() {
		return currentTicket;
	}

	private void changeViewForOrderType(OrderType orderType) {
		if (orderType.isRetailOrder()) {
			showRetailView();
		}
		else {
			showDefaultView();
		}
		revalidate();
		repaint();
	}

	private void setHideButtonForRetailView() {
		btnTotalPlus.setVisible(false);
		btnCancel.setVisible(false);
		btnDeliveryInfo.setVisible(false);
		btnDiscount.setVisible(false);
		btnGuestNo.setVisible(false);
		btnOrderType.setVisible(false);
		btnSeatNo.setVisible(false);
		btnTableNumber.setVisible(false);
		btnSend.setVisible(false);
	}

	private void setVisibleButtonForOrderView() {
		btnTotalPlus.setVisible(true);
		btnCancel.setVisible(true);
		btnDiscount.setVisible(true);
		btnGuestNo.setVisible(true);
		btnHold.setVisible(true);
		btnMisc.setVisible(true);
		btnSeatNo.setVisible(true);
		btnSend.setVisible(true);
		btnTableNumber.setVisible(true);
	}

	private void showRetailView() {
		removeAll();
		if (paymentView == null) {
			paymentView = new PaymentView(ticketProcessor, this);
		}

		ticketSummaryView.setVisible(true);

		add(ticketViewContainer, java.awt.BorderLayout.CENTER);
		add(paymentView,         java.awt.BorderLayout.EAST);
		add(bottomBar,           java.awt.BorderLayout.SOUTH);
	}

	private void showDefaultView() {
		removeAll();
		setVisibleButtonForOrderView();
		ticketSummaryView.setVisible(true);
		add(categoryView,         java.awt.BorderLayout.EAST);
		add(ticketViewContainer,  java.awt.BorderLayout.WEST);
		add(midContainer,         java.awt.BorderLayout.CENTER);
		add(bottomBar,            java.awt.BorderLayout.SOUTH);
	}

	public void setCurrentTicket(Ticket newTicket) {
		if (this.currentTicket != null && newTicket != null) {
			OrderType currentOrderType = this.currentTicket.getOrderType();
			OrderType newOrderType = newTicket.getOrderType();
			if (!currentOrderType.equals(newOrderType)) {
				currentTicket = newTicket;
				changeViewForOrderType(newOrderType);
			}
		}
		else if (this.currentTicket == null && newTicket != null) {
			OrderType newOrderType = newTicket.getOrderType();
			changeViewForOrderType(newOrderType);
		}

		this.currentTicket = newTicket;
		ticketView.setTicket(newTicket);
		if (paymentView != null) {
			paymentView.setTicket(newTicket);
		}
		updateView();
		resetView();
	}

	public void updateView() {
		if (currentTicket != null) {
			currentTicket.calculatePrice();
			updatePayTotalButton(currentTicket.getTotalAmount());
			OrderType type = currentTicket.getOrderType();
			if (type.isRetailOrder()) {
				ticketView.updateView();
				updateTicketSummeryView();
				paymentView.setTicket(currentTicket);
				setHideButtonForRetailView();
			}
		} else {
			updatePayTotalButton(0.0);
		}
	}

	public synchronized static OrderView getInstance() {
		if (instance == null) {
			instance = new OrderView();
		}
		return instance;
	}

	public void resetView() {
	}

	@Override
	public void setVisible(boolean aFlag) {
		if (aFlag) {
			try {
				actionUpdate();
				categoryView.initialize();
			} catch (Throwable t) {
				POSMessageDialog.showError(Application.getPosWindow(), com.floreantpos.POSConstants.ERROR_MESSAGE, t);
			}
		}
		else {
			categoryView.cleanup();
			if (TerminalConfig.isActiveCustomerDisplay()) {
				DrawerUtil.setCustomerDisplayMessage(TerminalConfig.getCustomerDisplayPort(), Messages.getString("OrderView.23")); //$NON-NLS-1$
			}
		}
		super.setVisible(aFlag);
	}

	@Override
	public String getViewName() {
		return VIEW_NAME;
	}

	@Override
	public void paymentDone() {
		//		ticketView.doFinishOrder();
		//		clearTicketSummeryView();
		//		paymentView.updateView();
		//		ticketView.getTicketViewerTable().updateView();
		ticketView.doFinishOrder();
		updateView();
	}

	@Override
	public void paymentCanceled() {
		if (currentTicket == null) {
			return;
		}
		if (currentTicket.getId() != null && currentTicket.getDueAmount() > 0.0) {
			POSMessageDialog.showError(POSUtil.getFocusedWindow(), Messages.getString("OrderView.24")); //$NON-NLS-1$
			return;
		}

		if (currentTicket.getId() != null) {
			TicketDAO.getInstance().delete(currentTicket);
		}
		currentTicket.getTicketItems().clear();

		ticketView.doCancelOrder();
		updateView();
	}

	@Override
	public void paymentDataChanged() {
		updateView();

	}

	public PaymentView getPaymentView() {
		return paymentView;
	}

	public SettleTicketProcessor getTicketProcessor() {
		return ticketProcessor;
	}

	@Override
	public void itemAdded(Ticket ticket, TicketItem item) {
		ticket.calculatePrice();
		updatePayTotalButton(ticket.getTotalAmount());
		if (ticket.getOrderType().isRetailOrder()) {
			updateView();
			paymentView.updateView();
		}
	}

	@Override
	public void itemRemoved(TicketItem item) {
		if (currentTicket != null) {
			currentTicket.calculatePrice();
			updatePayTotalButton(currentTicket.getTotalAmount());
		}
		if (currentTicket.getOrderType().isRetailOrder()) {
			updateView();
			paymentView.updateView();
		}
	}

	@Override
	public void refresh() {
		Ticket ticket = TicketDAO.getInstance().loadFullTicket(currentTicket.getId());
		setCurrentTicket(ticket);
	}
}