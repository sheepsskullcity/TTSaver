package lib.textdata.array;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class XMLParser {
	
	public ArrayList<String> dataArray = new ArrayList<>();
	private final List<String> textTagsList;
	private final List<String> skipTagsList;
	private List<String> listTagsList;
	private List<String> listItemTagsList;
	
	public XMLParser(List<String> textTags, List<String> skipTags) {
		textTagsList = textTags;
		skipTagsList = skipTags;
	}
	
	public XMLParser(List<String> textTags, List<String> skipTags
			, List<String> listTags, List<String> listItemTags) {
		textTagsList = textTags;
		skipTagsList = skipTags;
		listTagsList = listTags;
		listItemTagsList = listItemTags;
	}
	
	public void parseXML(XmlPullParser xpp) throws XmlPullParserException, IOException {
        int eventType = xpp.getEventType();
	    while (eventType != XmlPullParser.END_DOCUMENT) {
	    	StringBuilder temp = new StringBuilder();
	        if (eventType == XmlPullParser.START_TAG) {
	            if (checkStartName(xpp)) {
	            	String txtTag = xpp.getName();
	            	while (eventType != XmlPullParser.END_DOCUMENT) {
	            		eventType = xpp.next();
	            		if (eventType == XmlPullParser.START_TAG) {
	            			if (checkSkipName(xpp)) {
	            				String skipTag = xpp.getName();
	            				while (eventType != XmlPullParser.END_DOCUMENT) {
	            					if (eventType == XmlPullParser.END_TAG) {
	            						if (checkEndName(xpp, skipTag)) {
	            							break;
	            						}
	            					}
	            					eventType = xpp.next();
	            				}
	            			}
	            		}
	            		if (eventType == XmlPullParser.TEXT) {
	            			temp.append(xpp.getText());
	            		}
	            		if (eventType == XmlPullParser.END_TAG) {
	            			if (checkEndName(xpp, txtTag)) {
	            				break;
	            			}
	            		}
	            	}
	            	appendToDArray(temp.toString());
	            }
	        }
	        eventType = xpp.next();
	    }
	}
	
	public void parseXHTML(Document doc) {
		List<Node> tn = doc.childNodes();
		for(Node node : tn) {
	    	processNode(node);
	    }
	}
	
	private void processNode(Node n) {
    	StringBuilder temp = new StringBuilder();
    	List<Node> nl = n.childNodes();
    	String nodeName = n.nodeName();
    	String nodeChildName;
        if (textTagsList.contains(nodeName)) {
        	for (int j = 0; j < nl.size(); j++) {
        		nodeChildName = nl.get(j).nodeName();
        		if (skipTagsList.contains(nodeChildName)) {
        			nl.get(j).remove();
        		}
        	}
        	temp = new StringBuilder(nodeTxt(n));
			if (temp.length() > 0) appendToDArray(temp.toString());
        } else if (listTagsList.contains(nodeName)) {
        	parseListNodes(nl);
        } else if (nodeName.equalsIgnoreCase("div")) {
    	    for (int y = 0; y < nl.size(); y++) {
    	    	nodeChildName = nl.get(y).nodeName();
        		if (skipTagsList.contains(nodeChildName)) {
        			continue;
        		}
        		if (listTagsList.contains(nodeChildName)) {
        			parseListNodes(nl.get(y).childNodes());
        			continue;
        		}
    	    	if (nodeChildName.equalsIgnoreCase("div") || textTagsList.contains(nodeChildName)) {
    	    		if (temp.length() > 0) {
    	    			appendToDArray(temp.toString());
    	    			temp = new StringBuilder();
    	    		}
    	    		processNode(nl.get(y));
    	    	} else {
    	    		String str = nodeTxt(nl.get(y));
    	    		if (!str.isEmpty()) {
    	    			temp.append(str);
    	    		}
    	    	}
    	    }
    		if (temp.length() > 0) {
    			appendToDArray(temp.toString());
    		}
        } else {
    	    for (int i = 0; i < nl.size(); i++) {
    	    	processNode(nl.get(i));
    	    }
        }
	}
	
	private boolean checkStartName(XmlPullParser xpp) {
		return textTagsList.contains(xpp.getName());
	}
	
	private boolean checkSkipName(XmlPullParser xpp) {
		return skipTagsList.contains(xpp.getName());
	}
	
	private boolean checkEndName(XmlPullParser xpp, String str) {
		return xpp.getName().equalsIgnoreCase(str);
	}
	
	private void parseListNodes(List<Node> nl) {
		String nodeName;
		String nodeChildName;
		String temp;
    	for (int k = 0; k < nl.size(); k++) {
    		Node n = nl.get(k);
    		nodeName =  n.nodeName();
    		if (listTagsList.contains(nodeName)) {
    			parseListNodes(n.childNodes());
    		} else if (listItemTagsList.contains(nodeName)) {
    			if (n.childNodeSize() > 0) {
    				List<Node> nl2 = n.childNodes();
    				boolean b = false;
    				for (int l = 0; l < nl2.size(); l++) {
    					nodeChildName = nl2.get(l).nodeName();
    					if (listTagsList.contains(nodeChildName)) {
    						b = true;
    						break;
    					}
    				}
    				if (b) {
    					parseListNodes(nl2);
    				} else {
    		        	temp = nodeTxt(n);
    					if (!temp.isEmpty()) appendToDArray(temp);
    				}
    			} else {
    	        	temp = nodeTxt(n);
    				if (!temp.isEmpty()) appendToDArray(temp);
    			}
    		}
    	}
	}
	
	private void appendToDArray(String temp) {
		temp = temp.replaceAll("\r+|\n+", " ");
        temp = temp.replaceAll("[\t\\xA0\u1680\u180e\u2000-\u200a\u202f\u205f\u3000]", " ");
        temp = temp.replaceAll("\\s\\s+", " ");
        temp = temp.replaceAll("^\\s|\\s$", "");
        if (!temp.isEmpty()) {
        	dataArray.add(temp);
        }
	}
	
	private String nodeTxt(Node n) {
		String temp = "";
		if (n instanceof TextNode) {
			temp =((TextNode) n).text();
		} else if (n instanceof Element) {
			temp =((Element) n).text();
		}
		return temp;
	}

}
