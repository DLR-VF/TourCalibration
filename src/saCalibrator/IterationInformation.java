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

public class IterationInformation {

	private int number;
	private ExtendedCarrierPlan currentPlan;
	private ExtendedCarrierPlan bestPlan;
	private ExtendedCarrierPlan neighborPlan;
	private PlanCopier copier;
	private double innerTemperature;
	private double outerTemperature;
	
	public IterationInformation() {
		copier = new PlanCopier();
	}
	
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public ExtendedCarrierPlan getCurrentPlan() {
		return currentPlan;
	}
	public void setCurrentPlan(ExtendedCarrierPlan currentPlan) {
		this.currentPlan = copier.makeDeepCopyOfPlan(currentPlan);
	}
	public ExtendedCarrierPlan getBestPlan() {
		return bestPlan;
	}
	public void setBestPlan(ExtendedCarrierPlan bestPlan) {
		this.bestPlan = copier.makeDeepCopyOfPlan(bestPlan);
	}
	public ExtendedCarrierPlan getNeighborPlan() {
		return neighborPlan;
	}
	public void setNeighborPlan(ExtendedCarrierPlan neighborPlan) {
		this.neighborPlan = copier.makeDeepCopyOfPlan(neighborPlan);
	}
	public double getInnerTemperature() {
		return innerTemperature;
	}
	public void setInnerTemperature(double temperature) {
		this.innerTemperature = temperature;
	}
	public double getOuterTemperature() {
		return outerTemperature;
	}
	public void setOuterTemperature(double outerTemperature) {
		this.outerTemperature = outerTemperature;
	}	
}
