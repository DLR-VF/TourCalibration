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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.router.TimeAndSpaceTourRouter;
import org.matsim.vehicles.Vehicle;


public class AdaptiveOperationsWithHeterogeneusFleet {

		
	public static ExtendedCarrierPlan switchShipmentWithinTour(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
	
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
			router.route(newTour);
			plan.getScheduledTours().remove(chosenTour);
			plan.getScheduledTours().add(newTour);

		return plan;
		}
		
	}
	
	
	public static ExtendedCarrierPlan switchShipmentBetweenTours(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
		final class ServicePair{
			ScheduledTour firstTour;
			ScheduledTour secondTour;
			ServiceActivity firstService;
			ServiceActivity secondService;
		}
		
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		
		ArrayList<ServicePair> feasibleServicePairs = new ArrayList<>();
		
		for(ScheduledTour firstScheduledTour : tourList) {
			for(ScheduledTour secondScheduledTour : tourList) {
				if(firstScheduledTour.getTour().getTourElements().size()>=3 && secondScheduledTour.getTour().getTourElements().size()>=3 && !areToursEqual(firstScheduledTour, secondScheduledTour)) {
					
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
										pair.firstTour = firstScheduledTour;
										pair.secondTour = secondScheduledTour;
										feasibleServicePairs.add(pair);
									}
								}
							}
						}
					}
				}
			}
		}
		
		
		
			if(!feasibleServicePairs.isEmpty()) {
				Collections.shuffle(feasibleServicePairs, random);
				ServicePair pair = feasibleServicePairs.get(0);
						
				Tour.Builder newFirstTourBuilder = Tour.Builder.newInstance();
				newFirstTourBuilder.scheduleStart(pair.firstTour.getTour().getStartLinkId());

				for(TourElement element : pair.firstTour.getTour().getTourElements()) {
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
				
				newFirstTourBuilder.scheduleEnd(pair.firstTour.getTour().getEndLinkId());
				ScheduledTour newFirstScheduledTour = ScheduledTour.newInstance(newFirstTourBuilder.build(), pair.firstTour.getVehicle(), 0);
				router.route(newFirstScheduledTour);
				
				plan.getScheduledTours().add(newFirstScheduledTour);
				plan.getScheduledTours().remove(pair.firstTour);
				
			
				Tour.Builder newSecondTourBuilder = Tour.Builder.newInstance();
				newSecondTourBuilder.scheduleStart(pair.secondTour.getTour().getStartLinkId());
	
				for(TourElement element : pair.secondTour.getTour().getTourElements()) {
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
				newSecondTourBuilder.scheduleEnd(pair.secondTour.getTour().getEndLinkId());
				ScheduledTour newSecondScheduledTour = ScheduledTour.newInstance(newSecondTourBuilder.build(), pair.secondTour.getVehicle(), 0);
				router.route(newSecondScheduledTour);
				
				plan.getScheduledTours().add(newSecondScheduledTour);
				plan.getScheduledTours().remove(pair.secondTour);				
			}

		return plan;
	}
	

	public static ExtendedCarrierPlan moveShipmentToAnotherTour(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
		final class ServiceAndTours{
			ServiceActivity service;
			ScheduledTour releasingTour;
			ScheduledTour receivingTour;
		}
		
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		ArrayList<ServiceAndTours> feasibleServicesToMove = new ArrayList<>();
		
		for(ScheduledTour firstScheduledTour : tourList) {
			for(ScheduledTour secondScheduledTour : tourList) {
				if(!areToursEqual(firstScheduledTour, secondScheduledTour)) {
					for(TourElement firstTourElement : firstScheduledTour.getTour().getTourElements()) {
						if(firstTourElement instanceof ServiceActivity){
							ServiceActivity candidateService = (ServiceActivity) firstTourElement;
							int totalLoad = 0;
							for(TourElement secondTourElement : secondScheduledTour.getTour().getTourElements()) {
								if(secondTourElement instanceof ServiceActivity) {
									ServiceActivity secondTourService = (ServiceActivity) secondTourElement;
									totalLoad = totalLoad + secondTourService.getService().getCapacityDemand();
								}
							}
							if ( (totalLoad + candidateService.getService().getCapacityDemand()) <= secondScheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity()) {
								ServiceAndTours serviceAndTours = new ServiceAndTours();
								serviceAndTours.releasingTour = firstScheduledTour;
								serviceAndTours.receivingTour = secondScheduledTour;
								serviceAndTours.service = candidateService;
								feasibleServicesToMove.add(serviceAndTours);
							}
						}
					}
				}
			}
		}
		
		
			
		if(!feasibleServicesToMove.isEmpty()){
			Collections.shuffle(feasibleServicesToMove, random);
			ServiceAndTours serviceAndTours = feasibleServicesToMove.get(0);
			
			Tour.Builder tourWithoutRemovedServiceBuilder = Tour.Builder.newInstance();
			tourWithoutRemovedServiceBuilder.scheduleStart(serviceAndTours.releasingTour.getTour().getStartLinkId());
				
			boolean legAfterRemovedService = false;
			for(TourElement element : serviceAndTours.releasingTour.getTour().getTourElements()) {
					
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					if(service == serviceAndTours.service) {
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
				tourWithoutRemovedServiceBuilder.scheduleEnd(serviceAndTours.releasingTour.getTour().getEndLinkId());
				ScheduledTour tourAfterServiceRemoval = ScheduledTour.newInstance(tourWithoutRemovedServiceBuilder.build(), serviceAndTours.releasingTour.getVehicle(), 0);
				router.route(tourAfterServiceRemoval);
				
				plan.getScheduledTours().add(tourAfterServiceRemoval);
				plan.getScheduledTours().remove(serviceAndTours.releasingTour);
				
				
				ScheduledTour tourToInsert = serviceAndTours.receivingTour;
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
							tourWithInsertedServiceBuilder.scheduleService(serviceAndTours.service.getService());
							tourWithInsertedServiceBuilder.addLeg(new Leg());
						}
						else {
							tourWithInsertedServiceBuilder.addLeg(leg);
						}
					}					
				}
				
				tourWithInsertedServiceBuilder.scheduleEnd(tourToInsert.getTour().getEndLinkId());				
				ScheduledTour tourAfterServiceInsertion = ScheduledTour.newInstance(tourWithInsertedServiceBuilder.build(), tourToInsert.getVehicle(), 0);
				router.route(tourAfterServiceInsertion);
				
				plan.getScheduledTours().add(tourAfterServiceInsertion);
				plan.getScheduledTours().remove(tourToInsert);
			}
		
		return plan;
	}

	public static ExtendedCarrierPlan addFurtherVehicle(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		ArrayList<CarrierVehicleType> typeList = new ArrayList<>(plan.getCarrier().getCarrierCapabilities().getVehicleTypes());
		Collections.shuffle(typeList, random);
		CarrierVehicleType newType = typeList.get(0);
		
		
		
		Id<Link> depotId = tourList.get(0).getVehicle().getLocation();
		Id<Vehicle> newVehicleId = Id.createVehicleId(newType.getId().toString());
		CarrierVehicle.Builder newVehicleBuilder = CarrierVehicle.Builder.newInstance(newVehicleId, depotId);
		newVehicleBuilder.setLatestEnd(Double.MAX_VALUE);
		newVehicleBuilder.setType(newType);
		newVehicleBuilder.setTypeId(newType.getId());
		CarrierVehicle newVehicle = newVehicleBuilder.build();
		
		
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
					if((rand < (averageNumberOfShipmentsAfter/plan.getNumberOfTours()) ) && ((loadOfServicesInNewTour + service.getService().getCapacityDemand()) <= newVehicle.getVehicleType().getCarrierVehicleCapacity())) {
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
			Tour.Builder builderOfTourWithRemovedServices = Tour.Builder.newInstance(); 
			builderOfTourWithRemovedServices.scheduleStart(scheduledTour.getTour().getStartLinkId());
			
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof Leg) {
					Leg leg = (Leg) element;
					if(!legsToDelete.contains(leg)) {
						builderOfTourWithRemovedServices.addLeg(leg);
					}
				}	
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					if(!servicesToDelete.contains(service)) {
						builderOfTourWithRemovedServices.scheduleService(service.getService());
					}
				}
				
			}
			
			builderOfTourWithRemovedServices.scheduleEnd(scheduledTour.getTour().getEndLinkId());
			ScheduledTour scheduledTourWithRemovedServices = ScheduledTour.newInstance(builderOfTourWithRemovedServices.build(), scheduledTour.getVehicle(),0);
			toursToDelete.add(scheduledTour);
			toursToInsertInPlan.add(scheduledTourWithRemovedServices);
			
		}
		
		if(!servicesInNewTour.isEmpty()) {
			Tour.Builder builderOfAdditionalTour = Tour.Builder.newInstance();
			builderOfAdditionalTour.scheduleStart(newVehicle.getLocation());
					
			
			for(ServiceActivity serviceActivity : servicesInNewTour) {
				Leg leg = new Leg();	
				builderOfAdditionalTour.addLeg(leg);
				builderOfAdditionalTour.scheduleService(serviceActivity.getService());
			}
			
			builderOfAdditionalTour.addLeg(new Leg());
			builderOfAdditionalTour.scheduleEnd(newVehicle.getLocation());
			Tour tourOfAdditionalVehicle = builderOfAdditionalTour.build();
			ScheduledTour scheduledTourOfAdditionalVehicle = ScheduledTour.newInstance(tourOfAdditionalVehicle, newVehicle, 0);
			router.route(scheduledTourOfAdditionalVehicle);
			plan.getScheduledTours().add(scheduledTourOfAdditionalVehicle);
		}
		
		plan.getScheduledTours().removeAll(toursToDelete);
		
		for(ScheduledTour scheduledTour : toursToInsertInPlan) {
			router.route(scheduledTour);
		}
		
		plan.getScheduledTours().addAll(toursToInsertInPlan);
		
		tourList.addAll(toursWithOneService);
		
		return plan;
	}
	
	public static ExtendedCarrierPlan exchangeVehicleType(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		ArrayList<CarrierVehicleType> typeList = new ArrayList<>(plan.getCarrier().getCarrierCapabilities().getVehicleTypes());
		ArrayList <TourVehicleTypePair> feasibleVehiclesAndTours = new ArrayList<>();
		
		
		for(ScheduledTour scheduledTour : tourList) {
			int totalLoadOfTour = 0;
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					totalLoadOfTour = totalLoadOfTour + service.getService().getCapacityDemand();
				}
			}
			
			for(CarrierVehicleType type : typeList) {
				if(type.getId() != scheduledTour.getVehicle().getVehicleType().getId() && totalLoadOfTour < type.getCarrierVehicleCapacity()) {
					TourVehicleTypePair feasiblePair = new TourVehicleTypePair();
					feasiblePair.tour = scheduledTour;
					feasiblePair.type = type;
					feasibleVehiclesAndTours.add(feasiblePair);
				}
			}
		}
		
		
		
		
		if(!feasibleVehiclesAndTours.isEmpty()) {
		
			Collections.shuffle(feasibleVehiclesAndTours, random);
			ScheduledTour tourToExchangeVehicle = feasibleVehiclesAndTours.get(0).tour;
			CarrierVehicleType newType = 	feasibleVehiclesAndTours.get(0).type;
			Id<Link> depotId = tourToExchangeVehicle.getVehicle().getLocation();
			Id<Vehicle> newVehicleId = Id.createVehicleId(newType.getId().toString());
			CarrierVehicle.Builder newVehicleBuilder = CarrierVehicle.Builder.newInstance(newVehicleId, depotId);
			newVehicleBuilder.setLatestEnd(Double.MAX_VALUE);
			newVehicleBuilder.setType(newType);
			newVehicleBuilder.setTypeId(newType.getId());
			CarrierVehicle newVehicle = newVehicleBuilder.build();
		
			Tour.Builder newTourBuilder = Tour.Builder.newInstance();
			newTourBuilder.scheduleStart(depotId);
			for(TourElement element : tourToExchangeVehicle.getTour().getTourElements()) {
				if(element instanceof Leg){
					Leg leg = (Leg) element;
					newTourBuilder.addLeg(leg);
				}
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					newTourBuilder.scheduleService(service.getService());
				}
			}
			
			
			newTourBuilder.scheduleEnd(depotId);
			ScheduledTour newScheduledTour = ScheduledTour.newInstance(newTourBuilder.build(), newVehicle, 0);
			router.route(newScheduledTour);
			
			plan.getScheduledTours().remove(tourToExchangeVehicle);
			plan.getScheduledTours().add(newScheduledTour);
		}
	
		return plan;
	}

	public static ExtendedCarrierPlan removeVehicle(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
		
		ArrayList<ScheduledTour> tourList = new ArrayList<>(plan.getScheduledTours());
		Collections.sort(tourList, new LoadComparator());
		ArrayList<TourCapacityPair> toursToMoveShipmentsTo = new ArrayList<>();
		ArrayList<ScheduledTour> toursThatCanBeRemoved = new ArrayList<>();
		
		//toursToMoveShipmentsTo = getSortedToursToInsert(tourList);
		
		tours:
		for(int indexOfTourToBeRemoved = 0; indexOfTourToBeRemoved < tourList.size(); indexOfTourToBeRemoved++) {

			ScheduledTour tourToRemove = tourList.get(indexOfTourToBeRemoved);

			toursToMoveShipmentsTo = getSortedToursToInsert(tourList);
			ArrayList<CarrierService> sortedServices = getSortedCarrierServices(tourToRemove);
			
			if(sortedServices.isEmpty()) {
				continue;
			}
			
			for(CarrierService service : sortedServices) {
				boolean serviceAssigned = false;
				Collections.sort(toursToMoveShipmentsTo, new TourCapacityComparator());
				for(TourCapacityPair pair : toursToMoveShipmentsTo) {
					if(areToursEqual(pair.tour, tourToRemove)) {
						continue;
					}
					if(pair.capacity < service.getCapacityDemand() && serviceAssigned == false) {
						serviceAssigned = false;
					}
					if(pair.capacity >= service.getCapacityDemand() && serviceAssigned == false) {
						pair.capacity = pair.capacity - service.getCapacityDemand();
						serviceAssigned = true;
					}
				}	
				if(serviceAssigned == false) {
					continue tours;
				}
			}	
			toursThatCanBeRemoved.add(tourToRemove);
		}

		if(!toursThatCanBeRemoved.isEmpty()) {
			Collections.shuffle(toursThatCanBeRemoved, random);
			ScheduledTour tourToRemove = toursThatCanBeRemoved.get(0);
			ArrayList<CarrierService> sortedServices = getSortedCarrierServices(tourToRemove);
			tourList.remove(tourToRemove);
			toursToMoveShipmentsTo.clear();
			toursToMoveShipmentsTo = getSortedToursToInsert(tourList);
			
			HashMap<ScheduledTour, ArrayList<CarrierService>> newRemainingTours = new HashMap<>();
			
			for(ScheduledTour scheduledTour : tourList) {
				ArrayList<CarrierService> serviceList = new ArrayList<>();
				newRemainingTours.put(scheduledTour, serviceList);
			}
			
			services:
			for(CarrierService service : sortedServices) {
				Collections.sort(toursToMoveShipmentsTo, new TourCapacityComparator());
				for(TourCapacityPair pair : toursToMoveShipmentsTo) {
					if(pair.capacity < service.getCapacityDemand()) {
						//nix
					}
					else {
						pair.capacity = pair.capacity - service.getCapacityDemand();
						newRemainingTours.get(pair.tour).add(service);
						continue services;
					}
				}
			}
	
			plan.getScheduledTours().clear();
			
			for(Entry<ScheduledTour, ArrayList<CarrierService>> entry : newRemainingTours.entrySet()) {
				Tour.Builder tourBuilder = Tour.Builder.newInstance();
				tourBuilder.scheduleStart(entry.getKey().getTour().getStartLinkId());
				for(TourElement element : entry.getKey().getTour().getTourElements()) {
					if(element instanceof Leg) {
						Leg leg = (Leg) element;
						tourBuilder.addLeg(leg);
					}
					if(element instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) element;
						tourBuilder.scheduleService(service.getService());
					}
				}
				if(!entry.getValue().isEmpty()) {
					for(CarrierService service : entry.getValue()) {
						tourBuilder.scheduleService(service);
						tourBuilder.addLeg(new Leg());
					}
					

				}
				tourBuilder.scheduleEnd(entry.getKey().getTour().getEndLinkId());
				ScheduledTour newTour = ScheduledTour.newInstance(tourBuilder.build(), entry.getKey().getVehicle(), 0);
				router.route(newTour);
				plan.getScheduledTours().add(newTour);
			}
		
		}
		return plan;
	}

	
	public static ExtendedCarrierPlan removeEmptyTours(ExtendedCarrierPlan plan) {
		ArrayList<ScheduledTour> emptyTours = new ArrayList<>();
		for(ScheduledTour scheduledTour :plan.getScheduledTours()) {
			if(scheduledTour.getTour().getTourElements().size() < 3) {
				emptyTours.add(scheduledTour);
			}
		}
		if(!emptyTours.isEmpty()) {
			System.out.println("removed " + emptyTours.size() + " emptyTours");
		}
		ArrayList<ScheduledTour> remainingTours = new ArrayList<>();
		for(ScheduledTour scheduledTour : plan.getScheduledTours()) {
			if(!emptyTours.contains(scheduledTour)) {
				remainingTours.add(scheduledTour);
			}
		}
		plan.getScheduledTours().clear();
		plan.getScheduledTours().addAll(remainingTours);
			
		return plan;
	}
	
	
	public static boolean areToursEqual(ScheduledTour tour1, ScheduledTour tour2) {
		boolean equal = true;
		if(tour1.getTour().getTourElements().size() != tour2.getTour().getTourElements().size()) {
			return false;
		}
		
		for(TourElement element : tour1.getTour().getTourElements()) {
			if(element instanceof ServiceActivity) {
				ServiceActivity service = (ServiceActivity) element;
				int index = tour1.getTour().getTourElements().indexOf(element);
				TourElement secondElement = tour2.getTour().getTourElements().get(index);
				if(secondElement instanceof ServiceActivity) {
					ServiceActivity secondService = (ServiceActivity) secondElement;
					if(service.getService() == secondService.getService()) {	
				
					}
					else {
						return false;
					}
				}
				else {
					return false;
				}
			}
		}
		
		return equal;
	}
	
	
	static class TourCapacityPair {
		ScheduledTour tour;
		int capacity;
	}
	
	private static ArrayList<TourCapacityPair>getSortedToursToInsert(ArrayList<ScheduledTour> tourList){
		ArrayList<TourCapacityPair> pairs = new ArrayList<>();
		pairs = getRemainingCapacities(tourList);
		Collections.sort(pairs, new TourCapacityComparator());
		return pairs;
	}
	
	
	private static ArrayList<TourCapacityPair> getRemainingCapacities(ArrayList<ScheduledTour> tourList){
		ArrayList<TourCapacityPair> capaList = new ArrayList<>();
		for(ScheduledTour scheduledTour : tourList) {
			TourCapacityPair pair = new TourCapacityPair();
			pair.tour = scheduledTour;
			int capa = scheduledTour.getVehicle().getVehicleType().getCarrierVehicleCapacity();
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					capa = capa - service.getService().getCapacityDemand();
				}
			}
			pair.capacity = capa;
			capaList.add(pair);
		}
		return capaList;
	}

	static class TourCapacityComparator implements Comparator<TourCapacityPair>{

		@Override
		public int compare(TourCapacityPair arg0, TourCapacityPair arg1) {
			if(arg0.capacity < arg1.capacity) {
				return -1;
			}
			if(arg0.capacity > arg1.capacity) {
				return 1;
			}
			else {
				return 0;
			}	
		}
	}

	static class LoadComparator implements Comparator<ScheduledTour>{
		
		private int getLoad(ScheduledTour scheduledTour) {
			int load = 0;
			for(TourElement element : scheduledTour.getTour().getTourElements()) {
				if(element instanceof ServiceActivity) {
					ServiceActivity service = (ServiceActivity) element;
					load = load + service.getService().getCapacityDemand();
				}
			}
			return load;
		}
		
		@Override
		public int compare(ScheduledTour o1, ScheduledTour o2) {
			int loadO1 = getLoad(o1);
			int loadO2 = getLoad(o2);
			if(loadO1 < loadO2) {
				return -1;
			}
			if(loadO1 > loadO2) {
				return 1;
			}
			else {
				return 0;
			}
		}	
	}

	
	private static ArrayList<CarrierService> getSortedCarrierServices(ScheduledTour scheduledTour){
		ArrayList<CarrierService> services = new ArrayList<>();
		for(TourElement element : scheduledTour.getTour().getTourElements()) {
			if(element instanceof ServiceActivity) {
				ServiceActivity service = (ServiceActivity) element;
				services.add(service.getService());
			}
		}
		Collections.sort(services, new ShipmentSizeComparator());
		Collections.reverse(services);
		return services;
	}
	
	static class ShipmentSizeComparator  implements Comparator<CarrierService>{

		@Override
		public int compare(CarrierService o1, CarrierService o2) {
			if(o1.getCapacityDemand() < o2.getCapacityDemand()) {
				return -1;
			}
			if(o1.getCapacityDemand() > o2.getCapacityDemand()) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	static class TourVehicleTypePair {
		ScheduledTour tour;
		CarrierVehicleType type;
	}
	
}

