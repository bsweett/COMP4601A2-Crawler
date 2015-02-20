package edu.carleton.comp4601.assignment2.crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
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
	
	private ConcurrentHashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document> documentMap;
	private ConcurrentHashMap<Integer, ArrayList<String>> imageAltMap;
	private ConcurrentHashMap<Integer, Metadata> metadataMap;

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
		
		documentMap = new ConcurrentHashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document>();
		imageAltMap = new ConcurrentHashMap<Integer, ArrayList<String>>();
		metadataMap = new ConcurrentHashMap<Integer, Metadata>();
	}

	@Override
	public void onBeforeExit() {
		
		// When crawl is complete 
		// 1) Save graph we have been working on
		try {
			String name = this.crawlGraph.getName();
			byte[] bytes = Marshaller.serializeObject(crawlGraph);
			DatabaseManager.getInstance().addNewGraph(name, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// TODO:
		// 2) Save document to DB
		for (Entry<Integer, edu.carleton.comp4601.assignment2.dao.Document> entry : documentMap.entrySet()) {
		    int key = entry.getKey();
		    edu.carleton.comp4601.assignment2.dao.Document value = entry.getValue();
		    
		    // DatabaseManager.getInstance().addNewDocument(myDoc);
		}
		
		// 3) Index with Lucene , image alts, metadata, docs
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

		// Content Type
		if (page.getParseData() instanceof HtmlParseData) {
			parseHTMLToDocument(page, currentTime);

		} else if(page.getParseData() instanceof BinaryParseData) {
			parseBinaryToDocument(page, currentTime);

		}
		
		this.sleepTime = this.sleepTime * 10;
	}


	private void parseBinaryToDocument(Page page, long currentTime) {

		try {
			InputStream inputStream = new ByteArrayInputStream(page.getContentData());
			
			// The metadata should only be used for the lucene document
			Metadata metadata = TikaParsingManager.getInstance().parseUsingAutoDetect(inputStream);
			
			if(metadata != null) {
				edu.carleton.comp4601.assignment2.dao.Document doc = new edu.carleton.comp4601.assignment2.dao.Document();
				
				String name = metadata.get(TikaCoreProperties.TITLE);
				doc.setId(page.getWebURL().getDocid());
				doc.setName(name);
				
				documentMap.put(doc.getId(), doc);
				metadataMap.put(doc.getId(), metadata);
				
				// Graph the page
				buildVertexForPage(page, currentTime);
				
			} else {
				System.err.println("Could not parse metadata using auto detect");
				
			}

		} catch (Exception e) {
			System.err.println("Exception while parsing nonHTML: " + e.getLocalizedMessage());
			
		}
	}

	private void parseHTMLToDocument(Page page, long currentTime) {

		try {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();
			int docId = page.getWebURL().getDocid();

			Document doc = Jsoup.parse(html);
			Elements allImages = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
			Elements allText = doc.select("p,h1,h2,h3,h4,h5");

			// Store document basic values
			edu.carleton.comp4601.assignment2.dao.Document myDoc = new edu.carleton.comp4601.assignment2.dao.Document(docId);
			myDoc.setName(doc.title());
			myDoc.setScore(0);

			// TODO: Tags should be keywords not actual DOM tags
			myDoc.addTag(doc.title());
			
			// Store all document text
			String rawText = "";
			for(Element elem : allText) {
				rawText += (" " + elem.text());
				
			}
			myDoc.setText(rawText);

			// Store all image src and alt
			ArrayList<String> imagealts = new ArrayList<String>();
			for(Element elem : allImages) {
				String imageAlt = elem.attr("alt");
				
				if(!imageAlt.isEmpty())
					imagealts.add(imageAlt);
				
			}
			imageAltMap.put(myDoc.getId(), imagealts);
			
			// Add current page vertex
			PageVertex current = buildVertexForPage(page, currentTime);
			
			// Add links to document and add vertices and edges for links
			List<WebURL> links = htmlParseData.getOutgoingUrls();
			for(WebURL link : links) {
				myDoc.addLink(link.getURL());
				addOutGoingLinkToGraph(link.getURL(), current);
				
			}
			
			documentMap.put(myDoc.getId(), myDoc);

		} catch (Exception e) {
			System.err.println("Exception while parsing HTML: " + e.getLocalizedMessage());

		}
	}
	
	/**
	 * Builds and adds the graph vertex for a given page and returns it.
	 * 
	 * @param page
	 * @param time
	 * @return
	 */
	private PageVertex buildVertexForPage(Page page, long time) {
		String parentUrl = page.getWebURL().getParentUrl();
		int parentId = page.getWebURL().getParentDocid();
		
		String url = page.getWebURL().getURL();
		int docId = page.getWebURL().getDocid();

		PageVertex newPage = new PageVertex(docId, url, time);
		this.crawlGraph.addVertex(newPage);

		if(!parentUrl.isEmpty()) {
			PageVertex parentPage = new PageVertex(parentId, parentUrl, time);
			this.crawlGraph.addVertex(parentPage);
			this.crawlGraph.addEdge(parentPage, newPage);
			
		}
		
		return newPage;
	}
	
	/**
	 * Adds a vertex for a given url if it doesn't exist in the graph. Maps
	 * edges to the vertex and the current pages vertex.
	 * 
	 * @param url
	 * @param currentPage
	 */
	private void addOutGoingLinkToGraph(String url, PageVertex currentPage) {
		PageVertex vertex = this.crawlGraph.findVertex(url);
		
		if(vertex != null) {
			this.crawlGraph.addEdge(currentPage, vertex);
			
		} else {
			Date date = new Date();
			long currentTime = date.getTime();
			PageVertex newVertex = new PageVertex(this.crawlGraph.getIdCounter(), url, currentTime);
			this.crawlGraph.addVertex(newVertex);
			this.crawlGraph.addEdge(currentPage, newVertex);
			
		}
	}
	
}
