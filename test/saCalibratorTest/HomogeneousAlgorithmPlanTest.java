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
import java.util.Random;
import java.util.Map.Entry;

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
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import saCalibrator.CarrierPlanComparator;
import saCalibrator.CarrierPlanEvaluator;
import saCalibrator.ExtendedCarrierPlan;
import saCalibrator.IterationInformation;
import saCalibrator.OperationsWithHomogeneousFleet;
import saCalibrator.PlanCopier;

public class HomogeneousAlgorithmPlanTest {

	
		private Carrier getReferenceCarrier(Network network, Random random) {
			CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("6_tonner", VehicleType.class));
			vehicleTypeBuilder.setCapacity(6000);
			vehicleTypeBuilder.setCostPerDistanceUnit(6.0);
			vehicleTypeBuilder.setFixCost(1000);
			vehicleTypeBuilder.setCostPerTimeUnit(0);
			CarrierVehicleType vehicleType = vehicleTypeBuilder.build();
			
				
			Id<Carrier> id = Id.create("defaultCarrier", Carrier.class);
			Carrier carrier = CarrierImpl.newInstance(id);
			
			
			ArrayList<Id<Link>> linkIdList = new ArrayList<>(network.getLinks().keySet());
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
		
		private CarrierPlan getOptimalPlan(Carrier carrier, Network network){

			
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			

			NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,carrier.getCarrierCapabilities().getVehicleTypes() );
			NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
			vrpBuilder.setRoutingCost(netbasedTransportcosts);
			VehicleRoutingProblem vrp = vrpBuilder.build();
			
			VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp).buildAlgorithm();
			vra.setMaxIterations(10);
			Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
			VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);
		
			CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
			
			NetworkRouter.routePlan(plan, netbasedTransportcosts);
			
			return plan;
					
		}
	
		
		private ExtendedCarrierPlan modifyPlan(ExtendedCarrierPlan plan, Random random) {
			ArrayList<Integer> shuffleList = new ArrayList<>(Arrays.asList(1,2,3));
			Collections.shuffle(shuffleList, random);
			int strategyNumber = shuffleList.get(0);
			if(strategyNumber == 1) {
				//System.out.println("in same tour");
				return OperationsWithHomogeneousFleet.switchShipmentWithinTour(plan, random);
			}
			if(strategyNumber == 2) {
				//System.out.println("between tours");
				return OperationsWithHomogeneousFleet.switchShipmentBetweenTours(plan, random);
			}
			else {
				//System.out.println("other tour");
				return OperationsWithHomogeneousFleet.moveShipmentToAnotherTour(plan, random);
			}
		}
		
		
		
	@Test
	public void TestAlgorithmPlan() {
		Random random = new Random(1);
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("input/berlinNetwork.xml");
		Carrier carrier = getReferenceCarrier(network, random);
		CarrierPlanEvaluator evaluator = new CarrierPlanEvaluator();
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,carrier.getCarrierCapabilities().getVehicleTypes() );
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		ArrayList<IterationInformation> infoList = new ArrayList<>();
		
		ExtendedCarrierPlan referencePlan = evaluator.evaluateCarrierPlan(getRealisticPlan(carrier, network), network);
		
		ExtendedCarrierPlan initialPlan = evaluator.evaluateCarrierPlan(getOptimalPlan(carrier, network), network);
		
		CarrierPlanComparator comparator = new CarrierPlanComparator();
		
		comparator.setIncumbentPlan(referencePlan);
		
		ExtendedCarrierPlan neighborPlan = initialPlan;
		
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		
		IterationInformation info = new IterationInformation();
		info.setNumber(0);
		info.setBestPlan(currentBestPlan);
		info.setCurrentPlan(currentPlan);
		infoList.add(info);
		
		
		double avrageDisimprovement = getAverageDisimprovement(initialPlan, comparator, random, network, evaluator,netbasedTransportcosts);
		double initialTemperature = (-1* avrageDisimprovement)/(Math.log(0.8));
		
		double t = initialTemperature;
		
		double alpha = 0.90;
		
		
		double endTemperature = (-1* avrageDisimprovement)/(Math.log(0.01));
		int iteration = 0;
		int iterationAtCurrentTemperature = 0;
		
		double minimumDistance = Double.MAX_VALUE;
		
		int numberOfImprovements = 0;
		
		while(t>endTemperature) {
			iterationAtCurrentTemperature++;
			iteration++;
			IterationInformation information = new IterationInformation();
			information.setNumber(iteration);
			information.setInnerTemperature(t);
			information.setCurrentPlan(currentPlan);
			neighborPlan = modifyPlan(currentPlan, random);
			information.setNeighborPlan(neighborPlan);
			NetworkRouter.routePlan(neighborPlan, netbasedTransportcosts);
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			comparator.setEntrantPlan(currentBestPlan);
			double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
			comparator.setEntrantPlan(neighborPlan);
			double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
			System.out.println(iteration + "  " + distanceToNeighborPlan);
			if(distanceToNeighborPlan < minimumDistance) {
				minimumDistance = distanceToNeighborPlan;
			}
			if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
				System.out.println("improvement");
				numberOfImprovements++;
				currentBestPlan = neighborPlan;
				currentPlan = neighborPlan;
				information.setCurrentPlan(neighborPlan);
				information.setBestPlan(neighborPlan);
				if(iterationAtCurrentTemperature==10) {
					iterationAtCurrentTemperature = 0;
					t = alpha *t;
				}
			}
			else {	
				double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
				double rnd = random.nextDouble();
				if(rnd < Math.exp((-1* distanceOfDistances)/(t))) {
					currentPlan = neighborPlan;
					information.setCurrentPlan(neighborPlan);
					if(iterationAtCurrentTemperature==10) {
						iterationAtCurrentTemperature = 0;
						t = alpha *t;
					}
				}
				else {
					if(iterationAtCurrentTemperature==10) {
						iterationAtCurrentTemperature = 0;
						t = alpha *t;
					}
				}
			}
			infoList.add(information);
		}
	
		
		for(int i = 0 ; i < initialPlan.getNumberOfTours(); i++) {
			t = initialTemperature;
			iterationAtCurrentTemperature = 0;
			currentPlan  = OperationsWithHomogeneousFleet.addFurtherVehicle(currentPlan, random);
			NetworkRouter.routePlan(currentPlan, netbasedTransportcosts);
			while(t>endTemperature) {
				iteration++;
				iterationAtCurrentTemperature++;
				IterationInformation information = new IterationInformation();
				information.setNumber(iteration);
				information.setInnerTemperature(t);
				information.setCurrentPlan(currentPlan);
				neighborPlan = modifyPlan(currentPlan, random);
				information.setNeighborPlan(neighborPlan);
				NetworkRouter.routePlan(neighborPlan, netbasedTransportcosts);
				neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
				comparator.setEntrantPlan(currentBestPlan);
				double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(neighborPlan);
				double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
				System.out.println(iteration + "  " + distanceToNeighborPlan);
				if(distanceToNeighborPlan < minimumDistance) {
					minimumDistance = distanceToNeighborPlan;
				}
				if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
					System.out.println("improvement");
					numberOfImprovements++;
					currentBestPlan = neighborPlan;
					currentPlan = neighborPlan;
					information.setCurrentPlan(neighborPlan);
					information.setBestPlan(neighborPlan);
					if(iterationAtCurrentTemperature==10) {
						iterationAtCurrentTemperature = 0;
						t = alpha *t;
					}
				}
				else {
					double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
					double rnd = random.nextDouble();
					if(rnd < Math.exp((-1* distanceOfDistances)/(t))) {
						currentPlan = neighborPlan;
						information.setCurrentPlan(neighborPlan);
						if(iterationAtCurrentTemperature==10) {
							iterationAtCurrentTemperature = 0;
							t = alpha *t;
						}
					}
					else {
						if(iterationAtCurrentTemperature==10) {
							iterationAtCurrentTemperature = 0;
							t = alpha *t;
						}
					}
				}
				infoList.add(information);
			}
		}
		System.out.println("referencePlan");
		System.out.println("Average Capacity Utilization: " +referencePlan.getAverageCapacityUtilization());
		System.out.println("Average Distance Between Stops: " +referencePlan.getAverageDistanceBetweenStops());
		System.out.println("Average Stops Per Tour: " +referencePlan.getAverageStopsPerTour());
		System.out.println("Average Tour Length: " + referencePlan.getAverageTourLength());
		System.out.println("Number Of Tours: "+ referencePlan.getNumberOfTours());
		System.out.println("Overall Length: " +referencePlan.getOverallLength());
		for(Entry entry : referencePlan.getVehicleMap().entrySet()) {
			System.out.println(entry);
		}
		System.out.println("initialPlan (jsprit)");
		System.out.println("Average Capacity Utilization: " + initialPlan.getAverageCapacityUtilization());
		System.out.println("Average Distance Between Stops: " +initialPlan.getAverageDistanceBetweenStops());
		System.out.println("Average Stops Per Tour: " +initialPlan.getAverageStopsPerTour());
		System.out.println("Average Tour Length: " + initialPlan.getAverageTourLength());
		System.out.println("Number Of Tours: "+ initialPlan.getNumberOfTours());
		System.out.println("Overall Length: " +initialPlan.getOverallLength());
		for(Entry entry : initialPlan.getVehicleMap().entrySet()) {
			System.out.println(entry);
		}
		System.out.println("bestPlan");
		System.out.println("Average Capacity Utilization: " +currentBestPlan.getAverageCapacityUtilization());
		System.out.println("Average Distance Between Stops: " +currentBestPlan.getAverageDistanceBetweenStops());
		System.out.println("Average Stops Per Tour: " +currentBestPlan.getAverageStopsPerTour());
		System.out.println("Average Tour Length: " +currentBestPlan.getAverageTourLength());
		System.out.println("Number Of Tours: " + currentBestPlan.getNumberOfTours());
		System.out.println("Overall Length: " + currentBestPlan.getOverallLength());
		for(Entry entry : currentBestPlan.getVehicleMap().entrySet()) {
			System.out.println(entry);
		}
		System.out.println(minimumDistance);
	}
	
	private double getAverageDisimprovement(ExtendedCarrierPlan initialPlan, CarrierPlanComparator comparator, Random random, Network network,CarrierPlanEvaluator evaluator, NetworkBasedTransportCosts netbasedTransportcosts ) {
		ArrayList<Double> disimprovements = new ArrayList<>();
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		
		
		for(int i = 0; i < 100; i++) {
			comparator.setEntrantPlan(currentBestPlan);
			double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
			ExtendedCarrierPlan neighborPlan = modifyPlan(currentPlan, random);
			NetworkRouter.routePlan(neighborPlan, netbasedTransportcosts);
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			comparator.setEntrantPlan(neighborPlan);
			double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
			if(distanceToNeighborPlan  > distanceToCurrentBestPlan) {
				disimprovements.add(Math.abs(distanceToNeighborPlan-distanceToCurrentBestPlan));
				currentPlan = neighborPlan;
			}
			else {	
				currentPlan = neighborPlan;
				currentBestPlan = neighborPlan;
			}
		}
		
		double averageDisimprovement  = 0;
		for(Double dis : disimprovements) {
			averageDisimprovement = averageDisimprovement + dis;
		}
		averageDisimprovement = averageDisimprovement/disimprovements.size();
		
		return averageDisimprovement;
	}
	
}
