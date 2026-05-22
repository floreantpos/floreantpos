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

import javax.xml.bind.annotation.XmlRootElement;

import com.floreantpos.model.base.BaseTerminal;

@XmlRootElement(name = "terminal")
public class Terminal extends BaseTerminal {
	private static final long serialVersionUID = 1L;
	
	public final static String PROP_AUTO_LOGIN_ENABLE = "auto.login.enable";
	public final static String PROP_AUTO_LOGIN_USER_AUTO_ID = "auto.login.userAutoId";

	/*[CONSTRUCTOR MARKER BEGIN]*/
	public Terminal () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Terminal (java.lang.Integer id) {
		super(id);
	}

	/*[CONSTRUCTOR MARKER END]*/

	/**
	 * Return the value associated with the column: HAS_CASH_DRAWER
	 */
	@Override
	public java.lang.Boolean isHasCashDrawer() {
		return hasCashDrawer == null ? Boolean.FALSE : hasCashDrawer;
	}
	
	public boolean isCashDrawerAssigned() {
		return getAssignedUser() != null;
	}

	@Override
	public String toString() {
		return getName();
	}
	
	public void putProperty(String name, String value) {
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
}