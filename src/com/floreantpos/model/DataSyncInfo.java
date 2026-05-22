package com.floreantpos.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true, value = {})
public class DataSyncInfo implements Serializable {

	private String ref;
	private Class beanClass;
	private Date lastUpdateTime;
	private Integer rowCount;
	private String outletId;
	private List<String> syncErrorIds;
	private String msg;
	private int responseCode;

	public DataSyncInfo() {
	}

	public DataSyncInfo(Class modelClass, String ref) {
		this.beanClass = modelClass;
		this.ref = ref;
	}

	public DataSyncInfo(Class modelClass, String ref, String outletId) {
		this.beanClass = modelClass;
		this.ref = ref;
		this.outletId = outletId;
	}

	public DataSyncInfo(Class modelClass, String ref, Date lastUpdateTime) {
		this.beanClass = modelClass;
		this.ref = ref;
		this.lastUpdateTime = lastUpdateTime;
	}

	public Class getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class beanClass) {
		this.beanClass = beanClass;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public Integer getRowCount() {
		return rowCount;
	}

	public void setRowCount(Integer rowCount) {
		this.rowCount = rowCount;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	@Override
	public String toString() {
		return ref;
	}

	public String getOutletId() {
		return outletId;
	}

	public void setOutletId(String outletId) {
		this.outletId = outletId;
	}

	public boolean hasOutlet() {
		return StringUtils.isNotBlank(outletId);
	}

	public List<String> getSyncErrorIds() {
		return syncErrorIds;
	}

	public void setSyncErrorIds(List<String> syncErrorIds) {
		this.syncErrorIds = syncErrorIds;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
