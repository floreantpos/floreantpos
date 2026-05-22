package com.floreantpos.bo.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.floreantpos.swing.ButtonStyleDialog;
import com.floreantpos.util.POSUtil;

public class EditButtonStyleAction extends AbstractAction {

    public EditButtonStyleAction() {
        super("Button Style"); //$NON-NLS-1$
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ButtonStyleDialog(POSUtil.getFocusedWindow()).setVisible(true);
    }
}
