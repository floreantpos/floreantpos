package com.floreantpos.model.dao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.floreantpos.model.OnlineOrder;
import com.floreantpos.model.PaymentStatusFilter;
import com.floreantpos.swing.PaginatedTableModel;

public class OnlineOrderDAO extends BaseOnlineOrderDAO {

	/**
	 * Default constructor.  Can be used in place of getInstance()
	 */
	public OnlineOrderDAO() {
	}

	public int getNumOfOnlineOrders(String source) {
		Session session = null;
		try {
			session = createNewSession();
			Criteria criteria = session.createCriteria(OnlineOrder.class);
			if (StringUtils.isNotBlank(source)) {
				criteria.add(Restrictions.eq(OnlineOrder.PROP_SOURCE, source));
			}
			criteria.setProjection(Projections.rowCount());
			criteria.add(Restrictions.isNotNull(OnlineOrder.PROP_TICKET_JSON));
			updateCriteriaFilters(criteria);
			Number result = (Number) criteria.uniqueResult();
			int rowCount = result == null ? 0 : result.intValue();
			return rowCount;
		} finally {
			closeSession(session);
		}
	}

	public List<OnlineOrder> loadData(PaginatedTableModel tableModel, String source) {
		Session session = null;
		try {
			session = createNewSession();
			Criteria criteria = session.createCriteria(OnlineOrder.class);
			criteria.add(Restrictions.isNotNull(OnlineOrder.PROP_TICKET_JSON));
			updateCriteriaFilters(criteria);
			if (StringUtils.isNotBlank(source)) {
				criteria.add(Restrictions.eq(OnlineOrder.PROP_SOURCE, source));
			}
			criteria.addOrder(Order.desc(OnlineOrder.PROP_ORDER_DATE));
			criteria.setFirstResult(tableModel.getCurrentRowIndex());
			criteria.setMaxResults(tableModel.getPageSize());
			return criteria.list();
		} finally {
			closeSession(session);
		}
	}

	private void updateCriteriaFilters(Criteria criteria) {
		PaymentStatusFilter paymentStatusFilter = com.floreantpos.config.TerminalConfig.getPaymentStatusFilter();
		if (paymentStatusFilter == PaymentStatusFilter.OPEN) {
			criteria.add(Restrictions.eq(OnlineOrder.PROP_PAID, Boolean.FALSE));
			criteria.add(Restrictions.eq(OnlineOrder.PROP_CLOSED, Boolean.FALSE));
		}
		else if (paymentStatusFilter == PaymentStatusFilter.PAID) {
			criteria.add(Restrictions.eq(OnlineOrder.PROP_PAID, Boolean.TRUE));
			criteria.add(Restrictions.eq(OnlineOrder.PROP_CLOSED, Boolean.FALSE));
		}
		else if (paymentStatusFilter == PaymentStatusFilter.CLOSED) {
			criteria.add(Restrictions.eq(OnlineOrder.PROP_CLOSED, Boolean.TRUE));
		}
	}

	public int getNumTickets() {
		Session session = null;
		Criteria criteria = null;
		try {
			session = createNewSession();
			criteria = session.createCriteria(getReferenceClass());
			criteria.setProjection(Projections.rowCount());
			criteria.add(Restrictions.isNotNull(OnlineOrder.PROP_TICKET_JSON));
			Number rowCount = (Number) criteria.uniqueResult();
			if (rowCount != null) {
				return rowCount.intValue();

			}
			return 0;
		} finally {
			closeSession(session);
		}
	}

	public List<OnlineOrder> find(int currentRowIndex, int pageSize) {
		Session session = null;
		Criteria criteria = null;

		try {
			session = createNewSession();
			criteria = session.createCriteria(getReferenceClass());
			criteria.add(Restrictions.isNotNull(OnlineOrder.PROP_TICKET_JSON));
			criteria.setFirstResult(currentRowIndex);
			criteria.setMaxResults(pageSize);
			criteria.addOrder(Order.desc(OnlineOrder.PROP_ORDER_DATE));
			return criteria.list();
		} finally {
			closeSession(session);
		}
	}

	public OnlineOrder findByTicketId(String ticketId) {
		Session session = null;
		Criteria criteria = null;

		try {
			session = createNewSession();
			criteria = session.createCriteria(getReferenceClass());
			criteria.setMaxResults(1);
			criteria.add(Restrictions.eq(OnlineOrder.PROP_TICKET_ID, ticketId));
			criteria.addOrder(Order.desc(OnlineOrder.PROP_ORDER_DATE));
			return (OnlineOrder) criteria.uniqueResult();
		} finally {
			closeSession(session);
		}
	}

}