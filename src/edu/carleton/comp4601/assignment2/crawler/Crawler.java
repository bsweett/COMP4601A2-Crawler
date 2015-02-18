package edu.carleton.comp4601.assignment2.crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.carleton.comp4601.assignment2.graphing.*;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.carleton.comp4601.assignment2.database.DatabaseManager;

public class Crawler extends WebCrawler {

	private Grapher crawlGraph;
	private static String[] domains;
	private long sleepTime;

	private static final Pattern filters = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v"
			+ "|rm|smil|wmv|swf|wma|zip|rar|gz|bmp))$");

	private static final Pattern allowedPatterns = Pattern.compile(".*(\\.(pdf|png|gif|jpe?g|xls|xlsx|ppt|pptx|doc|docx?))$");

	public static void configure(String[] domain) {
		domains = domain;
	}

	@Override
	public void onStart() {
		String graphName = "graph";
		this.crawlGraph = new Grapher(graphName);
		this.sleepTime = 0;
	}

	@Override
	public void onBeforeExit() {
		
		// When crawl is complete 
		// 1) Save graph we have been working on
		// 2) Index everything
		try {
			String name = this.crawlGraph.getName();
			byte[] bytes = Marshaller.serializeObject(crawlGraph);
			DatabaseManager.getInstance().addNewGraph(name, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	/**
	 * You should implement this function to specify whether
	 * the given url should be crawled or not (based on your
	 * crawling logic).
	 */
	@Override
	public boolean shouldVisit(WebURL url) {
		String href = url.getURL().toLowerCase();
		if (filters.matcher(href).matches()) {
			return false;
		}

		if (allowedPatterns.matcher(href).matches()) {
			return true;
		}

		for (String domain : domains) {
			if (href.startsWith(domain)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * This function is called when a page is fetched and ready 
	 * to be processed by your program.
	 */
	@Override
	public void visit(Page page) {    
		
		try {
			Thread.sleep(this.sleepTime);
			
		} catch (InterruptedException e) {
			System.err.println("-- Sleep Interrupted");
			
		}
		
		Date date = new Date();
		long currentTime = date.getTime();

		String url = page.getWebURL().getURL();
		int docId = page.getWebURL().getDocid();

		System.out.println("URL: " + url);

		Tika tika = new Tika();
		MediaType mediaType = null;

		try {
			mediaType = MediaType.parse(tika.detect(new URL(url)));
			
		} catch (IOException e) {
			System.err.println("Exception while getting mime type: " + e.getLocalizedMessage());
			
		}

		//Initial Vertex and Edge
		String parentUrl = page.getWebURL().getParentUrl();
		int parentId = page.getWebURL().getParentDocid();

		PageVertex newPage = new PageVertex(docId, url, currentTime);
		this.crawlGraph.addVertex(newPage);

		if(!parentUrl.isEmpty()) {
			PageVertex parentPage = new PageVertex(parentId, parentUrl, currentTime);
			this.crawlGraph.addVertex(parentPage);
			this.crawlGraph.addEdge(parentPage, newPage);
			
		}

		// Content Type
		if (page.getParseData() instanceof HtmlParseData) {
			parseHTMLToDocument(page, url, currentTime);

		} else if(page.getParseData() instanceof BinaryParseData && mediaType != null) {
			parseBinaryToDocument(page, mediaType, url, currentTime);

		}
		
		this.sleepTime = this.sleepTime * 10;
	}


	// TODO: OtherDocument -> Document (what do i store where)?
	private boolean parseBinaryToDocument(Page page, MediaType mediaType, String url, long currentTime) {

		try {
			String type = mediaType.getSubtype();
			InputStream inputStream = new ByteArrayInputStream(page.getContentData());
			Metadata metadata = null;
			edu.carleton.comp4601.assignment2.dao.Document doc = null;

			// Not the same kind of document as a page
			// The metadata should only be used for the lucene document
			metadata = TikaParsingManager.getInstance().parseUsingAutoDetect(inputStream);
			//doc = buildMimeDocFromMetadata(metadata, page.getWebURL().getDocid(), url, currentTime);

		} catch (Exception e) {
			System.err.println("Exception while parsing nonHTML: " + e.getLocalizedMessage());
		}

		return false;
	}

	// TODO: Cleanup.. Soup stuff is probably needs by image's as well
	private boolean parseHTMLToDocument(Page page, String url, long currentTime) {

		try {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			//String text = htmlParseData.getText();
			String html = htmlParseData.getHtml();
			int docId = page.getWebURL().getDocid();
			//List<WebURL> links = htmlParseData.getOutgoingUrls();

			Document doc = Jsoup.parse(html);
			Elements jsoupLinks = doc.select("a[href]");
			//Elements allImages = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
			Elements allText = doc.select("p,h1,h2,h3,h4,h5");

			// Store document basic values
			//Document myDoc = new Document(docId);
			edu.carleton.comp4601.assignment2.dao.Document myDoc = new edu.carleton.comp4601.assignment2.dao.Document(docId);
			myDoc.setName(doc.title());
			//myDoc.setUrl(url);
			myDoc.setScore(0);
			//myDoc.setTime(currentTime);

			// Store all tag names
			for(Element elem : doc.getAllElements()) {
				String tag = elem.tagName();

				if(!tag.isEmpty())
					myDoc.addTag(tag);
			}

			// Store all links
			for(Element elem : jsoupLinks) {
				String linkHref = elem.attr("href");

				if(!linkHref.isEmpty())
					myDoc.addLink(linkHref);
			}

			/*
			// Store all image src and alt
			for(Element elem : allImages) {
				String imageSrc = elem.attr("src");
				String imageAlt = elem.attr("alt");


				if(!imageSrc.isEmpty())
					myDoc.addImage(imageSrc);

				if(!imageAlt.isEmpty())
					myDoc.addImage(imageAlt);
			}*/

			// Store all document text
			String rawText = "";
			for(Element elem : allText) {
				rawText += (" " + elem.text());
			}
			myDoc.setText(rawText);

			DatabaseManager.getInstance().addNewDocument(myDoc);

			return true;
		} catch (Exception e) {
			System.err.println("Exception while parsing HTML: " + e.getLocalizedMessage());
			return false;
		}
	}


}
