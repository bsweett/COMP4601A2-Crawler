package edu.carleton.comp4601.assignment2.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.carleton.comp4601.assignment2.crawler.CrawlData;

public class CrawlIndexer {

	final static Logger logger = LoggerFactory.getLogger(CrawlIndexer.class);
	
	private long count;
	private String dirPath;
	
	private HashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document> documentMap;
	private HashMap<Integer, ArrayList<String>> imageAltMap;
	private HashMap<Integer, Metadata> metadataMap;
	private HashMap<Integer, String> urlMap;
	
	/** Creates a new instance of Indexer */
	public CrawlIndexer(String dirPath, List<Object> localDataList) {
		this.count = 0;
		this.dirPath = dirPath;
		
		imageAltMap = new HashMap<Integer, ArrayList<String>>();
		metadataMap = new HashMap<Integer, Metadata>();
		urlMap = new HashMap<Integer, String>();
		documentMap = new HashMap<Integer, edu.carleton.comp4601.assignment2.dao.Document>();
		
		logger.info("Pooling crawler data");
		for (Object localData : localDataList) {
			CrawlData data = (CrawlData) localData;
			
			imageAltMap.putAll(data.getImageAltMap());
			metadataMap.putAll(data.getMetadataMap());
			urlMap.putAll(data.getUrlMap());
			documentMap.putAll(data.getDocumentMap());
		}
		
		//this.documentMap = DatabaseManager.getInstance().getDocumentsAsMap();
	}

	private IndexWriter indexWriter = null;

	/**
	 * 
	 * @param create
	 * @return
	 * @throws IOException
	 */
	private IndexWriter getIndexWriter(boolean create) throws IOException {
		if (indexWriter == null) {
			Directory indexDir = FSDirectory.open(new File( this.dirPath + "index-directory" ));
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, new StandardAnalyzer());
			indexWriter = new IndexWriter(indexDir, config);
		}
		return indexWriter;
	}    

	/**
	 * 
	 * @throws IOException
	 */
	private void closeIndexWriter() throws IOException {
		if (indexWriter != null) {
			indexWriter.close();
		}
	}

	/**
	 * 
	 * @param myDocument
	 * @param imageAlts
	 * @param data
	 * @throws IOException
	 */
	private void indexHTMLDocument(edu.carleton.comp4601.assignment2.dao.Document myDocument, ArrayList<String> imageAlts, Metadata data, String url) throws IOException {
		
		logger.info("Indexing Document: " + this.count);
		IndexWriter writer = getIndexWriter(false);
		Document doc = new Document();
		
		doc.add(new TextField("docId", myDocument.getId().toString(), Field.Store.YES));
		
		String name = myDocument.getName();
		String text = myDocument.getText();
		
		if(name != null) {
			doc.add(new StringField("docName", myDocument.getName(), Field.Store.YES));
		}
		
		if(text != null) {
			doc.add(new StringField("docText", myDocument.getText(), Field.Store.YES));
		}
		
		for (String tag : myDocument.getTags()) {
			doc.add(new StringField("docTag", tag, Field.Store.YES));
		}
		
		for (String link : myDocument.getLinks()) {
			doc.add(new StringField("docLink", link, Field.Store.YES));
		}
		
		if(imageAlts != null && !imageAlts.isEmpty()) {
			
			for(String alt : imageAlts) {
				doc.add(new StringField("docAlt", alt, Field.Store.YES));
			}
			
		}
		
		if(url != null) {
			doc.add(new StringField("url", url, Field.Store.YES));
		}
		
		Date date = new Date();
		doc.add(new LongField("date", date.getTime(), Field.Store.YES));
		
		if(data != null) {
			String type = data.get(Metadata.CONTENT_TYPE);
			String title = data.get(TikaCoreProperties.TITLE);
			String author = data.get(TikaCoreProperties.CREATOR);
			
			if(type != null) {
				doc.add(new StringField("mimeType", type, Field.Store.YES));
			} else {
				doc.add(new StringField("mimeType", "UNKNOWN", Field.Store.YES));
			}
				
			if(title != null)
				doc.add(new StringField("metaName", title, Field.Store.YES));
			if(author != null)
				doc.add(new StringField("metaAuthor", author, Field.Store.YES));
			
		} else {
			doc.add(new StringField("mimeType", "text/html", Field.Store.YES));
			
		}
		
		String contents = myDocument.getName() + " " + myDocument.getText();
		doc.add(new TextField("contents", contents, Field.Store.YES));	
		doc.add(new TextField("i", "ben", Field.Store.YES));	
		
		writer.addDocument(doc);
		this.count++;
	}   

	/**
	 * 
	 * @param documentMap
	 * @param imageAltMap
	 * @param metadataMap
	 * @throws IOException
	 */
	public void rebuildIndexes() throws IOException {
		
		logger.info("Total Documents with Image Alts: " + this.imageAltMap.size());
		logger.info("Total Documents with Metadata: " + this.metadataMap.size());
		logger.info("Indexing Documents with Alts and Metadata ...");
		
		// Erase existing index
		getIndexWriter(true);
		
		// Index all entries
		for (Entry<Integer, edu.carleton.comp4601.assignment2.dao.Document> entry : this.documentMap.entrySet()) {
		    int key = entry.getKey();
		    edu.carleton.comp4601.assignment2.dao.Document value = entry.getValue();
		    indexHTMLDocument(value, this.imageAltMap.get(key), this.metadataMap.get(key), this.urlMap.get(key));
		}
		logger.info("Done ...");
		
		// Close the index writer when done
		closeIndexWriter();
	}    
	
}
