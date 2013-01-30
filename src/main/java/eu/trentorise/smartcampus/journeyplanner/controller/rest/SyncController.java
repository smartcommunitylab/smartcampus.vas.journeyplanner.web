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
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.JourneyPlannerUserProfile;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import eu.trentorise.smartcampus.ac.provider.AcService;
import eu.trentorise.smartcampus.ac.provider.filters.AcProviderFilter;
import eu.trentorise.smartcampus.journeyplanner.sync.BasicItinerary;
import eu.trentorise.smartcampus.journeyplanner.sync.BasicJourneyPlannerUserProfile;
import eu.trentorise.smartcampus.journeyplanner.sync.BasicRecurrentJourneyParameters;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.presentation.data.SyncData;
import eu.trentorise.smartcampus.presentation.data.SyncDataRequest;
import eu.trentorise.smartcampus.presentation.storage.sync.BasicObjectSyncStorage;

@Controller
public class SyncController {

	@Autowired
	private BasicObjectSyncStorage storage;

	@Autowired
	private DomainEngineClient domainClient;

	@Autowired
	private AcService acService;

	private Logger log = Logger.getLogger(this.getClass());

	@RequestMapping(method = RequestMethod.POST, value = "/sync")
	public ResponseEntity<SyncData> synchronize(HttpServletRequest request, @RequestParam long since) throws Exception {
		// public ResponseEntity<SyncData> synchronize(HttpServletRequest request,
		// @RequestParam long since) throws Exception{
		String token = request.getHeader(AcProviderFilter.TOKEN_HEADER);
		// User user = acService.getUserByToken(token);
		String userId = getUserId(request);

		Map<String, Object> obj = (Map<String, Object>) extractContent(request, Map.class);
		SyncDataRequest syncReq = Util.convertRequest(obj, since);

		processData(syncReq.getSyncData(), userId);

		SyncData result = storage.getSyncData(syncReq.getSince(), userId);
		storage.cleanSyncData(syncReq.getSyncData(), userId);

		return new ResponseEntity<SyncData>(result, HttpStatus.OK);

	}

	private void processData(SyncData syncData, String user) throws DataException {
		processUserProfile(syncData, user);
		processItinerary(syncData, user);
		processRecurrentJourney(syncData, user);
	}

