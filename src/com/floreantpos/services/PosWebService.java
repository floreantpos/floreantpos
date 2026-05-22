package com.floreantpos.services;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;

import com.floreantpos.PosLog;
import com.floreantpos.config.TerminalConfig;
import com.floreantpos.model.Restaurant;
import com.floreantpos.model.dao.RestaurantDAO;
import com.orocube.common.util.TerminalUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class PosWebService {

	private static final String SERVICE_URL = TerminalConfig.getWebServiceUrl();

	public static final String LOG_SERVER_URL = "https://backoffice.p-os.com"; //$NON-NLS-1$
	public static final String SERVICE_ACCESSLOG_SAVE = "/service/accessLog/save"; //$NON-NLS-1$

	public PosWebService() {
	}

	public String getAvailableNewVersions(String terminalKey, String currentPosVersion) {
		try {
			Client client = Client.create();
			client.getProperties();
			MultivaluedMap map = new MultivaluedMapImpl();
			map.add("terminal_key", terminalKey); //$NON-NLS-1$
			map.add("pos_version", currentPosVersion); //$NON-NLS-1$
			WebResource webResource = client.resource(SERVICE_URL + "/public/posuser/update"); //$NON-NLS-1$
			ClientResponse response = webResource.accept("application/json").post(ClientResponse.class, map); //$NON-NLS-1$
			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus()); //$NON-NLS-1$
			}
			String versionInfo = response.getEntity(String.class);
			PosLog.info(getClass(), "\n============update info============"); //$NON-NLS-1$
			PosLog.info(getClass(), versionInfo);
			return versionInfo;

		} catch (Exception e) {
			//PosLog.error(getClass(), e);
		}
		return null;
	}

	public void doSendHeartBeat() {
		try {
			MultivaluedMap<String, String> map = new MultivaluedMapImpl();

			map.add("productId", "819278037"); //$NON-NLS-1$ //$NON-NLS-2$
			map.add("productName", "Floreant POS"); //$NON-NLS-1$ //$NON-NLS-2$
			map.add("terminalKey", TerminalUtil.getSystemUID()); //$NON-NLS-1$

			Restaurant restaurant = RestaurantDAO.getRestaurant();
			map.add("storeId", String.valueOf(restaurant.getUniqueId())); //$NON-NLS-1$
			map.add("storeName", restaurant.getName()); //$NON-NLS-1$

			String storeAddress = ""; //$NON-NLS-1$
			if (StringUtils.isNotBlank(restaurant.getAddressLine1())) {
				storeAddress += restaurant.getAddressLine1() + ", "; //$NON-NLS-1$
			}
			if (StringUtils.isNotBlank(restaurant.getAddressLine2())) {
				storeAddress += restaurant.getAddressLine2() + ", "; //$NON-NLS-1$
			}
			if (StringUtils.isNotBlank(restaurant.getAddressLine3())) {
				storeAddress += restaurant.getAddressLine3() + ", "; //$NON-NLS-1$
			}
			map.add("storeAddress", storeAddress); //$NON-NLS-1$

			String path = LOG_SERVER_URL + SERVICE_ACCESSLOG_SAVE;
			WebResource webResource = createWebClient().resource(path);
			webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, map);
		} catch (Exception ex) {
			//PosLog.error(PosWebService.class, ex.getMessage());
		}
	}

	private Client createWebClient() {
		Client client = Client.create();
		client.getProperties();
		return client;
	}

	public static PosWebService get() {
		return new PosWebService();
	}
}
