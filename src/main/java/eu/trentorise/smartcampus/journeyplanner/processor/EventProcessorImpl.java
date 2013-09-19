/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.journeyplanner.processor;

import it.sayservice.platform.client.DomainUpdateListener;
import it.sayservice.platform.core.message.Core.DomainEvent;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.trentorise.smartcampus.journeyplanner.controller.rest.JourneyPlannerController;
import eu.trentorise.smartcampus.journeyplanner.util.HTTPConnector;

public class EventProcessorImpl implements DomainUpdateListener {

	private static final int RECENT_NOTIFICATION_TIME_DIFFERENCE = 1000 * 60 * 2;
	private static final String ALERT_DELAY = "alertDelay";
	private static final String ALERT_STRIKE = "alertStrike";
	private static final String ALERT_PARKING = "alertParking";
	private static final String ALERT_ACCIDENT = "alertAccident";

	private static final String CUSTOM = "CUSTOM";

	public static final String ITINERARY_OBJECT = "smartcampus.services.journeyplanner.ItineraryObject";
	public static final String RECURRENT_JOURNEY_OBJECT = "smartcampus.services.journeyplanner.RecurrentJourneyObject";
	public static final String ALERT_FACTORY = "smartcampus.services.journeyplanner.AlertFactory";
	public static final String TRAINS_ALERT_SENDER = "smartcampus.services.journeyplanner.TrainsAlertsSender";
	public static final String PARKING_ALERT_SENDER = "smartcampus.services.journeyplanner.ParkingAlertsSender";
	public static final String ROAD_ALERT_SENDER = "smartcampus.services.journeyplanner.RoadAlertSender";

	@Autowired
	@Value("${otp.url}")
	private String otpURL;

	private static Log logger = LogFactory.getLog(EventProcessorImpl.class);

	public void onDomainEvents(String subscriptionId, List<DomainEvent> events) {
		for (DomainEvent event : events) {
			 if (event.getDoType().equals(ALERT_FACTORY) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getDoType().equals(TRAINS_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getDoType().equals(PARKING_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (event.getAllTypesList().contains(ROAD_ALERT_SENDER) && event.getEventType().equals(CUSTOM)) {
				try {
					forwardEvent(event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void forwardEvent(DomainEvent e) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(e.getPayload(), Map.class);
		
		if (e.getEventSubtype().equals("alertStrike")) {
			AlertStrike alert = mapper.convertValue(map.get("alert"), AlertStrike.class);
			// TODO, need stopId?
//			alert.setId();
			String req = mapper.writeValueAsString(alert);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAS", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);
		} else if (e.getEventSubtype().equals("alertDelay")) {
			AlertDelay alert = mapper.convertValue(map.get("alert"), AlertDelay.class);
			String req = mapper.writeValueAsString(alert);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAD", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);			
		} if (e.getEventSubtype().equals("alertAllParking")) {
			AlertParking alert = mapper.convertValue(map.get("alert"), AlertParking.class);
			String req = mapper.writeValueAsString(alert);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAP", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);	
		} if (e.getEventSubtype().equals("alertAccident")) {
			AlertAccident alert = mapper.convertValue(map.get("alert"), AlertAccident.class);
			String req = mapper.writeValueAsString(alert);
			String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAE", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
			logger.info(result);	
		} if (e.getEventSubtype().equals("sendRoadAlerts")) {
			AlertRoad[] alerts = mapper.convertValue(map.get("data"), AlertRoad[].class);
			if (alerts != null) {
				for (AlertRoad alertRoad : alerts) {
					String req = mapper.writeValueAsString(alertRoad);
					String result = HTTPConnector.doPost(otpURL + JourneyPlannerController.SMARTPLANNER + "updateAR", req, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
					logger.info(result);	
				}
			}
		}
	}
}
