package nCoV;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Scanner;

import nCoV.Main.*;

/**
 * Class containing static methods to read input
 */
public class InputReader
{
	public static InputSettings readParameterSettings(String fileName) throws FileNotFoundException {

		// initialize
		double virusTransmissionProbability = 0; 
		int totalNumberSimulations = 0;
		int scenarioNumber = 0;
		int totalEpochsHorizon = 0;
		String folderName = "";

		// Create the scanner
		Scanner in = new Scanner(new File( System.getProperty("user.dir") + "/input/" + fileName));

		// Create the line scanner
		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			if(words[0].equals("virusTransmissionProbability"))
			{
				virusTransmissionProbability = Double.parseDouble(words[1]);
			}
			else if(words[0].equals("totalNumberSimulations")) {
				totalNumberSimulations = Integer.parseInt(words[1]);
			}
			else if(words[0].equals("scenarioNumber"))
			{
				scenarioNumber = Integer.parseInt(words[1]);
			}
			else if(words[0].equals("totalEpochsHorizon"))
			{
				totalEpochsHorizon = Integer.parseInt(words[1]);
			}			
			else if(words[0].equals("folderNameOutput"))
			{
				if(words.length > 1) { // only when input is given

					folderName = words[1];
					if(!folderName.trim().isEmpty()) { // only when the folderName doesn't consist of spaces or tabs

						folderName = folderName.replace("\\", "/");
						while (folderName.startsWith("/")) {
							folderName = folderName.substring(1);
						}
						while(folderName.endsWith("/")) {
							folderName = folderName.substring(0, folderName.length() - 1);
						}

						String outputFolderExtension =  System.getProperty("user.dir") + "/output/" + folderName + "/";
						File dir = new File(outputFolderExtension);

						if(!dir.exists()) throw new IllegalArgumentException("Output folder " + dir.toString() + " does not exist."); 

					}
				}
			}
		}
		in.close();
		InputSettings inputSet = new InputSettings(scenarioNumber, totalNumberSimulations, virusTransmissionProbability,totalEpochsHorizon,folderName);
		return inputSet; 

	}


	public static EnumMap<City, EnumMap<City, Integer>> readCommuteCSV(String fileName) throws FileNotFoundException
	{
		// Try to open the file
		Scanner in = new Scanner(new File(fileName));
		in.useLocale(Locale.ENGLISH); // Avoid dot-comma mess

		// Create output map
		EnumMap<City, EnumMap<City, Integer>> map = new EnumMap<>(City.class);
		// Fill the map with maps
		for (City city : City.values())
		{
			map.put(city, new EnumMap<>(City.class));
		}

		// Fill the array
		// It is a double for-loop over the cities. We'll check if the file is formatted well
		// and has the right dimensions
		for (City cFrom : City.values())
		{
			// Create a scanner over a line
			if (!in.hasNextLine())
			{
				in.close();
				throw new IllegalStateException("File has too little rows. Are some cities missing?");
			}
			Scanner line = new Scanner(in.nextLine());
			line.useLocale(Locale.ENGLISH);
			line.useDelimiter(",");

			for (City cTo : City.values())
			{
				if (!line.hasNextDouble())
				{
					line.close();
					throw new IllegalStateException("File has too little columns. Are some cities missing?");
				}
				map.get(cFrom).put(cTo, (int) (line.nextDouble()*1000));
			}

			line.close();
		}

		in.close();

		return map;
	}

	public static EnumMap<City, Integer> readPopulationNumberCSV(String fileName) throws FileNotFoundException
	{
		// Create the scanner
		Scanner in = new Scanner(new File(fileName));

		//Create the output map
		EnumMap<City, Integer> map = new EnumMap<>(City.class);

		for (City c : City.values())
		{
			if (!in.hasNextLine())
			{
				in.close();
				throw new IllegalStateException("File has too little rows. Are some cities missing?");
			}
			// Create the line scanner
			Scanner line = new Scanner(in.nextLine());
			line.useDelimiter(",");

			// Do the city string check
			if (!line.hasNext())
			{
				line.close();
				throw new IllegalStateException("File misses city name column.");
			}
			String cityName = line.next();
			cityName = cityName.replaceAll("\\(CR\\)","");
			cityName = cityName.replace('-', '_');
			cityName = cityName.replace('\'', '_');
			cityName = cityName.replace('/', '_');

			if (!c.toString().equals(cityName))
			{
				line.close();
				throw new IllegalStateException("Provided city name does not match: " + c + " expected, found " + cityName + ".");
			}

			// Get the integer
			if (!line.hasNextInt())
			{
				line.close();
				throw new IllegalStateException("Integer not found for city " + c + ".");
			}
			map.put(c, line.nextInt());

			line.close();
		}

		in.close();

		return map;
	}

	public static EnumMap<Hospital, Integer> readICcapacityCSV(String fileName) throws FileNotFoundException{
		// Create the scanner
		Scanner in = new Scanner(new File(fileName));

		//Create the output map
		EnumMap<Hospital, Integer> map = new EnumMap<>(Hospital.class);

		for(Hospital hos : Hospital.values()) {
			map.put(hos, 0);
		}

		if(in.hasNextLine()) {
			in.nextLine(); //skip first line
		}

		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			String coropName = words[1];
			Integer ICbeds = 5000000 * 2 * Integer.parseInt(words[13]); // Large IC capacity

			Hospital hos = null;

			try {
				hos = Hospital.valueOf(coropName);
			} catch (IllegalArgumentException ex) {  
			}

			Integer currentICbeds = map.get(hos);
			map.put(hos, ICbeds + currentICbeds);
		}
		in.close();
		return map;
	}



	public static EnumMap<AgeGroup, EnumMap<Stage, EnumMap<Stage, Double>>> readTransitionProbabilities(String fileName) throws FileNotFoundException{

		// Create the scanner
		Scanner in = new Scanner(new File(fileName));

		//Create the output map
		EnumMap<AgeGroup, EnumMap<Stage, EnumMap<Stage, Double>>> map = new EnumMap<>(AgeGroup.class);

		for(AgeGroup ageGroup: AgeGroup.values()) {
			EnumMap<Stage, EnumMap<Stage, Double>> tempMap = new EnumMap<>(Stage.class);
			for(Stage stage : Stage.values()) {
				EnumMap<Stage, Double> tempMap2 = new EnumMap<>(Stage.class);
				for(Stage stage2: Stage.values()) {
					tempMap2.put(stage2, 0.0);
				}
				tempMap.put(stage, tempMap2);
			}
			map.put(ageGroup, tempMap);
		}

		String[] firstLine = null;

		if(in.hasNextLine()) {
			firstLine = in.nextLine().split(";"); 
		}

		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			String ageName = words[0];
			String currentStageName = words[1];

			AgeGroup ageGroup = null;
			Stage currentStage = null;

			try {
				ageGroup = AgeGroup.valueOf(ageName);
				currentStage = Stage.valueOf(currentStageName);
				//yes
			} catch (IllegalArgumentException ex) {  
				//nope
			}

			for(Stage otherStage : Stage.values()) {
				double infectionRate = Double.parseDouble(words[otherStage.ordinal() + 2]); // +2 for the first two columns
				map.get(ageGroup).get(currentStage).put(otherStage, infectionRate);
			}	
		}

		in.close();
		return map;
	}


	public static EnumMap<City, EnumMap<AgeGroup, EnumMap<Stage, Integer>>> readInitialInfectionCSV(String fileName) throws FileNotFoundException{

		// Create the scanner
		Scanner in = new Scanner(new File(fileName));

		//Create the output map
		EnumMap<City, EnumMap<AgeGroup, EnumMap<Stage, Integer>>> map = new EnumMap<>(City.class);

		// Initialize
		for(City city : City.values()) {
			EnumMap<AgeGroup, EnumMap<Stage, Integer>> tempMap = new EnumMap<>(AgeGroup.class);
			for(AgeGroup ageGroup: AgeGroup.values()) {
				EnumMap<Stage, Integer> tempMap2 = new EnumMap<>(Stage.class);
				for(Stage stage : Stage.values()) {
					tempMap2.put(stage, 0);
				}
				tempMap.put(ageGroup, tempMap2);
			}
			map.put(city, tempMap);
		}

		if(in.hasNextLine()) {
			in.nextLine(); 
		}

		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			String coropName = words[0];
			String ageGroupName = words[1];
			String stageName = words[2];

			City city = null;
			AgeGroup ageGroup = null;
			Stage currentStage = null;


			try {
				city = City.valueOf(coropName);
				ageGroup = AgeGroup.valueOf(ageGroupName);
				currentStage = Stage.valueOf(stageName);
				//yes
			} catch (IllegalArgumentException ex) {  
				//nope
			}

			Integer numberInThisStage = Integer.parseInt(words[3]);
			map.get(city).get(ageGroup).put(currentStage, numberInThisStage);

		}

		in.close();
		return map;
	}


	public static EnumMap<City, EnumMap<AgeGroup, Integer>> readPopulationDistributionCSV(String fileName) throws FileNotFoundException{

		// Create the scanner
		Scanner in = new Scanner(new File(fileName));

		//Create the output map
		EnumMap<City, EnumMap<AgeGroup, Integer>> map = new EnumMap<>(City.class);

		// Initialize
		for(City city : City.values()) {
			EnumMap<AgeGroup, Integer> tempMap = new EnumMap<>(AgeGroup.class);
			for(AgeGroup ageGroup: AgeGroup.values()) {
				tempMap.put(ageGroup, 0);
			}
			map.put(city, tempMap);
		}

		if(in.hasNextLine()) {
			in.nextLine(); 
		}

		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			String coropName = words[0];
			String ageGroupName = words[1];

			City city = null;
			AgeGroup ageGroup = null;

			try {
				city = City.valueOf(coropName);
				ageGroup = AgeGroup.valueOf(ageGroupName);
				//yes
			} catch (IllegalArgumentException ex) {  
				//nope
			}

			Integer numberInThisStage = Integer.parseInt(words[2]);
			map.get(city).put(ageGroup, numberInThisStage);
		}

		in.close();
		return map;
	}

	public static EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> readContactPatternCSV(String fileName) throws FileNotFoundException{

		// Create the scanner
		Scanner in = new Scanner(new File(fileName));
		in.useLocale(Locale.ENGLISH); 

		//Create the output map
		EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> map = new EnumMap<>(AgeGroup.class);

		// Initialize
		for(AgeGroup ageGroup : AgeGroup.values()) {
			EnumMap<AgeGroup, Double> tempMap = new EnumMap<>(AgeGroup.class);
			for(AgeGroup ageGroup2 : AgeGroup.values()) {
				tempMap.put(ageGroup2, 0.0);
			}
			map.put(ageGroup, tempMap);
		}

		if(in.hasNextLine()) {
			in.nextLine(); // skip header
		}

		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			String ageGroupName = words[0];

			AgeGroup ageGroup = null;

			try {
				ageGroup = AgeGroup.valueOf(ageGroupName);
			} catch (IllegalArgumentException ex) {  
			}
			for(AgeGroup ageGroup2 : AgeGroup.values()) {
				Double numberContact = Double.parseDouble(words[ageGroup2.ordinal() +1 ]); // +1 for the agegroup defined in [0]
				map.get(ageGroup).put(ageGroup2, numberContact);
			}
		}

		in.close();
		return map;
	}

	public static EnumMap<AgeGroup, Double> readDailyContacts(String fileName) throws FileNotFoundException{

		// Create the scanner
		Scanner in = new Scanner(new File(fileName));
		in.useLocale(Locale.ENGLISH); 

		//Create the output map
		EnumMap<AgeGroup, Double> output = new EnumMap<>(AgeGroup.class);

		while(in.hasNextLine()) {
			String line = in.nextLine();
			String[] words = line.split(",");

			AgeGroup ageGroup = null;
			String ageGroupName = words[0];

			try {
				ageGroup = AgeGroup.valueOf(ageGroupName);
			} catch (IllegalArgumentException ex) {  
			}

			double number = Double.parseDouble(words[1]);
			output.put(ageGroup, number);
		}

		return output;
	}

}
