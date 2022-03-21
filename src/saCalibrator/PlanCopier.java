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

import java.util.ArrayList;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

public class PlanCopier {

	public ExtendedCarrierPlan makeDeepCopyOfPlan(ExtendedCarrierPlan plan) {
		ArrayList<ScheduledTour> tourCopies = new ArrayList<>();
		Carrier carrierCopy = CarrierImpl.newInstance(plan.getCarrier().getId());
		for(ScheduledTour scheduledTour : plan.getScheduledTours()) {
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					carrierCopy.getServices().add(service.getService());
				}
			}
		}
		carrierCopy.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
		carrierCopy.getCarrierCapabilities().getVehicleTypes().addAll(plan.getCarrier().getCarrierCapabilities().getVehicleTypes());
		carrierCopy.getCarrierCapabilities().getCarrierVehicles().addAll(plan.getCarrier().getCarrierCapabilities().getCarrierVehicles());
		
		
		for(ScheduledTour scheduledTour : plan.getScheduledTours()) {
			Tour.Builder tourCopyBuilder = Tour.Builder.newInstance();
			tourCopyBuilder.scheduleStart(scheduledTour.getTour().getStartLinkId());
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof Leg) {
					Leg leg  = (Leg)element;
					tourCopyBuilder.addLeg(leg);
				}
				if(element instanceof ServiceActivity) {
					ServiceActivity service  = (ServiceActivity)element;
					tourCopyBuilder.scheduleService(service.getService());
				}
			}
		    tourCopyBuilder.scheduleEnd(scheduledTour.getTour().getEndLinkId());
		    tourCopies.add(ScheduledTour.newInstance(tourCopyBuilder.build(), scheduledTour.getVehicle() ,0));
		}
		
		ExtendedCarrierPlan planCopy = new ExtendedCarrierPlan(carrierCopy, tourCopies);
		planCopy.setAverageCapacityUtilization(plan.getAverageCapacityUtilization());
		planCopy.setAverageDistanceBetweenStops(plan.getAverageDistanceBetweenStops());
		planCopy.setAverageStopsPerTour(plan.getAverageStopsPerTour());
		planCopy.setAverageTourLength(plan.getAverageTourLength());
		planCopy.setNumberOfTours(plan.getNumberOfTours());
		planCopy.setOverallLength(plan.getOverallLength());
		planCopy.setVehicleMap(plan.getVehicleMap());
		return planCopy;
		
	}

}

