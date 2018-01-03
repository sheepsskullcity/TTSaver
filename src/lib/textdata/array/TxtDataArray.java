package lib.textdata.array;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.mozilla.universalchardet.UniversalDetector;


public class TxtDataArray {

		public ArrayList<String> dataArray = new ArrayList<String>();
		private Charset charset;
		private int bSize;
		private boolean cancel = false;
		
		public TxtDataArray(ArrayList<String> data) {
			dataArray = data;
			bSize = dataArray.size();
		}
		
		public TxtDataArray(String fileName) {
			dataArray = new ArrayList<String>();
	        File f = new File(fileName);
	        try {
				charset = getCharset(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        readData(fileName);
		}
		
		public void useRules(ArrayList<String[]> list, int start, int end) {
			if (end <= 0 || end > dataArray.size())
				end = dataArray.size();
			
			if (start > 0 && start < end)
				start = start - 1;
			else
				start = 0;

			for (int i = start; i < end && !cancel; i++) {
				for (String[] s : list) {
					if (cancel || Thread.currentThread().isInterrupted())
						break;
					dataArray.set(i, dataArray.get(i).replaceAll(s[0], s[1]));
				}
			}
		}
		
		public void stopRules() {
			cancel = true;
		}
		
		private Charset getCharset(File f) throws java.io.IOException {
		    byte[] buf = new byte[512];
		    FileInputStream fis = new FileInputStream(f);
		    
		    UniversalDetector detector = new UniversalDetector(null);

		    int nread;
		    while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
		      detector.handleData(buf, 0, nread);
		    }

		    detector.dataEnd();

		    String encoding = detector.getDetectedCharset();

		    detector.reset();
		    fis.close();
		    if (encoding != null) {
		    	if (encoding.equalsIgnoreCase("MACCYRILLIC")) {
		    		return Charset.forName("Cp1251");
		    	}
				return Charset.forName(encoding);
			} else {
				System.out.println("No encoding detected.");
				return Charset.forName("UTF-8");
			}
		}
		
		private void readData(String fileName) {
			bSize = 0;
			String temp = null;
			File file = null;
			FileInputStream iS = null;
			InputStreamReader iSR = null;
			BufferedReader reader = null;
			if (charset != null) {
				try {
					file = new File(fileName);
					iS = new FileInputStream(file);
					iSR = new InputStreamReader(iS, charset);
					reader = new BufferedReader(iSR);
					while ((temp = reader.readLine()) != null) {
						temp = temp.replaceAll("\r+|\n+|\t+", "");
		            	temp = temp.replaceAll("^\\s|\\s$", "");
		            	temp = temp.replaceAll("\\s\\s+", " ");
		            	if (!temp.isEmpty()) {
		            		dataArray.add(temp);
		            	}
					}
					bSize = dataArray.size();
					iSR.close();
					iS.close();
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException("Unsupported encoding");
				} catch (FileNotFoundException e) {
					throw new RuntimeException("File not found");
				} catch (IOException e) {
					throw new RuntimeException("IO Error occured");
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				finally {
					if (reader != null) {
						try {
							reader.close();
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			else {
	            System.out.println("Book unrecognized charset.");
	        }
		}
		
		public int dataSize() {
			return bSize;
		}
		
		public String getDataString(int i) {
			return dataArray.get(i);
		}
		
		public void setDataString(String str, int i) {
			dataArray.set(i, str);
		}
		
		public void writeDataToFile(String fileName) {
			File file = null;
			BufferedWriter writer = null;
			int i = 0;
			try {
				file = new File(fileName);
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				while (i < dataArray.size()) {
					writer.write(dataArray.get(i));
					writer.write("\n");
					i++;
				}
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
