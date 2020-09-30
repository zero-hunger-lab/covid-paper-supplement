package nCoV;

import nCoV.Main.*;

public class Individual {

	
	private AgeGroup ageGroup; 
	private Stage stage;
	private int timeInStage;
	private City residentPlace;
	private City commutePlace;
	private int number; // unique number to indentify a person
	private boolean inHospital; // true when currently in hospital
	private Hospital hospital;
	private boolean inQueue; // true when currently in hospital queue
	
	public Individual(AgeGroup ageGroup, Stage stage, int timeInStage, City residentPlace, City commutePlace, Hospital hospital, int number, boolean inHospital, boolean inQueue){
		this.ageGroup = ageGroup;
		this.stage = stage;
		this.timeInStage = timeInStage;
		this.residentPlace = residentPlace;
		this.commutePlace = commutePlace;
		this.number = number;
		this.inHospital = inHospital;
		this.hospital = hospital;
	}

	public AgeGroup getAgeGroup() {
		return ageGroup;
	}

	public void setAgeGroup(AgeGroup ageGroup) {
		this.ageGroup = ageGroup;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public City getResidentPlace() {
		return residentPlace;
	}

	public void setResidentPlace(City residentPlace) {
		this.residentPlace = residentPlace;
	}

	public City getCommutePlace() {
		return commutePlace;
	}

	public void setCommutePlace(City commutePlace) {
		this.commutePlace = commutePlace;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public boolean inHospital() {
		return inHospital;
	}

	public void setInHospital(boolean inHospital) {
		this.inHospital = inHospital;
	}

	public Hospital getHospital() {
		return hospital;
	}
	
	public boolean inQueue() {
		return inQueue;
	}
	
	public void setQueue(boolean inQueue) {
		this.inQueue = inQueue;
	}

	public int getTimeInStage() {
		return timeInStage;
	}

	public void setTimeInStage(int timeInStage) {
		this.timeInStage = timeInStage;
	}

	@Override
	public String toString() {
		return "Individual [ageGroup=" + ageGroup + ", stage=" + stage + ", number=" + number + "]";
	}

	@Override
	public int hashCode() {
		//final int prime = 31;
		//int result = 1;
		//result = prime * result + number;
		//return result;
		return number;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Individual other = (Individual) obj;
		if (number != other.number)
			return false;
		return true;
	}
	

}
