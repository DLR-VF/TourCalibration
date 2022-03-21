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

import java.io.File;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class InfoListEvaluator {

	private ArrayList<IterationInformation> infoList;
	private ExtendedCarrierPlan referencePlan;
	
	public InfoListEvaluator(ArrayList<IterationInformation> infoList,ExtendedCarrierPlan referencePlan) {
		this.infoList  = infoList;
		this.referencePlan = referencePlan;
	}
	
	private ArrayList<Double> distanceOfDistancesCurrent(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add(Math.abs((referencePlan.getOverallLength() - info.getCurrentPlan().getOverallLength())/referencePlan.getOverallLength()));
		}
		return distances;
	}

	private ArrayList<Double> distanceOfDistancesBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add(Math.abs((referencePlan.getOverallLength() - info.getBestPlan().getOverallLength())/referencePlan.getOverallLength()));
		}
		return distances;
	}
	
	private ArrayList<Double> distanceOfAverageDistancesBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add(Math.abs((referencePlan.getAverageTourLength() - info.getBestPlan().getAverageTourLength())/referencePlan.getAverageTourLength()));
		}
		return distances;
	}
	
	private ArrayList<Double> distanceOfBetweenDistancesBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add(Math.abs((referencePlan.getAverageDistanceBetweenStops() - info.getBestPlan().getAverageDistanceBetweenStops())/referencePlan.getAverageDistanceBetweenStops()));
		}
		return distances;
	}
	
	private ArrayList<Double> distanceOfStopsBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add(Math.abs((referencePlan.getAverageStopsPerTour() - info.getBestPlan().getAverageStopsPerTour())/referencePlan.getAverageStopsPerTour()));
		}
		return distances;
	}
	
	private ArrayList<Double> distanceOfToursBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add((double) Math.abs((referencePlan.getNumberOfTours() - info.getBestPlan().getNumberOfTours())/referencePlan.getNumberOfTours()));
		}
		return distances;
	}
	
	private ArrayList<Double> distanceOfCapacitiesBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			distances.add(Math.abs((referencePlan.getAverageCapacityUtilization() - info.getBestPlan().getAverageCapacityUtilization())/referencePlan.getAverageCapacityUtilization()));
		}
		return distances;
	}
	
	private ArrayList<Double> distanceOfVehicletypesBest(){
		ArrayList<Double> distances = new ArrayList<>();
		for(IterationInformation info : infoList) {
			CarrierPlanComparator comparator = new CarrierPlanComparator();
			
			distances.add(comparator.getDistanceBetweenVehicleTypes(referencePlan, info.getBestPlan()));
		}
		return distances;
	}
	
	
	
	private ArrayList<Double> distanceOfObjectiveFuntionsCurrent(){
		ArrayList<Double> distances = new ArrayList<>();
		CarrierPlanComparator comparator = new CarrierPlanComparator();
		comparator.setIncumbentPlan(referencePlan);
		for(IterationInformation info : infoList) {
			comparator.setEntrantPlan(info.getCurrentPlan());
			distances.add(comparator.getDistanceToBestPlan());
		}
		return distances;
	}
		
	private ArrayList<Double> bestObjectiveFunctions(){
		ArrayList<Double> objectives = new ArrayList<>();
		CarrierPlanComparator comparator = new CarrierPlanComparator();
		comparator.setIncumbentPlan(referencePlan);
		for(IterationInformation info : infoList) {
			comparator.setEntrantPlan(info.getBestPlan());
			objectives.add(comparator.getDistanceToBestPlan());
		}
		return objectives;
	}
	
	public void createOverallDistanceLineChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		ArrayList<Double> distancesCurrent = distanceOfDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfDistancesBest();
		
		int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}

		i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Overall distance","Iteration",
		         "Standardized absolute deviation of overall distances",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/OverallDistances.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }

	public void createDistanceBetweeenStopsLineChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		//ArrayList<Double> distancesCurrent = distanceOfBetweenDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfBetweenDistancesBest();
		
		/*int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}*/

		int i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Distance between stops","Iteration",
		         "Standardized absolute deviation of distance between stops",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/DistanceBetweenJobs.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }
	
	public void createDistanceBetweeenNumberOfStopsLineChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		//ArrayList<Double> distancesCurrent = distanceOfBetweenDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfStopsBest();
		
		/*int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}*/

		int i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Distance between number of stops","Iteration",
		         "Standardized absolute deviation of distance between number of stops",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/DistanceBetweenNumberOfJobs.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }
	
	public void createDistanceBetweeenCapacityUtilizationLineChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		//ArrayList<Double> distancesCurrent = distanceOfBetweenDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfCapacitiesBest();
		
		/*int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}*/

		int i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Distance between capacity utilitzation","Iteration",
		         "Standardized absolute deviation of distance between capacity utilitzation",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/DistanceBetweenCapacityUtilization.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }
	
	public void createDistanceBetweeenNumberOfToursLineChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		//ArrayList<Double> distancesCurrent = distanceOfBetweenDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfToursBest();
		
		/*int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}*/

		int i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Distance between number of tours","Iteration",
		         "Standardized absolute deviation of distance between number of tours",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/DistanceBetweenNumberOfTours.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }
	
	public void createDistanceBetweeenVehicleTypesLineChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		//ArrayList<Double> distancesCurrent = distanceOfBetweenDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfVehicletypesBest();
		
		/*int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}*/

		int i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Distance between vehicle types","Iteration",
		         "Standardized absolute deviation of distance between vehicle types",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/DistanceBetweenVehicleTypes.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }
	
	public void createDistanceBetweeenAverageDistancesChart() throws Exception{
		DefaultCategoryDataset line_chart_current = new DefaultCategoryDataset();
		DefaultCategoryDataset line_chart_best = new DefaultCategoryDataset();
		//ArrayList<Double> distancesCurrent = distanceOfBetweenDistancesCurrent();
		ArrayList<Double> distancesBest = distanceOfAverageDistancesBest();
		
		/*int i = 1;
		for(Double dist : distancesCurrent) {
			line_chart_current.addValue(dist, "deviation of current to reference solution", i+"");	
			i++;
		}*/

		int i = 1;
		for(Double dist : distancesBest) {
			line_chart_best.addValue(dist, "deviation of best to reference solution", i+"");	
			i++;
		}
		
		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Distance between average distances","Iteration",
		         "Standardized absolute deviation of distance between average distances",
		         line_chart_best,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/DistanceBetweenAverageDistances.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }
	
	
	public void createObjectiveFunctionLineChart() throws Exception{
		DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
		ArrayList<Double> distances = bestObjectiveFunctions();
		
		int i = 1;
		for(Double dist : distances) {
			line_chart_dataset.addValue(dist, "objective function", i+"");	
			i++;
		}

		
		JFreeChart lineChartObject = ChartFactory.createLineChart(
		         "Convergence of algorithm: Objective function","Iteration",
		         "Objective function",
		         line_chart_dataset,PlotOrientation.VERTICAL,
		         true,true,false);

		      int width = 640;    /* Width of the image */
		      int height = 480;   /* Height of the image */ 
		      File lineChart = new File( "output/ObjectiveFunctions.jpeg" ); 
		      ChartUtils.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
	 }		
	
	
}
