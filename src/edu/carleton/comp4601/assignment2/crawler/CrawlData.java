package edu.carleton.comp4601.assignment2.crawler;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tika.metadata.Metadata;

public class CrawlData {

	private ConcurrentHashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document> documentMap;
	private ConcurrentHashMap<Integer, ArrayList<String>> imageAltMap;
	private ConcurrentHashMap<Integer, Metadata> metadataMap;
	private ConcurrentHashMap<Integer, String> urlMap;
	
	public CrawlData() {
		setDocumentMap(new ConcurrentHashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document>());
		setImageAltMap(new ConcurrentHashMap<Integer, ArrayList<String>>());
		setMetadataMap(new ConcurrentHashMap<Integer, Metadata>());
		setUrlMap(new ConcurrentHashMap<Integer, String>());
	}

	public synchronized ConcurrentHashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document> getDocumentMap() {
		return documentMap;
	}

	public synchronized void setDocumentMap(ConcurrentHashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document> documentMap) {
		this.documentMap = documentMap;
	}
	
	public synchronized void addVisitedDocument(int docId, edu.carleton.comp4601.assignment2.dao.Document doc) {
		this.documentMap.put(docId, doc);
	}

	public synchronized ConcurrentHashMap<Integer, ArrayList<String>> getImageAltMap() {
		return imageAltMap;
	}

	public synchronized void setImageAltMap(ConcurrentHashMap<Integer, ArrayList<String>> imageAltMap) {
		this.imageAltMap = imageAltMap;
	}
	
	public synchronized void addVisitedImageAltList(int docId, ArrayList<String> alts) {
		this.imageAltMap.put(docId, alts);
	}

	public synchronized ConcurrentHashMap<Integer, Metadata> getMetadataMap() {
		return metadataMap;
	}

	public synchronized void setMetadataMap(ConcurrentHashMap<Integer, Metadata> metadataMap) {
		this.metadataMap = metadataMap;
	}
	
	public synchronized void addVisitedMetadata(int docId, Metadata data) {
		this.metadataMap.put(docId, data);
	}

	public synchronized ConcurrentHashMap<Integer, String> getUrlMap() {
		return urlMap;
	}

	public synchronized void setUrlMap(ConcurrentHashMap<Integer, String> urlMap) {
		this.urlMap = urlMap;
	}
	
	public synchronized void addVisitedUrl(int docId, String url) {
		this.urlMap.put(docId, url);
	}
	
	
}
