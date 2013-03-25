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
package eu.trentorise.smartcampus.journeyplanner.controller.rest;

import it.sayservice.platform.client.DomainEngineClient;
import it.sayservice.platform.client.DomainObject;
import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.core.common.util.ServiceUtil;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.SimpleLeg;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.Transport;
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;
import it.sayservice.platform.smartplanner.data.message.journey.JourneyPlannerUserProfile;
import it.sayservice.platform.smartplanner.data.message.journey.JourneyRecurrence;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;
import it.sayservice.platform.smartplanner.data.message.journey.old.OldRecurrentJourneyParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.ac.provider.AcService;
import eu.trentorise.smartcampus.ac.provider.AcServiceException;
import eu.trentorise.smartcampus.ac.provider.filters.AcProviderFilter;
import eu.trentorise.smartcampus.ac.provider.model.User;
import eu.trentorise.smartcampus.journeyplanner.sync.BasicItinerary;
import eu.trentorise.smartcampus.journeyplanner.sync.BasicJourneyPlannerUserProfile;
import eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourney;
import eu.trentorise.smartcampus.journeyplanner.sync.OldBasicRecurrentJourneyParameters;
import eu.trentorise.smartcampus.journeyplanner.util.ConnectorException;
import eu.trentorise.smartcampus.journeyplanner.util.HTTPConnector;
import eu.trentorise.smartcampus.presentation.storage.BasicObjectStorage;

@Controller
public class JourneyPlannerController {

	@Autowired
	private DomainEngineClient domainClient;

	@Autowired
	private AcService acService;

	@Autowired
	private BasicObjectStorage storage;

	@Autowired
	@Value("${otp.url}")
	private String otpURL;

	private Logger log = Logger.getLogger(this.getClass());

	// private static final String OTP_LOCATION = "http://213.21.154.84:7070";
	// private static final String OTP_LOCATION = "http://localhost:7070";

	public static final String SMARTPLANNER = "/smart-planner/api-webapp/planner/";
	// http://localhost:7070

	private static final String PLAN = "plan";
	private static final String RECURRENT = "recurrentJourney";

