package nCoV;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nCoV.Main.AgeGroup;
import nCoV.Main.City;
import nCoV.Main.Hospital;
import nCoV.Main.Stage;

public class Simulation {

	/*
	 *  Input for the simulation
	 */
	private EnumMap<AgeGroup, EnumMap<City, EnumMap<City,Integer>>> COMMUTE_DISTRIBUTION; 
	// Number of agents (for each age group) that commute between one city and the other
	private int totalEpochsHorizon; 
	// Number of epochs, Epoch is day or night. During day the person works in the commute area, during night in his hometown
	private EnumMap<AgeGroup, EnumMap<Stage, EnumMap<Stage, Double>>> VIRUS_PROGRESSION; 
	// Health transmission matrix (Markov-chain)
	private EnumMap<Hospital, Integer> HOSPITAL_CAPACITY; 
	// Number of available IC beds in each corop, is currently not used (set to reasonable large)
	private EnumMap<City, EnumMap<AgeGroup, EnumMap<Stage, Integer>>> INFECTION_NUMBERS; 
	// Number of initial agents in certain health stages for each corop and age group. Number of susceptible agents do not have to be given
	private EnumMap<City, EnumMap<AgeGroup, Integer>> POPULATION_NUMBER; 
	// Number of agents of a certain age group living in a corop
	private EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> CONTACT_RATIO;
	// Is defined as \mathbb{P}\{E_{aa'}\}, which is the probability that an individual from age group $a$ encounters an individual from age group $a'$
	private EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> NUMBER_DAILY_CONTACTS_PERAGEGROUP;
	// $[\#\textup{DailyContacts}]_{a,a'}$ is the number of daily contacts an agent of age group $a$ has with agents with age group $a'$
	private double VIRUS_TRANSMISSION_PROBABILITY;
	// Our P(T)
	private EnumMap<AgeGroup, EnumMap<City, EnumMap<City, Double>>> ALPHA;
	// Alpha as defined in the paper, fraction of people from age group $a'$, living in corop $c$ and being present in corop $c'$ during the current epoch
	private String[] fileNames;
	// Names for the output files


	/*
	 *  Used/filled throughout the simulation
	 */
	private Individual[] allIndividuals;
	// Contains all simulated agents
	private EnumMap<Hospital, Integer> patientsHospital;
	// Number of patients in each hospital(/corop)
	private EnumMap<Hospital, EnumMap<AgeGroup, List<Individual>>> patientsHospitalPerAgeGroup;
	// For each hospital a list of (hospitalized) agents is stored given the patients' age groups. 
	private List<Individual> queueHospital;
	//  Not used ATM, but it queues the individuals waiting for an ICU spot. Handled according to FIFO.
	private EnumMap<Hospital, int[]> patientNumberHospital;
	// Stores the number of patients in each hospital at each time epoch. Easy way to retrieve info.
	private EnumMap<City, Integer> residentsPerCity;
	// Stores the number of citizens in each city. It is used in the initialization of the individuals.
	private List<Hospital> shuffleHospitals;
	// Not used ATM, but in case hospital of preference is full, a random other hospital is chosen which is not full
	private EnumMap<City, EnumMap<AgeGroup, List<Individual>>> individualsCategorizedInitialisation;
	// Categorizes the individuals given the corop they live in and their age group. Used for the initialisation. 
	private EnumMap<City, EnumMap<AgeGroup,double[]>> infectionRate; 
	// Stores the infection rate P_{a,c,t} (the infection probability in each corop for each age group and epoch)
	private EnumMap<AgeGroup, EnumMap<Stage, int[]>> numberOfPeopleInEachStage; 
	// Counts the number of people (for each agegroup and stage) who have been in that particular stage&agegroup for that many days. So the array int[] goes over the epochs. If you have {1, 0, 4} for stage x and agegroup y then this means that 1 person in y has been in stage x for 1 epoch, 0 persons in y have been in stage x for 2 epochs and 4 persons in y have been in stage x for 3 epochs 
	private Map<Integer, EnumMap<AgeGroup,EnumMap<Stage, EnumMap<Stage, Integer>>>> numberSwitchersStage; 
	// For a certain time epoch: counts the number of people who went from stage x to stage y, used for output purposes. 
	private EnumMap<AgeGroup, EnumMap<City, Double>> ALPHA_SHORTCUT_HOME;
	// Shortcut for ALPHA, contains precomputed values such that computation is faster
	private Set<Stage> notEncounterStages;
	// Contains all health stages of people you could not meet on the street (e.g. Deceased, ICU admission, ICU refusal)

	/*
	 * Other
	 */
	private Random rand;
	// Random generator to replicate the experiments
	private boolean firstTimeInitialising;
	// Boolean checks true if the simulation is the first time initialised. First time more initialisation is needed, when using the method again certain substeps do not need to be performed.
	private boolean justDoneASimulation;
	// In case a simulation has been performed, all arrays etc are filled and need to be reinitialised before another simulation can be performed. When true, it needs to be reinitialized. 


