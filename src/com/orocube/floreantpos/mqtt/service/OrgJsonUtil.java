package com.orocube.floreantpos.mqtt.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.floreantpos.model.Customer;
import com.floreantpos.model.Ticket;
import com.floreantpos.model.TicketItem;
import com.floreantpos.model.TicketStatus;
import com.floreantpos.util.NumberUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orocube.floreantpos.mqtt.MqttSender;
import com.orocube.floreantpos.mqtt.service.server.ForceIntDeserializer;
import com.orocube.floreantpos.mqtt.service.server.ForceStringDeserializer;

public class OrgJsonUtil {
	public static String getString(JSONObject jsonObject, String key) {
		if (!jsonObject.has(key)) {
			return null;
		}

		return jsonObject.getString(key);
	}

	public static Boolean getBoolean(JSONObject jsonObject, String key) {
		if (!jsonObject.has(key)) {
			return false;
		}

		return jsonObject.getBoolean(key);
	}

	public static Integer getInt(JSONObject jsonObject, String key) {
		if (!jsonObject.has(key)) {
			return 0;
		}

		return jsonObject.getInt(key);
	}

	public static Double getDouble(JSONObject jsonObject, String key) throws ParseException {
		if (!jsonObject.has(key)) {
			return 0d;
		}
		Object object = jsonObject.get(key);
		if (object instanceof String) {
			String s = (String) jsonObject.get(key);
			return NumberUtil.parse(s).doubleValue();
		}
		else if (object instanceof Integer) {
			Integer in = (Integer) jsonObject.get(key);
			return NumberUtil.parse(in.toString()).doubleValue();
		}
		else if (object instanceof Double) {
			Double s = (Double) jsonObject.get(key);
			return NumberUtil.roundToTwoDigit(s);
		}
		return jsonObject.getNumber(key).doubleValue();
	}

	public static Object fromJson(JSONObject dataJson, Class<?> type) throws Exception {
		return getObjectMapper().readValue(dataJson.toString(), type);
	}

	public static Ticket fromJsonToTicket(String ticketJson) throws Exception {
		JSONObject jsonObject = convertTicketToJsonObject(ticketJson);
		Ticket ticket = (Ticket) getObjectMapper().readValue(ticketJson, Ticket.class);
		loadUnresolvedProperties(ticket, jsonObject);
		return ticket;
	}

	public static JSONObject convertTicketToJsonObject(String ticketJson) {
		JsonParser jsonParser = new JsonParser();
		JsonObject ja = (JsonObject) jsonParser.parse(ticketJson);
		ja.remove("classType");
		//ja.remove("version");
		return new JSONObject(ja.toString());
	}

	public static void loadUnresolvedProperties(Ticket ticket, JSONObject jsonObject) {
		if (jsonObject.has("extraProperties")) {
			String extraProperties = jsonObject.getString("extraProperties");
			if (extraProperties != null && extraProperties.startsWith("{")) {
				JSONObject propertyContainer = new JSONObject(extraProperties);
				Set<String> keys = propertyContainer.keySet();
				for (String key : keys) {
					ticket.addProperty(key, propertyContainer.getString(key));
				}
			}
		}
		ticket.setGlobalId(jsonObject.getString("id"));
	}

	public String toJson(Ticket ticket) throws Exception {
		return getObjectMapper().writeValueAsString(ticket);
	}

	public static ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.setVisibility(VisibilityChecker.Std.defaultInstance());
		mapper.setDateFormat(new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SSSZ")); //$NON-NLS-1$
		mapper.registerModule(
				new SimpleModule().addDeserializer(String.class, new ForceStringDeserializer()).addDeserializer(Integer.class, new ForceIntDeserializer()));
		return mapper;
	}

	//WooCommerce code block
	public static Ticket fromWooCommerceJsonToTicket(String ticketJson) throws Exception {
		JSONObject jsonObject = convertWooCommerceJsonToJsonObject(ticketJson);
		return fromWooCommerceJsonObjectToTicket(jsonObject);
	}

	public static JSONObject convertWooCommerceJsonToJsonObject(String ticketJson) {
		return new JSONObject(ticketJson);
	}

	public static Ticket fromWooCommerceJsonObjectToTicket(JSONObject ticketJson) throws ParseException {
		Ticket ticket = new Ticket();
		ticket.setGlobalId(String.valueOf(ticketJson.getInt("id")));
		ticket.setCreateDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(ticketJson.getString("date_created")));
		JSONArray productArray = ticketJson.getJSONArray("line_items");
		if (productArray.length() > 0) {
			for (int i = 0; i < productArray.length(); i++) {
				JSONObject productObj = productArray.getJSONObject(i);
				TicketItem ticketItem = new TicketItem();
				ticketItem.setTicket(ticket);
				ticketItem.setMenuItemId(String.valueOf(productObj.getInt("product_id")));
				ticketItem.setName(productObj.getString("name"));
				ticketItem.setQuantity(productObj.getDouble("quantity"));
				ticketItem.setUnitPrice(productObj.getDouble("price"));
				ticket.addToticketItems(ticketItem);
			}
		}
		Integer customerId = ticketJson.getInt("customer_id");

		JSONObject jsonBilling = ticketJson.getJSONObject("billing");
		String firstName = jsonBilling.getString("first_name");
		String lastName = jsonBilling.getString("last_name");
		String address1 = jsonBilling.getString("address_1");
		String address2 = jsonBilling.getString("address_2");
		String city = jsonBilling.getString("city");
		String state = jsonBilling.getString("state");
		String phone = jsonBilling.getString("phone");
		String email = jsonBilling.getString("email");
		String country = jsonBilling.getString("country");

		Customer customer = new Customer(customerId);
		customer.setFirstName(firstName);
		customer.setLastName(lastName);
		customer.setEmail(email);
		customer.setMobileNo(phone);
		customer.setAddress(address1);

		StringBuilder deliveryAddress = new StringBuilder();
		if (StringUtils.isNotBlank(address1)) {
			deliveryAddress.append(address1);
		}
		if (StringUtils.isNotBlank(address2)) {
			deliveryAddress.append(", " + address1);
		}
		if (StringUtils.isNotBlank(city)) {
			deliveryAddress.append(", " + city);
		}
		if (StringUtils.isNotBlank(state)) {
			deliveryAddress.append(", " + state);
		}
		if (StringUtils.isNotBlank(country)) {
			deliveryAddress.append(", " + country);
		}
		ticket.setDeliveryAddress(deliveryAddress.toString());
		ticket.setCustomer(customer);
		ticket.setSource(MqttSender.WOOCOMMERCE.name());
		ticket.setTicketStatus(getStatus(ticketJson.getString("status")));
		ticket.calculatePrice();
		return ticket;
	}

	private static TicketStatus getStatus(String status) {
		switch (status) {
			case "processing":
				return TicketStatus.Pending;
			case "on-hold":
				return TicketStatus.HOLD;
			case "completed":
				return TicketStatus.Completed;
			case "cancelled":
				return TicketStatus.Canceled;
			case "failed":
				return TicketStatus.DeliveryFailed;

			case "pending-payment":
			case "refunded":
			case "checkout-draft":
			default:
				return TicketStatus.Unknown;
		}
	}

	public static String fromTicketStatus(String status) {
		switch (status) {
			case "Pending":
				return "processing";
			case "Confirmed":
			case "HOLD":
				return "on-hold";
			case "Completed":
				return "completed";
			case "Canceled":
				return "cancelled";
			case "DeliveryFailed":
				return "failed";

		}
		return "";
	}
}
