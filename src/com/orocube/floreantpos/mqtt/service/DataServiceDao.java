package com.orocube.floreantpos.mqtt.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.floreantpos.PosLog;
import com.floreantpos.main.Application;
import com.floreantpos.model.OnlineOrder;
import com.floreantpos.model.Terminal;
import com.floreantpos.model.Ticket;
import com.floreantpos.model.TicketItem;
import com.floreantpos.model.TicketItemModifier;
import com.floreantpos.model.dao.OnlineOrderDAO;
import com.floreantpos.model.dao.TerminalDAO;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.orocube.floreantpos.mqtt.MqttSender;
import com.orocube.floreantpos.mqtt.service.server.DataService;

public class DataServiceDao implements DataService {
	private static final String GSON_PARSING_DATE_FORMAT = "MMM dd, yyyy HH:mm:ss.SSSZ"; //$NON-NLS-1$
	public static final String DATA = "data"; //$NON-NLS-1$

	public List<Object> saveOrUpdateTickets(String request, MqttSender mqttSender) throws Exception {
		return saveOrUpdateTickets(request, true, mqttSender);
	}

	public List<Object> saveOrUpdateTickets(String request, boolean shouldPublishMqtt, MqttSender mqttSender) throws Exception {
		if (mqttSender == MqttSender.WOOCOMMERCE) {
			JSONObject rootElement = new JSONObject(request);
			String ticketJson = (String) rootElement.get(DATA);
			PosLog.debug(getClass(), "Request received: " + request); //$NON-NLS-1$
			Ticket ticket = OrgJsonUtil.fromWooCommerceJsonToTicket(ticketJson);
			return Arrays.asList(saveOrUpdateTicket(new JSONObject(ticketJson), ticket, mqttSender));
		}
		else {
			JSONObject rootElement = new JSONObject(request);
			JSONArray ticketsArray = rootElement.getJSONArray(DATA);
			PosLog.debug(getClass(), "Request received: " + request); //$NON-NLS-1$
			return populateAndSaveOrUpdateTickets(ticketsArray.toString(), mqttSender);
		}
	}

