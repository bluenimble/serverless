package com.bluenimble.platform.servers.broker.listeners.impls;

import java.io.Serializable;

public class GenericObject implements Serializable {
	
	private static final long serialVersionUID = -4865184503811376208L;

	private String event;
	private Object transaction;
	private String [] channel;
	private boolean refreshPeer;
	private Object data;

	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public Object getTransaction() {
		return transaction;
	}
	public void setTransaction(Object transaction) {
		this.transaction = transaction;
	}
	public String[] getChannel() {
		return channel;
	}
	public void setChannel(String[] channel) {
		this.channel = channel;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public boolean isRefreshPeer() {
		return refreshPeer;
	}
	public void setRefreshPeer(boolean refreshPeer) {
		this.refreshPeer = refreshPeer;
	}
	
}
