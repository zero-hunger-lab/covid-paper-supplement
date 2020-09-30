package nCoV;

import java.util.EnumMap;
import nCoV.Main.AgeGroup;
import nCoV.Main.City;
/**
 * These functions are used to determine the daily contact patterns and to determine alpha.
 * Parameter alpha (\alpha_{c,c',a}) represents the fraction of people from age group a living in corop c and being present in corop c' during the day epoch.  
 * Parameter alpha is used in the simulation to determine for a given corop c the number of agents staying at home during the day (H_{a,c,t}),
 * the number of agents that work in c during the day (W_{a,c,t}) and the total number of agents present (T_{a.c.t}).
 * 
 */ 

public class HelperFunction {

	public static EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> determineNumberDailyContactsPerAgeGroup(EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> CONTACT_RATIO, EnumMap<AgeGroup, Double> NUMBER_DAILY_CONTACTS){

		EnumMap<AgeGroup, EnumMap<AgeGroup, Double>> output = new EnumMap<>(AgeGroup.class);

		for(AgeGroup age : AgeGroup.values()) {
			EnumMap<AgeGroup, Double> tempMap = new EnumMap<>(AgeGroup.class);

			for(AgeGroup age2 : AgeGroup.values()) {
				double contact = CONTACT_RATIO.get(age).get(age2);
				double dailyContact = contact * NUMBER_DAILY_CONTACTS.get(age);
				tempMap.put(age2, dailyContact);
			}
			output.put(age, tempMap);
		}
		return output;
	}

	public static EnumMap<AgeGroup, EnumMap<City, EnumMap<City, Double>>> determineAlpha(EnumMap<AgeGroup, EnumMap<City, EnumMap<City,Integer>>> COMMUTE_DISTRIBUTION, EnumMap<City, EnumMap<AgeGroup, Integer>> POPULATION_NUMBER){

		EnumMap<AgeGroup, EnumMap<City, EnumMap<City, Double>>> output = new EnumMap<>(AgeGroup.class);

		for(AgeGroup agegroup : AgeGroup.values()) {
			EnumMap<City, EnumMap<City, Double>> tempMap = new EnumMap<>(City.class);

			for(City city : City.values()) {

				EnumMap<City, Double> tempMapinside = new EnumMap<>(City.class);
				double numberPeopleLiving = POPULATION_NUMBER.get(city).get(agegroup);
				
				double numberWorkers = 0.0;

				for(City cityCommute : City.values()) {
					numberWorkers += COMMUTE_DISTRIBUTION.get(agegroup).get(city).get(cityCommute);
				}
				
				double peopleStayingHome = numberPeopleLiving - numberWorkers;

				for(City cityCommute : City.values()) {
					if(city.equals(cityCommute)) {
						double alpha_a_c_cprime = (COMMUTE_DISTRIBUTION.get(agegroup).get(city).get(cityCommute) + peopleStayingHome) / numberPeopleLiving;
						tempMapinside.put(cityCommute, alpha_a_c_cprime);
					}
					else {
						double alpha_a_c_cprime = COMMUTE_DISTRIBUTION.get(agegroup).get(city).get(cityCommute) / numberPeopleLiving; 
						tempMapinside.put(cityCommute, alpha_a_c_cprime);
					}
				}
				tempMap.put(city, tempMapinside);
			}

			output.put(agegroup, tempMap);
		}
		return output;
	}
}
