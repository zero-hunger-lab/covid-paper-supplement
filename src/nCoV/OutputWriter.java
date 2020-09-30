package nCoV;

import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import nCoV.Main.*;

public class OutputWriter {

	/**
	 * When the simulation has finished, you can call this function to write down all relevant output to .csv
	 * This specific function writes for a given time horizon all patients inside each hospital.
	 */
	public static void outputWriterHospitalInfo(PrintWriter printer, EnumMap<Hospital, EnumMap<AgeGroup,List<Individual>>> idvPerHospital, int nrInQueue, int epoch){
		if(epoch == 0){
			StringBuilder expl = new StringBuilder();
			expl.append("Number of patients in each hospital given their age group. Patients in queue is the total for all age groups, and prints the same value for all age groups.");
			printer.println(expl.toString());

			StringBuilder lineHosp = new StringBuilder();
			lineHosp.append("Time,AgeGroup"); 
			for(Hospital hos : Hospital.values()){
				lineHosp.append("," + hos.toString());	
			}
			lineHosp.append("," + "Patients in queue");
			printer.print(lineHosp.toString());
			printer.println();
		}

		for(AgeGroup ageGroup : AgeGroup.values()) {
			StringBuilder line = new StringBuilder();

			line.append(epoch + "," + ageGroup.toString()); 

			for(Hospital hos : Hospital.values()){
				int numberPatients = idvPerHospital.get(hos).get(ageGroup).size();
				line.append("," + numberPatients);
			}
			line.append("," + nrInQueue);
			printer.println(line.toString());
		}
	}


	/**
	 * When the simulation has finished, you can call this function to write down all relevant output to .csv
	 * This specific function writes for a given time horizon number of people in each infection Stage
	 */
	public static void outputWriterAggregateStageInfection(PrintWriter printer, Individual[] individuals, int epoch){
		if(epoch == 0){
			StringBuilder firstLine = new StringBuilder();
			firstLine.append("Time");
			for(Stage stage : Stage.values()){
				firstLine.append("," + stage.toString());
			}
			printer.println(firstLine);
		}

		EnumMap<Stage, Integer> countingPeopleInStage = new EnumMap<>(Stage.class);
		for(Stage stage : Stage.values()){ //initializing
			countingPeopleInStage.put(stage, 0);
		}

		for(Individual idv : individuals){
			Stage currentStage = idv.getStage();
			countingPeopleInStage.put(currentStage, countingPeopleInStage.get(currentStage) + 1); // adding number of people in this stage
		}

		StringBuilder line = new StringBuilder();
		line.append(epoch);
		for(Stage stage : Stage.values()){
			line.append("," + countingPeopleInStage.get(stage));
		}

		printer.println(line);
	}


	/**
	 * When the simulation has finished, you can call this function to write down all relevant output to .csv
	 * This specific function writes for a given time horizon number of people in each infection Stage, but now for each city in specific
	 */
	public static void outputWriterAggregateStageInfectionCityTotal(PrintWriter printer, Individual[] individuals, int epoch){
		if(epoch == 0){
			StringBuilder firstLine = new StringBuilder();
			StringBuilder secondLine = new StringBuilder();

			secondLine.append("Agegroup,Time");
			boolean ignoreFirst = true;

			for(City city : City.values()){
				if(ignoreFirst){
					ignoreFirst = false;
					firstLine.append("," + city.toString());
				}
				else{
					firstLine.append("," + city.toString());
					secondLine.append(",Time");
				}

				for(Stage stage : Stage.values()){
					secondLine.append("," + stage.toString());
					firstLine.append("," + city.toString());
				}
			}

			printer.println(firstLine);
			printer.println(secondLine);
		}

		EnumMap<Stage, EnumMap<AgeGroup,EnumMap<City,Integer>>> countingPeopleInStageInAgeGroup = new EnumMap<>(Stage.class);
		for(Stage stage : Stage.values()){ //initializing

			EnumMap<AgeGroup, EnumMap<City, Integer>> countingPeopleInStage = new EnumMap<>(AgeGroup.class);
			for(AgeGroup ageGroup : AgeGroup.values()) {
				EnumMap<City, Integer> initMap = new EnumMap<City, Integer>(City.class);

				for(City city : City.values()){
					initMap.put(city, 0);
				}
				countingPeopleInStage.put(ageGroup, initMap);
			}
			countingPeopleInStageInAgeGroup.put(stage, countingPeopleInStage);

		}

		for(Individual idv : individuals){
			Stage currentStage = idv.getStage();
			City ownCity = idv.getResidentPlace();
			AgeGroup ageGroup =idv.getAgeGroup();
			EnumMap<City, Integer> storeMap = countingPeopleInStageInAgeGroup.get(currentStage).get(ageGroup);
			storeMap.put(ownCity, storeMap.get(ownCity) + 1); // adding number of people in this stage to hometown city
		}


		for(AgeGroup ageGroup : AgeGroup.values()) {
			boolean doOnce = true;
			StringBuilder line = new StringBuilder();
			for(City city : City.values()){
				if(doOnce){
					line.append(ageGroup.toString() + "," + epoch);
					doOnce = false;
				}
				else{
					line.append("," + epoch);
				}
				for(Stage stage : Stage.values()){

					line.append("," + countingPeopleInStageInAgeGroup.get(stage).get(ageGroup).get(city));
				}
			}
			printer.println(line);
		}
	}


