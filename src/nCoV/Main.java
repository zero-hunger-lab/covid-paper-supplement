package nCoV;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.EnumMap;

public class Main {	

	public static void main(String[] args) throws FileNotFoundException{

		/*
		 * Catching errors in an error log file 
		 */
		try {
			System.setErr(new PrintStream(new FileOutputStream(System.getProperty("user.dir")+"/errorLog.log")));
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}

		/*
		 * Reading in the parameter settings for the runs
		 */
		String settingsFile = "parameterSettings.txt";
		InputSettings inputSet = InputReader.readParameterSettings(settingsFile);	
		
		/*
		 * Parameter settings
		 */
		int scenario = inputSet.getScenario();
		int totalNumberSimulations = inputSet.getTotalNumberSimulations();
		double VIRUS_TRANSMISSION_PROBABILITY = inputSet.getVirusTransmissionProbability();
		int totalEpochsHorizon = inputSet.getTotalEpochsHorizon();
		String outputMap = inputSet.getFolderName();

		/*
		 * 
		 */
		for(int runNumber = 1; runNumber <= totalNumberSimulations; runNumber ++) {
			String runName = VIRUS_TRANSMISSION_PROBABILITY + "_" + runNumber ;
			String transitionName = "matrix-" + scenario;
			String startSituationName =	"startSit_"  + scenario;

			/*
			 * Filenames for the input files.
			 */
			String fileInput_contactRatio = System.getProperty("user.dir") + "/input/ContactData/ratioContacten.csv";
			String fileInput_numberDailyContacts = System.getProperty("user.dir") + "/input/ContactData/number_daily_contacts.csv";
			String fileInput_populationNumber = System.getProperty("user.dir") + "/input/Demographics/Bevolking_corop_leeftijd.csv";
			String fileInput_commute = System.getProperty("user.dir") + "/input/Commute/Woonwerk_"; // filename is finished in the loop below, output is x1000 in inputreader
			String fileInput_transitions = System.getProperty("user.dir") + "/input/TransitionMatrices/" + transitionName + ".csv"; 
			String fileInput_hospital = System.getProperty("user.dir") + "/input/HospitalCapacity/Ziekenhuizen_NL.csv"; // output is set at a reasonable high number such that IC capacity is never an issue
			String fileInput_initialInfection = System.getProperty("user.dir") + "/input/StartSituations/" + startSituationName + ".csv"; // updated 10-05

			/* 
			 * Filenames for the output files. 
			 */
			String fileOutput_totalInfections = System.getProperty("user.dir") + "/output/" + outputMap+ "/" +  "totalInfection_" + transitionName + "_" + runName + ".txt";
			String fileOutput_totalInfections_perAgegroup_perCity = System.getProperty("user.dir") + "/output/" + outputMap+  "/" +  "totalInfectionPerAgeGroupPerCity_" + transitionName + "_" + runName + ".txt";
			String fileOutput_hospital = System.getProperty("user.dir") + "/output/" +outputMap+  "/" + "hospitalInfo_" + transitionName + "_" + runName + ".txt";
			String fileOutput_timeInEachStage = System.getProperty("user.dir") + "/output/" + outputMap+  "/" +"timeInEachStage_" + transitionName + "_" + runName + ".txt"; 
			String fileOutput_infectionRateInEachCorop = System.getProperty("user.dir") + "/output/" +outputMap+  "/" +"infectionRateInEachCorop_" + transitionName + "_" + runName + ".txt"; 
			String fileOutput_switchStages = System.getProperty("user.dir") + "/output/" +outputMap+   "/" +"switchStages_" + transitionName + "_" + runName + ".txt"; 
			String[] allFileNames = {fileOutput_totalInfections, fileOutput_totalInfections_perAgegroup_perCity, fileOutput_hospital, fileOutput_timeInEachStage, fileOutput_infectionRateInEachCorop, fileOutput_switchStages};

			/*
			 * Input: contact patterns of individuals (given their age group)
			 */
			EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> CONTACT_RATIO = null;
			EnumMap<AgeGroup, Double> NUMBER_DAILY_CONTACTS = null;

			try {
				CONTACT_RATIO = InputReader.readContactPatternCSV(fileInput_contactRatio);
				NUMBER_DAILY_CONTACTS = InputReader.readDailyContacts(fileInput_numberDailyContacts);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}	

			EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> NUMBER_DAILY_CONTACTS_PERAGEGROUP = HelperFunction.determineNumberDailyContactsPerAgeGroup(CONTACT_RATIO, NUMBER_DAILY_CONTACTS);

			/* 
			 * Input: Demographics and commute characteristics
			 * Number of people in each corop and commute distribution per age group
			 */
			EnumMap<AgeGroup, EnumMap<City, EnumMap<City,Integer>>> COMMUTE_DISTRIBUTION = new EnumMap<>(AgeGroup.class);		
			EnumMap<City, EnumMap<AgeGroup, Integer>> POPULATION_NUMBER = null;
			int[] agesForTheGroups = {0, 9, 10, 19, 20, 29, 30, 39, 40, 49, 50, 59, 60, 69, 70, 79, 80, 150}; // HARDCODED

			try
			{
				POPULATION_NUMBER = InputReader.readPopulationDistributionCSV(fileInput_populationNumber);
				for(int i = 0; i < agesForTheGroups.length; i = i + 2) { // HARDCODED

					int x = agesForTheGroups[i];
					int y = agesForTheGroups[i + 1];

					String ageName = "Age_" + x + "_" + y;
					AgeGroup ageGroup = AgeGroup.valueOf(ageName);
					if(ageGroup == null) { throw new IllegalArgumentException("Input stage: age group does not exist.");				}

					String fileInput2 = fileInput_commute + x + "tot" + y + "jaar.csv";
					EnumMap<City, EnumMap<City, Integer>> cDist = InputReader.readCommuteCSV(fileInput2);
					COMMUTE_DISTRIBUTION.put(ageGroup, cDist);
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}		

			EnumMap<AgeGroup, EnumMap<City, EnumMap<City, Double>>> ALPHA = HelperFunction.determineAlpha(COMMUTE_DISTRIBUTION, POPULATION_NUMBER);

			/* 
			 * Input: Related to virus characteristics 
			 * Progression of the virus into the next stages. Virus progression depends on each age group
			 */
			EnumMap<AgeGroup,EnumMap<Stage, EnumMap<Stage, Double>>> VIRUS_PROGRESSION = null;
			try {
				VIRUS_PROGRESSION = InputReader.readTransitionProbabilities(fileInput_transitions);			
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			/*
			 *  Input: Capacity of each hospital. Now it overwrites the original capacity to a high number, can be changed to read original capacity.
			 */
			EnumMap<Hospital, Integer> HOSPITAL_CAPACITY = null;

			try {
				HOSPITAL_CAPACITY = InputReader.readICcapacityCSV(fileInput_hospital);	

			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			/*
			 * Number of individuals in each stage of their infection in their age group for every city
			 */
			EnumMap<City, EnumMap<AgeGroup, EnumMap<Stage, Integer>>> INFECTION_NUMBERS = null;

			try {
				INFECTION_NUMBERS = InputReader.readInitialInfectionCSV(fileInput_initialInfection);	

			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			/*
			 * Starting the simulation
			 */

			long timeBegin = System.nanoTime();
			Simulation sim = new Simulation(
					totalEpochsHorizon, 
					COMMUTE_DISTRIBUTION, 
					VIRUS_PROGRESSION,
					HOSPITAL_CAPACITY,
					INFECTION_NUMBERS,
					POPULATION_NUMBER, 
					CONTACT_RATIO,
					NUMBER_DAILY_CONTACTS_PERAGEGROUP,
					VIRUS_TRANSMISSION_PROBABILITY,
					ALPHA,
					allFileNames);

			sim.initializeSimulation(runNumber);
			long timeEnd = System.nanoTime() - timeBegin;
			long timeB2 = System.nanoTime();
			sim.startSimulation();
			long timeEnd2 = System.nanoTime() - timeB2;

			System.out.println("Initialize first time: " + timeEnd/1e9);
			System.out.println("Simulation time: " + timeEnd2/1e9);

			System.out.println("Done");
		}

	}

	public enum AgeGroup{
		Age_0_9,
		Age_10_19,
		Age_20_29,
		Age_30_39,
		Age_40_49,
		Age_50_59,
		Age_60_69,
		Age_70_79,
		Age_80_150
	}

	public enum Stage{
		HEALTHY,
		INFECTED_NOSYMPTOMS_NOTCONTAGIOUS,
		INFECTED_NOSYMPTOMS_ISCONTAGIOUS,
		INFECTED_SYMPTOMS_MILD,
		INFECTED_SYMPTOMS_SEVERE_ICpossible,
		INFECTED_SYMPTOMS_SEVERE_ICnotpossible,
		INFECTED_SYMPTOMS_SEVERE_QUEUE,
		CURED,
		DEAD
	}
	
//	## Stage conversion
//
//	The table below shows a conversion table indicating the stage names
//	and abbreviations used in the data files that were used during
//	development, and the stage names and abbreviations as they are used in
//	the final report.
//
//
//	| ID | short | long                                   | paper                   | paper-code |
//	|----|-------|----------------------------------------|-------------------------|------------|
//	| 0  | H     | HEALTHY                                | Susceptible             | S          |
//	| 1  | INN   | INFECTED_NOSYMPTOMS_NOTCONTAGIOUS      | Exposed                 | E          |
//	| 2  | INY   | INFECTED_NOSYMPTOMS_ISCONTAGIOUS       | Infectious asymptomatic | I-a        |
//	| 3  | IM    | INFECTED_SYMPTOMS_MILD                 | Infectious symptomatic  | I-s        |
//	| 4  | ISY   | INFECTED_SYMPTOMS_SEVERE_ICpossible    | ICU admission           | ICU-a      |
//	| 5  | ISN   | INFECTED_SYMPTOMS_SEVERE_ICnotpossible | ICU refusal             | ICU-r      |
//	| 6  | ISQ   | INFECTED_SYMPTOMS_SEVERE_QUEUE         | N/A                     | N/A        |
//	| 7  | C     | CURED                                  | Immune                  | IM         |
//	| 8  | D     | DEAD                                   | Deceased                | D          |

	/**
	 * All corops in the Netherlands
	 *
	 */
	public enum City{
		Achterhoek,
		Agglomeratie_s_Gravenhage,
		AgglomeratieHaarlem,
		AgglomeratieLeidenenBollenstreek,
		Alkmaarenomgeving,
		Arnhem_Nijmegen,
		DelftenWestland,
		Delfzijlenomgeving,
		Flevoland,
		Groot_Amsterdam,
		Groot_Rijnmond,
		HetGooienVechtstreek,
		IJmond,
		KopvanNoord_Holland,
		Midden_Limburg,
		Midden_Noord_Brabant,
		Noord_Drenthe,
		Noord_Friesland,
		Noord_Limburg,
		Noord_Overijssel,
		Noordoost_Noord_Brabant,
		Oost_Groningen,
		Oost_Zuid_Holland,
		OverigGroningen,
		OverigZeeland,
		Twente,
		Utrecht,
		Veluwe,
		West_Noord_Brabant,
		Zaanstreek,
		Zeeuwsch_Vlaanderen,
		Zuid_Limburg,
		Zuidoost_Drenthe,
		Zuidoost_Friesland,
		Zuidoost_Noord_Brabant,
		Zuidoost_Zuid_Holland,
		Zuidwest_Drenthe,
		Zuidwest_Friesland,
		Zuidwest_Gelderland,
		Zuidwest_Overijssel
	}

	public enum Hospital{
		H_Achterhoek,
		H_Agglomeratie_s_Gravenhage,
		H_AgglomeratieHaarlem,
		H_AgglomeratieLeidenenBollenstreek,
		H_Alkmaarenomgeving,
		H_Arnhem_Nijmegen,
		H_DelftenWestland,
		H_Delfzijlenomgeving,
		H_Flevoland,
		H_Groot_Amsterdam,
		H_Groot_Rijnmond,
		H_HetGooienVechtstreek,
		H_IJmond,
		H_KopvanNoord_Holland,
		H_Midden_Limburg,
		H_Midden_Noord_Brabant,
		H_Noord_Drenthe,
		H_Noord_Friesland,
		H_Noord_Limburg,
		H_Noord_Overijssel,
		H_Noordoost_Noord_Brabant,
		H_Oost_Groningen,
		H_Oost_Zuid_Holland,
		H_OverigGroningen,
		H_OverigZeeland,
		H_Twente,
		H_Utrecht,
		H_Veluwe,
		H_West_Noord_Brabant,
		H_Zaanstreek,
		H_Zeeuwsch_Vlaanderen,
		H_Zuid_Limburg,
		H_Zuidoost_Drenthe,
		H_Zuidoost_Friesland,
		H_Zuidoost_Noord_Brabant,
		H_Zuidoost_Zuid_Holland,
		H_Zuidwest_Drenthe,
		H_Zuidwest_Friesland,
		H_Zuidwest_Gelderland,
		H_Zuidwest_Overijssel
	}
}
