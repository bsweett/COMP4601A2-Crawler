package edu.carleton.comp4601.assignment2.crawler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.carleton.comp4601.assignment2.index.CrawlIndexer;
import edu.carleton.comp4601.assignment2.util.FileUtils;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {

	final static Logger logger = LoggerFactory.getLogger(Controller.class);
	
	// Options
	final static String homePath = System.getProperty("user.home");
	final static String crawlStorageFolder = "/data/crawl/root";
	final static String luceneIndexFolder = "/data/lucene/";
	final static int numberOfThreads = 12;
	final static String[] crawlDomains = new String[] { "http://www.carleton.ca" };
	//final static String[] crawlDomains = new String[] { "http://sikaman.dyndns.org:8888/courses/4601/resources/", "http://www.carleton.ca", "http://daydreamdev.com" };
	
	/**
	 * 
	 * @param config
	 * @param domains
	 * @return
	 * @throws Exception
	 */
	public static CrawlController initController(CrawlConfig config, String[] domains) throws Exception {

		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

		 for (String domain : domains) {
		      controller.addSeed(domain);
		}

		return controller;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		logger.info("Web Crawler Controller Started");
		
		FileUtils.createDirectory(homePath + luceneIndexFolder);
		
		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(homePath + crawlStorageFolder);	
		config.setPolitenessDelay(300);
		config.setIncludeBinaryContentInCrawling(true);
		
		config.setMaxPagesToFetch(100); // TODO: Remove limit for submission

		Crawler.configure(crawlDomains);
		CrawlController controller = initController(config, crawlDomains);
		controller.start(Crawler.class, numberOfThreads);
	
		logger.info("Done crawling");
		CrawlIndexer indexer = new CrawlIndexer(homePath + luceneIndexFolder, controller.getCrawlersLocalData());
		
		try {
			indexer.rebuildIndexes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.info("All Done Goodbye!");
		System.exit(0);
	}

}
