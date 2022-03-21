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
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Before;
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
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import saCalibrator.CarrierPlanEvaluator;
import saCalibrator.ExtendedCarrierPlan;


public class HeterogeneousReferenceCase {

	
	public Carrier getReferenceCarrier(Network network, Random random) {
		CarrierVehicleType.Builder sechsTonnerTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("6_tonner", VehicleType.class));
		sechsTonnerTypeBuilder.setCapacity(6000);
		sechsTonnerTypeBuilder.setCostPerDistanceUnit(6.0);
		sechsTonnerTypeBuilder.setFixCost(1000);
		sechsTonnerTypeBuilder.setCostPerTimeUnit(0);
		CarrierVehicleType sechsTonnerType = sechsTonnerTypeBuilder.build();
		
		
		CarrierVehicleType.Builder siebenTonnerTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("7_tonner", VehicleType.class));
		siebenTonnerTypeBuilder.setCapacity(7000);
		siebenTonnerTypeBuilder.setCostPerDistanceUnit(7.0);
		siebenTonnerTypeBuilder.setFixCost(1000);
		siebenTonnerTypeBuilder.setCostPerTimeUnit(0);
		CarrierVehicleType siebenTonnerType = siebenTonnerTypeBuilder.build();
		
		Id<Carrier> id = Id.create("defaultCarrier", Carrier.class);
		Carrier carrier = CarrierImpl.newInstance(id);
		
		
		ArrayList<Id<Link>> linkIdList = new ArrayList<>(network.getLinks().keySet());
		Collections.shuffle(linkIdList, random);
		
		Link depotLink = network.getLinks().get(linkIdList.get(0));
		CarrierVehicle.Builder sechsTonnerBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("sechsTonner"), depotLink.getId());
		sechsTonnerBuilder.setEarliestStart(0);
		sechsTonnerBuilder.setLatestEnd(Double.MAX_VALUE);
		sechsTonnerBuilder.setType(sechsTonnerType);
		sechsTonnerBuilder.setTypeId(sechsTonnerType.getId());
		
		
		carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
		carrier.getCarrierCapabilities().getVehicleTypes().add(sechsTonnerType);
		carrier.getCarrierCapabilities().getCarrierVehicles().add(sechsTonnerBuilder.build());
		
		
		
		
		CarrierVehicle.Builder siebenTonnerBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("siebenTonner"), depotLink.getId());
		siebenTonnerBuilder.setEarliestStart(0);
		siebenTonnerBuilder.setLatestEnd(Double.MAX_VALUE);
		siebenTonnerBuilder.setType(siebenTonnerType);
		siebenTonnerBuilder.setTypeId(siebenTonnerType.getId());
		
		CarrierVehicle siebenTonner = siebenTonnerBuilder.build();
		
		carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
		carrier.getCarrierCapabilities().getVehicleTypes().add(siebenTonnerType);
		carrier.getCarrierCapabilities().getCarrierVehicles().add(siebenTonner);
		
		
		ArrayList<Integer> loadList = new ArrayList<>(Arrays.asList(1000,2000,3000,4000,5000));
		
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
	
	
	
	
	public CarrierPlan getOptimalPlan(Carrier carrier, Network network){

	
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		

		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,carrier.getCarrierCapabilities().getVehicleTypes());
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		vrpBuilder.setRoutingCost(netbasedTransportcosts);
		VehicleRoutingProblem vrp = vrpBuilder.build();
		
		VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp).buildAlgorithm();
		vra.setMaxIterations(10);
		vra.searchSolutions();
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
		VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);
	
		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
		
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		
		return plan;
				
	}
	
	public CarrierPlan getRealisticPlan(Carrier carrier, Network network, Random random) {

		ArrayList<CarrierVehicle> vehicleList = new ArrayList<>(carrier.getCarrierCapabilities().getCarrierVehicles());
		CarrierVehicle vehicle;
		ArrayList<CarrierService> services = new ArrayList<>();
		for(CarrierService service : carrier.getServices()) {
			services.add(service);
		}
		
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
		
		while(!services.isEmpty()) {
			if(random.nextDouble() <0.5) {
				vehicle = vehicleList.get(0);
			}
			else {
				vehicle = vehicleList.get(1);
			}
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
	public void testPlans() {
		Random random = new Random(1);
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("input/network.xml");
		Carrier carrier = getReferenceCarrier(network, random);
		CarrierPlanEvaluator evaluator = new CarrierPlanEvaluator();
		
		CarrierPlan optimalPlan = getOptimalPlan(carrier, network);
		ExtendedCarrierPlan extendedOptimalPlan = evaluator.evaluateCarrierPlan(optimalPlan, network);
		for(Entry entry: extendedOptimalPlan.getVehicleMap().entrySet()) {
			System.out.println(entry);
		}
		
		CarrierPlan realisticPlan = getRealisticPlan(carrier, network, random);
		ExtendedCarrierPlan extendedRealisticPlan = evaluator.evaluateCarrierPlan(realisticPlan, network);
		for(Entry entry: extendedRealisticPlan.getVehicleMap().entrySet()) {
			System.out.println(entry);
		}
	
	}	
	
}
