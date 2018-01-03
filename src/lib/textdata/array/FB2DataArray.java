package lib.textdata.array;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.input.XmlStreamReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class FB2DataArray extends XMLParser {

	private int bSize;
	private boolean cancel = false;
	
	public FB2DataArray(String fileName, boolean ignoreLinks){
		
		super(Arrays.asList("p", "v"), Arrays.asList(ignoreLinks ? "a" : ""));
		
        try {
			readData(fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public FB2DataArray(ZipFile zf, boolean ignoreLinks) {
		super(Arrays.asList("p", "v"), Arrays.asList(ignoreLinks ? "a" : ""));
        for (Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zf.entries(); e.hasMoreElements();) {
        	ZipEntry ze = e.nextElement();
        	String name = ze.getName().toLowerCase();
        	if (!ze.isDirectory() && name.endsWith(".fb2")) {
				try {
					readData(zf.getInputStream(ze));
					break;
				} catch (XmlPullParserException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
        	}
        }
	}
	
	public void useRules(ArrayList<String[]> list, int start, int end) {
		if (end <= 0 || end > super.dataArray.size())
			end = super.dataArray.size();
		
		if (start > 0 && start < end)
			start = start - 1;
		else
			start = 0;
		
		for (int i = start; i < end && !cancel; i++) {
			for (String[] s : list) {
				if (cancel || Thread.currentThread().isInterrupted())
					break;
				super.dataArray.set(i, super.dataArray.get(i).replaceAll(s[0], s[1]));
			}
		}
	}
	
	public void stopRules() {
		cancel = true;
	}
	
	private void readData(String inputFile) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        XmlStreamReader xr = new XmlStreamReader(new FileInputStream(inputFile));
        xpp.setInput(xr);
        parseXML(xpp);
	    xr.close();
	    bSize = super.dataArray.size();
	}
	
	private void readData(InputStream is) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        XmlStreamReader xr = new XmlStreamReader(is);
        xpp.setInput(xr);
        parseXML(xpp);
        is.close();
	    bSize = super.dataArray.size();
	}
	
	public int dataSize() {
		return bSize;
	}
	
	public String getDataString(int i) {
		return super.dataArray.get(i);
	}
	
	public void setDataString(String str, int i) {
		super.dataArray.set(i, str);
	}
	
}
