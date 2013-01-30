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

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ItinerarySorter {

	public static void sort(List<Itinerary> itineraries, RType criterion) {
		if (criterion != null) {
			switch (criterion) {
			case fastest:
				sortFaster(itineraries);
				break;
			case greenest:
				sortByGreener(itineraries);
				break;
			case healthy:
				sortByHealthier(itineraries);
				break;
			case leastChanges:
				sortByLeastChanges(itineraries);
				break;
			case leastWalking:
				sortByLessWalking(itineraries);
				break;
			case safest:
				break;
			}
		}
	}

	private static void sortFaster(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return (int) (o1.getEndtime() - o2.getEndtime());
			}
		});
	}

	private static void sortByGreener(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeGreenness(o2) - computeGreenness(o1);
			}

			private int computeGreenness(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += leg.getLegGeometery().getLength() * leg.getTransport().getType().getGreen();
				}
				return h;
			}

		});
	}

	private static void sortByHealthier(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeHealthiness(o2) - computeHealthiness(o1);
			}

			private int computeHealthiness(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += leg.getLegGeometery().getLength() * leg.getTransport().getType().getHealth();
				}
				return h;
			}

		});
	}

	private static void sortByLeastChanges(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return (int) (o1.getLeg().size() - o2.getLeg().size());
			}
		});
	}

	private static void sortByLessWalking(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeWalking(o1) - computeWalking(o2);
			}

			private int computeWalking(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += (leg.getTransport().getType().equals(TType.WALK) ? leg.getLegGeometery().getLength() : 0);
				}
				return h;
			}

		});
	}

}
