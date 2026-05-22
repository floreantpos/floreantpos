package com.floreantpos.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.calendar.SingleDaySelectionModel;

import com.floreantpos.Messages;
import com.floreantpos.POSConstants;
import com.floreantpos.swing.PosSmallButton;
import net.miginfocom.swing.MigLayout;

public class DateTimePicker extends JXDatePicker {
	private JSpinner timeSpinner;
	private JPanel timePanel;
	private DateFormat timeFormat;
	private boolean isShowSecond = Boolean.FALSE;

	public DateTimePicker() {
		super();
		getMonthView().setSelectionModel(new SingleDaySelectionModel());
		updateTextFieldFormat();
	}

	public DateTimePicker(Date d) {
		this();
		setDate(d);
	}

	public DateTimePicker(Date d, boolean isShowSecond) {
		super();
		this.isShowSecond = isShowSecond;

		getMonthView().setSelectionModel(new SingleDaySelectionModel());
		setDate(d);
		updateTextFieldFormat();
	}

	public void commitEdit() throws ParseException {
		commitTime();
		//super.commitEdit();
	}

	public void cancelEdit() {
		super.cancelEdit();
		setTimeSpinners();
	}

	@Override
	public JPanel getLinkPanel() {
		super.getLinkPanel();
		if (timePanel == null) {
			timePanel = createTimePanel();
		}
		setTimeSpinners();
		return timePanel;
	}

	private JPanel createTimePanel() {
		JPanel footerPannl = new JPanel(new BorderLayout(5, 5));
		footerPannl.setBorder(new EmptyBorder(5, 5, 5, 5));
		JPanel centerPanel = new JPanel(new MigLayout("fill", "[][grow]", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SpinnerDateModel dateModel = new SpinnerDateModel();
		timeSpinner = new JSpinner(dateModel);
		centerPanel.add(new JLabel(Messages.getString("DateTimePicker.0")), "growy"); //$NON-NLS-1$ //$NON-NLS-2$
		centerPanel.add(timeSpinner, "grow"); //$NON-NLS-1$
		centerPanel.setBackground(Color.WHITE);

		PosSmallButton btnDone = new PosSmallButton("Done");

		footerPannl.add(centerPanel, BorderLayout.CENTER);
		footerPannl.add(btnDone, BorderLayout.EAST);

		btnDone.addActionListener(e -> getMonthView().commitSelection());
		return footerPannl;
	}

	private void updateTextFieldFormat() {
		if (timeSpinner == null)
			return;

		if (isShowSecond) {
			timeFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT);
		}
		else {
			timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		}

		JFormattedTextField tf = ((JSpinner.DefaultEditor) timeSpinner.getEditor()).getTextField();
		DefaultFormatterFactory factory = (DefaultFormatterFactory) tf.getFormatterFactory();
		DateFormatter formatter = (DateFormatter) factory.getDefaultFormatter();
		// Change the date format to only show the hours
		formatter.setFormat(timeFormat);
	}

	private void commitTime() {
		Date date = getDate();
		if (date != null) {
			Date time = (Date) timeSpinner.getValue();
			GregorianCalendar timeCalendar = new GregorianCalendar();
			timeCalendar.setTime(time);

			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
			calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
			if (isShowSecond) {
				calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
			}
			else {
				calendar.set(Calendar.SECOND, 0);
			}
			calendar.set(Calendar.MILLISECOND, 0);

			Date newDate = calendar.getTime();
			setDate(newDate);
		}

	}

	private void setTimeSpinners() {
		Date date = getDate();
		if (date != null) {
			timeSpinner.setValue(date);
		}
	}

	public DateFormat getTimeFormat() {
		return timeFormat;
	}

	public void setTimeFormat(DateFormat timeFormat) {
		this.timeFormat = timeFormat;
		updateTextFieldFormat();
	}
	
	@Override
	public void setDate(Date date) {
		// TODO Auto-generated method stub
		super.setDate(date);
	}
}