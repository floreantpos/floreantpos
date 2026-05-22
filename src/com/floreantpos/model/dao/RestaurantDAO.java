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
package com.floreantpos.model.dao;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;

import com.floreantpos.model.Restaurant;

public class RestaurantDAO extends BaseRestaurantDAO {

	/**
	 * Default constructor.  Can be used in place of getInstance()
	 */
	public RestaurantDAO() {
	}

	public Timestamp geTimestamp() {
		Query query = getSession().createQuery("select current_timestamp() from Restaurant");
		query.setFirstResult(0);
		query.setMaxResults(1);
		return (Timestamp) query.list().get(0);
	}

	public static Restaurant getRestaurant() {
		Restaurant restaurant = getInstance().get(Integer.valueOf(1));
		if (restaurant == null) {
			List<Restaurant> list = getInstance().findAll();
			if (list != null && list.size() > 0) {
				return list.get(0);
			}

			restaurant = new Restaurant(1);
			getInstance().save(restaurant);
			return restaurant;
		}
		return restaurant;
	}

	@Override
	protected Serializable save(Object obj, Session s) {
		createUuid(obj);
		return super.save(obj, s);
	}

	@Override
	protected void update(Object obj, Session s) {
		createUuid(obj);
		super.update(obj, s);
	}

	@Override
	protected void saveOrUpdate(Object obj, Session s) {
		createUuid(obj);
		super.saveOrUpdate(obj, s);
	}

	private void createUuid(Object obj) {
		if (obj instanceof Restaurant) {
			Restaurant restaurant = (Restaurant) obj;
			String uuid = restaurant.getUuid();
			if (StringUtils.isBlank(uuid)) {
				restaurant.setUuid(UUID.randomUUID().toString());
			}
		}
	}
}