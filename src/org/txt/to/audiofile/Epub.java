package org.txt.to.audiofile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lib.textdata.array.XMLParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

public class Epub extends XMLParser {
	
	public Epub(String epub_path) {
		
		super(Arrays.asList("p", "h1", "h2", "h3", "h4", "h5", "h6")
				, Arrays.asList("img", "del", "video", "audio")
				, Arrays.asList("ol", "ul", "dl")
				, Arrays.asList("li", "dd", "dt"));
		
		List<MediaType> types = new ArrayList<MediaType>();
		types.add(MediatypeService.XHTML);
		Book book;
		try {
			book = (new EpubReader()).readEpubLazy(epub_path, Constants.CHARACTER_ENCODING, types);
			Spine s = book.getSpine();
			for (int i = 0; i < s.size(); i++) {
				Resource res = s.getResource(i);
				if (res.getMediaType() == MediatypeService.XHTML) {
					String str = new String(res.getData(), Constants.CHARACTER_ENCODING);
					Document doc = Jsoup.parse(str);
					parseXHTML(doc);
				}
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> getParsedText() {
		return super.dataArray;
	}
}

