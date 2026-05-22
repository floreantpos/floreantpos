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
package com.floreantpos.main;

import com.floreantpos.config.AppConfig;

/**
 * Manages Training Mode state for the POS terminal.
 * When enabled, the system uses a separate training database,
 * requires no PIN, and simulates payments without processing them.
 *
 * Training mode is toggled via a restart: the preference is persisted to
 * AppConfig and applied at startup before the database is initialized.
 */
public class TrainingMode {

    public static final String TRAINING_CONNECT_STRING =
            "jdbc:derby:database/derby-single/posdb-training;create=true"; //$NON-NLS-1$
    public static final String NORMAL_CONNECT_STRING =
            "jdbc:derby:database/derby-single/posdb"; //$NON-NLS-1$
    private static final String TRAINING_MODE_KEY = "training_mode"; //$NON-NLS-1$

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Persist the training mode preference to config. Takes effect on next restart.
     */
    public static void savePreference(boolean trainingEnabled) {
        AppConfig.put(TRAINING_MODE_KEY, trainingEnabled);
    }

    /**
     * Read the persisted preference and apply it (set connect string + enabled flag).
     * Must be called before DatabaseUtil.initialize() at startup.
     */
    public static void loadAndApply() {
        boolean pref = AppConfig.getBoolean(TRAINING_MODE_KEY, false);
        if (pref) {
            AppConfig.setConnectString(TRAINING_CONNECT_STRING);
            enabled = true;
        } else {
            enabled = false;
        }
    }
}
