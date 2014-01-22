package com.recomdata.i2b2;

/**
 * Copyright(c)  2011-2012 Recombinant Data Corp., All rights Reserved
 * This is a handler's command-line client that can loading other source ODM XML files
 * @author: Alex Wu
 * @date: October 28, 2011
 */

import java.io.File;
import java.io.FileNotFoundException;

import org.cdisk.odm.jaxb.ODM;

import com.recomdata.config.Config;
import com.recomdata.i2b2.dao.I2B2DBUtils;
import com.recomdata.odm.ODMLoader;

/**
 * This class will be used by both command-line and web app to load ODM files
 * from different sources.
 * 
 * @author awu
 * 
 */
public class I2B2ODMStudyHandlerCMLClient {
	/**
	 * Whether the export should go to the database (true) or to a file (false).
	 */
	public static final boolean EXPORT_TO_DATABASE = false;

	/**
	 * File path used for exporting to file (if EXPORT_TO_DATABASE is false).
	 */
	public static final String EXPORT_FILE_PATH = "C:\\Ward\\2014\\workspace\\ODM-to-i2b2\\odm-to-i2b2.txt";

	/**
	 * method to process odm xml file and save data into i2b2
	 * 
	 * @param odmXmlPath
	 * @throws Exception
	 */
	public void loadODMFile2I2B2(String odmXmlPath) throws Exception {
		File xml = new File(odmXmlPath);

		if (!xml.exists()) {
			throw new FileNotFoundException(xml.getPath());
		}

		// Load and parse ODM xml here by jaxb
		ODMLoader odmLoader = new ODMLoader();
		ODM odm = odmLoader.unmarshall(xml);

		if (odm == null || odm.getStudy() == null || odm.getStudy().size() == 0) {
			// TODO: Define more specific exception
			throw new Exception("No study definitions were found in ODM file");
		}

		 // parse ODM XML and save as i2b2 metadata and demodata records
		I2B2ODMStudyHandler odmHandler = new I2B2ODMStudyHandler(odm, EXPORT_TO_DATABASE);
		odmHandler.processODM();

		if (odmHandler.exportedToFile()) {
			odmHandler.closeExportWriter();
		}
	}

	/**
	 * main method for command-line user
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				System.out.println("You must provide the path of an ODM file to process.");
				return;
			}

			String odmFilename = args[0];

			if (EXPORT_TO_DATABASE) {
				System.out.println("Initializing database connection...");
				Config config = Config.getConfig();
				I2B2DBUtils.init(config);
			}

			System.out.println("Loading ODM file " + odmFilename + " to i2b2...");

			I2B2ODMStudyHandlerCMLClient client = new I2B2ODMStudyHandlerCMLClient();
			client.loadODMFile2I2B2(odmFilename);

			if (EXPORT_TO_DATABASE) {
				System.out.println("Releasing database connection...");
				I2B2DBUtils.shutdown();
			}

			System.out.println("Processing complete.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
