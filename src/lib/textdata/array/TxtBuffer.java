package lib.textdata.array;

public class TxtBuffer {
	
	public final static int MAX_LENGTH = 2000;
	private StringBuilder buffer;
	private String temp;
	private TxtDataArray td_array;
	private FB2DataArray fd_array;
	private int startItem;
	private int currentItem;
	private int maxItem;
	private boolean fb2;
	
	public TxtBuffer (TxtDataArray t) {
		buffer = new StringBuilder("");
		currentItem = 0;
		startItem = currentItem;
		td_array = t;
		maxItem = td_array.dataSize(); 
	}
	
	public TxtBuffer (TxtDataArray t, int startFrom) {
		buffer = new StringBuilder("");
		td_array = t;
		maxItem = td_array.dataSize();
		
		if (startFrom > 0 && startFrom < maxItem)
			currentItem = startFrom - 1;
		else
			currentItem = 0;
		
		startItem = currentItem;
	}
	
	public TxtBuffer (TxtDataArray t, int startFrom, int endWith) {
		buffer = new StringBuilder("");
		td_array = t;
		
		if (endWith > 0 && endWith <= td_array.dataSize())
			maxItem = endWith;
		else
			maxItem = td_array.dataSize();
		
		if (startFrom > 0 && startFrom < maxItem)
			currentItem = startFrom - 1;
		else
			currentItem = 0;
		
		startItem = currentItem;
	}
	
	public TxtBuffer (FB2DataArray f) {
		buffer = new StringBuilder("");
		currentItem = 0;
		startItem = currentItem;
		fd_array = f;
		maxItem = fd_array.dataSize(); 
		fb2 = true;
	}
	
	public TxtBuffer (FB2DataArray f, int startFrom) {
		buffer = new StringBuilder("");
		fd_array = f;
		maxItem = fd_array.dataSize();
		
		if (startFrom > 0 && startFrom < maxItem)
			currentItem = startFrom - 1;
		else
			currentItem = 0;
		
		startItem = currentItem;
		fb2 = true;
	}
	
	public TxtBuffer (FB2DataArray f, int startFrom, int endWith) {
		buffer = new StringBuilder("");
		fd_array = f;
		
		if (endWith > 0 && endWith <= fd_array.dataSize())
			maxItem = endWith;
		else
			maxItem = fd_array.dataSize();
		
		if (startFrom > 0 && startFrom < maxItem)
			currentItem = startFrom - 1;
		else
			currentItem = 0;
		
		startItem = currentItem;
		fb2 = true;
	}
	
	public void stopRegexFilter() {
		if (fb2)
			fd_array.stopRules();
		else
			td_array.stopRules();
	}
	
	private void setBuffer() {
		if (maxItem > 0) {
			while (buffer.length() < MAX_LENGTH && currentItem < maxItem) {
				readData();
				if (buffer.length() != 0)
					buffer.append('\n');
				buffer.append(temp);
			}
		}
	}
	
	public String getBuffer() {
		setBuffer();
		if (currentItem != maxItem)
			return processBuffer();
		else
			if (buffer.length() != 0) {
				if (buffer.length() <= MAX_LENGTH) {
					String s = buffer.toString();
					buffer = new StringBuilder("");
					return s;
				} else {
					return processBuffer();
				}
			} else {
				return null;
			}
	}
	
	private void readData() {
		if (fb2)
			temp = fd_array.getDataString(currentItem);
		else
			temp = td_array.getDataString(currentItem);
		currentItem++;
	}
	
	private String processBuffer() {
		String str;
		if (stopOffset() != 0) {
			str = buffer.substring(0, stopOffset());
			buffer.delete(0, stopOffset());
		} else if (spaceAndComaOffset() != 0) {
			str = buffer.substring(0, spaceAndComaOffset());
			buffer.delete(0, spaceAndComaOffset());
		} else {
			str = buffer.substring(0, MAX_LENGTH - 1);
			buffer.delete(0, MAX_LENGTH - 1);
		}
		return str;
	}
	
	private int stopOffset() {
		int x = MAX_LENGTH - 1;
		for (int i = MAX_LENGTH - 1; i >= 0   && buffer.charAt(i) != '\n' && buffer.charAt(i) != '.'
									&&	buffer.charAt(i) != '!' &&	buffer.charAt(i) != '?'
									&&	buffer.charAt(i) != ';'; i--) {
			x = i;
		}
		return x;
	}
	
	private int spaceAndComaOffset() {
		int x = MAX_LENGTH - 1;
		for (int i = MAX_LENGTH - 1; i >= 0 && buffer.charAt(i) != ',' &&	buffer.charAt(i) != ' '; i--) {
			x = i;
		}
		return x;
	}
	
	public float checkProgress() {
		float x = ((float)(currentItem - startItem)) / ((float)(maxItem - startItem));
		return x;
	}
	
	public void clearState() {
		currentItem = startItem;
		temp = "";
		buffer = new StringBuilder("");
	}
}
