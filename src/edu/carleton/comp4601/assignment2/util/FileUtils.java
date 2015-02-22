package edu.carleton.comp4601.assignment2.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * FileUtils contains a collection of functions for modifying files
 * on the OS's file system.
 *
 * @author Ben Sweett & Colin Tinney
 * @version 1.0
 * @since 2014-09-29
 */
public class FileUtils {
	
	private static Logger LOGGER = Logger.getLogger(FileUtils.class.getName());
	
    /**
     * Returns a list of files/folders within a directory.
     * 
     * @param directoryName The root directory to list contents of.
     * @param extensions A list of valid extensions. If not null/empty, only files with an extension contained in this list will be added.
     * @param includeFolders If true, folders are included in the list.
     * @return A list of files/folders contained within the directory.
     */
    public static List<File> listFiles(String directoryName, String[] extensions, Boolean includeFolders) {
    	
    	List<File> results = new ArrayList<File>();    	
    	
        File folder = new File(directoryName);
        
        File[] fList = folder.listFiles();
        if (fList == null) {
        	LOGGER.warning("Cannot list files of a directory which does not exit - " + directoryName);
        	return results;
        }
        
        for (File file : fList) {
        	
        	if ((includeFolders && file.isDirectory()) || extensions == null) {
    			results.add(file);
    			continue;
        	}
    		
    		if (validExtension(file, extensions)) {
    			results.add(file);
    		}
        	
        }
        
        return results;
    }
     
    /**
     * Attempts to delete a file at a given directory with a 
     * given name
     * 
     * @param String directory
     * @param String fileName
     * @return boolean
     */
    public static boolean deleteFile(String directory, String fileName) {
    	
    	Boolean success = false;
    	
		try {
			
	    	Path path = Paths.get(directory + fileName);
			success = Files.deleteIfExists(path);
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (InvalidPathException e) {
			e.printStackTrace();
			
		}
    	
    	if (!success) {
    		LOGGER.warning("File could not be deleted because it does not exist.");
    	}
    	
    	return success;
    }
    
    /**
     * Attempts to create a directory at the given path
     * 
     * @param String path
     * @return boolean
     */
    public static boolean createDirectory(String path) {

    	boolean success = false;
    	
    	File dir = new File(path);
    	if (!dir.exists()) {
    		 
    		try {
    			success = dir.mkdir();
    			
    		} catch (Exception e) {
    			e.printStackTrace();
    			
    		}
    		 
    	} else {
    		success = true;
    		
    	}
    	
    	return success;
    }
    
    /**
     * Returns true if a file ends with an extension contained in the specified list of extensions.
     * 
     * @param File file to check
     * @param String[] extensions array
     * @return boolean true if valid false if not
     */
    private static boolean validExtension(File file, String[] extensions) {
    	
    	boolean valid = false;
    	
    	for (String extension : extensions) {
			
			if (file.getName().endsWith(extension) || file.getName().endsWith("." + extension)) {
				valid = true;
				break;
			}
			
		}
    	
    	return valid;
    }

}
