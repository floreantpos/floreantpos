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
package com.floreantpos.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;

import com.floreantpos.main.TrainingMode;
import com.floreantpos.model.base.BaseRestaurant;
import com.floreantpos.util.POSUtil;

@XmlRootElement(name = "restaurant")
public class Restaurant extends BaseRestaurant {
	private static final long serialVersionUID = 1L;

	public static final String APP_VERSION = "app.version"; //$NON-NLS-1$
	public static final String APP_NUMERIC_VERSION = "app.numeric_version"; //$NON-NLS-1$
	public static final String APP_DB_VERSION = "app.db_version"; //$NON-NLS-1$

	/*[CONSTRUCTOR MARKER BEGIN]*/
	public Restaurant() {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Restaurant(java.lang.Integer id) {
		super(id);
	}

	/*[CONSTRUCTOR MARKER END]*/

	private String defaultOutletId;

	@Override
	public String getName() {
		if (TrainingMode.isEnabled()) {
			return "Training Restaurant"; //$NON-NLS-1$
		}
		return super.getName();
	}

	@Override
	public String getCurrencyName() {
		String currencyName = super.getCurrencyName();
		if (StringUtils.isEmpty(currencyName)) {
			return "Sample Currency"; //$NON-NLS-1$
		}
		return currencyName;
	}

	@Override
	public String getCurrencySymbol() {
		String currencySymbol = super.getCurrencySymbol();
		if (StringUtils.isEmpty(currencySymbol)) {
			currencySymbol = "$"; //$NON-NLS-1$
		}
		return currencySymbol;
	}

	public void addProperty(String name, String value) {
		if (getProperties() == null) {
			setProperties(new HashMap<String, String>());
		}

		getProperties().put(name, value);
	}

	public boolean hasProperty(String key) {
		return getProperty(key) != null;
	}

	public String getProperty(String key) {
		if (getProperties() == null) {
			return null;
		}

		return getProperties().get(key);
	}

	public String getProperty(String key, String defaultValue) {
		if (getProperties() == null) {
			return null;
		}

		String string = getProperties().get(key);
		if (StringUtils.isEmpty(string)) {
			return defaultValue;
		}

		return string;
	}

	public void removeProperty(String propertyName) {
		Map<String, String> properties = getProperties();
		if (properties == null) {
			return;
		}

		properties.remove(propertyName);
	}

	public boolean isPropertyValueTrue(String propertyName) {
		String property = getProperty(propertyName);

		return POSUtil.getBoolean(property);
	}

	public int getAppNumericVersion() {
		return POSUtil.parseInteger(getProperty(APP_NUMERIC_VERSION));
	}

	public void setAppNumericVersion(int numericVersion) {
		addProperty(APP_NUMERIC_VERSION, String.valueOf(numericVersion));
	}

	public String getAppVersion() {
		return getProperty(APP_VERSION, ""); //$NON-NLS-1$
	}

	public void setAppVersion(String appVersion) {
		addProperty(APP_VERSION, appVersion);
	}

	public int getAppDbVersion() {
		return POSUtil.parseInteger(getProperty(APP_DB_VERSION));
	}

	public void setAppDbVersion(int numericVersion) {
		addProperty(APP_DB_VERSION, String.valueOf(numericVersion));
	}

	public String getDefaultOutletId() {
		return defaultOutletId;
	}

	public void setDefaultOutletId(String defaultOutletId) {
		this.defaultOutletId = defaultOutletId;
	}
}