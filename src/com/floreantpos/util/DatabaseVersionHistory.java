package com.floreantpos.util;

public class DatabaseVersionHistory {
	/**
	 * Version 101:
	 * Added uuid field in Restaurant.
	 * Added source field in OnlineOrder.
	 * 
	 * Version 100:
	 * 
	 * remove version column from ticket, shopTable, shopTableStatus
	 * added properties (table :RESTAURANT_PROPERTIES) in Restaurant.
	 * 
	 */
	public final static int DATABASE_VERSION = 101;
}