	/**
	 * Constructor of the simulation. 
	 * @param totalEpochsHorizon
	 * @param COMMUTE_DISTRIBUTION
	 * @param VIRUS_PROGRESSION
	 * @param HOSPITAL_CAPACITY
	 * @param INFECTION_NUMBERS
	 * @param POPULATION_NUMBER
	 * @param CONTACT_RATIO
	 * @param NUMBER_DAILY_CONTACTS_PERAGEGROUP
	 * @param VIRUS_TRANSMISSION_PROBABILITY
	 * @param ALPHA
	 * @param fileNames
	 */
	public Simulation( 
			int totalEpochsHorizon, 
			EnumMap<AgeGroup, EnumMap<City, EnumMap<City,Integer>>> COMMUTE_DISTRIBUTION, 
			EnumMap<AgeGroup, EnumMap<Stage, EnumMap<Stage, Double>>> VIRUS_PROGRESSION,
			EnumMap<Hospital, Integer> HOSPITAL_CAPACITY,
			EnumMap<City, EnumMap<AgeGroup, EnumMap<Stage, Integer>>> INFECTION_NUMBERS,
			EnumMap<City, EnumMap<AgeGroup, Integer>> POPULATION_NUMBER,
			EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> CONTACT_RATIO,
			EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> NUMBER_DAILY_CONTACTS_PERAGEGROUP,
			double VIRUS_TRANSMISSION_PROBABILITY,
			EnumMap<AgeGroup, EnumMap<City, EnumMap<City, Double>>> ALPHA,
			String[] fileNames
			){
		this.COMMUTE_DISTRIBUTION = COMMUTE_DISTRIBUTION; 
		this.totalEpochsHorizon = totalEpochsHorizon;
		this.VIRUS_PROGRESSION = VIRUS_PROGRESSION;
		this.HOSPITAL_CAPACITY = HOSPITAL_CAPACITY;
		this.INFECTION_NUMBERS = INFECTION_NUMBERS;
		this.POPULATION_NUMBER = POPULATION_NUMBER;
		this.CONTACT_RATIO = CONTACT_RATIO;
		this.NUMBER_DAILY_CONTACTS_PERAGEGROUP = NUMBER_DAILY_CONTACTS_PERAGEGROUP;
		this.VIRUS_TRANSMISSION_PROBABILITY = VIRUS_TRANSMISSION_PROBABILITY;
		this.ALPHA = ALPHA;
		this.fileNames = fileNames;

		this.patientsHospital = new EnumMap<Hospital, Integer>(Hospital.class);
		this.patientsHospitalPerAgeGroup =  new EnumMap<Hospital, EnumMap<AgeGroup, List<Individual>>>(Hospital.class);
		this.queueHospital = new ArrayList<>();
		this.patientNumberHospital = new EnumMap<Hospital, int[]>(Hospital.class);
		this.residentsPerCity = new EnumMap<City, Integer>(City.class);
		this.numberOfPeopleInEachStage = new EnumMap<>(AgeGroup.class);
		this.shuffleHospitals = new ArrayList<>();
		this.infectionRate = new EnumMap<>(City.class);
		this.individualsCategorizedInitialisation = new EnumMap<>(City.class);

		int totalPersons = 0;
		for(City city : City.values()) {		
			for(AgeGroup ageGroup : AgeGroup.values()) {
				totalPersons += POPULATION_NUMBER.get(city).get(ageGroup);
			}
		}

		this.notEncounterStages = new HashSet<>(); // Used for the infection probability, used to identify agents you could encounter (e.g. not hospitalized or staying at home because of being sick)
		notEncounterStages.add(Stage.INFECTED_SYMPTOMS_SEVERE_ICnotpossible);
		notEncounterStages.add(Stage.INFECTED_SYMPTOMS_SEVERE_ICpossible);
		notEncounterStages.add(Stage.INFECTED_SYMPTOMS_SEVERE_QUEUE); // Note that the queue was not needed in our research, but we still include it to be able to see its effects
		notEncounterStages.add(Stage.DEAD);

		this.allIndividuals = new Individual[totalPersons];
		this.numberSwitchersStage = new HashMap<>();
		this.ALPHA_SHORTCUT_HOME = new EnumMap<>(AgeGroup.class);

		this.firstTimeInitialising = true; // set TRUE as first time this instance is initialized. 
		this.justDoneASimulation = true; // is set TRUE when a simulation has finished and needs reinitalising
		this.createIndividuals(); // create all agents and their characteristics, also set up a 
		this.createAllMaps(); // create all arrays, maps and data structures used during the simulation
	}


