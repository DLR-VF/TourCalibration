/*
 * Copyright (c) 2022 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "TourCalibration" tool
 * http://github.com/DLR-VF/TourCalibration
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rudower Chaussee 7
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */


package saCalibrator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.End;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.Start;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleType;




public class CarrierPlanEvaluator {

	public ExtendedCarrierPlan evaluateCarrierPlan(CarrierPlan plan, Network network) {
		
		ExtendedCarrierPlan extendedPlan = new ExtendedCarrierPlan(plan.getCarrier(), plan.getScheduledTours());
		
		extendedPlan.setNumberOfTours(plan.getScheduledTours().size());
		extendedPlan.setAverageCapacityUtilization(calculateAverageUtilization(plan.getScheduledTours()));
		extendedPlan.setAverageStopsPerTour(calculateAverageStopsPerTour(plan.getScheduledTours()));
		extendedPlan.setAverageTourLength(calculateAverageTourLength(plan.getScheduledTours(), network));
		extendedPlan.setAverageDistanceBetweenStops(calculateAverageDistanceBetweenStops(plan.getScheduledTours(), network));
		extendedPlan.setOverallLength(calculateOverallLength(plan.getScheduledTours(), network));
		extendedPlan.setVehicleMap(collectVehicleTypes(plan.getScheduledTours()));
		return extendedPlan;
	
	}
	
	
	private double calculateAverageUtilization(Collection<ScheduledTour> scheduledTours) {
		double sumOfAverages = 0;
		double numberOfTours = scheduledTours.size();
		for(ScheduledTour scheduledTour : scheduledTours) {
			double capacity = scheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity();
			double totalLoad = 0;
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity){
					ServiceActivity service = (ServiceActivity) element;
					totalLoad = totalLoad + service.getService().getCapacityDemand();
				}
			}
			sumOfAverages = sumOfAverages + (totalLoad/capacity);
		}
		return sumOfAverages/numberOfTours;
	}
	
	private double calculateAverageStopsPerTour(Collection<ScheduledTour> scheduledTours) {
		double totalStops = 0;
		double numberOfTours = scheduledTours.size();
		for(ScheduledTour scheduledTour : scheduledTours) {
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity){
					totalStops = totalStops + 1;
				}
			}
		}
		return totalStops/numberOfTours;
	}
	
	private double calculateAverageTourLength(Collection<ScheduledTour> scheduledTours, Network network) {
		double totalDistance = 0;
		double numberOfTours = scheduledTours.size();
		for(ScheduledTour scheduledTour : scheduledTours) {
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity){
					ServiceActivity service = (ServiceActivity) element;
					Link serviceLink = network.getLinks().get(service.getLocation());
					totalDistance = totalDistance + serviceLink.getLength();
				}
				if(element instanceof Start) {
					Start start = (Start) element;
					Link startLink = network.getLinks().get(start.getLocation());
					totalDistance = totalDistance + startLink.getLength();
				}
				if(element instanceof End) {
					End end = (End) element;
					Link endLink = network.getLinks().get(end.getLocation());
					totalDistance = totalDistance + endLink.getLength();
				}
				if(element instanceof Leg) {
					Leg leg = (Leg) element;
					if(leg.getRoute() instanceof NetworkRoute) {
						NetworkRoute netRoute = (NetworkRoute) leg.getRoute();	
						for(Id<Link> linkId : netRoute.getLinkIds()) {
							Link link = network.getLinks().get(linkId);
							totalDistance = totalDistance + link.getLength();
						}
					}					
				}
			}
		}
		return totalDistance/numberOfTours;
	}
	
	private double calculateOverallLength(Collection<ScheduledTour> scheduledTours, Network network) {
		double totalDistance = 0;
		for(ScheduledTour scheduledTour : scheduledTours) {
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity){
					ServiceActivity service = (ServiceActivity) element;
					Link serviceLink = network.getLinks().get(service.getLocation());
					totalDistance = totalDistance + serviceLink.getLength();
				}
				if(element instanceof Start) {
					Start start = (Start) element;
					Link startLink = network.getLinks().get(start.getLocation());
					totalDistance = totalDistance + startLink.getLength();
				}
				if(element instanceof End) {
					End end = (End) element;
					Link endLink = network.getLinks().get(end.getLocation());
					totalDistance = totalDistance + endLink.getLength();
				}
				if(element instanceof Leg) {
					Leg leg = (Leg) element;
					if(leg.getRoute() instanceof NetworkRoute) {
						NetworkRoute netRoute = (NetworkRoute) leg.getRoute();	
						for(Id<Link> linkId : netRoute.getLinkIds()) {
							Link link = network.getLinks().get(linkId);
							totalDistance = totalDistance + link.getLength();
						}
					}					
				}
			}
		}
		return totalDistance;
	}
	
	private double calculateAverageDistanceBetweenStops(Collection<ScheduledTour> scheduledTours, Network network) {
		double totalDistance = 0;
		double numberOfStops = 0;
		for(ScheduledTour scheduledTour : scheduledTours) {
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity){
					ServiceActivity service = (ServiceActivity) element;
					Link serviceLink = network.getLinks().get(service.getLocation());
					totalDistance = totalDistance + serviceLink.getLength();
					numberOfStops = numberOfStops + 1;
				}
				if(element instanceof Leg) {
					Leg leg = (Leg) element;
					if(leg.getRoute() instanceof NetworkRoute) {
						NetworkRoute netRoute = (NetworkRoute) leg.getRoute();	
						for(Id<Link> linkId : netRoute.getLinkIds()) {
							Link link = network.getLinks().get(linkId);
							totalDistance = totalDistance + link.getLength();
						}
					}					
				}
			}
		}
		return totalDistance/numberOfStops;
	}
	
	private HashMap<Id<VehicleType>, Double> collectVehicleTypes(Collection<ScheduledTour> scheduledTours){
		HashMap<Id<VehicleType>, Double> vehicleMap = new HashMap<>();
		
		for(ScheduledTour scheduledTour : scheduledTours) {
			if(vehicleMap.containsKey(scheduledTour.getVehicle().getVehicleType().getId())) {
				double numberOfVehiclesBefore = vehicleMap.get(scheduledTour.getVehicle().getVehicleType().getId());
				numberOfVehiclesBefore++;
				vehicleMap.put(scheduledTour.getVehicle().getVehicleType().getId(), new Double(numberOfVehiclesBefore));
			}
			else {
				vehicleMap.put(scheduledTour.getVehicle().getVehicleType().getId(), 1.0);
			}
		}
		
		double allVehicles = 0;
		
		for(Entry<Id<VehicleType>, Double> entry : vehicleMap.entrySet()) {
			allVehicles = allVehicles + entry.getValue();
		}
		
		for(Entry<Id<VehicleType>, Double> entry : vehicleMap.entrySet()) {
			double absoluteValue = entry.getValue();
			double relativeValue = entry.getValue()/allVehicles;
			entry.setValue(relativeValue);
		}
		
		return vehicleMap;
	}


}


