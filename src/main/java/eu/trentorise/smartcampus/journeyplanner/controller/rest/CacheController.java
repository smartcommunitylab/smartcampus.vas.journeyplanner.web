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
import it.sayservice.platform.smartplanner.data.message.cache.CacheUpdateResponse;
import it.sayservice.platform.smartplanner.data.message.otpbeans.CompressedTransitTimeTable;
import it.sayservice.platform.smartplanner.data.message.otpbeans.TransitTimeTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.ac.provider.AcServiceException;
import eu.trentorise.smartcampus.journeyplanner.util.ConnectorException;
import eu.trentorise.smartcampus.journeyplanner.util.HTTPConnector;
import eu.trentorise.smartcampus.presentation.common.util.Util;

@Controller
public class CacheController {

	@Autowired
	@Value("${otp.url}")
	private String otpURL;	
	
	public static final String OTP  = "/smart-planner/rest/";

    private static ObjectMapper fullMapper = new ObjectMapper();
    static {
        fullMapper.setAnnotationIntrospector(NopAnnotationIntrospector.nopInstance());
        fullMapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
        fullMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        fullMapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);

        fullMapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
        fullMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
    }

  	@RequestMapping(method = RequestMethod.POST, value = "/getcachestatus")
  	public @ResponseBody
  	Map<String, CacheUpdateResponse> getCacheStatus(HttpServletRequest request, HttpServletResponse response, HttpSession session, @RequestBody Map<String, String> versions) {
		try {
			String address =  otpURL + OTP + "getCacheStatus";
			
			ObjectMapper mapper = new ObjectMapper();
			String content = mapper.writeValueAsString(versions);
			String res = HTTPConnector.doPost(address, content, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
			
			Map<String, CacheUpdateResponse> result = mapper.readValue(res, Map.class);
			
			return result;

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}
  	
  	@RequestMapping(method = RequestMethod.GET, value = "/getcacheupdate/{agencyId}/{fileName}")
  	public @ResponseBody
  	CompressedTransitTimeTable getCacheUpdate(HttpServletRequest request, HttpServletResponse response, HttpSession session,  @PathVariable String agencyId,  @PathVariable String fileName) {
  		try {
			String address =  otpURL + OTP + "getCacheUpdate/" + agencyId + "/" + fileName;
			
			String res = HTTPConnector.doGet(address, null, null, MediaType.APPLICATION_JSON, "UTF-8");
			
			ObjectMapper mapper = new ObjectMapper();
			CompressedTransitTimeTable result = mapper.readValue(res, CompressedTransitTimeTable.class);
			
			return result;

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
	}  	
  	
		
}
