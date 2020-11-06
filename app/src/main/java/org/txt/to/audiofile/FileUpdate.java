package org.txt.to.audiofile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class FileUpdate {
	
	public static void writeData(String str, String fileName, boolean append) {
		File file;
		BufferedWriter writer = null;
		try {
			file = new File(fileName);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8));
			
			writer.write(str);
			writer.write("\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
