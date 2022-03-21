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


package saCalibratorTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.VehicleType;

import saCalibrator.CarrierPlanEvaluator;
import saCalibrator.ExtendedCarrierPlan;
import saCalibrator.OperationsWithHomogeneousFleet;

public class AddFurtherVehicleTest {
	public Carrier getReferenceCarrier(Network network, Random random) {
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("6_tonner", VehicleType.class));
		vehicleTypeBuilder.setCapacity(6000);
		vehicleTypeBuilder.setCostPerDistanceUnit(6.0);
		vehicleTypeBuilder.setFixCost(1000);
		vehicleTypeBuilder.setCostPerTimeUnit(0);
		CarrierVehicleType vehicleType = vehicleTypeBuilder.build();
		
			
		Id<Carrier> id = Id.create("defaultCarrier", Carrier.class);
		Carrier carrier = CarrierImpl.newInstance(id);
		
		
		ArrayList<Id<Link>> linkIdList = new ArrayList(network.getLinks().keySet());
		Collections.shuffle(linkIdList, random);
		
		Link depotLink = network.getLinks().get(linkIdList.get(0));
		CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("defaultVehicle"), depotLink.getId());
		vehicleBuilder.setEarliestStart(0);
		vehicleBuilder.setLatestEnd(Double.MAX_VALUE);
		vehicleBuilder.setType(vehicleType);
		vehicleBuilder.setTypeId(vehicleType.getId());
		
		
		carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
		carrier.getCarrierCapabilities().getVehicleTypes().add(vehicleType);
		carrier.getCarrierCapabilities().getCarrierVehicles().add(vehicleBuilder.build());
		
		ArrayList<Integer> loadList = new ArrayList<>(Arrays.asList(1000));
		
		for(int i = 0 ; i < 20; i++) {
			Collections.shuffle(linkIdList, random);
			CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create("" + i, CarrierService.class), network.getLinks().get(linkIdList.get(0)).getId());
			Collections.shuffle(loadList, random);
			serviceBuilder.setCapacityDemand(loadList.get(0));
			serviceBuilder.setServiceDuration(loadList.get(0)*180);
			carrier.getServices().add(serviceBuilder.build());
		}
		return carrier;
	}
	
	public CarrierPlan getRealisticPlan(Carrier carrier, Network network) {

		CarrierVehicle vehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		
		ArrayList<CarrierService> services = new ArrayList<>();
		for(CarrierService service : carrier.getServices()) {
			services.add(service);
		}
		
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
		
		while(!services.isEmpty()) {
			ArrayList<ScheduledTour> tours = new ArrayList<>();
			int cumulativeCapacity = 0;
			Tour.Builder tourBuilder = Tour.Builder.newInstance();
			tourBuilder.scheduleStart(vehicle.getLocation());
			
			ArrayList<CarrierService> servicesToBeRemoved = new ArrayList<>();
			for(CarrierService service : services) {
				if((cumulativeCapacity + service.getCapacityDemand() > vehicle.getVehicleType().getCarrierVehicleCapacity())) {
					break;
				}
				else {
					cumulativeCapacity = cumulativeCapacity + service.getCapacityDemand();
					servicesToBeRemoved.add(service);
					Leg leg = new Leg();
					tourBuilder.addLeg(leg);
					tourBuilder.scheduleService(service);				
				}
			}
			Leg leg = new Leg();
			tourBuilder.addLeg(leg);
			tourBuilder.scheduleEnd(vehicle.getLocation());
			Tour tour = tourBuilder.build();
			ScheduledTour scheduledTour = ScheduledTour.newInstance(tour, vehicle, 0);
			scheduledTours.add(scheduledTour);
			services.removeAll(servicesToBeRemoved);
		}
		
		CarrierPlan plan = new CarrierPlan(carrier, scheduledTours);
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,carrier.getCarrierCapabilities().getVehicleTypes() );
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		
		return plan;	
		
	}

	@Test
	public void testAdditionOfFurtherVehicle() {
		Random random = new Random(1);
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("input/network.xml");
		Carrier carrier = getReferenceCarrier(network, random);
		CarrierPlan realisticPlan = getRealisticPlan(carrier, network);
		CarrierPlanEvaluator evaluator = new CarrierPlanEvaluator();
		ExtendedCarrierPlan extendedPlan = evaluator.evaluateCarrierPlan(realisticPlan, network);
		
		ArrayList<ScheduledTour> tourList = new ArrayList<>(extendedPlan.getScheduledTours());
		
		OperationsWithHomogeneousFleet.addFurtherVehicle(extendedPlan, random);
	}

}
