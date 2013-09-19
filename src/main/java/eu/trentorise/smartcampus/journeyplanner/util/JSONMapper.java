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
package eu.trentorise.smartcampus.journeyplanner.util;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;

public class JSONMapper extends ObjectMapper {

	public JSONMapper() {
		super();
        setup();
	}

	private void setup() {
		getDeserializationConfig().set(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
        getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        getSerializationConfig().set(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
        getSerializationConfig().set(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
	}

	public JSONMapper(JsonFactory jf, SerializerProvider sp,
			DeserializerProvider dp, SerializationConfig sconfig,
			DeserializationConfig dconfig) {
		super(jf, sp, dp, sconfig, dconfig);
		setup();
	}

	public JSONMapper(JsonFactory jf, SerializerProvider sp,
			DeserializerProvider dp) {
		super(jf, sp, dp);
		setup();
	}

	public JSONMapper(JsonFactory jf) {
		super(jf);
		setup();
	}

	
	
}