	public List<Object> populateAndSaveOrUpdateTickets(String request, MqttSender mqttSender) throws Exception {
		if (request == null)
			return null;

		List<Object> tickets = new ArrayList<>();
		JsonParser jsonParser = new JsonParser();
		JsonArray ja = (JsonArray) jsonParser.parse(request);
		ja.remove(new JsonPrimitive("classType"));
		JSONArray jsonArray = new JSONArray(ja.toString());

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject ticketJson = jsonArray.getJSONObject(i);
			Ticket ticket = saveOrUpdateTicket(ticketJson, mqttSender);
			if (ticket != null) {
				tickets.add(ticket);
			}
		}
		PosLog.debug(getClass(), "Successfully saved order history"); //$NON-NLS-1$
		return tickets;

	}

	public Ticket saveOrUpdateTicket(JSONObject ticketJson, MqttSender mqttSender) throws Exception {
		if (mqttSender == MqttSender.ONLINE_ORDER) {
			Ticket ticket = (Ticket) OrgJsonUtil.fromJson(ticketJson, Ticket.class);
			OrgJsonUtil.loadUnresolvedProperties(ticket, ticketJson);
			return saveOrUpdateTicket(ticketJson, ticket, mqttSender);
		}
		else if (mqttSender == MqttSender.WOOCOMMERCE) {
			Ticket ticket = OrgJsonUtil.fromWooCommerceJsonObjectToTicket(ticketJson);
			return saveOrUpdateTicket(ticketJson, ticket, mqttSender);
		}
		return null;
	}

	public Ticket saveOrUpdateTicket(JSONObject ticketJson, Ticket ticket, MqttSender mqttSender) throws Exception {
		Ticket existingTicket = null;
		boolean newOrderHistory = false;

		String id = "";
		String outletId = null;
		if (mqttSender == MqttSender.ONLINE_ORDER) {
			id = ticketJson.getString("id"); //$NON-NLS-1$
			outletId = ticketJson.getString("outletId"); //$NON-NLS-1$
		}
		else if (mqttSender == MqttSender.WOOCOMMERCE) {
			id = ticket.getGlobalId();
		}

		Object customerIdObj = ticketJson.has("customerId") ? ticketJson.get("customerId") : null; //$NON-NLS-1$
		String customerId = customerIdObj != null ? customerIdObj.toString() : null;
		OnlineOrder orderHistory = OnlineOrderDAO.getInstance().get(id);
		if (orderHistory == null) {
			orderHistory = new OnlineOrder(id);
			orderHistory.setTicketId(id);
			newOrderHistory = true;
		}
		else {
			try {
				if (mqttSender == MqttSender.ONLINE_ORDER) {
					existingTicket = OrgJsonUtil.fromJsonToTicket(orderHistory.getTicketJson());
					if (existingTicket != null && ticket.getVersion() == existingTicket.getVersion()) {
						PosLog.info(getClass(), "Ticket #" + orderHistory.getTicketId() + " already updated.");
						return null;
					}
				}
				else if (mqttSender == MqttSender.WOOCOMMERCE) {
					existingTicket = OrgJsonUtil.fromWooCommerceJsonToTicket(orderHistory.getTicketJson());
				}

			} catch (Exception e) {
				PosLog.error(getClass(), e);
			}
		}
		Terminal currentTerminal = Application.getInstance().getTerminal();
		for (TicketItem ticketItem : ticket.getTicketItems()) {
			if (ticketItem.getId() != null && ticketItem.getId() == 0) {
				ticketItem.setId(null);
			}
			List<TicketItemModifier> ticketItemModifiers = ticketItem.getTicketItemModifiers();
			if (ticketItemModifiers != null && ticketItemModifiers.size() > 0) {
				for (TicketItemModifier ticketItemModifier : ticketItemModifiers) {
					ticketItemModifier.setId(null);
					ticketItemModifier.setTicketItem(ticketItem);
				}
			}
		}
		if (ticket.getTerminal() == null && currentTerminal != null) {
			ticket.setTerminal(currentTerminal);
		}
		for (TicketItem ticketItem : ticket.getTicketItems()) {
			ticketItem.setTicket(ticket);
		}
		orderHistory.setTicketJson(ticketJson.toString());
		orderHistory.setOrderDate(ticket.getCreateDate());
		orderHistory.setOutletId(outletId);
		orderHistory.setSource(mqttSender.name());
		orderHistory.setCustomerId(customerId);
		orderHistory.setClosed(ticket.isClosed());
		orderHistory.setPaid(ticket.isPaid());
		orderHistory.setLastUpdateTime(new Date());
		if (newOrderHistory) {
			OnlineOrderDAO.getInstance().save(orderHistory);
		}
		else {
			OnlineOrderDAO.getInstance().update(orderHistory);
		}
		return ticket;
	}

	public List<String> getTicketIds(String request, MqttSender mqttSender) throws Exception {
		if (request == null) {
			return null;
		}

		List<String> ticketIds = new ArrayList<String>();

		if (mqttSender == MqttSender.ONLINE_ORDER) {
			JSONObject rootElement = new JSONObject(request);
			JSONArray ticketsArray = rootElement.getJSONArray(DATA);
			PosLog.debug(getClass(), "Request received: " + request); //$NON-NLS-1$
			JsonParser jsonParser = new JsonParser();
			JsonArray ja = (JsonArray) jsonParser.parse(ticketsArray.toString());
			ja.remove(new JsonPrimitive("classType"));
			JSONArray jsonArray = new JSONArray(ja.toString());

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject ticketJson = jsonArray.getJSONObject(i);
				String id = ticketJson.getString("id"); //$NON-NLS-1$
				ticketIds.add(id);
			}
		}
		else if (mqttSender == MqttSender.WOOCOMMERCE) {

			JSONObject rootElement = new JSONObject(request);
			String ticketJson = (String) rootElement.get(DATA);
			PosLog.debug(getClass(), "Request received: " + request); //$NON-NLS-1$
			JSONObject convertWooCommerceJsonToJsonObject = OrgJsonUtil.convertWooCommerceJsonToJsonObject(ticketJson);
			String id = String.valueOf(convertWooCommerceJsonToJsonObject.getInt("id"));
			ticketIds.add(id);
		}

		return ticketIds;
	}

	public Object findByUUID(Class<?> referenceClass, String uuId) {
		Session session = null;
		try {
			session = TerminalDAO.getInstance().createNewSession();
			Criteria criteria = session.createCriteria(referenceClass);
			criteria.add(Restrictions.eq("uuid", uuId)); //$NON-NLS-1$
			criteria.setMaxResults(1);
			return criteria.uniqueResult();
		} finally {
			TerminalDAO.getInstance().closeSession(session);
		}
	}

	public static GsonBuilder getGsonBuilder() {
		return new GsonBuilder().setDateFormat(GSON_PARSING_DATE_FORMAT);
	}

	public static DataServiceDao getInstance() {
		return new DataServiceDao();
	}
}
