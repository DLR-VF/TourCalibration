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
import java.util.Collections;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.vehicles.Vehicle;


public class OperationsWithHomogeneousFleet {

		
	public static ExtendedCarrierPlan switchShipmentWithinTour(ExtendedCarrierPlan plan, Random random) {
	
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		ArrayList<ScheduledTour> toursWithOnlyOneService = new ArrayList<>();
		
		for(ScheduledTour  scheduledTour : tourList) {
			if(scheduledTour.getTour().getTourElements().size() <= 3) {
				toursWithOnlyOneService.add(scheduledTour);
			}
		}
		
		tourList.removeAll(toursWithOnlyOneService);
		
		if(tourList.isEmpty()) {
			return plan;
		}
		else {
			Collections.shuffle(tourList, random);
			ScheduledTour chosenTour = tourList.get(0);		
		
			ArrayList<Integer> positionsOfServices = new ArrayList<>();
			
			int i = 0;
			for(TourElement element: chosenTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					positionsOfServices.add(i);
				}
				i = i+1;
			}
			
						
			Collections.shuffle(positionsOfServices, random);
			int positionOfFirstService = positionsOfServices.get(0);
			int positionOfSecondService = positionsOfServices.get(1);
			
			ServiceActivity firstService = (ServiceActivity) chosenTour.getTour().getTourElements().get(positionOfFirstService);
			ServiceActivity secondService = (ServiceActivity) chosenTour.getTour().getTourElements().get(positionOfSecondService);
			
			Tour.Builder tourBuilder = Tour.Builder.newInstance();
			tourBuilder.scheduleStart(chosenTour.getTour().getStart().getLocation());
			
			
			for(TourElement element : chosenTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					if(service.getService().getId() == firstService.getService().getId()) {
						tourBuilder.scheduleService(secondService.getService());
						continue;
					}
					if(service.getService().getId() == secondService.getService().getId()) {
						tourBuilder.scheduleService(firstService.getService());
						continue;
					}
					else {
						tourBuilder.scheduleService(service.getService());
					}	
				}	
				if(element instanceof Leg) {
					tourBuilder.addLeg(new Leg());
				}
			}
			tourBuilder.scheduleEnd(chosenTour.getTour().getStart().getLocation());
						
			ScheduledTour newTour = ScheduledTour.newInstance(tourBuilder.build(), chosenTour.getVehicle(), 0);
			
			plan.getScheduledTours().remove(chosenTour);
			plan.getScheduledTours().add(newTour);
			