	/**
	 * This method creates all individuals. Each individual is unique, has a certain age group, resident city and commute city.
	 * In case the individual has no commute city, their night corop is their resident city. 
	 * 
	 */
	private void createIndividuals() {
		int uniqueID = 0;

		/*
		 *  Initializing some maps, needed to categorize the generated individuals (only for first initializing) 
		 *  For convenience, it also initializes the infection rate array, as we already use this double loop (although it belongs more to createAllMaps())
		 */
		for(City city : City.values()) {
			EnumMap<AgeGroup, List<Individual>> tempMap = new EnumMap<>(AgeGroup.class);
			int sumCity = 0;
			EnumMap<AgeGroup, double[]> tempDing = new EnumMap<>(AgeGroup.class);

			for(AgeGroup ageGroup : AgeGroup.values()) {
				List<Individual> shuffleSet = new ArrayList<>();
				tempMap.put(ageGroup, shuffleSet);
				sumCity += POPULATION_NUMBER.get(city).get(ageGroup);

				double[] infectionPerEpoch = new double[totalEpochsHorizon];
				tempDing.put(ageGroup, infectionPerEpoch);
			}

			infectionRate.put(city, tempDing);
			individualsCategorizedInitialisation.put(city, tempMap);
			residentsPerCity.put(city, sumCity);
		}

		/*
		 *  Initialising work corop of individuals
		 */
		for(City residentCity : City.values()) {

			for(AgeGroup ageGroup : AgeGroup.values()) {

				int nrPeopleTotalNeeded_agegroup = POPULATION_NUMBER.get(residentCity).get(ageGroup); // each corop needs a certain number of agents of a particular age group 

				for(City commuteCity : City.values()) {

					int nrPeopleToThisCommuteCity_agegroup = COMMUTE_DISTRIBUTION.get(ageGroup).get(residentCity).get(commuteCity); // in each corop a certain number of agents of a particular age group commute to another corop.

					for(int i = 0 ; i < nrPeopleToThisCommuteCity_agegroup; i++) { // Here we only create agents which commute!

						Stage stage = Stage.HEALTHY; // each agent is initially Healthy (Susceptible). Agents can later be assigned another health stage (according to INFECTION_NUMBERS)
						int timeInStage = 0;
						Hospital hospital = Hospital.values()[residentCity.ordinal()]; // IMPORTANT: now 1-1 relation city to hospital
						int number = uniqueID;
						boolean inHospital = false;
						boolean inQueue = false;

						Individual idv = new Individual(ageGroup, stage , timeInStage, residentCity, commuteCity, hospital, number, inHospital, inQueue);
						allIndividuals[uniqueID] = idv;
						individualsCategorizedInitialisation.get(residentCity).get(ageGroup).add(idv);
						uniqueID ++;	
						nrPeopleTotalNeeded_agegroup--; // this number is decreased as we have simulated a commuting agent of this category. Later we simulate the remaining non-commuting agents
					}
				}

				if(nrPeopleTotalNeeded_agegroup < 0) throw new IllegalArgumentException("Cannot generate number of people needed."); // Error: more agents are apparently commuting than living inside this corop!
				for(int i = 0; i < nrPeopleTotalNeeded_agegroup; i++) { // Here we simulate agents who do not commute, that is if not all people needed for this age group are simulated yet
					Stage stage = Stage.HEALTHY; 
					int timeInStage = 0;
					Hospital hospital = Hospital.values()[residentCity.ordinal()]; // IMPORTANT: now 1-1 relation city to hospital
					int number = uniqueID;
					boolean inHospital = false;
					boolean inQueue = false;

					// Individual which do not commute is assumed to be around in its own resident city
					Individual idv = new Individual(ageGroup, stage , timeInStage, residentCity, residentCity, hospital, number, inHospital, inQueue);
					allIndividuals[uniqueID] = idv;
					individualsCategorizedInitialisation.get(residentCity).get(ageGroup).add(idv);
					uniqueID ++;	
				}
			}
		}
	}
	/**
	 * Creates all maps/arrays/data structures needed for the simulation. Only needed for first initialization. 
	 */
	private void createAllMaps() {
		/*
		 *  create this Map
		 */
		for(Hospital hos : Hospital.values()){

			EnumMap<AgeGroup, List<Individual>> tempMap = new EnumMap<>(AgeGroup.class);
			for(AgeGroup ageGroup : AgeGroup.values()) {
				List<Individual> individualList = new ArrayList<>();
				tempMap.put(ageGroup, individualList);
			}
			patientsHospitalPerAgeGroup.put(hos, tempMap);
			patientsHospital.put(hos, 0);
			patientNumberHospital.put(hos, new int[totalEpochsHorizon]);
			shuffleHospitals.add(hos);
		}

		/*
		 *  create map
		 */
		for(AgeGroup ageGroup : AgeGroup.values()) {
			EnumMap<Stage, int[]> insideMap = new EnumMap<>(Stage.class);

			for(Stage stage : Stage.values()) {
				int[] nrPeopleForEachDay = new int[totalEpochsHorizon + 1]; // int[] has a timeline, its max size needs to be totalEpochsHorizon
				// + 1 is included as in the last time epoch still people get updated. 
				insideMap.put(stage, nrPeopleForEachDay);
			}
			numberOfPeopleInEachStage.put(ageGroup, insideMap);
		}

		/*
		 * create map 
		 */
		for(int i = 0; i < totalEpochsHorizon ; i ++) {
			EnumMap<AgeGroup,EnumMap<Stage, EnumMap<Stage, Integer>>> ageMap = new EnumMap<>(AgeGroup.class);

			for(AgeGroup agegroup : AgeGroup.values()) {
				EnumMap<Stage, EnumMap<Stage, Integer>> switchers = new EnumMap<>(Stage.class);

				for(Stage stage : Stage.values()) {
					EnumMap<Stage, Integer> tempMap = new EnumMap<>(Stage.class);

					for(Stage stage2 : Stage.values()) {
						tempMap.put(stage2, 0);
					}
					switchers.put(stage, tempMap);
				}
				ageMap.put(agegroup, switchers);
			}

			numberSwitchersStage.put(i, ageMap);
		}

		/*
		 * Create ALPHA_SHORTCUT_HOME, already summed without own city. Shortcut for ALPHA, used for precompution
		 */
		for(AgeGroup ageGroup : AgeGroup.values()) {
			EnumMap<City, Double> tempMap = new EnumMap<>(City.class);

			for(City city : City.values()) {

				double sum = 0.0;

				for(City otherCity : City.values()) {
					if(!otherCity.equals(city)) {
						sum += ALPHA.get(ageGroup).get(city).get(otherCity);
					}
				}				
				tempMap.put(city, sum);
			}
			ALPHA_SHORTCUT_HOME.put(ageGroup, tempMap);
		}
	}

