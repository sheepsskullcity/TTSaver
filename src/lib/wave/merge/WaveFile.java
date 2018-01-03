package lib.wave.merge;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WaveFile {
	
	static final int RIFF = 1179011410;
	static final int FMT = 544501094;
	static final int DATA = 1635017060;
	static final int WAVE = 1163280727;

		protected class RIFFChunk {
			public int chunkID;
			public int chunkSize;
			public int format;
		}
		protected class fmtChunk {
			public int chunkID;
			public int chunkSize;
			public short audioFormat;
			public short numChannels;
			public int sampleRate;
			public int byteRate;
			public short blockAlign;
			public short bitsPerSample;
		}
		protected class WaveHeader
		{
			public RIFFChunk riff;
			public fmtChunk fmt;
			public int hdata = 1635017060;
			WaveHeader() {
				riff = new RIFFChunk();
				fmt = new fmtChunk();
			}
		}
	
		protected enum AudioFormat {
			UNKNOWN(0), PCM(1);
			private int value;
			
	        private AudioFormat(int value) {
	        	this.value = value;
	        }
	        
	        public short getValue() {
	        	return (short)(value);
	        }
		}
			
		protected class Meta {
			AudioFormat audioFormat;
			public short numChannels;
			public int sampleRate;
			public short bitsPerSample;
		}
		
		protected Meta meta;
		protected WaveHeader header;
		protected byte[] data;
		protected int dataSize;
		
		private int pause = -1;
		
		public WaveFile()
		{
			meta = new Meta();
		}
		
		public void setMaxPause(int p) {
			pause = p;
		}
		
		public void loadWaveFile(String filename)
		{
			try {
				Load(filename);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private boolean isLoaded()
		{
			return (data != null && dataSize != 0);
		}
		
		public AudioFormat getAudioFormat()
		{
			return meta.audioFormat;
		}
		
		int getNumChannels()
		{
			return meta.numChannels;
		}
		
		int getSampleRate()
		{
			return meta.sampleRate;
		}
		
		int getBitsPerSample()
		{
			return meta.bitsPerSample;
		}

		byte[] getData()
		{
			return data;
		}
		
		int getDataSize()
		{
			return dataSize;
		}
		
		private void Unload()
		{
			data = null;
			dataSize = 0;
		}
	
	protected int intFromBytes(byte[] b_array) {
		ByteBuffer bb = ByteBuffer.allocate(b_array.length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(b_array);
		bb.rewind();
		return bb.getInt();
	}
	
	protected short shortFromBytes(byte[] b_array) {
		ByteBuffer bb = ByteBuffer.allocate(b_array.length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(b_array);
		bb.rewind();
		return (bb.getShort());
	}

	private boolean Load(String filename) throws IOException
	{
		if (isLoaded())
		{
			Unload();
		}

		File f = new File(filename);
		InputStream in = new BufferedInputStream(new FileInputStream(f));

		header = new WaveHeader();
		byte[] buff4 = new byte[4];
		byte[] buff2 = new byte[2]; 

		while (in.available() > 0)
		{
			int chunkID;
			int chunkSize;
			
			in.read(buff4);
			chunkID = intFromBytes(buff4);
			in.read(buff4);
			chunkSize = intFromBytes(buff4);

			switch (chunkID)
			{
			case RIFF:
				{
					header.riff.chunkID = chunkID;
					header.riff.chunkSize = chunkSize;
					in.read(buff4);
					header.riff.format = intFromBytes(buff4);

					if (header.riff.format != WAVE)
					{
						System.out.println("Error: Not a valid WAVE file.");
						return false;
					}

					break;
				}
			case FMT:
				{
					header.fmt.chunkID = chunkID;
					header.fmt.chunkSize = chunkSize;
					int x = chunkSize;
					in.read(buff2);
					header.fmt.audioFormat = shortFromBytes(buff2);
					x = x - buff2.length;
					in.read(buff2);
					header.fmt.numChannels = shortFromBytes(buff2);
					x = x - buff2.length;
					in.read(buff4);
					header.fmt.sampleRate = intFromBytes(buff4);
					x = x - buff4.length;
					in.read(buff4);
					header.fmt.byteRate = intFromBytes(buff4);
					x = x - buff4.length;
					in.read(buff2);
					header.fmt.blockAlign = shortFromBytes(buff2);
					x = x - buff2.length;
					if (header.fmt.chunkSize == 16) {
						in.read(buff2);
						header.fmt.bitsPerSample = shortFromBytes(buff2);
					}
					else
					{
						in.read(buff2);
						header.fmt.bitsPerSample = shortFromBytes(buff2);
						x = x - buff2.length;
						in.skip((long)(x));
					}

					if (header.fmt.audioFormat != AudioFormat.PCM.getValue())
					{
						System.out.println("Error: Not in PCM format");
						return false;
					}
					if (header.fmt.bitsPerSample % 2 != 0)
					{
						System.out.println("Error: Invalid number of bits per sample");
						return false;
					}
					if (header.fmt.byteRate != (header.fmt.sampleRate * header.fmt.numChannels * header.fmt.bitsPerSample / 8))
					{
						System.out.println("Error: Invalid byte rate");
						return false;
					}
					if (header.fmt.blockAlign != (header.fmt.numChannels * header.fmt.bitsPerSample / 8))
					{
						System.out.println("Error: Invalid block align");
						return false;
					}

					break;
				}
			case DATA:
				{
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
					dataSize = chunkSize;
					int brate;
					int nPause;
					if (header.fmt.sampleRate % 11025 == 0 && pause != -1) {
						if (pause % 200 == 0) {
							nPause = pause;
						} else if (pause != 50) {
							nPause = pause + 20;
						} else {
							nPause = 40;
						}
					} else {
						nPause = pause;
					}
					byte[] a = new byte[dataSize];
					in.read(a);
					for (int i = 0; i < a.length && pause != -1; i++) {
						if (a[i] == 0 && (i % ((header.fmt.bitsPerSample * header.fmt.numChannels) / 8) == 0)) {
							int count = 0;
							while (i < a.length && a[i] == 0) {
								count++;
								i++;
							}
							brate = (header.fmt.byteRate * nPause) / 1000;
							if (count > brate) {
								outputStream.write(new byte[brate + ((count % ((header.fmt.bitsPerSample * header.fmt.numChannels) / 8)))]);
							} else {
								outputStream.write(new byte[count]);
							}
						}
						if (i < a.length){
							outputStream.write(a, i, 1);
						}
					}
					if (pause != -1) {
						data = outputStream.toByteArray();
						dataSize = data.length;
					} else {
						data = a;
					}
					break;
				}
			default:
				{
					in.skip((long)(chunkSize));

					break;
				}
			}
		}
		
		in.close();

		// Check that we got all chunks
		if (header.riff.chunkID != RIFF)
		{
			System.out.println("Error: Missing RIFF chunk.");
			return false;
		}
		if (header.fmt.chunkID != FMT)
		{
			System.out.println("Error: Missing fmt chunk.");
			return false;
		}
		if (data == null || dataSize == 0)
		{
			System.out.println("Error: Missing data chunk.");
			return false;
		}

		// Fill meta struct
		if (header.fmt.audioFormat == AudioFormat.PCM.getValue())
			meta.audioFormat = AudioFormat.PCM;
		else
			meta.audioFormat = AudioFormat.UNKNOWN;
		
		meta.numChannels   = header.fmt.numChannels;
		meta.sampleRate    = header.fmt.sampleRate;
		meta.bitsPerSample = header.fmt.bitsPerSample;

		return true;
	}
}
