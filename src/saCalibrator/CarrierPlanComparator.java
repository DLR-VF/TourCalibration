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

import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.vehicles.VehicleType;

public class CarrierPlanComparator {

	private ExtendedCarrierPlan incumbentPlan;
	private ExtendedCarrierPlan entrantPlan;
	
	public void setIncumbentPlan(ExtendedCarrierPlan incumbentPlan) {
		this.incumbentPlan = incumbentPlan;
	}
	public void setEntrantPlan(ExtendedCarrierPlan entrantPlan) {
		this.entrantPlan = entrantPlan;
	}
	
	public double getDistanceToBestPlan() {
		double distanceOfAverageLengths = Math.abs((incumbentPlan.getAverageTourLength()/1000 - entrantPlan.getAverageTourLength()/1000)/(incumbentPlan.getAverageTourLength()/1000));
		double distanceOfTours = Math.abs((incumbentPlan.getNumberOfTours() - entrantPlan.getNumberOfTours())/(incumbentPlan.getNumberOfTours()));
		double distanceOfStopsPerTour = Math.abs((incumbentPlan.getAverageStopsPerTour() - entrantPlan.getAverageStopsPerTour())/(incumbentPlan.getAverageStopsPerTour()));
		double distanceOfCapacityUtilization = Math.abs((incumbentPlan.getAverageCapacityUtilization() - entrantPlan.getAverageCapacityUtilization())/(incumbentPlan.getAverageCapacityUtilization()));
		double distanceOfDistancePerStops = Math.abs((incumbentPlan.getAverageDistanceBetweenStops()/1000 - entrantPlan.getAverageDistanceBetweenStops()/1000)/(incumbentPlan.getAverageDistanceBetweenStops()/1000));
		double distanceOfTotalLength = Math.abs((incumbentPlan.getOverallLength()/1000 - entrantPlan.getOverallLength()/1000)/(incumbentPlan.getOverallLength()/1000));
		double vehicleTypeDistance = getDistanceBetweenVehicleTypes();
		
		return  distanceOfTours +  distanceOfAverageLengths +  distanceOfStopsPerTour + distanceOfCapacityUtilization + distanceOfDistancePerStops + vehicleTypeDistance +distanceOfTotalLength ;

	}
	
	private double getDistanceBetweenVehicleTypes() {
		double distance = 0;

		if(!incumbentPlan.getVehicleMap().isEmpty() && !entrantPlan.getVehicleMap().isEmpty()) {
			HashMap<Id<VehicleType>, Double>	incumbentMap = incumbentPlan.getVehicleMap();
			HashMap<Id<VehicleType>, Double>	entrantMap = entrantPlan.getVehicleMap();
				
			for(Id<VehicleType> id : incumbentMap.keySet()) {
				if(entrantMap.containsKey(id)) {
					distance = distance + Math.abs(entrantMap.get(id)- incumbentMap.get(id));	
				}
				else {
					distance = distance + incumbentMap.get(id);
				}
			}
			for(Id<VehicleType> id : entrantMap.keySet()) {
				if(incumbentMap.containsKey(id)) {
				}
				else {
					distance = distance + entrantMap.get(id);
				}
			}
		}
		return distance;
	}
	
	public double getDistanceBetweenVehicleTypes(ExtendedCarrierPlan incumbentPlan, ExtendedCarrierPlan entrantPlan) {
		double distance = 0;

		if(!incumbentPlan.getVehicleMap().isEmpty() && !entrantPlan.getVehicleMap().isEmpty()) {
			HashMap<Id<VehicleType>, Double>	incumbentMap = incumbentPlan.getVehicleMap();
			HashMap<Id<VehicleType>, Double>	entrantMap = entrantPlan.getVehicleMap();
				
			for(Id<VehicleType> id : incumbentMap.keySet()) {
				if(entrantMap.containsKey(id)) {
					distance = distance + Math.abs(entrantMap.get(id)- incumbentMap.get(id));	
				}
				else {
					distance = distance + incumbentMap.get(id);
				}
			}
			for(Id<VehicleType> id : entrantMap.keySet()) {
				if(incumbentMap.containsKey(id)) {
				}
				else {
					distance = distance + entrantMap.get(id);
				}
			}
		}
		return distance;
	}
	
}
