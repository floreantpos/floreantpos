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
package com.floreantpos.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.ImageIcon;

import com.floreantpos.IconFactory;
import com.floreantpos.POSConstants;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.main.Application;
import com.floreantpos.model.OrderType;
import com.floreantpos.model.User;
import com.floreantpos.model.UserPermission;
import com.floreantpos.model.UserType;
import com.floreantpos.ui.views.LoginView;

public class OrderTypeLoginButton extends PosButton implements ActionListener {
	private OrderType orderType;

	public OrderTypeLoginButton() {
		super("");
	}

	public OrderTypeLoginButton(OrderType orderType) {
		super();
		this.orderType = orderType;
		if (orderType != null) {
			setText(orderType.getName());
			ImageIcon icon = getIconForOrderType(orderType.getName());
			if (icon != null) {
				setIcon(new ImageIcon(icon.getImage().getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH)));
				setIconTextGap(10);
				setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
				setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
			}
		}
		else {
			setText(POSConstants.TAKE_OUT_BUTTON_TEXT);
		}
		addActionListener(this);
	}

	private static ImageIcon getIconForOrderType(String name) {
		if (name == null) return null;
		String lower = name.toLowerCase();
		String iconFile;
		if (lower.contains("dine") || lower.contains("table")) {
			iconFile = "ordertype_dinein.png";
		} else if (lower.contains("take") || lower.contains("pickup") || lower.contains("pick up") || lower.contains("to go")) {
			iconFile = "ordertype_takeout.png";
		} else if (lower.contains("deliv")) {
			iconFile = "ordertype_delivery.png";
		} else if (lower.contains("retail") || lower.contains("shop")) {
			iconFile = "ordertype_retail.png";
		} else if (lower.contains("bar") || lower.contains("tab") || lower.contains("drink")) {
			iconFile = "ordertype_bartab.png";
		} else if (lower.contains("drive")) {
			iconFile = "ordertype_drivethru.png";
		} else if (lower.contains("cater")) {
			iconFile = "ordertype_catering.png";
		} else if (lower.contains("online") || lower.contains("web") || lower.contains("mobile")) {
			iconFile = "ordertype_online.png";
		} else {
			return null;
		}
		return IconFactory.getIcon("/ui_icons/", iconFile);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
//		if(!hasPermission()) {
//			POSMessageDialog.showError("You do not have permission to create order");
//			return;
//		}
		
		TerminalConfig.setDefaultView(orderType.getName());
		LoginView.getInstance().doLogin();
	}

	private boolean hasPermission() {
		User user = Application.getCurrentUser();
		UserType userType = user.getType();
		if (userType != null) {
			Set<UserPermission> permissions = userType.getPermissions();
			for (UserPermission permission : permissions) {
				if (permission.equals(UserPermission.CREATE_TICKET)) {
					return true;
				}
			}
		}
		return false;
	}
}
