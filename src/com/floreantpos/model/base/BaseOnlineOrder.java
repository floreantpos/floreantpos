package com.floreantpos.model.base;

import java.lang.Comparable;
import java.io.Serializable;


/**
 * This is an object that contains data related to the ORDER_HISTORY table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="ORDER_HISTORY"
 */

public abstract class BaseOnlineOrder  implements Comparable, Serializable {

	public static String REF = "OrderHistory"; //$NON-NLS-1$
	public static String PROP_OUTLET_ID = "outletId"; //$NON-NLS-1$
	public static String PROP_EXPIRY_DATE = "expiryDate"; //$NON-NLS-1$
	public static String PROP_STORE_NAME = "storeName"; //$NON-NLS-1$
	public static String PROP_CUSTOMER_ID = "customerId"; //$NON-NLS-1$
	public static String PROP_PROPERTIES = "properties"; //$NON-NLS-1$
	public static String PROP_SOURCE = "source"; //$NON-NLS-1$
	public static String PROP_TICKET_JSON = "ticketJson"; //$NON-NLS-1$
	public static String PROP_ORDER_DATE = "orderDate"; //$NON-NLS-1$
	public static String PROP_ORDER_STATUS = "orderStatus"; //$NON-NLS-1$
	public static String PROP_TICKET_ID = "ticketId"; //$NON-NLS-1$
	public static String PROP_ORDER_TYPE = "orderType"; //$NON-NLS-1$
	public static String PROP_STORE_ID = "storeId"; //$NON-NLS-1$
	public static String PROP_LAST_UPDATE_TIME = "lastUpdateTime"; //$NON-NLS-1$
	public static String PROP_LAST_SYNC_TIME = "lastSyncTime"; //$NON-NLS-1$
	public static String PROP_STORE_SCHEMA = "storeSchema"; //$NON-NLS-1$
	public static String PROP_CLOSED = "closed"; //$NON-NLS-1$
	public static String PROP_PAID = "paid"; //$NON-NLS-1$
	public static String PROP_ID = "id"; //$NON-NLS-1$