	// no crud
	@RequestMapping(method = RequestMethod.POST, value = "/plansinglejourney")
	public @ResponseBody
	List<Itinerary> planSingleJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody SingleJourney journeyRequest) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			List<String> reqs = buildItineraryPlannerRequest(journeyRequest);

			ObjectMapper mapper = new ObjectMapper();

			List<Itinerary> itineraries = new ArrayList<Itinerary>();

			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + PLAN, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List its = mapper.readValue(plan, List.class);
				for (Object it : its) {
					Itinerary itinerary = mapper.convertValue(it, Itinerary.class);
					itineraries.add(itinerary);
				}
			}

			ItinerarySorter.sort(itineraries, journeyRequest.getRouteType());

			return itineraries;
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	private List<String> buildItineraryPlannerRequest(SingleJourney request) {
		List<String> reqs = new ArrayList<String>();
		for (TType type : request.getTransportTypes()) {
			int its = 1;
			if (type.equals(TType.TRANSIT)) {
				its = 3;
			}
			String req = String.format("from=%s,%s&to=%s,%s&date=%s&departureTime=%s&transportType=%s&numOfItn=%s", request.getFrom().getLat(), request.getFrom().getLon(), request.getTo().getLat(), request.getTo().getLon(), request.getDate(), request.getDepartureTime(), type, its);
			reqs.add(req);
		}

		return reqs;
		// String[] resp = new String[request.getTransportTypes().length];
		// return reqs.toArray(resp);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicItinerary")
	public @ResponseBody
	void saveItinerary(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody BasicItinerary itinerary) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("itinerary", itinerary.getData());
			String clientId = itinerary.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
			}
			pars.put("clientId", clientId);
			pars.put("userId", userId);
			pars.put("originalFrom", itinerary.getOriginalFrom());
			pars.put("originalTo", itinerary.getOriginalTo());
			pars.put("name", itinerary.getName());
			domainClient.invokeDomainOperation("saveItinerary", "smartcampus.services.journeyplanner.ItineraryFactory", "smartcampus.services.journeyplanner.ItineraryFactory.0", pars, userId, "vas_journeyplanner_subscriber");
			storage.storeObject(itinerary);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicItinerary")
	public @ResponseBody
	List<BasicItinerary> getItineraries(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws InvocationException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.ItineraryObject", pars, "vas_journeyplanner_subscriber");

			List<BasicItinerary> itineraries = new ArrayList<BasicItinerary>();
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			for (String r : res) {
				DomainObject obj = new DomainObject(r);
				BasicItinerary itinerary = mapper.convertValue(obj.getContent(), BasicItinerary.class);
				itineraries.add(itinerary);
			}

			return itineraries;
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicItinerary/{clientId}")
	public @ResponseBody
	BasicItinerary getItinerary(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId) throws InvocationException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			DomainObject res = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.ItineraryObject");
			if (res == null) {
				return null;
			}

			if (checkUser(res, userId) == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			Map<String, Object> content = res.getContent();
			BasicItinerary itinerary = mapper.convertValue(content, BasicItinerary.class);

			return itinerary;
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicItinerary/{clientId}")
	public @ResponseBody
	void deleteItinerary(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			String objectId = getObjectIdByClientId(clientId, "smartcampus.services.journeyplanner.ItineraryObject");

			if (objectId != null) {
				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("userId", userId);
				domainClient.invokeDomainOperation("deleteItinerary", "smartcampus.services.journeyplanner.ItineraryObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	// no crud
	@RequestMapping(method = RequestMethod.GET, value = "/monitoritinerary/{clientId}/{monitor}")
	public @ResponseBody
	boolean monitorItinerary(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId, @PathVariable boolean monitor) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return false;
			}

			String objectId = getObjectIdByClientId(clientId, "smartcampus.services.journeyplanner.ItineraryObject");

			if (objectId != null) {
				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("flag", monitor);
				pars.put("userId", userId);
				// domainClient.invokeDomainOperation("setMonitorFlag",
				// "smartcampus.services.journeyplanner.ItineraryObject", objectId,
				// pars, userId, "vas_journeyplanner_subscriber");
				byte[] b = (byte[]) domainClient.invokeDomainOperationSync("setMonitorFlag", "smartcampus.services.journeyplanner.ItineraryObject", objectId, pars, "vas_journeyplanner_subscriber");
				String s = (String) ServiceUtil.deserializeObject(b);
				return Boolean.parseBoolean(s);
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}

	// OLD RECURRENT

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourneyParameters")
	public @ResponseBody
	void oldSaveRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody OldBasicRecurrentJourneyParameters journeyRequest) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			OldRecurrentJourneyParameters parameters = journeyRequest.getData();

			RecurrentJourneyParameters newParameters = new RecurrentJourneyParameters();
			newParameters.setFrom(parameters.getFrom());
			newParameters.setFromDate(parameters.getFromDate());
			newParameters.setInterval(parameters.getInterval());
			newParameters.setResultsNumber(parameters.getResultsNumber());
			newParameters.setRouteType(parameters.getRouteType());
			newParameters.setTime(parameters.getTime());
			newParameters.setTo(parameters.getTo());
			newParameters.setToDate(parameters.getToDate());
			newParameters.setTransportTypes(parameters.getTransportTypes());
			List<Integer> recDays = new ArrayList<Integer>();
			if (parameters.getRecurrence() == JourneyRecurrence.EVERYDAY || parameters.getRecurrence() == JourneyRecurrence.WEEKENDS) {
				recDays.add(1);
				recDays.add(7);
			}
			if (parameters.getRecurrence() == JourneyRecurrence.EVERYDAY || parameters.getRecurrence() == JourneyRecurrence.WEEKDAYS) {
				for (int i = 2; i <= 6; i++) {
					recDays.add(i);
				}
			}
			newParameters.setRecurrence(recDays);

			List<String> reqs = buildRecurrentJourneyPlannerRequest(newParameters);
			List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
			ObjectMapper mapper = new ObjectMapper();
			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + RECURRENT, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List sl = mapper.readValue(plan, List.class);
				for (Object o : sl) {
					legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
				}
			}

			RecurrentJourney journey = new RecurrentJourney();
			journey.setParameters(newParameters);
			journey.setLegs(legs);
			journey.setMonitorLegs(buildMonitorMap(legs));

			BasicRecurrentJourney basicRecurrent = new BasicRecurrentJourney();
			basicRecurrent.setClientId(journeyRequest.getClientId());
			basicRecurrent.setData(journey);
			basicRecurrent.setMonitor(journeyRequest.isMonitor());
			basicRecurrent.setName(journeyRequest.getName());
			basicRecurrent.setUser(journeyRequest.getUser());
			basicRecurrent.setId(journeyRequest.getId());

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("recurrentJourney", basicRecurrent.getData());
			pars.put("name", basicRecurrent.getName());
			String clientId = basicRecurrent.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
			}
			pars.put("clientId", clientId);
			pars.put("userId", userId);
			pars.put("monitor", basicRecurrent.isMonitor());
			domainClient.invokeDomainOperation("saveRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyFactory", "smartcampus.services.journeyplanner.RecurrentJourneyFactory.0", pars, userId, "vas_journeyplanner_subscriber");
			storage.storeObject(basicRecurrent);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourneyParameters/{clientId}")
	public @ResponseBody
	void oldUpdateRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody OldBasicRecurrentJourneyParameters journeyRequest, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			OldRecurrentJourneyParameters parameters = journeyRequest.getData();

			RecurrentJourneyParameters newParameters = new RecurrentJourneyParameters();
			newParameters.setFrom(parameters.getFrom());
			newParameters.setFromDate(parameters.getFromDate());
			newParameters.setInterval(parameters.getInterval());
			newParameters.setResultsNumber(parameters.getResultsNumber());
			newParameters.setRouteType(parameters.getRouteType());
			newParameters.setTime(parameters.getTime());
			newParameters.setTo(parameters.getTo());
			newParameters.setToDate(parameters.getToDate());
			newParameters.setTransportTypes(parameters.getTransportTypes());
			List<Integer> recDays = new ArrayList<Integer>();
			if (parameters.getRecurrence() == JourneyRecurrence.EVERYDAY || parameters.getRecurrence() == JourneyRecurrence.WEEKENDS) {
				recDays.add(1);
				recDays.add(7);
			}
			if (parameters.getRecurrence() == JourneyRecurrence.EVERYDAY || parameters.getRecurrence() == JourneyRecurrence.WEEKDAYS) {
				for (int i = 2; i <= 6; i++) {
					recDays.add(i);
				}
			}
			newParameters.setRecurrence(recDays);

			List<String> reqs = buildRecurrentJourneyPlannerRequest(newParameters);
			List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
			ObjectMapper mapper = new ObjectMapper();
			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + RECURRENT, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List sl = mapper.readValue(plan, List.class);
				for (Object o : sl) {
					legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
				}
			}

			RecurrentJourney journey = new RecurrentJourney();
			journey.setParameters(newParameters);
			journey.setLegs(legs);
			journey.setMonitorLegs(buildMonitorMap(legs));

			BasicRecurrentJourney basicRecurrent = new BasicRecurrentJourney();
			basicRecurrent.setClientId(journeyRequest.getClientId());
			basicRecurrent.setData(journey);
			basicRecurrent.setMonitor(journeyRequest.isMonitor());
			basicRecurrent.setName(journeyRequest.getName());
			basicRecurrent.setUser(journeyRequest.getUser());
			basicRecurrent.setId(journeyRequest.getId());

			DomainObject res = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (res != null) {
				String objectId = checkUser(res, userId);
				if (objectId == null) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					return;
				}

				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("newJourney", journey);
				pars.put("newName", journeyRequest.getName());
				pars.put("newMonitor", journeyRequest.isMonitor());
				domainClient.invokeDomainOperation("updateRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourneyParameters/{clientId}")
	public @ResponseBody
	OldBasicRecurrentJourneyParameters oldGetRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId) throws InvocationException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			DomainObject obj = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (obj == null) {
				return null;
			}
			if (checkUser(obj, userId) == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			RecurrentJourney recurrent = mapper.convertValue(obj.getContent().get("data"), RecurrentJourney.class);

			OldBasicRecurrentJourneyParameters parameters = new OldBasicRecurrentJourneyParameters();

			RecurrentJourneyParameters newParameters = recurrent.getParameters();
			OldRecurrentJourneyParameters oldParameters = convertParametersToOld(newParameters);

			parameters.setData(oldParameters);
			parameters.setClientId((String) obj.getContent().get("clientId"));
			parameters.setName((String) obj.getContent().get("name"));
			parameters.setMonitor((Boolean) obj.getContent().get("monitor"));

			return parameters;

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourneyParameters")
	public @ResponseBody
	List<OldBasicRecurrentJourneyParameters> oldGetRecurrentJourneys(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws InvocationException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.RecurrentJourneyObject", pars, "vas_journeyplanner_subscriber");

			List<OldBasicRecurrentJourneyParameters> journeys = new ArrayList<OldBasicRecurrentJourneyParameters>();
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			for (String r : res) {
				DomainObject obj = new DomainObject(r);
				RecurrentJourney recurrent = mapper.convertValue(obj.getContent().get("data"), RecurrentJourney.class);

				OldBasicRecurrentJourneyParameters parameters = new OldBasicRecurrentJourneyParameters();

				RecurrentJourneyParameters newParameters = recurrent.getParameters();
				OldRecurrentJourneyParameters oldParameters = convertParametersToOld(newParameters);

				parameters.setData(oldParameters);
				parameters.setClientId((String) obj.getContent().get("clientId"));
				parameters.setName((String) obj.getContent().get("name"));
				parameters.setMonitor((Boolean) obj.getContent().get("monitor"));

				journeys.add(parameters);
			}

			return journeys;

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourneyParameters/{clientId}")
	public @ResponseBody
	void oldDeleteRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			String objectId = getObjectIdByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");

			if (objectId != null) {
				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("userId", userId);
				domainClient.invokeDomainOperation("deleteRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private OldRecurrentJourneyParameters convertParametersToOld(RecurrentJourneyParameters newParameters) {
		OldRecurrentJourneyParameters oldParameters = new OldRecurrentJourneyParameters();
		oldParameters.setFrom(newParameters.getFrom());
		oldParameters.setFromDate(newParameters.getFromDate());
		oldParameters.setInterval(newParameters.getInterval());
		oldParameters.setResultsNumber(newParameters.getResultsNumber());
		oldParameters.setRouteType(newParameters.getRouteType());
		oldParameters.setTime(newParameters.getTime());
		oldParameters.setTo(newParameters.getTo());
		oldParameters.setToDate(newParameters.getToDate());
		oldParameters.setTransportTypes(newParameters.getTransportTypes());
		List<Integer> recurrence = newParameters.getRecurrence();

		JourneyRecurrence oldRecurrence = null;
		for (int i : recurrence) {
			if (i >= 2 && i <= 6) {
				if (oldRecurrence == null) {
					oldRecurrence = JourneyRecurrence.WEEKDAYS;
				} else if (oldRecurrence == JourneyRecurrence.WEEKENDS) {
					oldRecurrence = JourneyRecurrence.EVERYDAY;
				}
			}
			if (i == 1 || i == 7) {
				if (oldRecurrence == null) {
					oldRecurrence = JourneyRecurrence.WEEKENDS;
				} else if (oldRecurrence == JourneyRecurrence.WEEKDAYS) {
					oldRecurrence = JourneyRecurrence.EVERYDAY;
				}
			}
		}
		oldParameters.setRecurrence(oldRecurrence);

		return oldParameters;
	}

	// RECURRENT

	@RequestMapping(method = RequestMethod.POST, value = "/planrecurrent")
	public @ResponseBody
	RecurrentJourney planRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @RequestBody RecurrentJourneyParameters parameters, HttpSession session) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
			List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
			ObjectMapper mapper = new ObjectMapper();
			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + RECURRENT, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List sl = mapper.readValue(plan, List.class);
				for (Object o : sl) {
					legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
				}
			}

			RecurrentJourney journey = new RecurrentJourney();
			journey.setParameters(parameters);
			journey.setLegs(legs);
			journey.setMonitorLegs(buildMonitorMap(legs));

			return journey;

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourney")
	public @ResponseBody
	void saveRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody BasicRecurrentJourney recurrent) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("recurrentJourney", recurrent.getData());
			pars.put("name", recurrent.getName());
			String clientId = recurrent.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
			}
			pars.put("clientId", clientId);
			pars.put("userId", userId);
			pars.put("monitor", recurrent.isMonitor());
			domainClient.invokeDomainOperation("saveRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyFactory", "smartcampus.services.journeyplanner.RecurrentJourneyFactory.0", pars, userId, "vas_journeyplanner_subscriber");
			storage.storeObject(recurrent);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/planrecurrent/{clientId}")
	public @ResponseBody
	RecurrentJourney planRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody RecurrentJourneyParameters parameters, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			List<String> reqs = buildRecurrentJourneyPlannerRequest(parameters);
			List<SimpleLeg> legs = new ArrayList<SimpleLeg>();
			ObjectMapper mapper = new ObjectMapper();
			for (String req : reqs) {
				String plan = HTTPConnector.doGet(otpURL + SMARTPLANNER + RECURRENT, req, MediaType.APPLICATION_JSON, null, "UTF-8");
				List sl = mapper.readValue(plan, List.class);
				for (Object o : sl) {
					legs.add((SimpleLeg) mapper.convertValue(o, SimpleLeg.class));
				}
			}

			DomainObject res = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (res != null) {
				String objectId = checkUser(res, userId);
				if (objectId == null) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				} else {
					RecurrentJourney oldJourney = mapper.convertValue(res.getContent().get("data"), RecurrentJourney.class);
					RecurrentJourney journey = new RecurrentJourney();
					journey.setParameters(parameters);
					journey.setLegs(legs);
					journey.setMonitorLegs(buildMonitorMap(legs, oldJourney.getMonitorLegs()));
					return journey;
				}
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return null;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourney/{clientId}")
	public @ResponseBody
	void updateRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody BasicRecurrentJourney recurrent, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			String objectClientId = recurrent.getClientId();
			if (!clientId.equals(objectClientId)) {
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				return;
			}
			DomainObject res = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (res != null) {
				String objectId = checkUser(res, userId);
				if (objectId == null) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					return;
				}

				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("newJourney", recurrent.getData());
				pars.put("newName", recurrent.getName());
				pars.put("newMonitor", recurrent.isMonitor());
				domainClient.invokeDomainOperation("updateRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private Map<String, Boolean> buildMonitorMap(List<SimpleLeg> legs) {
		Map<String, Boolean> result = new TreeMap<String, Boolean>();
		Map<String, Transport> transports = new TreeMap<String, Transport>();

		for (SimpleLeg leg : legs) {
			Transport transport = leg.getTransport();
			if (transport.getType() != TType.BUS && transport.getType() != TType.TRAIN) {
				continue;
			}
			String id = transport.getAgencyId() + "_" + transport.getRouteId();
			if (!result.containsKey(id)) {
				result.put(id, true);
			}
		}

		return result;
	}

	private Map<String, Boolean> buildMonitorMap(List<SimpleLeg> legs, Map<String, Boolean> old) {
		Map<String, Boolean> result = new TreeMap<String, Boolean>();
		Map<String, Transport> transports = new TreeMap<String, Transport>();

		for (SimpleLeg leg : legs) {
			Transport transport = leg.getTransport();
			if (transport.getType() != TType.BUS && transport.getType() != TType.TRAIN) {
				continue;
			}
			String id = transport.getAgencyId() + "_" + transport.getRouteId();
			if (!result.containsKey(id)) {
				if (old.containsKey(id)) {
					result.put(id, old.get(id));
				} else {
					result.put(id, true);
				}
			}
		}

		return result;
	}

	private List<String> buildRecurrentJourneyPlannerRequest(RecurrentJourneyParameters request) {
		List<String> reqs = new ArrayList<String>();
		for (TType type : request.getTransportTypes()) {
			String rec = request.getRecurrence().toString().replaceAll("[\\[\\] ]", "");
			String req = String.format("recurrence=%s&from=%s&to=%s&time=%s&interval=%s&transportType=%s&routeType=%s&fromDate=%s&toDate=%s&numOfItn=%s", rec, request.getFrom().toLatLon(), request.getTo().toLatLon(), request.getTime(), request.getInterval(), type, request.getRouteType(), request.getFromDate(), request.getToDate(), request.getResultsNumber());
			reqs.add(req);
		}
		return reqs;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourney")
	public @ResponseBody
	List<BasicRecurrentJourney> getRecurrentJourneys(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws InvocationException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();
			pars.put("userId", userId);
			List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.RecurrentJourneyObject", pars, "vas_journeyplanner_subscriber");

			List<BasicRecurrentJourney> journeys = new ArrayList<BasicRecurrentJourney>();
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			for (String r : res) {
				DomainObject obj = new DomainObject(r);
				RecurrentJourney recurrent = mapper.convertValue(obj.getContent().get("data"), RecurrentJourney.class);
				BasicRecurrentJourney recurrentJourney = new BasicRecurrentJourney();
				String clientId = (String) obj.getContent().get("clientId");
				recurrentJourney.setData(recurrent);
				recurrentJourney.setClientId(clientId);
				recurrentJourney.setName((String) obj.getContent().get("name"));
				recurrentJourney.setMonitor((Boolean) obj.getContent().get("monitor"));
				recurrentJourney.setUser((String) obj.getContent().get("userId"));

				journeys.add(recurrentJourney);
			}

			return journeys;

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourney/{clientId}")
	public @ResponseBody
	BasicRecurrentJourney getRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId) throws InvocationException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			DomainObject obj = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			if (obj == null) {
				return null;
			}
			if (checkUser(obj, userId) == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			Map<String, Object> content = obj.getContent();
			RecurrentJourney recurrent = mapper.convertValue(content.get("data"), RecurrentJourney.class);
			BasicRecurrentJourney recurrentJourney = new BasicRecurrentJourney();
			String objectClientId = (String) content.get("clientId");
			if (!clientId.equals(objectClientId)) {
				response.setStatus(HttpServletResponse.SC_CONFLICT);
				return null;
			}
			recurrentJourney.setData(recurrent);
			recurrentJourney.setClientId(clientId);
			recurrentJourney.setName((String) obj.getContent().get("name"));
			recurrentJourney.setMonitor((Boolean) obj.getContent().get("monitor"));
			recurrentJourney.setUser((String) obj.getContent().get("userId"));

			return recurrentJourney;
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourney/{clientId}")
	public @ResponseBody
	void deleteRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			String objectId = getObjectIdByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");

			if (objectId != null) {
				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("userId", userId);
				domainClient.invokeDomainOperation("deleteRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	// no crud
	@RequestMapping(method = RequestMethod.GET, value = "/monitorrecurrentjourney/{clientId}/{monitor}")
	public @ResponseBody
	boolean monitorRecurrentJourney(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String clientId, @PathVariable boolean monitor) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return false;
			}

			String objectId = getObjectIdByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");

			if (objectId != null) {

				Map<String, Object> pars = new HashMap<String, Object>();
				pars.put("flag", monitor);
				pars.put("userId", userId);
				// domainClient.invokeDomainOperation("setMonitorFlag",
				// "smartcampus.services.journeyplanner.RecurrentJourneyObject",
				// objectId, pars, userId, "vas_journeyplanner_subscriber");
				byte[] b = (byte[]) domainClient.invokeDomainOperationSync("setMonitorFlag", "smartcampus.services.journeyplanner.RecurrentJourneyObject", objectId, pars, "vas_journeyplanner_subscriber");
				String s = (String) ServiceUtil.deserializeObject(b);
				return Boolean.parseBoolean(s);
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return false;
	}

	// ALERTS

	// no crud
	@RequestMapping(method = RequestMethod.POST, value = "/submitalert")
	public @ResponseBody
	void submitAlert(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody Map<String, Object> map) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			AlertType type = AlertType.getAlertType((String) map.get("type"));

			ObjectMapper mapper = new ObjectMapper();

			Alert alert = null;
			String method = "";
			Map<String, Object> contentMap = (Map<String, Object>) map.get("content");
			switch (type) {
			case ACCIDENT:
				alert = mapper.convertValue(contentMap, AlertAccident.class);
				method = "submitAlertAccident";
				break;
			case DELAY:
				alert = mapper.convertValue(contentMap, AlertDelay.class);
				method = "submitAlertDelay";
				break;
			case PARKING:
				alert = mapper.convertValue(contentMap, AlertParking.class);
				method = "submitAlertParking";
				break;
			case STRIKE:
				alert = mapper.convertValue(contentMap, AlertStrike.class);
				method = "submitAlertStrike";
				break;
			}

			alert.setType(type);
			alert.setCreatorId(userId);
			alert.setCreatorType(CreatorType.USER);

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("newAlert", alert);
			// pars.put("userId", userId);
			domainClient.invokeDomainOperation(method, "smartcampus.services.journeyplanner.AlertFactory", "smartcampus.services.journeyplanner.AlertFactory.0", pars, userId, "vas_journeyplanner_subscriber");
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	// PROFILE

	@RequestMapping(method = RequestMethod.POST, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicJourneyPlannerUserProfile")
	public @ResponseBody
	void createUserProfile(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			BasicJourneyPlannerUserProfile profile = new BasicJourneyPlannerUserProfile();

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("userProfile", profile.getData());
			String clientId = profile.getClientId();
			if (clientId == null) {
				clientId = new ObjectId().toString();
			}
			pars.put("clientId", clientId);
			domainClient.invokeDomainOperation("createUserProfile", "smartcampus.services.journeyplanner.UserProfileFactory", "smartcampus.services.journeyplanner.UserProfileFactory.0", pars, userId, "vas_journeyplanner_subscriber");
			storage.storeObject(profile);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/eu.trentorise.smartcampus.journeyplanner.sync.BasicJourneyPlannerUserProfile/{clientId}")
	public @ResponseBody
	void updateUserProfile(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody JourneyPlannerUserProfile profile, @PathVariable String clientId) throws InvocationException, AcServiceException {
		try {
			User user = getUser(request);
			String userId = getUserId(user);
			if (userId == null) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			DomainObject toUpdate = getObjectByClientId(clientId, "smartcampus.services.journeyplanner.RecurrentJourneyObject");
			String objectId = toUpdate.getId();

			Map<String, Object> pars = new HashMap<String, Object>();
			pars.put("newProfile", profile);
			pars.put("userId", userId);
			domainClient.invokeDomainOperation("updateUserProfile", "smartcampus.services.journeyplanner.UserProfileObject", objectId, pars, userId, "vas_journeyplanner_subscriber");
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	// /////////////////////////////////////////////////////////////////////////////

	private String getObjectIdByClientId(String clientId, String type) throws Exception {
		DomainObject toUpdate = getObjectByClientId(clientId, type);
		if (toUpdate != null) {
			return toUpdate.getId();
		}
		return null;
	}

	private DomainObject getObjectByClientId(String id, String type) throws Exception {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("clientId", id);
		List<String> res = domainClient.searchDomainObjects(type, pars, "vas_journeyplanner_subscriber");
		if (res == null || res.size() == 0) {
			return null;
		}
		return new DomainObject(res.get(0));
	}

	private String checkUser(DomainObject res, String userId) throws IOException, InvocationException {
		if (res == null || userId == null) {
			return null;
		}
		String objectId = res.getId();
		String resUserId = (String) res.getContent().get("userId");
		if (resUserId == null || !resUserId.equals(userId)) {
			return null;
		}
		return objectId;
	}

	private String getUserId(User user) {
		return (user != null) ? user.getId().toString() : null;
	}

	private User getUser(HttpServletRequest request) throws AcServiceException {
		String token = request.getHeader(AcProviderFilter.TOKEN_HEADER);
		User user = acService.getUserByToken(token);
		return user;
	}

}
