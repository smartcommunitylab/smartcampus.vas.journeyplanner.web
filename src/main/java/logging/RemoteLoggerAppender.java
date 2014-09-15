package logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import eu.trentorise.smartcampus.journeyplanner.util.HTTPConnector;

public class RemoteLoggerAppender extends AppenderSkeleton

{
	private String appId;

	private String logURL;

	public void close() {
		// TODO Auto-generated method stub

	}

	public boolean requiresLayout() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		try {
			if (logURL != null && appId != null) {
				String msg = convertLogMessage((String) event.getMessage());
				if (msg != null) {
					String pars = (event.getStartTime() / 1000) + "/" + appId + "/" + msg;
					String url = logURL + pars;
					String res = HTTPConnector.doPost(url, "", null, null);
					System.out.println(res + " " + pars);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("cannot connect to log server");
		}
	}

	private String convertLogMessage(String msg) {
		if ((msg.replaceAll("[^~]", "")).length() == 2) {
			return msg.replace("~", "/");
		} else {
			return null;
		}
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getLogURL() {
		return logURL;
	}

	public void setLogURL(String logURL) {
		this.logURL = logURL;
	}

}