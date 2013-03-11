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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
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

@Controller
public class OTPController {

	@Autowired
	@Value("${otp.url}")
	private String otpURL;	
	
	public static final String OTP  = "/smart-planner/rest/";
	
	private Logger log = Logger.getLogger(this.getClass());
	
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
	
	@RequestMapping(method = RequestMethod.GET, value = "/gettimetable/{agencyId}/{routeId}/{stopId:.*}")
	public @ResponseBody
	void getTimeTable(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String agencyId, @PathVariable String routeId, @PathVariable String stopId) throws InvocationException, AcServiceException {
		try {
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
			String address =  otpURL + OTP + "getlimitedtimetable/" + agencyId + "/" + stopId + "/" + maxResults;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, null);

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}		
	
	@RequestMapping(method = RequestMethod.GET, value = "/getbustimes/{routeId}/{from}/{to}")
	public @ResponseBody
	void getBusTimes(HttpServletRequest request, HttpServletResponse response, HttpSession session, @PathVariable String routeId, @PathVariable Long from, @PathVariable Long to)  {
		try {
			String address =  otpURL + OTP + "getbustimes/" + routeId + "/" + from + "/" + to;
			
			String timetable = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, null);

			response.setContentType("application/json; charset=utf-8");
			response.getWriter().write(timetable);

		} catch (ConnectorException e0) {
			response.setStatus(e0.getCode());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}		
	
	
	
}
