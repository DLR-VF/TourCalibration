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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
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

import saCalibrator.AdaptiveOperationsWithHeterogeneusFleet;
import saCalibrator.CarrierPlanComparator;
import saCalibrator.CarrierPlanEvaluator;
import saCalibrator.DefaultTravelCosts;
import saCalibrator.DefaultTravelTime;
import saCalibrator.ExtendedCarrierPlan;
import saCalibrator.InfoListEvaluator;
import saCalibrator.IterationInformation;
import saCalibrator.OperationsWithHeterogeneusFleet;
import saCalibrator.OperationsWithHomogeneousFleet;
import saCalibrator.PlanCopier;

public class AdaptiveHeterogeneousAlgorithmPlanTest {

	
	public Carrier getReferenceCarrier(Network network, Random random) {
		CarrierVehicleType.Builder fuenfzehnTonnerTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("6_tonner", VehicleType.class));
		fuenfzehnTonnerTypeBuilder.setCapacity(6000);
		fuenfzehnTonnerTypeBuilder.setCostPerDistanceUnit(6.0);
		fuenfzehnTonnerTypeBuilder.setFixCost(1000);
		fuenfzehnTonnerTypeBuilder.setCostPerTimeUnit(0);
		CarrierVehicleType fuenfzehnTonnerType = fuenfzehnTonnerTypeBuilder.build();
		
		
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
		CarrierVehicle.Builder fuenfzehnTonnerBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId(fuenfzehnTonnerType.getId()), depotLink.getId());
		fuenfzehnTonnerBuilder.setEarliestStart(0);
		fuenfzehnTonnerBuilder.setLatestEnd(Double.MAX_VALUE);
		fuenfzehnTonnerBuilder.setType(fuenfzehnTonnerType);
		fuenfzehnTonnerBuilder.setTypeId(fuenfzehnTonnerType.getId());
		
		
		carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
		carrier.getCarrierCapabilities().getVehicleTypes().add(fuenfzehnTonnerType);
		carrier.getCarrierCapabilities().getCarrierVehicles().add(fuenfzehnTonnerBuilder.build());
		
		
		
		
		CarrierVehicle.Builder siebenTonnerBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId(siebenTonnerType.getId()), depotLink.getId());
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
			serviceBuilder.setServiceDuration(loadList.get(0)/1000*180);
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
		
		private ExtendedCarrierPlan modifyTours(ExtendedCarrierPlan plan, Random random, Integer strategyNumber, TimeAndSpaceTourRouter router) {
			
			if(strategyNumber == 1) {
				System.out.println("in same tour");
				return AdaptiveOperationsWithHeterogeneusFleet.switchShipmentWithinTour(plan, random, router);
			}
			if(strategyNumber == 2) {
				System.out.println("between tours");
				return AdaptiveOperationsWithHeterogeneusFleet.switchShipmentBetweenTours(plan, random, router);
			}
			else {
				System.out.println("other tour");
				return AdaptiveOperationsWithHeterogeneusFleet.moveShipmentToAnotherTour(plan, random, router);
			}
		}
		
		private ExtendedCarrierPlan modifyFleet(ExtendedCarrierPlan plan, Random random, Integer strategyNumber, TimeAndSpaceTourRouter router) {
			
			if(strategyNumber == 1) {	
				System.out.println("add vehicle");
				return AdaptiveOperationsWithHeterogeneusFleet.addFurtherVehicle(plan, random, router);
			}
			if(strategyNumber == 2) {
				System.out.println("exchange vehicle type");
				return AdaptiveOperationsWithHeterogeneusFleet.exchangeVehicleType(plan, random, router);
			}
			else {
				System.out.println("remove vehicle");
				return AdaptiveOperationsWithHeterogeneusFleet.removeVehicle(plan, random, router);
			}
		}
		

		private HashMap<Integer,Integer>getInitialStrategyMap(){
			HashMap<Integer,Integer> strategyMap = new HashMap<>();
			for(int i = 1; i < 4; i++) {
				strategyMap.put(i, 1);
			}
			
			return strategyMap;
		}
		
		public int getNextStrategy(HashMap<Integer,Integer> strategyMap, Random random) {
			int strategy = 0;
			double allWeights = 0;
			for(Entry<Integer,Integer> entry : strategyMap.entrySet()) {
				allWeights = allWeights + entry.getValue();
			}

			double rnd = random.nextDouble();
			double lowerBound = 0;
			
			for(Entry<Integer,Integer> entry : strategyMap.entrySet()) {
				double upperBound = lowerBound + (entry.getValue()/allWeights);
				if(rnd < upperBound && rnd > lowerBound) {
					return entry.getKey();
				}
				else {
					lowerBound = lowerBound + (entry.getValue()/allWeights);
				}
			}	
			return strategy;
		}
		
		
	@Test
	public void TestAlgorithmPlan() throws Exception{
		//Global random with seed for all calculations that are not from other libraries and have their own random number generator
		Random random = new Random(1);
		
		//Initializations that are necessary to route vehicles on a matsim network
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("input/berlinNetwork.xml");
		Carrier carrier = getReferenceCarrier(network, random);
		CarrierPlanEvaluator evaluator = new CarrierPlanEvaluator();
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,carrier.getCarrierCapabilities().getVehicleTypes() );
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		DefaultTravelTime defaultTime = new DefaultTravelTime();
		DefaultTravelCosts defaultCosts = new DefaultTravelCosts(carrier, defaultTime);
		TimeAndSpaceTourRouter router = new TimeAndSpaceTourRouter(new FastDijkstraFactory().createPathCalculator(network, defaultCosts, defaultTime), network, defaultTime);
		
		
		ArrayList<IterationInformation> infoList = new ArrayList<>();
		
		//Plan with tours that have the desired shapes. In reality, there has to be no complete plan. The CarrierPlan within the ExtendedCarrierPlan can also be empty and
		// only the tour characteristics can be full
		ExtendedCarrierPlan referencePlan = evaluator.evaluateCarrierPlan(getRealisticPlan(carrier, network, random), network);
		
		//Optimal plan created with jsprit
		ExtendedCarrierPlan initialPlan = evaluator.evaluateCarrierPlan(getOptimalPlan(carrier, network), network);
		
		//Compares two ExtendedCarrierPlans
		CarrierPlanComparator comparator = new CarrierPlanComparator();
		comparator.setIncumbentPlan(referencePlan);
		
		//All plans are now set to the initial (optimal) plan
		ExtendedCarrierPlan neighborPlan = initialPlan;
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		ExtendedCarrierPlan currentBestInnerPlan = initialPlan;
		
		IterationInformation info = new IterationInformation();
		info.setNumber(0);
		info.setBestPlan(currentBestPlan);
		info.setCurrentPlan(currentPlan);
		infoList.add(info);
		
		double innerAverageDisimprovement = getAverageInnerDisimprovement(initialPlan, comparator, random, network, evaluator,router);
		double initialInnerTemperature = (-1* innerAverageDisimprovement)/(Math.log(0.8));
		double t_inner = initialInnerTemperature;

		double alpha = 0.90;
		double endInnerTemperature = (-1* innerAverageDisimprovement)/(Math.log(0.01));
		
		
		double outerAverageDisimprovement = getAverageOuterDisimprovement(initialPlan, comparator, random, network, evaluator, router, initialInnerTemperature, endInnerTemperature, alpha);
		double initialOuterTemperature = (-1* outerAverageDisimprovement)/(Math.log(0.8));
		double endOuterTemperature = (-1* outerAverageDisimprovement)/(Math.log(0.01));
		double t_outer = initialOuterTemperature;

		int iteration = 0;
		int iterationAtCurrentTemperature = 0;
		int numberOfImprovements = 0;
		
		double minimumDistance = Double.MAX_VALUE;
		double distanceTolerance = 0.02;
		
		HashMap<Integer, Integer> outerStrategyMap = getInitialStrategyMap();
				
		while(t_outer>endOuterTemperature && minimumDistance >= distanceTolerance) {
			iterationAtCurrentTemperature++;
			
			int currentStrategy = getNextStrategy(outerStrategyMap, random);	
			neighborPlan =  modifyFleet(currentPlan, random, currentStrategy, router);
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
				
				//if a vehicle was to be removed, but this was not feasible, choose one of the other strategies
				if((currentStrategy ==3) && (currentPlan.getNumberOfTours() == neighborPlan.getNumberOfTours())) {
					double rnd = random.nextDouble();
					if(rnd < 0.5) {
						currentStrategy = 1;
					}
					else {
						currentStrategy = 2;
					}
					neighborPlan =  modifyFleet(currentPlan, random, currentStrategy, router);
					neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
					//If a vehicle type was to be switched, but this was not feasible, add a further vehicle (works always, empty tours are removed later)
					if((currentStrategy ==2) && sameFleet(currentPlan, neighborPlan)) {
						currentStrategy = 1;
						neighborPlan =  modifyFleet(currentPlan, random, currentStrategy, router);
						neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
					}			
				}
				
				//If a vehicle type was to be switched, but this was not feasible, choose one of the other strategies
				if((currentStrategy ==2) && sameFleet(currentPlan, neighborPlan)) {
						double rnd = random.nextDouble();
						if(rnd < 0.5) {
							currentStrategy = 1;
						}
						else {
							currentStrategy = 3;
						}
				
						neighborPlan =  modifyFleet(currentPlan, random, currentStrategy, router);
						neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
						//if a vehicle was to be removed, but this was not feasible, add a further vehicle (works always, empty tours are removed later)
						if((currentStrategy ==3) && (currentPlan.getNumberOfTours() == neighborPlan.getNumberOfTours())) {
							currentStrategy =1;
							neighborPlan =  modifyFleet(currentPlan, random, currentStrategy, router);
							neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
						}
				}			
			
			t_inner = initialInnerTemperature;			
			
			int iterationAtCurrentInnerTemperature = 0;
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			currentBestInnerPlan = neighborPlan;
			ExtendedCarrierPlan currentInnerPlan = neighborPlan;	
			
			HashMap<Integer, Integer> innerStrategyMap = getInitialStrategyMap();
			
			while(t_inner>endInnerTemperature) {	
				iteration++;
				iterationAtCurrentInnerTemperature++;
				IterationInformation information  = new IterationInformation();
				information.setNumber(iteration);
				information.setInnerTemperature(t_inner);
				information.setOuterTemperature(t_outer);
				information.setCurrentPlan(currentInnerPlan);
				
				//get inner strategy and execute it
				int currentInnerStrategy = getNextStrategy(innerStrategyMap, random);
				ExtendedCarrierPlan innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
				innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
				
				//If a service was to be switched between two tours, but this is not feasible, choose one of the other strategies
				if(currentInnerStrategy == 2 && arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
					double rnd = random.nextDouble();
					if(rnd < 0.5) {
						currentInnerStrategy = 3;
						innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
						innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
						//If a service was to be moved to another tour and this is not feasible, switch two services in the same tour (works always)
						if(arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
							currentInnerStrategy = 1;
							innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
							innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
						}
					}
				}
				//If a service was to be moved to another tour and this is not feasible, choose one of the other strategies
				if(currentInnerStrategy == 3 && arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
					double rnd = random.nextDouble();
					if(rnd < 0.5) {
						currentInnerStrategy = 2;
						innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
						innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
						//If a service was to be switched between two tours, but this is not feasible, switch two services in the same tour (works always)
						if(arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
							currentInnerStrategy = 1;
							innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
							innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
						}
					}
				}				
				innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
				
				
				//Empty tours that can come about, are removed before the evaluation for the solutions
				AdaptiveOperationsWithHeterogeneusFleet.removeEmptyTours(innerNeighborPlan);
				
				//Plans are evaluated before comparison and then compared
				currentPlan = evaluator.evaluateCarrierPlan(currentPlan, network);
				innerNeighborPlan = evaluator.evaluateCarrierPlan(innerNeighborPlan, network);
				comparator.setEntrantPlan(currentBestPlan);
				double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(innerNeighborPlan);
				double distanceToNeighborPlan = comparator.getDistanceToBestPlan();
				comparator.setEntrantPlan(currentBestInnerPlan);
				double distanceToBestInnerPlan = comparator.getDistanceToBestPlan();
				System.out.println(iteration + "  " + distanceToNeighborPlan);
				
				//Distance to best plan ever found is checked and updated
				if(distanceToNeighborPlan < minimumDistance) {
					minimumDistance = distanceToNeighborPlan;
				}
				
				//What to do if the found plan is better than the absolute best plan 				
				if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
					//Improve probability of outer strategy, as it contributed to the inner loop that led to the improvement 
					numberOfImprovements++;
					int weightOfCurrentOuterStrategy = outerStrategyMap.get(currentStrategy);
					weightOfCurrentOuterStrategy++;
					outerStrategyMap.put(currentStrategy, weightOfCurrentOuterStrategy);
					//Improve probability of inner strategy (improvement only holds within the current inner loop)
					int weightOfCurrentInnerStrategy = innerStrategyMap.get(currentInnerStrategy);
					weightOfCurrentInnerStrategy++;
					innerStrategyMap.put(currentInnerStrategy, weightOfCurrentInnerStrategy);
					//As the found plan is better than anything before, it is also the new best inner (currentBestInnerPlan) and outer plan (currentBestPlan) and the plan to move ahead from (currentInnerPlan)
					currentBestPlan = innerNeighborPlan;
					currentInnerPlan = innerNeighborPlan;
					currentBestInnerPlan = innerNeighborPlan;
					information.setNeighborPlan(innerNeighborPlan); 
					information.setCurrentPlan(innerNeighborPlan);
					information.setBestPlan(innerNeighborPlan);
					//Determines how many iterations of the inner loop are performed at the current inner temperature
					if(iterationAtCurrentInnerTemperature == 1) {
						iterationAtCurrentInnerTemperature = 0;
						t_inner = alpha *t_inner;
					}	
				}
				//What to do if the found plan is better than the absolute best plan
				else {	
					//What to do if the found plan is at least better than the best plan in the current inner loop
					if(distanceToNeighborPlan <= distanceToBestInnerPlan) {
						//As the found plan is better than the best plan in the current inner loop, it is the new best inner (currentBestInnerPlan) and the plan to move ahead from (currentInnerPlan)
						currentBestInnerPlan = innerNeighborPlan;
						currentInnerPlan = innerNeighborPlan;
						information.setNeighborPlan(innerNeighborPlan);
						information.setCurrentPlan(innerNeighborPlan);
						//Improve probability of inner strategy (improvement only holds within the current inner loop). 
						//Only the inner strategy was better, not the combination of the two strategies, as it is not the best plan in both loops
						int weightOfCurrentInnerStrategy = innerStrategyMap.get(currentInnerStrategy);
						weightOfCurrentInnerStrategy++;
						innerStrategyMap.put(currentInnerStrategy, weightOfCurrentInnerStrategy);
						//Determines how many iterations of the inner loop are performed at the current inner temperature
						if(iterationAtCurrentInnerTemperature == 1) {
							iterationAtCurrentInnerTemperature = 0;
							t_inner = alpha *t_inner;
						}
					}
					//What to do if the found plan is not an improvement in any respect
					else {
						//Determine acceptance probability
						double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
						double rnd = random.nextDouble();
							//What do to if inferior plan is accepted as new plan to proceed further from
							if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
								currentInnerPlan = innerNeighborPlan;
								information.setNeighborPlan(innerNeighborPlan);
								information.setCurrentPlan(innerNeighborPlan);
								//Determines how many iterations of the inner loop are performed at the current inner temperature
								if(iterationAtCurrentInnerTemperature == 1) {
									iterationAtCurrentInnerTemperature = 0;
									t_inner = alpha *t_inner;	
								}
							}
							//What to do, if the inferior plan is not accepted
							else {
								information.setNeighborPlan(innerNeighborPlan);
								//Determines how many iterations of the inner loop are performed at the current inner temperature
								if(iterationAtCurrentInnerTemperature == 1) {
									iterationAtCurrentInnerTemperature = 0;
									t_inner = alpha *t_inner;	
								}	
						}
					}
			  }
				infoList.add(information);
			}	

			//The best plan of the inner loop, i.e. the best outcome for the current outer strategy is now chosen as
			//the starting point for the comparison with the current and the best overall plan
			neighborPlan  = currentBestInnerPlan;
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			comparator.setEntrantPlan(currentBestPlan);
			double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
			comparator.setEntrantPlan(neighborPlan);
			double distanceToNeighborPlan = comparator.getDistanceToBestPlan();

			//What do to if the plan found in the inner loop is better than any plan before on the outer loop
			if(distanceToNeighborPlan <= distanceToCurrentBestPlan) {
				//The best overall plan is replaced with the plan found in the inner loop. The latter is also the starting point to proceed further
				currentBestPlan = neighborPlan;
				currentPlan = neighborPlan;
				//Determines how many iterations of the outer loop are performed at the current outer temperature
				if(iterationAtCurrentTemperature == 15) {
					iterationAtCurrentTemperature = 0;
					t_outer = alpha *t_outer;	
				}
				
			}
			//What do to if the plan found in the inner loop is not better than any plan before on the outer loop
			else {	
				//Calculate the acceptance probability
				double distanceOfDistances = Math.abs(distanceToCurrentBestPlan-distanceToNeighborPlan);
				double rnd = random.nextDouble();
				if(rnd < Math.exp((-1* distanceOfDistances)/(t_inner))) {
					//The found plan is the one to proceed further from
					currentPlan = neighborPlan;
					//Determines how many iterations of the outer loop are performed at the current outer temperature
					if(iterationAtCurrentTemperature == 15) {
						iterationAtCurrentTemperature = 0;
						t_outer = alpha *t_outer;
					}	
				}
				else {
					//Determines how many iterations of the outer loop are performed at the current outer temperature
					if(iterationAtCurrentTemperature == 15) {
						iterationAtCurrentTemperature = 0;
						t_outer = alpha *t_outer;
					}
				}
			}
		}
		
		InfoListEvaluator infoEvaluator = new InfoListEvaluator(infoList, referencePlan);
		//infoEvaluator.createOverallDistanceLineChart();
		//infoEvaluator.createObjectiveFunctionLineChart();
		
		for (Entry entry : outerStrategyMap.entrySet()) {
			System.out.println(entry);
		}
		
		System.out.println(numberOfImprovements);
		
		evaluator.evaluateCarrierPlan(currentBestPlan, network);
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
		System.out.println("Standardized deviation between plans");
		System.out.println("Average Capacity Utilization: " + Math.abs((referencePlan.getAverageCapacityUtilization()- currentBestPlan.getAverageCapacityUtilization())/referencePlan.getAverageCapacityUtilization()));
		System.out.println("Average Distance Between Stops: " + Math.abs((referencePlan.getAverageDistanceBetweenStops()- currentBestPlan.getAverageDistanceBetweenStops())/referencePlan.getAverageDistanceBetweenStops()));
		System.out.println("Average Stops Per Tour: " + Math.abs((referencePlan.getAverageStopsPerTour()- currentBestPlan.getAverageStopsPerTour())/referencePlan.getAverageStopsPerTour()));
		System.out.println("Average Tour Length: " + Math.abs((referencePlan.getAverageTourLength()- currentBestPlan.getAverageTourLength())/referencePlan.getAverageTourLength()));
		System.out.println("Number Of Tours: " + Math.abs((referencePlan.getNumberOfTours()- currentBestPlan.getNumberOfTours())/referencePlan.getNumberOfTours()));
		System.out.println("Overall Length: " +  Math.abs((referencePlan.getOverallLength()- currentBestPlan.getOverallLength())/referencePlan.getOverallLength()));
		System.out.println("VehicleTypes: " + comparator.getDistanceBetweenVehicleTypes(referencePlan, currentBestPlan));
		
	}
	
	private double getAverageInnerDisimprovement(ExtendedCarrierPlan initialPlan, CarrierPlanComparator comparator, Random random, Network network,CarrierPlanEvaluator evaluator,TimeAndSpaceTourRouter router) {
		System.out.println("Inner warmup starts");
		ArrayList<Double> disimprovements = new ArrayList<>();
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		HashMap<Integer, Integer> innerStrategyMap = getInitialStrategyMap();
		
		for(int i = 0; i < 100; i++) {
			comparator.setEntrantPlan(currentBestPlan);
			double distanceToCurrentBestPlan = comparator.getDistanceToBestPlan();
			int currentStrategy = getNextStrategy(innerStrategyMap, random);
			ExtendedCarrierPlan neighborPlan = modifyTours(currentPlan, random, currentStrategy, router);
			if(currentStrategy == 2 && arePlansEqual(neighborPlan, currentPlan)) {
				double rnd = random.nextDouble();
				if(rnd < 0.5) {
					currentStrategy = 3;
					neighborPlan = modifyTours(currentPlan, random, currentStrategy, router);
					if(arePlansEqual(neighborPlan, currentPlan)) {
						currentStrategy = 1;
						neighborPlan = modifyTours(currentPlan, random, currentStrategy, router);
					}
				}
			}
			if(currentStrategy == 3 && arePlansEqual(neighborPlan, currentPlan)) {
				double rnd = random.nextDouble();
				if(rnd < 0.5) {
					currentStrategy = 2;
					neighborPlan = modifyTours(currentPlan, random, currentStrategy, router);
					if(arePlansEqual(neighborPlan, currentPlan)) {
						currentStrategy = 1;
						neighborPlan = modifyTours(currentPlan, random, currentStrategy, router);
					}
				}
			}
			AdaptiveOperationsWithHeterogeneusFleet.removeEmptyTours(currentPlan);
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
				int weightOfCurrentStrategy = innerStrategyMap.get(currentStrategy);
				weightOfCurrentStrategy++;
				innerStrategyMap.put(currentStrategy, weightOfCurrentStrategy);
				
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
		ArrayList<Integer> strategies = new ArrayList<>(Arrays.asList(1,2,3));
		ExtendedCarrierPlan currentPlan = initialPlan;
		ExtendedCarrierPlan currentBestPlan = initialPlan;
		ExtendedCarrierPlan currentBestInnerPlan = initialPlan;
		ExtendedCarrierPlan neighborPlan;
		HashMap<Integer, Integer> outerStrategyMap = getInitialStrategyMap();
		
		for(int i   = 0; i < 100; i++) {
			double t_inner = initialInnerTemperature;
			int currentOuterStrategy = getNextStrategy(outerStrategyMap, random);
		
			neighborPlan = modifyFleet(currentPlan, random, currentOuterStrategy, router);
			
			if((currentOuterStrategy ==3) && (currentPlan.getNumberOfTours() == neighborPlan.getNumberOfTours())) {
				double rnd = random.nextDouble();
				if(rnd < 0.5) {
					currentOuterStrategy = 1;
				}
				else {
					currentOuterStrategy = 2;
				}
				neighborPlan =  modifyFleet(currentPlan, random, currentOuterStrategy, router);
				neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
				if((currentOuterStrategy ==2) && sameFleet(currentPlan, neighborPlan)) {
					currentOuterStrategy = 1;
					neighborPlan =  modifyFleet(currentPlan, random, currentOuterStrategy, router);
				}
			}
			
			if((currentOuterStrategy ==2) && sameFleet(currentPlan, neighborPlan)) {
				double rnd = random.nextDouble();
				if(rnd < 0.5) {
					currentOuterStrategy = 1;
				}
				else {
					currentOuterStrategy =3;
				}
				
				neighborPlan =  modifyFleet(currentPlan, random, currentOuterStrategy, router);
				neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
				if((currentOuterStrategy ==3) && (currentPlan.getNumberOfTours() == neighborPlan.getNumberOfTours())) {
					 currentOuterStrategy =1;
					 neighborPlan =  modifyFleet(currentPlan, random, currentOuterStrategy, router);
				}
			}
						
			neighborPlan = evaluator.evaluateCarrierPlan(neighborPlan, network);
			currentBestInnerPlan = neighborPlan;
			ExtendedCarrierPlan currentInnerPlan = neighborPlan;
			HashMap<Integer, Integer> innerStrategyMap = getInitialStrategyMap();
			while(t_inner>endInnerTemperature) {
				int currentInnerStrategy = getNextStrategy(innerStrategyMap, random);
				ExtendedCarrierPlan innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
				if(currentInnerStrategy == 2 && arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
					double rnd = random.nextDouble();
					if(rnd < 0.5) {
						currentInnerStrategy = 3;
						innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
						if(arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
							currentInnerStrategy = 1;
							innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
						}
					}
				}
				if(currentInnerStrategy == 3 && arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
					double rnd = random.nextDouble();
					if(rnd < 0.5) {
						currentInnerStrategy = 2;
						innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
						if(arePlansEqual(innerNeighborPlan, currentInnerPlan)) {
							currentInnerStrategy = 1;
							innerNeighborPlan = modifyTours(currentInnerPlan, random, currentInnerStrategy, router);
						}
					}
				}				
				AdaptiveOperationsWithHeterogeneusFleet.removeEmptyTours(innerNeighborPlan);
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
						int weightOfCurrentInnerStrategy = outerStrategyMap.get(currentInnerStrategy);
						weightOfCurrentInnerStrategy++;
						innerStrategyMap.put(currentOuterStrategy, weightOfCurrentInnerStrategy);
						t_inner = alpha *t_inner;
				}
				else {	
					if(distanceToNeighborPlan <= distanceToBestInnerPlan) {
						currentBestInnerPlan = innerNeighborPlan;
						currentInnerPlan = innerNeighborPlan;
						int weightOfCurrentInnerStrategy = outerStrategyMap.get(currentInnerStrategy);
						weightOfCurrentInnerStrategy++;
						innerStrategyMap.put(currentOuterStrategy, weightOfCurrentInnerStrategy);
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
					int weightOfCurrentOuterStrategy = outerStrategyMap.get(currentOuterStrategy);
					weightOfCurrentOuterStrategy++;
					innerStrategyMap.put(currentOuterStrategy, weightOfCurrentOuterStrategy);
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
	
	private boolean sameFleet(ExtendedCarrierPlan plan1, ExtendedCarrierPlan plan2) {
		boolean sameFleet = true;
		for(Id<VehicleType> id : plan1.getVehicleMap().keySet()) {
			if(plan2.getVehicleMap().containsKey(id)) {
				if(plan2.getVehicleMap().get(id) == plan1.getVehicleMap().get(id)) {
					
				} 
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		return sameFleet;
	}
	
	private boolean arePlansEqual(ExtendedCarrierPlan plan1, ExtendedCarrierPlan plan2) {
		 AdaptiveOperationsWithHeterogeneusFleet.removeEmptyTours(plan1);
		 AdaptiveOperationsWithHeterogeneusFleet.removeEmptyTours(plan2);
		if(plan1.getScheduledTours().size() != plan1.getScheduledTours().size()) {
			return false;
		}
		else {
			for(ScheduledTour scheduledTour : plan1.getScheduledTours()) {
				boolean allUnequal = true;
				for(ScheduledTour anotherScheduledTour : plan2.getScheduledTours()) {
					if(scheduledTour.getTour().getTourElements().size() == anotherScheduledTour.getTour().getTourElements().size()) {
						if(AdaptiveOperationsWithHeterogeneusFleet.areToursEqual(scheduledTour, anotherScheduledTour)) {
							allUnequal = false;
						}
					}
				}
				if(allUnequal) {
					return false;
				}
			}
		}
		return true;
	}
}