		return plan;
		}
		
	}
	
	
	public static ExtendedCarrierPlan switchShipmentBetweenTours(ExtendedCarrierPlan plan, Random random) {
		final class ServicePair{
			ServiceActivity firstService;
			ServiceActivity secondService;
		}
		
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		ScheduledTour firstScheduledTour;
		
		while(true) {
			Collections.shuffle(tourList, random);
			firstScheduledTour= tourList.get(0);
			if(firstScheduledTour.getTour().getTourElements().size()>=3) {
				break;
			}
		}
		
		
		if(tourList.size()>1){
			ArrayList<ServicePair> feasibleServicePairs = new ArrayList<>();
			ScheduledTour secondScheduledTour;
			int i = 1;
			while(true) {
				secondScheduledTour = tourList.get(i);
				i = i+1;
				if(secondScheduledTour.getTour().getTourElements().size()>=3) {
					break;
				}
			}
			
			int loadOfFirstTour = 0;
			for(TourElement element : firstScheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					loadOfFirstTour = loadOfFirstTour + service.getService().getCapacityDemand();
				}
			}
			
			int loadOfSecondTour = 0;
			
			for(TourElement element : secondScheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					loadOfSecondTour = loadOfSecondTour + service.getService().getCapacityDemand();
				}
			}
						
			
			for(TourElement firstElement : firstScheduledTour.getTour().getTourElements()) {
				if(firstElement instanceof ServiceActivity) {
					ServiceActivity firstService = (ServiceActivity) firstElement;
					for(TourElement secondElement : secondScheduledTour.getTour().getTourElements()) {
						if(secondElement instanceof ServiceActivity) {
							ServiceActivity secondService = (ServiceActivity) secondElement;
							double newLoadOfFirstTour = loadOfFirstTour - firstService.getService().getCapacityDemand() + secondService.getService().getCapacityDemand();
							double newLoadOfSecondTour = loadOfSecondTour - secondService.getService().getCapacityDemand() + firstService.getService().getCapacityDemand();
							
							if((newLoadOfFirstTour <= firstScheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity()) && (newLoadOfSecondTour <= secondScheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity())) {
								ServicePair pair = new ServicePair();
								pair.firstService = firstService;
								pair.secondService = secondService;
								feasibleServicePairs.add(pair);
							}
						}
					}
				}
			}
			
			if(!feasibleServicePairs.isEmpty()) {
				Collections.shuffle(feasibleServicePairs, random);
				ServicePair pair = feasibleServicePairs.get(0);
			
				Tour.Builder newFirstTourBuilder = Tour.Builder.newInstance();
				newFirstTourBuilder.scheduleStart(firstScheduledTour.getTour().getStartLinkId());

				for(TourElement element : firstScheduledTour.getTour().getTourElements()) {
					if(element instanceof Leg) {
						Leg leg = (Leg) element;
						newFirstTourBuilder.addLeg(leg);
					}
					if(element instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) element;
						if(service == pair.firstService) {
							newFirstTourBuilder.scheduleService(pair.secondService.getService());
						}
						else {
							newFirstTourBuilder.scheduleService(service.getService());
						}
					}
				}	
				newFirstTourBuilder.scheduleEnd(firstScheduledTour.getTour().getEndLinkId());
				ScheduledTour newFirstScheduledTour = ScheduledTour.newInstance(newFirstTourBuilder.build(), firstScheduledTour.getVehicle(), 0);
				
				plan.getScheduledTours().add(newFirstScheduledTour);
				plan.getScheduledTours().remove(firstScheduledTour);
				

				Tour.Builder newSecondTourBuilder = Tour.Builder.newInstance();
				newSecondTourBuilder.scheduleStart(secondScheduledTour.getTour().getStartLinkId());
				
				for(TourElement element : secondScheduledTour.getTour().getTourElements()) {
					if(element instanceof Leg) {
						Leg leg = (Leg) element;
						newSecondTourBuilder.addLeg(leg);
					}
					if(element instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) element;
						if(service == pair.secondService) {
							newSecondTourBuilder.scheduleService(pair.firstService.getService());
						}
						else {
							newSecondTourBuilder.scheduleService(service.getService());
						}
					}
				}	
				newSecondTourBuilder.scheduleEnd(secondScheduledTour.getTour().getEndLinkId());
				ScheduledTour newSecondScheduledTour = ScheduledTour.newInstance(newSecondTourBuilder.build(), secondScheduledTour.getVehicle(), 0);
				
				plan.getScheduledTours().add(newSecondScheduledTour);
				plan.getScheduledTours().remove(secondScheduledTour);				
			}
			
		}	
		else{
			switchShipmentWithinTour(plan, random);
		}
		
		return plan;
	}
	

	public static ExtendedCarrierPlan moveShipmentToAnotherTour(ExtendedCarrierPlan plan, Random random) {
			
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		ScheduledTour tourToRemoveServiceFrom = null;
		
		while(true) {
			Collections.shuffle(tourList, random);
			tourToRemoveServiceFrom = tourList.get(0);
			if(tourToRemoveServiceFrom.getTour().getTourElements().size() >=3) {
				tourList.remove(0);
				break;
			}
		}
		
		
		ArrayList<ServiceActivity> activitiesInChosenTour = new ArrayList<>();
		for(TourElement element : tourToRemoveServiceFrom.getTour().getTourElements()) {
			if(element instanceof ServiceActivity) {
				ServiceActivity service = (ServiceActivity) element;
				activitiesInChosenTour.add(service);
			}
		}
		
		Collections.shuffle(activitiesInChosenTour, random);
		ServiceActivity serviceToMove = activitiesInChosenTour.get(0);
		
		ArrayList<ScheduledTour> feasibleTours = new ArrayList<>();
		
		if(tourList.size()>1){
			for(ScheduledTour scheduledTour : tourList) {
				int totalLoad = 0;
				for(TourElement element : scheduledTour.getTour().getTourElements()) {
					if(element instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) element;
						totalLoad = totalLoad + service.getService().getCapacityDemand();
					}
				}
				if ( (totalLoad + serviceToMove.getService().getCapacityDemand()) <= scheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity()) {
					feasibleTours.add(scheduledTour);
				}
			}
			
			if(feasibleTours.size()> 0){
				
				Tour.Builder tourWithoutRemovedServiceBuilder = Tour.Builder.newInstance();
				tourWithoutRemovedServiceBuilder.scheduleStart(tourToRemoveServiceFrom.getTour().getStartLinkId());
				
				boolean legAfterRemovedService = false;
				for(TourElement element : tourToRemoveServiceFrom.getTour().getTourElements()) {
					
					if(element instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) element;
						if(service == serviceToMove) {
							legAfterRemovedService = true;
						}
						else {
							tourWithoutRemovedServiceBuilder.scheduleService(service.getService());
							legAfterRemovedService = false;
						}
					}
					if(element instanceof Leg) {
						Leg leg = (Leg) element;
						if(legAfterRemovedService == true) {

						}
						else {
							tourWithoutRemovedServiceBuilder.addLeg(leg);
						}
					}
				}
				tourWithoutRemovedServiceBuilder.scheduleEnd(tourToRemoveServiceFrom.getTour().getEndLinkId());
				ScheduledTour tourAfterServiceRemoval = ScheduledTour.newInstance(tourWithoutRemovedServiceBuilder.build(), tourToRemoveServiceFrom.getVehicle(), 0);
				
				plan.getScheduledTours().add(tourAfterServiceRemoval);
				plan.getScheduledTours().remove(tourToRemoveServiceFrom);
				
				Collections.shuffle(feasibleTours, random);
				ScheduledTour tourToInsert = feasibleTours.get(0);
				ArrayList<Leg> legList = new ArrayList<>(); 
				Leg legAfterWhichToInsert;
				for(TourElement element : tourToInsert.getTour().getTourElements()) {
					if((element instanceof Leg)) {
						legList.add((Leg) element);
					}
				}
				Collections.shuffle(legList, random);
				legAfterWhichToInsert = legList.get(0);
				
				Tour.Builder tourWithInsertedServiceBuilder = Tour.Builder.newInstance();
				tourWithInsertedServiceBuilder.scheduleStart(tourToInsert.getTour().getStartLinkId());
				
		
				for(TourElement element : tourToInsert.getTour().getTourElements()) {
					if(element instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) element;
						tourWithInsertedServiceBuilder.scheduleService(service.getService());
					}
					if((element instanceof Leg)) {
						Leg leg = (Leg) element;
						if(leg == legAfterWhichToInsert) {
							tourWithInsertedServiceBuilder.addLeg(leg);
							tourWithInsertedServiceBuilder.scheduleService(serviceToMove.getService());
							tourWithInsertedServiceBuilder.addLeg(new Leg());
						}
						else {
							tourWithInsertedServiceBuilder.addLeg(leg);
						}
					}					
				}
				
				tourWithInsertedServiceBuilder.scheduleEnd(tourToInsert.getTour().getEndLinkId());				
				ScheduledTour tourAfterServiceInsertion = ScheduledTour.newInstance(tourWithInsertedServiceBuilder.build(), tourToInsert.getVehicle(), 0);
				plan.getScheduledTours().add(tourAfterServiceInsertion);
				plan.getScheduledTours().remove(tourToInsert);
			
			}
			
			else {
				switchShipmentWithinTour(plan, random);	
			}
		}
		
		
		else {
			switchShipmentWithinTour(plan, random);		
		}
		
		
		return plan;
		
	}

	public static ExtendedCarrierPlan addFurtherVehicle(ExtendedCarrierPlan plan, Random random) {
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		Id<Link> depotId = tourList.get(0).getVehicle().getLocation();
		Id<Vehicle> vehicleId = tourList.get(0).getVehicle().getVehicleId();
		CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(vehicleId, depotId);
		vehicleBuilder.setLatestEnd(Double.MAX_VALUE);
		vehicleBuilder.setType(tourList.get(0).getVehicle().getVehicleType());
		vehicleBuilder.setTypeId(tourList.get(0).getVehicle().getVehicleTypeId());
		CarrierVehicle vehicle = vehicleBuilder.build();
		
		
		double averageNumberOfShipmentsBefore = plan.getAverageStopsPerTour();
		double averageNumberOfShipmentsAfter = (averageNumberOfShipmentsBefore * plan.getNumberOfTours())/(plan.getNumberOfTours()+1);
		averageNumberOfShipmentsAfter = Math.floor(averageNumberOfShipmentsAfter);
		
		ArrayList<ServiceActivity> servicesInNewTour = new ArrayList<>();

		int loadOfServicesInNewTour = 0;
		
		ArrayList<ScheduledTour> toursToInsertInPlan = new ArrayList<>();
		ArrayList<ScheduledTour> toursToDelete = new ArrayList<>();
		
		ArrayList<ScheduledTour> toursWithOneService = new ArrayList<>();
		
		for(ScheduledTour scheduledTour : tourList) {
			if(scheduledTour.getTour().getTourElements().size() <= 3) {
				toursWithOneService.add(scheduledTour);
			}	
		}
		
		tourList.removeAll(toursWithOneService);
		
		for(ScheduledTour scheduledTour : tourList) {
			int i = 0;
			ArrayList<ServiceActivity> servicesToDelete = new ArrayList<>();
			ArrayList<Leg> legsToDelete = new ArrayList<>();
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					double rand = random.nextDouble();
					if((rand < (averageNumberOfShipmentsAfter/plan.getNumberOfTours()) ) && ((loadOfServicesInNewTour + service.getService().getCapacityDemand()) <= scheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity())) {
						loadOfServicesInNewTour = loadOfServicesInNewTour + service.getService().getCapacityDemand();				
						servicesInNewTour.add(service);
						servicesToDelete.add(service);
						TourElement nextElement = scheduledTour.getTour().getTourElements().get(i+1);
						if(nextElement instanceof Leg) {
							Leg leg = (Leg) nextElement;
							legsToDelete.add(leg);
						}
					}	
				}
				i=i+1;
			}
			Tour.Builder newTourBuilder = Tour.Builder.newInstance(); 
			newTourBuilder.scheduleStart(scheduledTour.getTour().getStartLinkId());
			
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof Leg) {
					Leg leg = (Leg) element;
					if(!legsToDelete.contains(leg)) {
						newTourBuilder.addLeg(leg);
					}
				}	
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					if(!servicesToDelete.contains(service)) {
						newTourBuilder.scheduleService(service.getService());
					}
				}
				
			}
			
			newTourBuilder.scheduleEnd(scheduledTour.getTour().getEndLinkId());
			ScheduledTour newScheduledTour = ScheduledTour.newInstance(newTourBuilder.build(), scheduledTour.getVehicle(),0);
			toursToDelete.add(scheduledTour);
			toursToInsertInPlan.add(newScheduledTour);
			
		}
		
		Tour.Builder newTourBuilder = Tour.Builder.newInstance();
		newTourBuilder.scheduleStart(vehicle.getLocation());
				
		for(ServiceActivity serviceActivity : servicesInNewTour) {
			Leg leg = new Leg();	
			newTourBuilder.addLeg(leg);
			newTourBuilder.scheduleService(serviceActivity.getService());
		}
		
		newTourBuilder.addLeg(new Leg());
		newTourBuilder.scheduleEnd(vehicle.getLocation());
		Tour newTour = newTourBuilder.build();
		
		ScheduledTour newScheduledTour = ScheduledTour.newInstance(newTour, vehicle, 0);
		plan.getScheduledTours().add(newScheduledTour);
		plan.getScheduledTours().removeAll(toursToDelete);
		plan.getScheduledTours().addAll(toursToInsertInPlan);
		
		tourList.addAll(toursWithOneService);
		
		return plan;
	}
	
 
}

