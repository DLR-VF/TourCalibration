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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.vehicles.VehicleType;

public class ExtendedCarrierPlan extends CarrierPlan{

	private double averageCapacityUtilization;
	private int numberOfTours;
	private double averageTourLength;
	private double averageStopsPerTour;
	private double averageDistanceBetweenStops;
	private double overallLength;
	private HashMap <Id<VehicleType> , Double> vehicleMap;
	
	public ExtendedCarrierPlan(Carrier carrier, Collection<ScheduledTour> scheduledTours) {
		super(carrier, scheduledTours);
		vehicleMap =new HashMap<>();
	}

	public double getAverageCapacityUtilization() {
		return averageCapacityUtilization;
	}

	public int getNumberOfTours() {
		return numberOfTours;
	}

	public double getAverageTourLength() {
		return averageTourLength;
	}

	public double getAverageStopsPerTour() {
		return averageStopsPerTour;
	}

	public double getAverageDistanceBetweenStops() {
		return averageDistanceBetweenStops;
	}

	public void setAverageCapacityUtilization(double capacityUtilization) {
		this.averageCapacityUtilization = capacityUtilization;
	}

	public void setNumberOfTours(int numberOfTours) {
		this.numberOfTours = numberOfTours;
	}

	public void setAverageTourLength(double averageTourLength) {
		this.averageTourLength = averageTourLength;
	}

	public void setAverageStopsPerTour(double averageStopsPerTour) {
		this.averageStopsPerTour = averageStopsPerTour;
	}

	public void setAverageDistanceBetweenStops(double averageDistanceBetweenStops) {
		this.averageDistanceBetweenStops = averageDistanceBetweenStops;
	}

	public double getOverallLength() {
		return overallLength;
	}

	public void setOverallLength(double overallLength) {
		this.overallLength = overallLength;
	}

	public HashMap<Id<VehicleType>, Double> getVehicleMap() {
		return vehicleMap;
	}

	public void setVehicleMap(HashMap<Id<VehicleType>, Double> vehicleMap) {
		this.vehicleMap = vehicleMap;
	}
	
}