	private void processUserProfile(SyncData syncData, String user) throws DataException {
		List<BasicObject> objects = (List<BasicObject>) syncData.getUpdated().get(BasicJourneyPlannerUserProfile.class.getCanonicalName());
		if (objects != null) {
			for (BasicObject bo : objects) {
				BasicJourneyPlannerUserProfile bjp = (BasicJourneyPlannerUserProfile) bo;
				String clientId = bjp.getClientId();
				JourneyPlannerUserProfile profile = bjp.getData();

				boolean toUpdate = false;
				Map<String, Object> qpars = new TreeMap<String, Object>();
				qpars.put("clientId", clientId);
				List<String> res;
				try {
					res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.UserProfileObject", qpars, "vas_journeyplanner_subscriber");
				} catch (InvocationException e1) {
					continue;
				}
				if (res != null && res.size() == 1) {
					toUpdate = true;
				}

				if (!toUpdate) {
					// CREATE
					try {
						Map<String, Object> pars = new HashMap<String, Object>();
						pars.put("userProfile", profile);
						if (clientId == null) {
							clientId = new ObjectId().toString();
						}
						pars.put("clientId", clientId);
						domainClient.invokeDomainOperation("createUserProfile", "smartcampus.services.journeyplanner.UserProfileFactory", "smartcampus.services.journeyplanner.UserProfileFactory.0", pars, clientId, "vas_journeyplanner_subscriber");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						DomainObject obj = new DomainObject(res.get(0));
						Map<String, Object> pars = new HashMap<String, Object>();
						pars.put("newProfile", profile);
						domainClient.invokeDomainOperation("updateUserProfile", "smartcampus.services.journeyplanner.UserProfileObject", obj.getId(), pars, clientId, "vas_journeyplanner_subscriber");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void processItinerary(SyncData syncData, String user) throws DataException {
		List<BasicObject> objects = (List<BasicObject>) syncData.getUpdated().get(BasicItinerary.class.getCanonicalName());
		if (objects != null) {
			for (BasicObject bo : objects) {
				BasicItinerary bi = (BasicItinerary) bo;
				String clientId = bi.getClientId();
				String userId = bi.getUser();
				Itinerary itinerary = bi.getData();

				boolean toUpdate = false;
				Map<String, Object> qpars = new TreeMap<String, Object>();
				qpars.put("clientId", clientId);
				List<String> res;
				try {
					res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.ItineraryObject", qpars, "vas_journeyplanner_subscriber");
				} catch (InvocationException e1) {
					continue;
				}
				if (res != null && res.size() == 1) {
					toUpdate = true;
				} else {
					// TODO error
				}

				if (!toUpdate) {
					// CREATE
					try {
						Map<String, Object> pars = new HashMap<String, Object>();
						pars.put("itinerary", itinerary);
						if (clientId == null) {
							clientId = new ObjectId().toString();
						}
						pars.put("clientId", clientId);
						pars.put("userId", userId);
						domainClient.invokeDomainOperation("saveItinerary", "smartcampus.services.journeyplanner.ItineraryFactory", "smartcampus.services.journeyplanner.ItineraryFactory.0", pars, userId, "vas_journeyplanner_subscriber");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					// TODO assume only "monitored" is changed
					try {
						DomainObject obj = new DomainObject(res.get(0));
						Map<String, Object> pars = new HashMap<String, Object>();
						pars.put("flag", bi.isMonitor());
						domainClient.invokeDomainOperation("setMonitorFlag", "smartcampus.services.journeyplanner.ItineraryObject", obj.getId(), pars, userId, "vas_journeyplanner_subscriber");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		List<String> objectsId = (List<String>) syncData.getDeleted().get(BasicItinerary.class.getCanonicalName());
		if (objectsId != null) {
			for (String id : objectsId) {
				BasicItinerary bi;
				try {
					bi = (BasicItinerary) storage.getObjectById(id);
				} catch (NotFoundException e) {
					continue;
				}
				try {
					String clientId = bi.getClientId();
					String userId = bi.getUser();
					Map<String, Object> pars = new TreeMap<String, Object>();
					pars.put("clientId", clientId);
					List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.ItineraryObject", pars, "vas_journeyplanner_subscriber");
					if (res != null && res.size() == 1) {
						DomainObject obj = new DomainObject(res.get(0));
						pars = new HashMap<String, Object>();
						domainClient.invokeDomainOperation("deleteItinerary", "smartcampus.services.journeyplanner.ItineraryObject", obj.getId(), pars, userId, "vas_journeyplanner_subscriber");
					} else {
						// TODO exception?
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void processRecurrentJourney(SyncData syncData, String user) throws DataException {
		List<BasicObject> objects = (List<BasicObject>) syncData.getUpdated().get(BasicRecurrentJourneyParameters.class.getCanonicalName());
		if (objects != null) {
			for (BasicObject bo : objects) {
				BasicRecurrentJourneyParameters rjp = (BasicRecurrentJourneyParameters) bo;
				String clientId = rjp.getClientId();
				String userId = rjp.getUser();
				RecurrentJourneyParameters journey = rjp.getData();

				boolean toUpdate = false;
				Map<String, Object> qpars = new TreeMap<String, Object>();
				qpars.put("clientId", clientId);
				List<String> res;
				try {
					res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.RecurrentJourneyObject", qpars, "vas_journeyplanner_subscriber");
				} catch (InvocationException e1) {
					continue;
				}
				if (res != null && res.size() == 1) {
					toUpdate = true;
				} else {
					// TODO error
				}

				if (!toUpdate) {
					// CREATE
					try {
						Map<String, Object> pars = new HashMap<String, Object>();
						pars.put("recurrentJourney", journey);
						if (clientId == null) {
							clientId = new ObjectId().toString();
						}
						pars.put("clientId", clientId);
						pars.put("userId", userId);
						domainClient.invokeDomainOperation("saveRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyFactory", "smartcampus.services.journeyplanner.ItineraryFactory.0", pars, userId, "vas_journeyplanner_subscriber");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					// TODO assume only "monitored" is changed
					try {
						DomainObject obj = new DomainObject(res.get(0));
						Map<String, Object> pars = new HashMap<String, Object>();
						pars.put("flag", rjp.isMonitor());
						domainClient.invokeDomainOperation("setMonitorFlag", "smartcampus.services.journeyplanner.RecurrentJourneyObject", obj.getId(), pars, userId, "vas_journeyplanner_subscriber");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		List<String> objectsId = (List<String>) syncData.getDeleted().get(BasicRecurrentJourneyParameters.class.getCanonicalName());
		if (objectsId != null) {
			for (String id : objectsId) {
				BasicRecurrentJourneyParameters rjp;
				try {
					rjp = (BasicRecurrentJourneyParameters) storage.getObjectById(id);
				} catch (NotFoundException e) {
					continue;
				}
				try {
					String clientId = rjp.getClientId();
					String userId = rjp.getUser();
					Map<String, Object> pars = new TreeMap<String, Object>();
					pars.put("clientId", clientId);
					List<String> res = domainClient.searchDomainObjects("smartcampus.services.journeyplanner.RecurrentJourneyObject", pars, "vas_journeyplanner_subscriber");
					if (res != null && res.size() == 1) {
						DomainObject obj = new DomainObject(res.get(0));
						pars = new HashMap<String, Object>();
						domainClient.invokeDomainOperation("deleteRecurrentJourney", "smartcampus.services.journeyplanner.RecurrentJourneyObject", obj.getId(), pars, userId, "vas_journeyplanner_subscriber");
					} else {
						// TODO exception?
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * private void aprocessData(SyncData syncData, String user) throws
	 * DataException { List<BasicObject> personalized = (List<BasicObject>)
	 * syncData.getUpdated().get(PersonalSeminar.class.getCanonicalName()); if
	 * (personalized != null) { List<BasicObject> filtered = new
	 * ArrayList<BasicObject>(); for (BasicObject bo : personalized) {
	 * PersonalSeminar ps = (PersonalSeminar) bo; SeminarObject seminar; try {
	 * seminar = storage.getObjectById(ps.getSeminarId(), SeminarObject.class); if
	 * (seminar != null) { Map<String, Object> criteria = new HashMap<String,
	 * Object>(); criteria.put("seminarId", seminar.getId());
	 * Collection<PersonalSeminar> res =
	 * storage.searchObjects(PersonalSeminar.class, criteria, user); if (res ==
	 * null || res.isEmpty()) { Map<String, Object> parameters = new
	 * HashMap<String, Object>(); parameters.put("userId", user); try {
	 * domainEngineClient.invokeDomainOperation("personalize",
	 * SeminarController.SEMINAR_TYPE, seminar.getId(), parameters, null, null);
	 * ps.setUser(user); filtered.add(ps); } catch (InvocationException e) {
	 * e.printStackTrace(); } } } } catch (NotFoundException e) { continue; } }
	 * syncData.getUpdated().put(PersonalSeminar.class.getCanonicalName(),
	 * filtered); } }
	 */

	// TODO: change
	private String getUserId(HttpServletRequest request) {
		return "";
	}

	@SuppressWarnings("unchecked")
	private Object extractContent(ServletRequest request, Class clz) throws Exception {
		try {
			ServletInputStream sis = request.getInputStream();
			ObjectMapper mapper = new ObjectMapper();
			Object obj = mapper.readValue(sis, clz);
			return obj;
		} catch (Exception e) {
			log.error("Error converting request to " + clz.getName());
			throw e;
		}
	}

}
