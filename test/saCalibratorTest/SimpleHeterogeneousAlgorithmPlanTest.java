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
import org.matsim.contrib.freight.router.TimeAndSpaceTourRouter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import saCalibrator.CarrierPlanComparator;
import saCalibrator.CarrierPlanEvaluator;
import saCalibrator.DefaultTravelCosts;
import saCalibrator.DefaultTravelTime;
import saCalibrator.ExtendedCarrierPlan;
import saCalibrator.IterationInformation;
import saCalibrator.OperationsWithHeterogeneusFleet;
import saCalibrator.OperationsWithHomogeneousFleet;
import saCalibrator.PlanCopier;

public class SimpleHeterogeneousAlgorithmPlanTest {

	
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
		
		for(int i = 0 ; i < 100; i++) {
			Collections.shuffle(linkIdList, random);
			CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create("" + i, CarrierService.class), network.getLinks().get(linkIdList.get(0)).getId());
			Collections.shuffle(loadList, random);
			serviceBuilder.setCapacityDemand(loadList.get(0));
			serviceBuilder.setServiceDuration(loadList.get(0)*180);
			carrier.getServices().add(serviceBuilder.build());
		}
		return carrier;
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
	
		
	public CarrierPlan getOptimalPlan(Carrier carrier, Network network){

		
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		

		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,carrier.getCarrierCapabilities().getVehicleTypes());
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
		
		private ExtendedCarrierPlan modifyPlan(ExtendedCarrierPlan plan, Random random, TimeAndSpaceTourRouter router) {
			ArrayList<Integer> shuffleList = new ArrayList<>(Arrays.asList(1,2,3));
			Collections.shuffle(shuffleList, random);
			int strategyNumber = shuffleList.get(0);
			if(strategyNumber == 1) {
				//System.out.println("in same tour");
				return OperationsWithHeterogeneusFleet.switchShipmentWithinTour(plan, random, router);
			}
			if(strategyNumber == 2) {
				//System.out.println("between tours");
				return OperationsWithHeterogeneusFleet.switchShipmentBetweenTours(plan, random, router);
			}
			else {
				//System.out.println("other tour");
				return OperationsWithHeterogeneusFleet.moveShipmentToAnotherTour(plan, random, router);
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
		NetworkBasedTransportCosts netbasedTransportCosts = tpcostsBuilder.build();
		DefaultTravelTime defaultTime = new DefaultTravelTime();
		DefaultTravelCosts defaultCosts = new DefaultTravelCosts(carrier, defaultTime);
		TimeAndSpaceTourRouter router = new TimeAndSpaceTourRouter(new FastDijkstraFactory().createPathCalculator(network, defaultCosts, defaultTime), network, defaultTime);
		
		
		ArrayList<IterationInformation> infoList = new ArrayList<>();
		
		ExtendedCarrierPlan referencePlan = evaluator.evaluateCarrierPlan(getRealisticPlan(carrier, network, random), network);
		
		
		ExtendedCarrierPlan initialPlan = evaluator.evaluateCarrierPlan(getOptimalPlan(carrier, network), network);
		
		CarrierPlanComparator comparator = new CarrierPlanComparator();
		
		comparator.setIncumbentPlan(referencePlan);
		
		ExtendedCarrierPlan neighborPlan = initialPlan;
		
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		ExtendedCarrierPlan currentBestInnerPlan = initialPlan;
		
		IterationInformation info = new IterationInformation();
		info.setNumber(0);
		info.setBestPlan(currentBestPlan);
		info.setCurrentPlan(currentPlan);
		infoList.add(info);
		
		double innerAverageDisimprovement = getAverageInnerDisimprovement(initialPlan, comparator, random, network, evaluator, router);
		double initialInnerTemperature = (-1* innerAverageDisimprovement)/(Math.log(0.8));	
		double t_inner = initialInnerTemperature;

		double alpha = 0.90;
		double endInnerTemperature = (-1* innerAverageDisimprovement)/(Math.log(0.01));
		
		
		double outerAverageDisimprovement = getAverageOuterDisimprovement(initialPlan, comparator, random, network, evaluator, router, initialInnerTemperature, endInnerTemperature, alpha);
		double initialOuterTemperature = (-1* outerAverageDisimprovement)/(Math.log(0.8));
		double endOuterTemperature = (-1* outerAverageDisimprovement)/(Math.log(0.01));
		double t_outer = initialOuterTemperature;

		int iteration = 0;
		
		double minimumDistance = Double.MAX_VALUE;
		int numberOfImprovements = 0;
		
		while(t_outer>endOuterTemperature) {
			
			neighborPlan = OperationsWithHeterogeneusFleet.exchangeVehicleType(currentPlan, random, router);
			System.out.println("Vehicle Type exchanged");
			t_inner = initialInnerTemperature;
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			currentBestInnerPlan = neighborPlan;
			ExtendedCarrierPlan currentInnerPlan = neighborPlan;
			while(t_inner>endInnerTemperature) {
				iteration++;
				IterationInformation information  = new IterationInformation();
				information.setNumber(iteration);
				information.setInnerTemperature(t_inner);
				information.setCurrentPlan(currentInnerPlan);
				ExtendedCarrierPlan innerNeighborPlan = modifyPlan(currentInnerPlan, random, router);
				information.setNeighborPlan(innerNeighborPlan);
				innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
				comparator.setEntrantPlan(currentBestPlan);
				double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(innerNeighborPlan);
				double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(currentBestInnerPlan);
				double distanceToBestInnerPlan = comparator.getDistanceToBestPlan();
				System.out.println(iteration + "  " + distanceToNeighborPlan);
				
				if(distanceToNeighborPlan < minimumDistance) {
					minimumDistance = distanceToNeighborPlan;
				}
							
				if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
					System.out.println("improvement");
					numberOfImprovements++;
					currentBestPlan = innerNeighborPlan;
					currentInnerPlan = innerNeighborPlan;
					currentBestInnerPlan = innerNeighborPlan;
					information.setCurrentPlan(innerNeighborPlan);
					information.setBestPlan(innerNeighborPlan);
					t_inner = alpha *t_inner;
				}
				else {	
					if(distanceToNeighborPlan <= distanceToBestInnerPlan) {
						currentBestInnerPlan = innerNeighborPlan;
						currentInnerPlan = innerNeighborPlan;
						t_inner = alpha *t_inner;
					}
					else {
						double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
						double rnd = random.nextDouble();
						if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
							currentInnerPlan = innerNeighborPlan;
							information.setCurrentPlan(innerNeighborPlan);
							t_inner = alpha *t_inner;	
						}
						else {
								t_inner = alpha *t_inner;
						}
					}
				}
				//infoList.add(information);
			}
			
			neighborPlan  = currentBestInnerPlan;
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			comparator.setEntrantPlan(currentBestPlan);
			double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
			comparator.setEntrantPlan(neighborPlan);
			double distanceToNeighborPlan = comparator.getDistanceToBestPlan();

			if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
				numberOfImprovements++;
				currentBestPlan = neighborPlan;
				currentPlan = neighborPlan;
				t_outer = alpha *t_outer;	
			}
			else {	
				double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
				double rnd = random.nextDouble();
				if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
					currentPlan = neighborPlan;
					t_outer = alpha *t_outer;
				}
				else {
					t_outer = alpha *t_outer;
				}
			}
		}
		marke:
		for(int i = 0 ; i < initialPlan.getNumberOfTours()/2; i++) {
			t_inner = initialInnerTemperature;
			t_outer = initialOuterTemperature;
			int iterationAtCurrentTemperature = 0;
			currentPlan = OperationsWithHeterogeneusFleet.addFurtherVehicle(currentPlan, random, router);
			System.out.println("further vehcile added");
			while(t_outer>endOuterTemperature) {
				iterationAtCurrentTemperature++;
				neighborPlan = OperationsWithHeterogeneusFleet.exchangeVehicleType(currentPlan, random, router);
				System.out.println("Vehicle Type exchanged");
				neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);

				t_inner = initialInnerTemperature;
				int iterationAtCurrentInnerTemperature = 0;
				currentBestInnerPlan = neighborPlan;
				ExtendedCarrierPlan currentInnerPlan = neighborPlan;
				
				while(t_inner>endInnerTemperature) {
					iterationAtCurrentInnerTemperature++;
					iteration++;
					IterationInformation information  = new IterationInformation();
					information.setNumber(iteration);
					information.setInnerTemperature(t_inner);
					information.setCurrentPlan(currentInnerPlan);
					ExtendedCarrierPlan innerNeighborPlan = modifyPlan(currentInnerPlan, random, router);
					information.setNeighborPlan(innerNeighborPlan);
					innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
					comparator.setEntrantPlan(currentBestPlan);
					double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
					comparator.setEntrantPlan(innerNeighborPlan);
					double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
					comparator.setEntrantPlan(currentBestInnerPlan);
					double distanceToBestInnerPlan = comparator.getDistanceToBestPlan();
					System.out.println(iteration + "  " + distanceToNeighborPlan);
					
										
					if(distanceToNeighborPlan < minimumDistance) {
						minimumDistance = distanceToNeighborPlan;
					}
								
					if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
						System.out.println("improvement");
						numberOfImprovements++;
						currentBestPlan = innerNeighborPlan;
						currentInnerPlan = innerNeighborPlan;
						currentBestInnerPlan = innerNeighborPlan;
						information.setCurrentPlan(innerNeighborPlan);
						information.setBestPlan(innerNeighborPlan);
						if(distanceToNeighborPlan < 1) {
							break marke;
						}
						if(iterationAtCurrentInnerTemperature == 1) {
							iterationAtCurrentInnerTemperature = 0;
							t_inner = alpha *t_inner;
						}
					}
					
					
					else {	
						if(distanceToNeighborPlan <= distanceToBestInnerPlan) {
							currentBestInnerPlan = innerNeighborPlan;
							currentInnerPlan = innerNeighborPlan;
							if(iterationAtCurrentInnerTemperature == 1) {
								iterationAtCurrentInnerTemperature = 0;
								t_inner = alpha *t_inner;
							}
						}
						double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
						double rnd = random.nextDouble();
						if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
							currentInnerPlan = innerNeighborPlan;
							information.setCurrentPlan(innerNeighborPlan);
							if(iterationAtCurrentInnerTemperature == 1) {
								iterationAtCurrentInnerTemperature = 0;
								t_inner = alpha *t_inner;
							}
						}
						else {
							if(iterationAtCurrentInnerTemperature == 1) {
								iterationAtCurrentInnerTemperature = 0;
								t_inner = alpha *t_inner;
							}
						}
					}
					//infoList.add(information);
				}
				
				neighborPlan  = currentBestInnerPlan;
				neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
				comparator.setEntrantPlan(currentBestPlan);
				double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(neighborPlan);
				double distanceToNeighborPlan = comparator.getDistanceToBestPlan();

				if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
					numberOfImprovements++;
					currentBestPlan = neighborPlan;
					currentPlan = neighborPlan;
					if(iterationAtCurrentTemperature == 2) {
						iterationAtCurrentTemperature = 0;
						t_outer = alpha *t_outer;
					}
				}
				else {	
					double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
					double rnd = random.nextDouble();
					if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
						currentPlan = neighborPlan;
						if(iterationAtCurrentTemperature == 2) {
							iterationAtCurrentTemperature = 0;
							t_outer = alpha *t_outer;
						}
					}
					else {
						if(iterationAtCurrentTemperature == 2) {
							iterationAtCurrentTemperature = 0;
							t_outer = alpha *t_outer;
						}
					}
				}
			}
			
		}

		System.out.println(numberOfImprovements);
		System.out.println(minimumDistance);
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
	}
	
	private double getAverageInnerDisimprovement(ExtendedCarrierPlan initialPlan, CarrierPlanComparator comparator, Random random, Network network,CarrierPlanEvaluator evaluator,TimeAndSpaceTourRouter router) {
		System.out.println("Inner warmup starts");
		ArrayList<Double> disimprovements = new ArrayList<>();
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		
		
		for(int i = 0; i < 100; i++) {
			comparator.setEntrantPlan(currentBestPlan);
			double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
			ExtendedCarrierPlan neighborPlan = modifyPlan(currentPlan, random, router);
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			//NetworkRouter.routePlan(neighborPlan, netbasedTransportcosts);
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
		System.out.println("Inner warmup ends");
		return averageDisimprovement;
	}
	
	private double getAverageOuterDisimprovement(ExtendedCarrierPlan initialPlan, CarrierPlanComparator comparator, Random random, Network network,CarrierPlanEvaluator evaluator,TimeAndSpaceTourRouter router, double initialInnerTemperature, double endInnerTemperature, double alpha) {
		System.out.println("Outer warmup starts");
		ArrayList<Double> disimprovements = new ArrayList<>();
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		ExtendedCarrierPlan currentBestInnerPlan = initialPlan;
		ExtendedCarrierPlan neighborPlan;
		
		for(int i   = 0; i < 100; i++) {
			double t_inner = initialInnerTemperature;
			neighborPlan = OperationsWithHeterogeneusFleet.exchangeVehicleType(currentPlan, random, router);
			ExtendedCarrierPlan currentInnerPlan = neighborPlan;
				
			while(t_inner>endInnerTemperature) {
					
				ExtendedCarrierPlan innerNeighborPlan = modifyPlan(currentInnerPlan, random, router);
				innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
				comparator.setEntrantPlan(currentBestPlan);
				double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(innerNeighborPlan);
				double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(currentBestInnerPlan);
				double distanceToBestInnerPlan = comparator.getDistanceToBestPlan();
				
				if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
					currentBestPlan = innerNeighborPlan;
					currentInnerPlan = innerNeighborPlan;
					currentBestInnerPlan = innerNeighborPlan;
					t_inner = alpha *t_inner;
				}
				else {	
					if(distanceToNeighborPlan <= distanceToBestInnerPlan) {
						currentBestInnerPlan = innerNeighborPlan;
						currentInnerPlan = innerNeighborPlan;
						t_inner = alpha *t_inner;
					}
					else {
						double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
						double rnd = random.nextDouble();
						if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
							currentInnerPlan = innerNeighborPlan;
							t_inner = alpha *t_inner;
						}
						else {
							t_inner = alpha *t_inner;
						}
					}
				}
			}
				
				neighborPlan  = currentBestInnerPlan;
				neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
				comparator.setEntrantPlan(currentBestPlan);
				double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(neighborPlan);
				double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
				if(distanceToNeighborPlan > distanceToCurrentBestPlan) {
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
		System.out.println("Outer warmup ends");
		return averageDisimprovement;
	}
	
}
