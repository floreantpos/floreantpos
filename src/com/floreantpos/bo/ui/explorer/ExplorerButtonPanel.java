package com.floreantpos.bo.ui.explorer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.floreantpos.POSConstants;

public class ExplorerButtonPanel extends JPanel {

	private JButton editButton, addButton, deleteButton;

	public ExplorerButtonPanel() {
		setOpaque(false);
		setBorder(new EmptyBorder(6, 8, 6, 8));
		initComponents();
	}

	private void initComponents() {
		addButton    = makeButton(POSConstants.ADD);
		editButton   = makeButton(POSConstants.EDIT);
		deleteButton = makeButton(POSConstants.DELETE);

		add(addButton);
		add(editButton);
		add(deleteButton);
	}

	private static JButton makeButton(String text) {
		JButton btn = new JButton(text);
		btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
		return btn;
	}

	/** Creates a consistently sized button for extra actions in explorer panels. */
	public static JButton createButton(String text) {
		return makeButton(text);
	}

	public JButton getAddButton()    { return addButton; }
	public JButton getEditButton()   { return editButton; }
	public JButton getDeleteButton() { return deleteButton; }
}
