package nCoV;

/**
 * This class saves all input settings of a given simulation.
 *
 */
public class InputSettings {

	private int scenario;
	private int totalNumberSimulations;
	private double virusTransmissionProbability;
	private int totalEpochsHorizon;
	private String folderName;

	
	public InputSettings(int scenario,
						int totalNumberSimulations,
						double virusTransmissionProbability,
						int totalEpochsHorizon,
						String folderName) {
		
		this.scenario = scenario;
		this.totalNumberSimulations = totalNumberSimulations;
		this.virusTransmissionProbability = virusTransmissionProbability;
		this.totalEpochsHorizon = totalEpochsHorizon;
		this.folderName = folderName;
	}


	public int getTotalNumberSimulations() {
		return totalNumberSimulations;
	}


	public int getScenario() {
		return scenario;
	}

	public double getVirusTransmissionProbability() {
		return virusTransmissionProbability;
	}


	public int getTotalEpochsHorizon() {
		return totalEpochsHorizon;
	}

	public String getFolderName() {
		return folderName;
	}
	
	
}
