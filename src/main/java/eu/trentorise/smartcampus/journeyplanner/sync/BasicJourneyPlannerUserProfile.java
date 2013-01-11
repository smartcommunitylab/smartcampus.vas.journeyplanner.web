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
package eu.trentorise.smartcampus.journeyplanner.sync;

import it.sayservice.platform.smartplanner.data.message.journey.JourneyPlannerUserProfile;
import eu.trentorise.smartcampus.presentation.data.BasicObject;

public class BasicJourneyPlannerUserProfile extends BasicObject {

	private String clientId;
	private JourneyPlannerUserProfile data;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}	
	
	public JourneyPlannerUserProfile getData() {
		return data;
	}

	public void setData(JourneyPlannerUserProfile content) {
		this.data = content;
	}
	
	
	
}
