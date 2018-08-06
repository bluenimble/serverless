package com.bluenimble.platform.servers.socketio;

public class Message {
	
	private String userName;
    private String message;

    public Message() {
    }

    public Message (String userName, String message) {
        this.userName = userName;
        this.message = message;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
