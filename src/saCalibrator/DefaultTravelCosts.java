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
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class DefaultTravelCosts implements TravelDisutility {

	private TravelTime travelTime;
	private Map<String,VehicleTypeVarCosts> typeSpecificCosts;
	
	public DefaultTravelCosts(Carrier carrier, TravelTime travelTime) {
		this.travelTime = travelTime;
		typeSpecificCosts = new HashMap<String, VehicleTypeVarCosts>();
		for(CarrierVehicleType type : carrier.getCarrierCapabilities().getVehicleTypes()) {
			VehicleTypeVarCosts varcosts = new VehicleTypeVarCosts(type.getVehicleCostInformation().getPerDistanceUnit(), type.getVehicleCostInformation().getPerTimeUnit());
			typeSpecificCosts.put(type.getId().toString(), varcosts);
		}
	}
	
	
	


	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
		VehicleTypeVarCosts typeCosts = typeSpecificCosts.get(vehicle.getType().getId().toString());
		if(typeCosts == null) throw new IllegalStateException("type specific costs for " + vehicle.getType().getId().toString() + " are missing.");
		double tt = travelTime.getLinkTravelTime(link, time, person, vehicle);
		return typeCosts.perMeter*link.getLength() + typeCosts.perSecond*tt;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		double minDisutility = Double.MAX_VALUE;
		double free_tt = link.getLength()/link.getFreespeed();
		for(VehicleTypeVarCosts c : typeSpecificCosts.values()){
			double disu = c.perMeter*link.getLength() + c.perSecond*free_tt;
			if(disu < minDisutility) minDisutility=disu;
		}
		return minDisutility;
	}

	private static class VehicleTypeVarCosts {
		final double perMeter;
		final double perSecond;
		
		VehicleTypeVarCosts(double perMeter, double perSecond) {
			super();
			this.perMeter = perMeter;
			this.perSecond = perSecond;
		}
	}


}