	// constructors
	public BaseOnlineOrder () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseOnlineOrder (java.lang.String id) {
		this.setId(id);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.String id;

	 long version;

	// fields
			private java.util.Date lastUpdateTime;
				private java.util.Date lastSyncTime;
				private java.util.Date orderDate;
				private java.lang.String customerId;
				private java.lang.String storeId;
				private java.lang.String storeSchema;
				private java.lang.String storeName;
				private java.lang.String outletId;
				private java.lang.String ticketId;
				private java.lang.String orderType;
				private java.lang.String orderStatus;
				protected java.lang.Boolean paid;
				protected java.lang.Boolean closed;
				private java.util.Date expiryDate;
				protected java.lang.String source;
				private java.lang.String ticketJson;
				private java.lang.String properties;
	


	/**
	 * Return the unique identifier of this class
     * @hibernate.id
     *  generator-class="com.floreantpos.util.UUIdGenerator"
     *  column="ID"
     */
	public java.lang.String getId () {
		return id;
	}

	/**
	 * Set the unique identifier of this class
	 * @param id the new ID
	 */
	public void setId (java.lang.String id) {
		this.id = id;
		this.hashCode = Integer.MIN_VALUE;
	}



	/**
	 * Return the value associated with the column: VERSION_NO
	 */

	public long getVersion () {
			return version;
	}



	/**
	 * Set the value related to the column: VERSION_NO
	 * @param version the VERSION_NO value
	 */
			public void setVersion (long version) {
			this.version = version;
	}




	/**
	 * Return the value associated with the column: LAST_UPDATE_TIME
	 */

	public java.util.Date getLastUpdateTime () {
			return lastUpdateTime;
	}



	/**
	 * Set the value related to the column: LAST_UPDATE_TIME
	 * @param lastUpdateTime the LAST_UPDATE_TIME value
	 */
			public void setLastUpdateTime (java.util.Date lastUpdateTime) {
			this.lastUpdateTime = lastUpdateTime;
	}



	/**
	 * Return the value associated with the column: LAST_SYNC_TIME
	 */

	public java.util.Date getLastSyncTime () {
			return lastSyncTime;
	}



	/**
	 * Set the value related to the column: LAST_SYNC_TIME
	 * @param lastSyncTime the LAST_SYNC_TIME value
	 */
			public void setLastSyncTime (java.util.Date lastSyncTime) {
			this.lastSyncTime = lastSyncTime;
	}



	/**
	 * Return the value associated with the column: ORDER_DATE
	 */

	public java.util.Date getOrderDate () {
			return orderDate;
	}



	/**
	 * Set the value related to the column: ORDER_DATE
	 * @param orderDate the ORDER_DATE value
	 */
			public void setOrderDate (java.util.Date orderDate) {
			this.orderDate = orderDate;
	}



	/**
	 * Return the value associated with the column: CUST_ID
	 */

	public java.lang.String getCustomerId () {
			return customerId;
	}



	/**
	 * Set the value related to the column: CUST_ID
	 * @param customerId the CUST_ID value
	 */
			public void setCustomerId (java.lang.String customerId) {
			this.customerId = customerId;
	}



	/**
	 * Return the value associated with the column: STORE_ID
	 */

	public java.lang.String getStoreId () {
			return storeId;
	}



	/**
	 * Set the value related to the column: STORE_ID
	 * @param storeId the STORE_ID value
	 */
			public void setStoreId (java.lang.String storeId) {
			this.storeId = storeId;
	}



	/**
	 * Return the value associated with the column: STORE_SCHEMA
	 */

	public java.lang.String getStoreSchema () {
			return storeSchema;
	}



	/**
	 * Set the value related to the column: STORE_SCHEMA
	 * @param storeSchema the STORE_SCHEMA value
	 */
			public void setStoreSchema (java.lang.String storeSchema) {
			this.storeSchema = storeSchema;
	}



	/**
	 * Return the value associated with the column: STORE_NAME
	 */

	public java.lang.String getStoreName () {
			return storeName;
	}



	/**
	 * Set the value related to the column: STORE_NAME
	 * @param storeName the STORE_NAME value
	 */
			public void setStoreName (java.lang.String storeName) {
			this.storeName = storeName;
	}



	/**
	 * Return the value associated with the column: OUTLET_ID
	 */

	public java.lang.String getOutletId () {
			return outletId;
	}



	/**
	 * Set the value related to the column: OUTLET_ID
	 * @param outletId the OUTLET_ID value
	 */
			public void setOutletId (java.lang.String outletId) {
			this.outletId = outletId;
	}



	/**
	 * Return the value associated with the column: TICKET_ID
	 */

	public java.lang.String getTicketId () {
			return ticketId;
	}



	/**
	 * Set the value related to the column: TICKET_ID
	 * @param ticketId the TICKET_ID value
	 */
			public void setTicketId (java.lang.String ticketId) {
			this.ticketId = ticketId;
	}



	/**
	 * Return the value associated with the column: ORDER_TYPE
	 */

	public java.lang.String getOrderType () {
			return orderType;
	}



	/**
	 * Set the value related to the column: ORDER_TYPE
	 * @param orderType the ORDER_TYPE value
	 */
			public void setOrderType (java.lang.String orderType) {
			this.orderType = orderType;
	}



	/**
	 * Return the value associated with the column: ORDER_STATUS
	 */

	public java.lang.String getOrderStatus () {
			return orderStatus;
	}



	/**
	 * Set the value related to the column: ORDER_STATUS
	 * @param orderStatus the ORDER_STATUS value
	 */
			public void setOrderStatus (java.lang.String orderStatus) {
			this.orderStatus = orderStatus;
	}

	/**
	 * Return the value associated with the column: PAID
	 */
	public java.lang.Boolean isPaid () {
								return paid == null ? Boolean.FALSE : paid;
					}

	/**
	 * Set the value related to the column: PAID
	 * @param paid the PAID value
	 */
	public void setPaid (java.lang.Boolean paid) {
		this.paid = paid;
	}

	/**
	 * Return the value associated with the column: SETTLED
	 */
	public java.lang.Boolean isClosed () {
								return closed == null ? Boolean.FALSE : closed;
					}

	/**
	 * Set the value related to the column: SETTLED
	 * @param closed the SETTLED value
	 */
	public void setClosed (java.lang.Boolean closed) {
		this.closed = closed;
	}


	/**
	 * Return the value associated with the column: EXPIRY_DATE
	 */

	public java.util.Date getExpiryDate () {
			return expiryDate;
	}



	/**
	 * Set the value related to the column: EXPIRY_DATE
	 * @param expiryDate the EXPIRY_DATE value
	 */
			public void setExpiryDate (java.util.Date expiryDate) {
			this.expiryDate = expiryDate;
	}


			/**
			 * Return the value associated with the column: SOURCE
			 */
			public java.lang.String getSource () {
							return source;
					}

			/**
			 * Set the value related to the column: SOURCE
			 * @param source the SOURCE value
			 */
			public void setSource (java.lang.String source) {
				this.source = source;
			}


	/**
	 * Return the value associated with the column: TICKET_JSON
	 */

	public java.lang.String getTicketJson () {
			return ticketJson;
	}



	/**
	 * Set the value related to the column: TICKET_JSON
	 * @param ticketJson the TICKET_JSON value
	 */
			public void setTicketJson (java.lang.String ticketJson) {
			this.ticketJson = ticketJson;
	}



	/**
	 * Return the value associated with the column: PROPERTIES
	 */

	public java.lang.String getProperties () {
			return properties;
	}



	/**
	 * Set the value related to the column: PROPERTIES
	 * @param properties the PROPERTIES value
	 */
			public void setProperties (java.lang.String properties) {
			this.properties = properties;
	}





	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof com.floreantpos.model.OnlineOrder)) return false;
		else {
			com.floreantpos.model.OnlineOrder orderHistory = (com.floreantpos.model.OnlineOrder) obj;
			if (null == this.getId() || null == orderHistory.getId()) return this == obj;
			else return (this.getId().equals(orderHistory.getId()));
		}
	}

	public int hashCode () {
		if (Integer.MIN_VALUE == this.hashCode) {
			if (null == this.getId()) return super.hashCode();
			else {
				String hashStr = this.getClass().getName() + ":" + this.getId().hashCode();
				this.hashCode = hashStr.hashCode();
			}
		}
		return this.hashCode;
	}

	public int compareTo (Object obj) {
		if (obj.hashCode() > hashCode()) return 1;
		else if (obj.hashCode() < hashCode()) return -1;
		else return 0;
	}

	public String toString () {
		return super.toString();
	}


}