	/**
	 * This class sets up all information needed for this particular simulation input and the given seed.
	 * In case this method is called a second time (etc.), the data structures will be emptied.
	 * Furthermore, it randomly assigns each agent its health stage. 
	 */
	public void initializeSimulation(int seed){

		/*
		 * Reinitialise/empty maps when doing a new simulation
		 */
		if(firstTimeInitialising == true) { // first time doing the simulation no need to reinitialize it
			firstTimeInitialising = false;
			justDoneASimulation = false;
		}
		else {  // empty all data structures
			this.reinitialize();
		}

		/*
		 *  Settting up the random generator
		 */
		rand = new Random(seed);

		/*
		 * Randomly assigning which agent is in which stage. 
		 */
		for(City city : City.values()) {
			for(AgeGroup ageGroup : AgeGroup.values()) {

				List<Individual> idvList = new ArrayList<>(individualsCategorizedInitialisation.get(city).get(ageGroup)); // which agents in this corop with a certain age group are eligible to obtain a certain health stage

				for(Stage stage : Stage.values()) {
					int numberThisStage = INFECTION_NUMBERS.get(city).get(ageGroup).get(stage); // number of agents who should get this health stage

					if(numberThisStage > 0 ) { // only when assignment is needed. If all categories stay 0, all agents stay healthy/susceptible.

						int listSize = idvList.size();

						if(numberThisStage > idvList.size()) throw new IllegalStateException("Number of people in a certain stage exceed number of people in that corop and agegroup. Check your input files.");

						int[] randArray = rand.ints(0, listSize).distinct().limit(numberThisStage).toArray(); // this makes an array containing numbers which corresponds to the agents in numberThisStage and of those only listSize number of agents should be picked.
						List<Individual> removalList = new ArrayList<>(); // consists of all agents who need to be removed
						for(int i = 0 ; i < randArray.length ; i ++) {
							Individual idv = idvList.get(randArray[i]);
							idv.setStage(stage);
							removalList.add(idv);
						}
						idvList.removeAll(removalList); //individuals are removed, so they cannot be assigned multiple stages
					}
				}
			}
		}
	}

	/**
	 * Reinitialize the simulation. Is needed when doing multiple runs of the same simulation instance. 
	 */
	private void reinitialize() {

		/*
		 * Overwrite time in healthy stage
		 */
		for(Individual idv : allIndividuals) {
			idv.setStage(Stage.HEALTHY);
			idv.setTimeInStage(0);
			idv.setInHospital(false);
			idv.setQueue(false);
		}

		/*
		 * emptying maps 
		 */
		for(City city : City.values()) {
			for(AgeGroup agegroup : AgeGroup.values()) {
				double[] tempArray = new double[totalEpochsHorizon];
				infectionRate.get(city).put(agegroup, tempArray);		
			}
		}

		/*
		 *  emptying maps
		 */
		shuffleHospitals.clear(); // just to get the same order again
		queueHospital.clear(); 
		for(Hospital hos : Hospital.values()){
			EnumMap<AgeGroup, List<Individual>> tempMap = patientsHospitalPerAgeGroup.get(hos);
			for(AgeGroup ageGroup : AgeGroup.values()) {
				List<Individual> individualList = tempMap.get(ageGroup);
				individualList.clear();
			}
			patientsHospital.put(hos, 0);
			patientNumberHospital.put(hos, new int[totalEpochsHorizon]);
			shuffleHospitals.add(hos);
		}

		/*
		 *  emptying the map
		 */
		for(AgeGroup ageGroup : AgeGroup.values()) {
			EnumMap<Stage, int[]> insideMap = numberOfPeopleInEachStage.get(ageGroup);

			for(Stage stage : Stage.values()) {
				int[] nrPeopleForEachDay = new int[totalEpochsHorizon + 1]; // int[] has a timeline, its max size needs to be totalEpochsHorizon
				// + 1 is included as in the last time epoch still people get updated. 
				insideMap.put(stage, nrPeopleForEachDay);
			}
		}

		/*
		 * emptying map 
		 */
		for(int i = 0; i < totalEpochsHorizon ; i ++) {

			for(AgeGroup agegroup : AgeGroup.values()) {
				EnumMap<Stage, EnumMap<Stage, Integer>> switchers = numberSwitchersStage.get(i).get(agegroup);

				for(Stage stage : Stage.values()) {
					EnumMap<Stage, Integer> tempMap = switchers.get(stage);

					for(Stage stage2 : Stage.values()) {
						tempMap.put(stage2, 0);
					}
				}
			}
		}

		// overwrite boolean as now stuff is reinitialised
		justDoneASimulation = false;

	}

