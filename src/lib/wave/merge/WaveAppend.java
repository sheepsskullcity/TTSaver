package lib.wave.merge;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class WaveAppend extends WaveFile {

	int totalDataSize;
	public WaveAppend() {
		super();
		totalDataSize = 0;
	}
	
	private byte[] bytesFromInt(int x) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(x);
		bb.rewind();
		return bb.array();
	}
	
	private byte[] bytesFromShort(short x) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort(x);
		bb.rewind();
		return bb.array();
	}
	
	public void first(WaveFile wavf, String str) {
		meta.audioFormat = wavf.meta.audioFormat;
		meta.bitsPerSample = wavf.meta.bitsPerSample;
		meta.numChannels = wavf.meta.numChannels;
		meta.sampleRate = wavf.meta.sampleRate;
		header = wavf.header;
		totalDataSize = wavf.getDataSize();
		writeHeader(str);
		writeData(str, wavf);
	}
	
	public void next(WaveFile wavf, String str) {
		if (meta.bitsPerSample == wavf.meta.bitsPerSample &&
				meta.numChannels == wavf.meta.numChannels &&
				meta.sampleRate == wavf.meta.sampleRate) {
			totalDataSize = totalDataSize + wavf.getDataSize();
			writeData(str, wavf);
			updateHeader(str);
		} else {
			System.out.println("Wav meta not matching");
		}
	}
	
	public void firstRaw(WaveFile wavf, String str) {
		meta.audioFormat = wavf.meta.audioFormat;
		meta.bitsPerSample = wavf.meta.bitsPerSample;
		meta.numChannels = wavf.meta.numChannels;
		meta.sampleRate = wavf.meta.sampleRate;
		header = wavf.header;
		totalDataSize = wavf.getDataSize();
		writeData(str, wavf);
	}
	
	public void nextRaw(WaveFile wavf, String str) {
		if (meta.bitsPerSample == wavf.meta.bitsPerSample &&
				meta.numChannels == wavf.meta.numChannels &&
				meta.sampleRate == wavf.meta.sampleRate) {
			totalDataSize = totalDataSize + wavf.getDataSize();
			writeData(str, wavf);
		} else {
			System.out.println("Wav meta not matching");
		}
	}
	
	private void writeHeader(String str) {
		File file = new File(str);
		BufferedOutputStream buffOut = null;
		try {
			buffOut = new BufferedOutputStream(new FileOutputStream(file));
			buffOut.write(bytesFromInt(header.riff.chunkID));
			buffOut.write(bytesFromInt(header.riff.chunkSize));
			buffOut.write(bytesFromInt(header.riff.format));
			buffOut.write(bytesFromInt(header.fmt.chunkID));
			buffOut.write(bytesFromInt(header.fmt.chunkSize));
			buffOut.write(bytesFromShort(header.fmt.audioFormat));
			buffOut.write(bytesFromShort(header.fmt.numChannels));
			buffOut.write(bytesFromInt(header.fmt.sampleRate));
			buffOut.write(bytesFromInt(header.fmt.byteRate));
			buffOut.write(bytesFromShort(header.fmt.blockAlign));
			buffOut.write(
					header.fmt.chunkSize == 16 ? 
					bytesFromShort(header.fmt.bitsPerSample) :
					bytesFromInt((int)(header.fmt.bitsPerSample))
					);
			buffOut.write(bytesFromInt(header.hdata));
			buffOut.write(bytesFromInt(totalDataSize));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (buffOut != null)
					buffOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void writeData(String str, WaveFile wavf) {
		File file = new File(str);
		BufferedOutputStream buffOut = null;
		try {
			buffOut = new BufferedOutputStream(new FileOutputStream(file, true));
			buffOut.write(wavf.data);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (buffOut != null)
					buffOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void updateHeader(String str) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(new File(str), "rw");
		    raf.seek(header.fmt.chunkSize == 16 ? 40 : 42);
		    raf.write(bytesFromInt(totalDataSize));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    try {
		    	if (raf != null)
		    		raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
