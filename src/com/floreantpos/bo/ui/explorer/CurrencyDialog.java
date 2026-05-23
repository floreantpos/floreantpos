package com.floreantpos.bo.ui.explorer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JPanel;

import com.floreantpos.Messages;
import com.floreantpos.model.Currency;
import com.floreantpos.model.dao.CurrencyDAO;
import com.floreantpos.ui.dialog.OkCancelOptionDialog;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.util.POSUtil;

public class CurrencyDialog extends OkCancelOptionDialog {
	private CurrencyExplorer currencyExplorer;

	public CurrencyDialog() {
		JPanel contentPanel = getContentPanel();
		contentPanel.setLayout(new BorderLayout());
		setTitle(Messages.getString("CurrencyDialog.0")); //$NON-NLS-1$
		setTitlePaneText(Messages.getString("CurrencyDialog.0")); //$NON-NLS-1$
		setOkButtonText("Close");

		currencyExplorer = new CurrencyExplorer();
		contentPanel.add(currencyExplorer, BorderLayout.CENTER);

		// Bigger dialog: 50% larger than before so the split + form breathe
		setPreferredSize(new Dimension(1350, 960));
		setMinimumSize(new Dimension(1080, 780));
	}

	@Override
	public void doOk() {
		// Each row is saved individually via the inline form; OK just validates
		// that a Main currency exists and closes the dialog.
		List<Currency> currencyList = CurrencyDAO.getInstance().findAll();
		boolean isMainSelected = false;
		for (Currency currency : currencyList) {
			if (currency.isMain()) {
				isMainSelected = true;
				break;
			}
		}
		if (!isMainSelected) {
			POSMessageDialog.showMessage(POSUtil.getFocusedWindow(),
					Messages.getString("CurrencyDialog.2")); //$NON-NLS-1$
			return;
		}
		setCanceled(true);
		dispose();
	}
}