	/**
	 * This runs the simulation, for each epoch 
	 */
	public void startSimulation(){

		if(justDoneASimulation == true) {
			throw new IllegalStateException("Simulation cannot start until it is (re)initialised.");
		}
		else {
			justDoneASimulation = true;
		}

		/*
		 *  Opening all files and keeping them open until the simulation has finished. This makes writing down all info quicker, and saves memory as we immediately write down all important informaton. 
		 */
		PrintWriter print1 = null;
		PrintWriter print2 = null;
		PrintWriter print3 = null;
		PrintWriter print4 = null;
		PrintWriter print5 = null;
		PrintWriter print6 = null;

		try
		{
			print1 = new PrintWriter(new BufferedWriter(new FileWriter(fileNames[0])));
			print2 = new PrintWriter(new BufferedWriter(new FileWriter(fileNames[1])));
			print3 = new PrintWriter(new BufferedWriter(new FileWriter(fileNames[2])));
			print4 = new PrintWriter(new BufferedWriter(new FileWriter(fileNames[3])));
			print5 = new PrintWriter(new BufferedWriter(new FileWriter(fileNames[4])));
			print6 = new PrintWriter(new BufferedWriter(new FileWriter(fileNames[5])));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		for(int epoch = 0; epoch < totalEpochsHorizon; epoch++){

			/*
			 *  Writing output at the beginning of each epoch. 
			 */
			OutputWriter.outputWriterAggregateStageInfection(print1, allIndividuals, epoch);
			OutputWriter.outputWriterAggregateStageInfectionCityTotal(print2, allIndividuals, epoch);
			OutputWriter.outputWriterHospitalInfo(print3, patientsHospitalPerAgeGroup, queueHospital.size(), epoch);

			/*
			 * Determine all infection rate in each city. 
			 */

			//Intialize the map which contains the number of people who can spread the disease (Ia (infectious asymptomatic), Is (infectious symptomatic)) and are susceptible.
			EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIa = new EnumMap<>(City.class); // Ia who spread the disease
			EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIs = new EnumMap<>(City.class); // Is who spread the disease. 
			EnumMap<City, EnumMap<AgeGroup,Integer>> encounterGroup = new EnumMap<>(City.class); // Will contain all agents per corop and age group (for the denominator of P(I_{a',c,t})) that you could possible encounter

			this.initializeInfectionStructures(infectedWhoSpreadIa, infectedWhoSpreadIs, encounterGroup);

			// Counting which individual is Ia and Is given their resident corop. And counting number of individuals you could encounter
			this.countNumberInfections(infectedWhoSpreadIa, infectedWhoSpreadIs, encounterGroup);

			// Determine Infection probability P_{a,a',c,t}
			EnumMap<AgeGroup, EnumMap<AgeGroup, EnumMap<City, Double>>> infectionProbability = new EnumMap<>(AgeGroup.class); // for this given epoch: P_{a,a',c,t}
			this.determinePaact(infectedWhoSpreadIa, infectedWhoSpreadIs, encounterGroup, epoch, infectionProbability);

			// Determine infection probability P_{a,c,t}
			EnumMap<City, EnumMap<AgeGroup,Double>> infectionProbability_endversion = new EnumMap<>(City.class); // for this given epoch t: P_{a,c,t}
			this.determinePact(epoch, infectionProbability, infectionProbability_endversion);

			/*
			 * Already infected people have a probability of getting more sick or recovering. Healthy (susceptible) people have a probability of getting sick.
			 */
			for(Individual idv : allIndividuals){

				Stage currentStage = idv.getStage();
				AgeGroup ageGroup = idv.getAgeGroup();
				double randomNumber = rand.nextDouble();
				int timeInStage = idv.getTimeInStage() + 1; 
				Stage nextStage = null;

				if(!idv.getStage().equals(Stage.HEALTHY)) { 

					nextStage = this.determineRandomComponent(randomNumber, VIRUS_PROGRESSION.get(ageGroup).get(currentStage));

					if(nextStage.equals(currentStage)) {
						idv.setTimeInStage(timeInStage);
					}
					else {
						idv.setStage(nextStage); // stage of individual is overwritten
						numberOfPeopleInEachStage.get(ageGroup).get(currentStage)[timeInStage] ++;
						idv.setTimeInStage(0); // time is resetted. 
					}
				}
				else {
					City currentCity = this.getCurrentCityIndividual(epoch, idv);
					double infectionRisk = infectionProbability_endversion.get(currentCity).get(ageGroup) + VIRUS_PROGRESSION.get(ageGroup).get(currentStage).get(Stage.INFECTED_NOSYMPTOMS_NOTCONTAGIOUS); // From susceptible (healthy) to exposed (INFECTED_NOSYMPTOMS_NOTCONTAGIOUS)
					if(infectionRisk > 1) {throw new IllegalStateException("Infection rate to go to state Susceptible to Exposed exceeds 1.");}

					if(randomNumber > infectionRisk){  // nextstage is equal to currenstage => you stay healthy
						nextStage = currentStage; // stays healthy
						idv.setTimeInStage(timeInStage);	
					}
					else {// healthy person becomes sick/exposed
						nextStage = Stage.INFECTED_NOSYMPTOMS_NOTCONTAGIOUS;
						idv.setStage(nextStage);
						numberOfPeopleInEachStage.get(ageGroup).get(currentStage)[timeInStage] ++;
						idv.setTimeInStage(0); // time is resetted
					}	
				}
				// new 05-04; 
				numberSwitchersStage.get(epoch).get(ageGroup).get(currentStage).put(nextStage, numberSwitchersStage.get(epoch).get(ageGroup).get(currentStage).get(nextStage) + 1);

			}

			/*
			 * Remove deceased and cured individuals from the queue
			 * Not used ATM (as queues are not needed with the large hospital capacity), but when patients are deceased (D) or immune (IM) they are removed from the queue.
			 */
			Set<Individual> toBeRemoved = new HashSet<Individual>();
			for (Individual idv : queueHospital)
			{
				if(idv.getStage().equals(Stage.DEAD) || idv.getStage().equals(Stage.CURED) ||idv.getStage().equals(Stage.INFECTED_SYMPTOMS_SEVERE_ICnotpossible)){ // third condition is now unused, but can be used when needed
					toBeRemoved.add(idv);
					idv.setQueue(false);
				}
			}
			queueHospital.removeAll(toBeRemoved);

			/*
			 * Remove deceased and cured individuals from the hospitals.
			 * This method is currently needed, however in this set-up it is not needed to get people from the queue into the hospital.
			 * The reason agents enter from the queue here is to give them precedence over other new agents who have just obtained the health stage ICU admission,
			 * as the next Hospital entry loop lets all individuals enter the hospitals when there is a spot left. 
			 */

			System.out.println("Epoch " + epoch);
			for(Hospital hos : Hospital.values()) {

				for(AgeGroup ageGroup : AgeGroup.values()) { // checks for all age group in each hospital whether they contain deceased/cured patients
					List<Individual> idvInHospital = patientsHospitalPerAgeGroup.get(hos).get(ageGroup); // agents of this age group currently in the hospital
					List<Individual> loopList = new ArrayList<>(idvInHospital);  // looplist is created as we cannot remove directly from idvInHospital when looping over the list

					for (Individual idv : loopList) {

						if(!idv.inHospital()){throw new IllegalStateException("Patient should not be inside the hospital.");}

						if (idv.getStage().equals(Stage.DEAD) || idv.getStage().equals(Stage.CURED) || idv.getStage().equals(Stage.INFECTED_SYMPTOMS_SEVERE_ICnotpossible)){ // the ICU place of the this patient becomes available
							idv.setInHospital(false);
							patientsHospitalPerAgeGroup.get(hos).get(idv.getAgeGroup()).remove(idv);
							patientsHospital.put(hos, patientsHospital.get(hos) - 1); // there is now one patient less 

							if (!queueHospital.isEmpty()) { // people already in the queue, still alive and sick. Check whether they can enter directly the hospital. 
								Individual firstPatient = queueHospital.get(0); // first patient (alive and sick) in the queue gets selected

								if(firstPatient.getStage().equals(Stage.DEAD) || firstPatient.getStage().equals(Stage.CURED) || firstPatient.getStage().equals(Stage.INFECTED_SYMPTOMS_SEVERE_ICnotpossible) || firstPatient.getStage().equals(Stage.INFECTED_SYMPTOMS_SEVERE_ICpossible)){
									throw new IllegalStateException("First patient should have already been removed! Or it's stage is not correctly defined - should be QUEUE.");
								}

								firstPatient.setStage(Stage.INFECTED_SYMPTOMS_SEVERE_ICpossible); // in case the patient was in queue, a new stage should be assigned (as it current stage would now be ICQ
								firstPatient.setTimeInStage(0); // this is overwritten as now the patient goes from ICQ to ICY
								firstPatient.setInHospital(true); 
								firstPatient.setQueue(false);
								queueHospital.remove(0);
								patientsHospitalPerAgeGroup.get(hos).get(firstPatient.getAgeGroup()).add(firstPatient);
								patientsHospital.put(hos, patientsHospital.get(hos) + 1); // ICU bed is now occupied again
							}
						}
					}
				}
			}


			/*
			 * Hospital entry loop. Happens when the ICU still has capacity left for new entrances.
			 * Note that this part is not used in our research, however you can use it to see what happens with the hospital queue when the hospital capacity provided is not enough, 
			 */
			for (Individual idv : allIndividuals)
			{
				boolean stage_boolean = (idv.getStage().equals(Stage.INFECTED_SYMPTOMS_SEVERE_ICpossible) || idv.getStage().equals(Stage.INFECTED_SYMPTOMS_SEVERE_QUEUE)); // Note: 26-06-2020: I even think the last check on ICQ is not needed, however this does not influence the results. 

				if( stage_boolean && !idv.inHospital()){// only when hospitalization is needed and the individual is not yet hospitalized, patients enter the hospital. 

					Hospital hos;
					// When your own hospital is available you will go there. In our case this will always be possible, however when IC is limited this is not the case
					if(this.isOwnHospitalAvailable(idv, patientsHospital, HOSPITAL_CAPACITY)) {
						hos = idv.getHospital();
					}
					else {
						hos = this.findEmptyHospital(idv, patientsHospital, HOSPITAL_CAPACITY, shuffleHospitals); // randomly selects another hospital which has an ICU spot left. This is currently not needed to use, as we assume there will be enough capacity.
					}

					if(hos == null){ // if there is no hospital available, this means that hos remains null. This means the patient has to go to the queue. Again, this is not currently needed. 
						// Goes into the queue
						if(!idv.inQueue()){ // if individual not already waiting in the queue, the patient will put in the queue (this happens when this agent has obtained stage ICY this epoch, but the ICU remains fulls)
							queueHospital.add(idv);
							idv.setQueue(true);
							idv.setStage(Stage.INFECTED_SYMPTOMS_SEVERE_QUEUE); // Change from ICY to ICQ
							idv.setTimeInStage(0); // CHECK
						}
					}
					else { // Individual goes into hospital (NOTE: this implies that the queue is empty, as the queue was emptied in the previous section (Remove deceased and cured individuals from the hospitals.))
						patientsHospitalPerAgeGroup.get(hos).get(idv.getAgeGroup()).add(idv);
						patientsHospital.put(hos, patientsHospital.get(hos) + 1);

						// Important detail: in this case it is thus not necessary to overwrite the status to SEVERE_ICpossible, as the agent can only be SEVERE_ICpossible!!! (as the queue is empty!)
						//patientsHospital.get(hos).add(idv); // patient has not status QUEUE, but SEVERE_ICpossible
						idv.setInHospital(true);
						if (!queueHospital.isEmpty()) {
							throw new IllegalStateException("Hospital " + hos + " queue is not empty while below capacity!");
						}
					}
				}
			}

			OutputWriter.outputWriterStageSwitching(print6, numberSwitchersStage.get(epoch), epoch);
		}

		OutputWriter.outputWriterTimeSpendEachStage(print4, numberOfPeopleInEachStage, totalEpochsHorizon);
		OutputWriter.outputWriterInfectionRateInEachCorop(print5, infectionRate, totalEpochsHorizon);

		print1.close();
		print2.close();
		print3.close();
		print4.close();
		print5.close();
		print6.close();
	}

	/**
	 * Determines for a given EnumMap with a random probability p one of the elements, given the numerical value of the keys
	 */
	private <T extends Enum<T>> T determineRandomComponent(double p, EnumMap<T, Double> pMap)
	{
		T lastT = null;
		for(T t : pMap.keySet())
		{		
			double value = pMap.get(t);

			if(p <= value  && value > 1e-13)
			{
				return t; // now it immediately stops the for loop when found
			}
			else
			{
				p -= value;
				lastT = t;
			}
		}
		return lastT; // never happens when prob sums up to exactly 1 otherwise it can happen
	}
	/**
	 * Where is the individual at this given time epoch
	 * @return
	 */
	private City getCurrentCityIndividual(int epoch, Individual idv){

		if(epoch % 2 == 0){ //day time
			return idv.getCommutePlace();
		}
		else{ // night time
			return idv.getResidentPlace();
		}
	}
	/**
	 * Find a random free hospital. If none is found, null is returned
	 * @param idv
	 * @param hosList
	 * @param capacities
	 * @param shuffleHospitalList
	 * @return
	 */
	private Hospital findEmptyHospital(Individual idv, EnumMap<Hospital, Integer> hosList2, EnumMap<Hospital, Integer> capacities, List<Hospital> shuffleHospitalList)
	{
		Collections.shuffle(shuffleHospitalList, rand);

		for (Hospital hos : shuffleHospitalList) {
			if (hosList2.get(hos) < capacities.get(hos)) {return hos;}
		}
		return null;
	}

	/**
	 * Is your own hospital still available? Checks the capacity of your hospital of preference
	 * @param idv
	 * @param hosList
	 * @param capacities
	 * @return
	 */
	private boolean isOwnHospitalAvailable(Individual idv, EnumMap<Hospital, Integer> hosList2, EnumMap<Hospital, Integer> capacities) {

		Hospital ownHospital = idv.getHospital(); 
		if(hosList2.get(ownHospital)< capacities.get(ownHospital)) {
			return true;
		}
		return false;
	}

	/**
	 * Initialize the infection structures, set everything to 0 (each epoch needed, as we have to recount).
	 * @param infectedWhoSpreadIa
	 * @param infectedWhoSpreadIs
	 * @param encounterGroup
	 */
	private void initializeInfectionStructures(EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIa, EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIs, EnumMap<City, EnumMap<AgeGroup,Integer>> encounterGroup) {

		for(City city : City.values()){
			EnumMap<AgeGroup, Integer> tempMapIa = new EnumMap<>(AgeGroup.class);
			EnumMap<AgeGroup, Integer> tempMapIs = new EnumMap<>(AgeGroup.class);
			EnumMap<AgeGroup, Integer> tempMapSusp = new EnumMap<>(AgeGroup.class);

			for(AgeGroup ageGroup : AgeGroup.values()) {
				tempMapIa.put(ageGroup, 0);
				tempMapIs.put(ageGroup, 0);
				tempMapSusp.put(ageGroup, 0);
			}
			infectedWhoSpreadIa.put(city, tempMapIa);
			infectedWhoSpreadIs.put(city, tempMapIs);
			encounterGroup.put(city, tempMapSusp);
		}
	}


	private void countNumberInfections(EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIa, EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIs, EnumMap<City, EnumMap<AgeGroup,Integer>> encounterGroup) {

		// Counting which individual is Ia and Is given their resident corop. And counting number of susceptible individuals
		for(Individual idv : allIndividuals){

			Stage currentStage = idv.getStage();
			AgeGroup currentAge = idv.getAgeGroup();
			City currentCity = idv.getResidentPlace(); // obtain the resident place! IsPORTANT!!!

			if(currentStage.equals(Stage.INFECTED_NOSYMPTOMS_ISCONTAGIOUS)){ 
				// update number who spread
				infectedWhoSpreadIa.get(currentCity).put(currentAge, infectedWhoSpreadIa.get(currentCity).get(currentAge) + 1);
			}
			if(currentStage.equals(Stage.INFECTED_SYMPTOMS_MILD)) {
				infectedWhoSpreadIs.get(currentCity).put(currentAge, infectedWhoSpreadIs.get(currentCity).get(currentAge) + 1);
			}
			if(!notEncounterStages.contains(currentStage)) { // so basically agents that you could encounter
				encounterGroup.get(currentCity).put(currentAge, encounterGroup.get(currentCity).get(currentAge) + 1);	
			}
		}

	}


	private void determinePaact(EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIa, EnumMap<City, EnumMap<AgeGroup,Integer>> infectedWhoSpreadIs, EnumMap<City, EnumMap<AgeGroup,Integer>> encounterGroup, int epoch, EnumMap<AgeGroup, EnumMap<AgeGroup, EnumMap<City, Double>>> infectionProbability) {

		for(AgeGroup a : AgeGroup.values()) {

			EnumMap<AgeGroup, EnumMap<City, Double>> tempMap = new EnumMap<>(AgeGroup.class); // used to store infection prob info

			for(AgeGroup a_prime : AgeGroup.values()) {

				double E_a_aprime = CONTACT_RATIO.get(a).get(a_prime); // P(E_{a,a'})
				EnumMap<City, Double> tempMapInside = new EnumMap<>(City.class); // used to store infection prob info

				for(City city : City.values()) {

					double numerator = 0.0;
					double denominator = encounterGroup.get(city).get(a_prime);// + infectedWhoSpreadIM.get(city).get(a_prime); //  Part of the denominator of P(I_{a,c,t}), will contain more terms during day time. T_{a',c,t}
					
					double home_first = infectedWhoSpreadIa.get(city).get(a_prime) + infectedWhoSpreadIs.get(city).get(a_prime); // all agents living in this city that are infectious. (part of the) first term of H_{a',c,t}
		
					if(epoch % 2 == 0) { // day time. I make a distinction between the two, as the night time one is much easier to compute

						denominator *= ALPHA.get(a_prime).get(city).get(city); // use alpha to know which part of the corop stays in this corop during the day
						double infectedHome = infectedWhoSpreadIa.get(city).get(a_prime) + infectedWhoSpreadIs.get(city).get(a_prime); // all agents living in this city, but some are now in a different corop...
						double home_alpha = ALPHA_SHORTCUT_HOME.get(a_prime).get(city); // which part of this corop are in a different corop during the day, use the shortcut to know the total fraction
						double home_second = - home_alpha * (infectedHome); // this is the part of H_{a',c,t} that has to be subtracted., as some of the agents living in this city are currently working in a different corop

						double travel = 0.0; // used to extend the expression of H_{a'c,t}. This part counts the agents from a different corop travelling during the day to this corop
						for(City otherCity : City.values()) { // summing over all the other cities (c \neq c'). This is used for the denominator

							if(!otherCity.equals(city)) {
								double infectedTravel = infectedWhoSpreadIa.get(otherCity).get(a_prime) + infectedWhoSpreadIs.get(otherCity).get(a_prime); // total infected agents of the other corop, but only a fraction comes to this corop...
								double travel_alpha = ALPHA.get(a_prime).get(otherCity).get(city); // therefore, discount it! Note that we cannot use the shortcut ALPHA
								travel += travel_alpha * (infectedTravel);
								
								denominator += ALPHA.get(a_prime).get(otherCity).get(city) * (encounterGroup.get(otherCity).get(a_prime));// + infectedWhoSpreadIs.get(otherCity).get(a_prIse) );
							}
						}
						numerator = (home_first + home_second + travel); // So, home_first + home_second is the total infectious agents in this corop present during the day and travel is the total infectious agents living in other corops who are present during the day. 

					}
					else { // night tIse
						numerator = home_first; // as everybody stays at home you only count the agents that are contagious living in this corop
					}

					if(denominator == 0) {throw new IllegalStateException("Problem with P[I_{a', c, t}]: divide by zero!");}
					double I_aprime_c_t = numerator / denominator; // finally, you obtain P(I_{a',c,t})

					double totalProbability = I_aprime_c_t * E_a_aprime * VIRUS_TRANSMISSION_PROBABILITY; 	 // P(I_{a',c,t} * P(E_{a,a'}) * P(T) = P_{a,a',c,t}

					tempMapInside.put(city, totalProbability);
				}
				tempMap.put(a_prime, tempMapInside);
			}
			infectionProbability.put(a, tempMap);
		}
	}


	private void determinePact(int epoch, EnumMap<AgeGroup, EnumMap<AgeGroup, EnumMap<City, Double>>> infectionProbability, EnumMap<City, EnumMap<AgeGroup,Double>> infectionProbability_endversion) {
		for(City city : City.values()) {
			EnumMap<AgeGroup, Double> tempMapInfection = new EnumMap<>(AgeGroup.class); // place to store info

			for(AgeGroup agegroup : AgeGroup.values()) {
				double infecProb = 1.0; 				

				for(AgeGroup aprime : AgeGroup.values()) { // over all age groups

					double termA = (1 - infectionProbability.get(agegroup).get(aprime).get(city));
					double termB = 0.5 * NUMBER_DAILY_CONTACTS_PERAGEGROUP.get(agegroup).get(aprime); // multiply by 0.5 as it is assumed you meet halve of your contacts during the night and day
					infecProb *= Math.pow(termA, termB);
				}

				infecProb = 1 - infecProb; // as p_{a,c,t} =  1- \prod_{a' \in A} \left(1-p_{a,a',c,t}\right)^{[\#\textup{DailyContacts}]_{a,a'}}, so you start with 1 and subtract the rest

				tempMapInfection.put(agegroup, infecProb);
				infectionRate.get(city).get(agegroup)[epoch] = infecProb;
			}
			infectionProbability_endversion.put(city, tempMapInfection); // P_{a,c,t}s

		}
	}
}
