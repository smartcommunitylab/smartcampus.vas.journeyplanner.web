package eu.trentorise.smartcampus.journeyplanner.util;

public class ConnectorException extends Exception {

	private int code;
	
	public ConnectorException() {
	}

	public ConnectorException(String message, int code) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	

}
