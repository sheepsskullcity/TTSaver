package org.txt.to.audiofile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileUpdate {
	
	public static void writeData(String str, String fileName) {
		File file = null;
		BufferedWriter writer = null;
		try {
			file = new File(fileName);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF8"));
			
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
