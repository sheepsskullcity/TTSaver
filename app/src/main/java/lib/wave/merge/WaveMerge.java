package lib.wave.merge;

public class WaveMerge {
	
	private int numChannels;
	private int sampleRate;
	private int bitsPerSample;
	

	public void merge(String[] args, String outFileName, int pause) {
		WaveFile wavf = new WaveFile();
		wavf.setMaxPause(pause);
		wavf.loadWaveFile(args[0]);
		
		numChannels = wavf.getNumChannels();
		sampleRate = wavf.getSampleRate();
		bitsPerSample = wavf.getBitsPerSample();
		
		WaveAppend wavapp = new WaveAppend();
		wavapp.first(wavf, outFileName);
		for (int i = 1; i < args.length; i++) {
			wavf.loadWaveFile(args[i]);
			wavapp.next(wavf, outFileName);
		}
	}
	
	public void mergeToRaw(String[] args, String outFileName) {
		WaveFile wavf = new WaveFile();
		wavf.loadWaveFile(args[0]);
		
		numChannels = wavf.getNumChannels();
		sampleRate = wavf.getSampleRate();
		bitsPerSample = wavf.getBitsPerSample();
		
		WaveAppend wavapp = new WaveAppend();
		wavapp.firstRaw(wavf, outFileName);
		for (int i = 1; i < args.length; i++) {
			wavf.loadWaveFile(args[i]);
			wavapp.nextRaw(wavf, outFileName);
		}
	}
		
	public int getNumChannels() {
		return numChannels;
	}
	
	public int getSampleRate() {
		return sampleRate;
	}
	
	public int getBitsPerSample() {
		return bitsPerSample;
	}
		
/*		System.out.println("Rate - " + wavf.getSampleRate());
		System.out.println("Bits per sample - " + wavf.getBitsPerSample());
		System.out.println("Number of channels - " + wavf.getNumChannels());
		System.out.println("Data size - " + wavf.getDataSize());
		System.out.println("Data size - " + wavf.getAudioFormat());
*/

}
