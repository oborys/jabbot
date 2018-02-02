package org.wanna.jabbot.web.services;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Status {
	public enum StatusType {
		STOPPED,
		STARTED,
		CONNECTED
	}

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String id;
	private StatusType status;
	private Date lastUpdated;
	private Date lastConnectionAttempt;
	private Date lastConnected;

	public Status() {
		this.status = StatusType.STOPPED;
		this.lastUpdated = new Date();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


	public StatusType getStatus() {
		return status;
	}

	public void setStatus(StatusType status) {
		if(this.status != status){
			this.status = status;
			switch(status){
				case STOPPED: break;
				case STARTED:{
					lastConnectionAttempt = new Date();
					break;
				}
				case CONNECTED:{
					lastConnected = new Date();
					break;
				}
				default:{
					break;
				}
			}
		}
		this.lastUpdated = new Date();
	}

	public String getLastUpdated(){
		return sdf.format(lastUpdated);
	}

	public String getLastConnectionAttempt() {
		if(lastConnectionAttempt == null){
			return null;
		}else{
			return sdf.format(lastConnectionAttempt);
		}
	}

	public String getLastConnection(){
		if(lastConnected == null){
			return null;
		}else{
			return sdf.format(lastConnected);
		}
	}
}
