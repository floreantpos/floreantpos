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
package com.floreantpos.actions;

import javax.swing.JDialog;

import com.floreantpos.POSConstants;
import com.floreantpos.main.Application;
import com.floreantpos.model.UserPermission;
import com.floreantpos.swing.PosUIManager;
import com.floreantpos.ui.dialog.DrawerPullReportDialog;
import com.floreantpos.ui.dialog.POSMessageDialog;

public class DrawerPullAction extends PosAction {

	public DrawerPullAction() {
		super(POSConstants.DRAWER_PULL_BUTTON_TEXT, UserPermission.DRAWER_PULL); //$NON-NLS-1$
	}

	@Override
	public void execute() {
		try {
			// "DRAWER STATUS" tile now opens the unified drawer status dialog
			// where the user can open or close the drawer (no more standalone
			// pull-report nag — the report is generated when closing).
			com.floreantpos.ui.dialog.DrawerStatusDialog dialog =
					new com.floreantpos.ui.dialog.DrawerStatusDialog();
			dialog.setVisible(true);
		} catch (Exception e) {
			POSMessageDialog.showError(Application.getPosWindow(), e.getMessage(), e);
		}
	}

}
