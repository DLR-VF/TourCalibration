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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class DefaultTravelTime implements TravelTime{

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double velocity;
		/*if(vehicle.getType().getMaximumVelocity() < link.getFreespeed(time)){
			velocity = vehicle.getType().getMaximumVelocity();
		}*/
		//else 
		velocity = link.getFreespeed(time);
		if(velocity <= 0.0) throw new IllegalStateException("velocity must be bigger than zero");
		return link.getLength() / velocity;
	}

}
