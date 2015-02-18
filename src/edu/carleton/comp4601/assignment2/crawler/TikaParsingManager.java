package edu.carleton.comp4601.assignment2.crawler;

import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class TikaParsingManager {

	private static TikaParsingManager instance;
	
	public static void setInstance(TikaParsingManager instance) {
		TikaParsingManager.instance = instance;
	}
	
	public static TikaParsingManager getInstance() {

		if (instance == null)
			instance = new TikaParsingManager();
		return instance;

	}
	
	public Metadata parseUsingAutoDetect(InputStream is) throws Exception {
		Parser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		ContentHandler handler = new BodyContentHandler();
		ParseContext context = new ParseContext();
		
		try {
			parser.parse(is, handler, metadata, context);
		} finally {
			is.close();
		}
		
		return metadata;
	}
	
	
	public Metadata parseMetadataForPNG(InputStream is) throws Exception {
		Metadata metadata = new Metadata();
		metadata.set(Metadata.CONTENT_TYPE, "image/png");
		
		try {
			new ImageParser().parse(is, new DefaultHandler(), metadata, new ParseContext());
		} finally {
			is.close();
		}
		
		return metadata;
	}
	
	public Metadata parseMetadataForPDF(InputStream is) throws Exception {
		Metadata metadata = new Metadata();
		ContentHandler handler = new BodyContentHandler();
		
		try {
			new PDFParser().parse(is, handler, metadata, new ParseContext());
		} finally {
			is.close();
		}
		
		return metadata;
	}
	
	public Metadata parseMetadataForDOC(InputStream is) throws Exception {
		Metadata metadata = new Metadata();
		ContentHandler handler = new BodyContentHandler();
		
		try {
			new OfficeParser().parse(is, handler, metadata, new ParseContext());
		} finally {
			is.close();
		}
		
		return metadata;
	}

}
