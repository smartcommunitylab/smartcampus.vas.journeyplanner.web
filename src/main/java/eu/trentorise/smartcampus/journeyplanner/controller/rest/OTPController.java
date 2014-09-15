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

import it.sayservice.platform.client.InvocationException;
import it.sayservice.platform.smartplanner.data.message.otpbeans.TransitTimeTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.ac.provider.AcServiceException;
import eu.trentorise.smartcampus.journeyplanner.util.ConnectorException;
import eu.trentorise.smartcampus.journeyplanner.util.HTTPConnector;
import eu.trentorise.smartcampus.presentation.common.util.Util;

@Controller
public class OTPController {

	@Autowired
	@Value("${otp.url}")
	private String otpURL;	
	
	public static final String OTP  = "/smart-planner/rest/";
	
	private Logger logger = Logger.getLogger(this.getClass());

    private static ObjectMapper fullMapper = new ObjectMapper();
    static {
        fullMapper.setAnnotationIntrospector(NopAnnotationIntrospector.nopInstance());
        fullMapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
        fullMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        fullMapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);

        fullMapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
        fullMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
    }

	
//	http://localhost:7070/smart-planner/rest/gettimetable/12/401/216

	@RequestMapping(method = RequestMethod.GET, value = "/getroutes/{agencyId}")
	public @ResponseBody
	void getRoutes(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId) throws InvocationException, AcServiceException {
		try {
			String address =  otpURL + OTP + "getroutes/" + agencyId;
			
			String routes = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			
			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(routes);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/getstops/{agencyId}/{routeId}")
	public @ResponseBody
	void getStops(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String routeId) throws InvocationException, AcServiceException {
		try {
			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId;
			
			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(stops);
			
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/getstops/{agencyId}/{routeId}/{latitude}/{longitude}/{radius:.+}")
	public @ResponseBody
	void getStops(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable double latitude, @PathVariable double longitude, @PathVariable double radius) throws InvocationException, AcServiceException {
		try {
			String address =  otpURL + OTP + "getstops/" + agencyId + "/" + routeId + "/" + latitude + "/" + longitude + "/" + radius;
			
			String stops = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(stops);
			
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}		
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettimetable/{agencyId}/{routeId}/{stopId:.*}")
	public @ResponseBody
	void getTimeTable(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable String stopId) throws InvocationException, AcServiceException {
		try {
			
			logger.info(new Random().nextInt() + "~AppConsume~timetable=" + agencyId);
			
			String address =  otpURL + OTP + "gettimetable/" + agencyId + "/" + routeId + "/" + stopId;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, null);

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/getlimitedtimetable/{agencyId}/{stopId}/{maxResults:.*}")
	public @ResponseBody
	void getLimitedTimeTable(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String stopId, @PathVariable Integer maxResults) throws InvocationException, AcServiceException {
		try {
			logger.info(new Random().nextInt()  + "~AppConsume~timetable=" + agencyId);
			
			String address =  otpURL + OTP + "getlimitedtimetable/" + agencyId + "/" + stopId + "/" + maxResults;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			response.setContentType("application/json; charset=utf-8");
			// TODO temporal solution for backward compatibility
			if (request.getParameter("complex")==null) {
				Map<String,Map> ttt = fullMapper.readValue(timetable, Map.class);
				Map map = Util.convert(ttt, Map.class);
				for (String route : ttt.keySet()) {
					Map smartCheckRoute = ttt.get(route);
					Map<String, Map<String, String>> delays = (Map<String, Map<String, String>>)smartCheckRoute.get("delays");
					Map<String,Integer> newDelays = new HashMap<String, Integer>();
					if (delays != null) {
						for (String trip : delays.keySet()) {
							Map<String,String> delay = delays.get(trip);
							if (delay != null && !delay.isEmpty()) {
								String s = delay.entrySet().iterator().next().getValue();
								try {
									newDelays.put(trip, Integer.parseInt(s));
								} catch (Exception e) {
								} 
							}
						}
					}
					smartCheckRoute.put("delays", newDelays);
				}
				
				response.getWriter().write(fullMapper.writeValueAsString(map));
			} else {
				response.getWriter().write(timetable);
			}
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}		
	
//	@RequestMapping(method = RequestMethod.GET, value = "/getbustimes/{routeId}/{from}/{to}")
//	public @ResponseBody
//	void getBusTimes(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
//		try {
//			String address =  otpURL + OTP + "getbustimes/" + routeId + "/" + from + "/" + to;
//			
//			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");
//
//			response.setContentType("application/json; charset=utf-8");
//			response.getWriter().write(timetable);
//
//		} catch (ConnectorException e0) {
//			response.setStatus(e0.getCode());
//		} catch (Exception e) {
//			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//		}
//	}		
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransittimes/{routeId}/{from}/{to}")
	public @ResponseBody
	void getTransitTimes(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
			String address =  otpURL + OTP + "getTransitTimes/" + routeId + "/" + from + "/" + to;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			
			// TODO temporal solution for backward compatibility
			if (request.getParameter("complex")==null) {
				TransitTimeTable ttt = fullMapper.readValue(timetable, TransitTimeTable.class);
				Map map = Util.convert(ttt, Map.class);
				List<List<Integer>> list = new ArrayList<List<Integer>>();
				map.put("delays", list);
				for (List<Map<String,String>> daylist : ttt.getDelays()) {
					List<Integer> newDayList = new ArrayList<Integer>();
					list.add(newDayList);
					for (Map<String, String> tripMap : daylist) {
						if (tripMap != null && !tripMap.isEmpty()) {
							String s = tripMap.entrySet().iterator().next().getValue();
							try {
								newDayList.add(Integer.parseInt(s));
							} catch (Exception e) {
								newDayList.add(0);
							} 
						} else {
							newDayList.add(0); 
						}
					}
				}
				response.getWriter().write(fullMapper.writeValueAsString(map));
			} else {
				response.getWriter().write(timetable);
			}
			

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}			
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettransitdelays/{routeId}/{from}/{to}")
	public @ResponseBody
	void getTransitDelays(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
			String address =  otpURL + OTP + "getTransitDelays/" + routeId + "/" + from + "/" + to;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON,  "UTF-8");

			response.setContentType("application/json; charset=utf-8");
			
			response.getWriter().write(timetable);
		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
}