	/**
	 * When the simulation has finished, you can call this function to write down all relevant output to .csv
	 * This specific function writes for a given time horizon number of people in each infection Stage, but now for each city in specific
	 */
	public static void outputWriterAggregateStageInfectionCity(PrintWriter printer, Set<Individual> individuals, int epoch, AgeGroup givenAgeGroup){
		if(epoch == 0){
			StringBuilder firstLine = new StringBuilder();
			StringBuilder secondLine = new StringBuilder();

			secondLine.append("AgeGroup,Time");
			boolean ignoreFirst = true;

			for(City city : City.values()){
				if(ignoreFirst){
					ignoreFirst = false;
					firstLine.append("," + city.toString());
				}
				else{
					firstLine.append("," + city.toString());
					secondLine.append(",Time");
				}

				for(Stage stage : Stage.values()){
					secondLine.append("," + stage.toString());
					firstLine.append("," + city.toString());
				}
			}

			printer.println(firstLine);
			printer.println(secondLine);
		}

		EnumMap<Stage, EnumMap<City,Integer>> countingPeopleInStage = new EnumMap<>(Stage.class);
		for(Stage stage : Stage.values()){ //initializing
			EnumMap<City, Integer> initMap = new EnumMap<City, Integer>(City.class);

			for(City city : City.values()){
				initMap.put(city, 0);
			}
			countingPeopleInStage.put(stage, initMap);
		}

		for(Individual idv : individuals){
			Stage currentStage = idv.getStage();
			if(idv.getAgeGroup().equals(givenAgeGroup)) {
				City ownCity = idv.getResidentPlace();
				EnumMap<City, Integer> storeMap = countingPeopleInStage.get(currentStage);
				storeMap.put(ownCity, storeMap.get(ownCity) + 1); // adding number of people in this stage to hometown city
			}
		}

		boolean doOnce = true;
		StringBuilder line = new StringBuilder();
		for(City city : City.values()){
			if(doOnce){
				line.append(givenAgeGroup.toString() +"," + epoch);
				doOnce = false;
			}
			else{
				line.append("," + epoch);
			}
			for(Stage stage : Stage.values()){

				line.append("," + countingPeopleInStage.get(stage).get(city));
			}
		}
		printer.println(line);
	}

	public static void outputWriterTimeSpendEachStage(PrintWriter printer,EnumMap<AgeGroup, EnumMap<Stage, int[]>> numberOfPeopleInEachStage, int timeHorizon) {

		// Print header
		StringBuilder firstLine = new StringBuilder();
		StringBuilder secondLine = new StringBuilder();

		firstLine.append("Number persons been in that stage for that many days.");
		secondLine.append("Age Group,Stage");
		for(int i = 0 ; i < timeHorizon; i++) {
			secondLine.append("," + i);
		}

		printer.println(firstLine);
		printer.println(secondLine);

		// printing rest info
		for(AgeGroup ageGroup : AgeGroup.values()) {

			for(Stage stage : Stage.values()) {

				StringBuilder line = new StringBuilder();
				line.append(ageGroup.toString() +"," + stage.toString());
				int[] printInt = numberOfPeopleInEachStage.get(ageGroup).get(stage); 

				for(int i = 0 ; i < printInt.length ; i++) {
					line.append("," + printInt[i]);
				}
				printer.println(line);
			}
		}
	}


	public static void outputWriterInfectionRateInEachCorop(PrintWriter printer, EnumMap<City,EnumMap<AgeGroup, double[]>> infectionRate, int timeHorizon) {

		// Print header
		StringBuilder firstLine = new StringBuilder();
		StringBuilder secondLine = new StringBuilder();

		firstLine.append("This is the infection rate in each corop on each time stage for each age group.");

		secondLine.append("Corop,Agegroup");
		for(int i = 0 ; i < timeHorizon; i ++) {
			secondLine.append("," + i);
		}

		printer.println(firstLine);
		printer.println(secondLine);

		for(City city : City.values()) {

			for(AgeGroup agegroup : AgeGroup.values()) {
				StringBuilder line = new StringBuilder();
				line.append(city.toString() + "," + agegroup.toString());

				double[] allRates = infectionRate.get(city).get(agegroup);
				for(int i = 0 ; i < allRates.length; i ++) {
					line.append("," + allRates[i]);
				}
				printer.println(line);
			}
		}
	}

	public static void outputWriterStageSwitching(PrintWriter printer, EnumMap<AgeGroup,EnumMap<Stage, EnumMap<Stage, Integer>>> switchMap, int timeHorizon) {

		if(timeHorizon == 0) {
			StringBuilder line = new StringBuilder();
			line.append(",,");
			for(Stage stage2 : Stage.values()) {
				line.append("," + stage2.toString());
			}
			printer.println(line);
		}

		for(AgeGroup ageGroup : AgeGroup.values()) {
			for(Stage stage : Stage.values()) {

				StringBuilder line = new StringBuilder();
				line.append(timeHorizon + "," + ageGroup.toString() + "," + stage.toString());
				for(Stage stage2 : Stage.values()) {
					line.append("," + switchMap.get(ageGroup).get(stage).get(stage2));
				}
				printer.println(line);
			}
		}
	}
}
