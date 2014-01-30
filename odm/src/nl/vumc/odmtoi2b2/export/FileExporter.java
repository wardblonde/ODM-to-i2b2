/**
 * Copyright(c) 2014 VU University Medical Center.
 * Licensed under the Apache License version 2.0 (see http://opensource.org/licenses/Apache-2.0).
 */

package nl.vumc.odmtoi2b2.export;

import com.recomdata.i2b2.entity.I2B2ClinicalDataInfo;
import com.recomdata.i2b2.entity.I2B2StudyInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class supports exporting ODM data to files.
 *
 * @author <a href="mailto:w.blonde@vumc.nl">Ward Blondé</a>
 * @author <a href="mailto:f.debruijn@vumc.nl">Freek de Bruijn</a>
 */
public class FileExporter {
    /**
     * Logger for this class.
     */
    private static final Log log = LogFactory.getLog(FileExporter.class);

    /**
     * The writer for writing the clinical data.
     */
    private final BufferedWriter clinicalDataWriter;
    private boolean writeClinicalDataHeaders;

    private final String exportFilePath;

    /**
     * The writer for writing the concept map.
     */
    private BufferedWriter conceptMapWriter;

    private List<String> columnHeaders;
    private List<String> columnIds;
    private String currentPatientNumber;
    private Map<String, String> patientData;

    /**
     * Construct an export file.
     *
     * @param exportFilePath the path to the directory of the export file.
     * @param exportFileName the name of the export file
     * @throws IOException when creating the file fails.
     */
    public FileExporter(String exportFilePath, String exportFileName) throws IOException {
        String exportFile = exportFilePath + exportFileName;
        clinicalDataWriter = new BufferedWriter(new FileWriter(exportFile));
        writeClinicalDataHeaders = true;
        this.exportFilePath = exportFilePath;
        System.out.println("Writing export data to file " + exportFile);
        System.out.println("Writing logging to file " + exportFilePath + "log.txt");
        columnHeaders = new ArrayList<>();
        columnIds = new ArrayList<>();
        patientData = new HashMap<>();
    }

    /**
     * Write the metadata about study information to a columns file and a word map file.
     *
     * @param studyInfo the metadata study information
     */
    public void writeExportStudyInfo(final I2B2StudyInfo studyInfo) {
        writeExportStudyInfo(studyInfo, false);
    }

    /**
     * Write the metadata about study information to a columns file and a word map file.
     *
     * @param studyInfo the metadata study information
     * @param addToConceptMap whether to add this record to the concept map
     */
    public void writeExportStudyInfo(final I2B2StudyInfo studyInfo, final boolean addToConceptMap) {
        if (addToConceptMap) {
            writeConceptMap(studyInfo);
        }

        final String className = studyInfo.getClass().getName();
        log.info("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        log.info("+ " + studyInfo.getCfullname());
        log.info("+ " + studyInfo.getNamePath());
        log.info("+ " + studyInfo.getCname());
        log.info("+ " + studyInfo.getCbasecode());
        log.info("");
    }

    /**
     * Write the concept mapping in two columns. The first column represents the tree structure as it will
     * appear in i2b2, the second column is the original tree structure from the ODM. Concepts are separated
     * by + symbols.
     *
     * @param studyInfo the metadata study information
     */
    private void writeConceptMap(final I2B2StudyInfo studyInfo) {
        writeLine(conceptMapWriter, studyInfo.getNamePath() + "\t" + studyInfo.getNamePath());
        //writeLine(studyInfo.getCname());
        columnHeaders.add(studyInfo.getCname());
        columnIds.add(studyInfo.getNamePath());
    }

    /**
     * Write the clinical data to a tab-delimited text file.
     *
     * @param clinicalDataInfo the clinical data to be written to the file
     */
    public void writeExportClinicalDataInfo(final I2B2ClinicalDataInfo clinicalDataInfo) {
        final String className = clinicalDataInfo.getClass().getName();
        log.info("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        log.info("+ " + clinicalDataInfo.getPatientNum());
        log.info("+ " + clinicalDataInfo.getConceptCd());
        log.info("+ " + clinicalDataInfo.getTvalChar());
        log.info("");

        if (!clinicalDataInfo.getPatientNum().equals(currentPatientNumber)) {
            writePatientData();
            currentPatientNumber = clinicalDataInfo.getPatientNum();
        }
        patientData.put(clinicalDataInfo.getConceptCd(), clinicalDataInfo.getTvalChar());
    }

    private void writePatientData() {
        if (writeClinicalDataHeaders) {
            final StringBuilder headers = new StringBuilder();
            for (final String columnHeader : columnHeaders) {
                if (headers.length() > 0)
                    headers.append("\t");
                headers.append(columnHeader);
            }
            writeLine(clinicalDataWriter, headers.toString());
            writeClinicalDataHeaders = false;
        }

        if (!patientData.isEmpty()) {
            StringBuilder line = new StringBuilder();
            for (final String columnId : columnIds) {
                if (line.length() > 0)
                    line.append("\t");
                if (patientData.containsKey(columnId))
                    line.append(patientData.get(columnId));
            }
            writeLine(clinicalDataWriter, line.toString());
            patientData.clear();
        }
    }

    /**
     * Write a line to the export file.
     *
     * @param writer the writer to write to.
     * @param line the line to be written.
     */
    private void writeLine(final BufferedWriter writer, final String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the export files.
     */
    public void close() {
        try {
            writePatientData();
            clinicalDataWriter.close();
            conceptMapWriter.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * For debugging: write all the data fields that would be written to the database in text-form.
     *
     * @param dataObject the data object that is prepared for loading to the database
     */
    @SuppressWarnings("UnusedDeclaration")
    public void writeExportDataObject(final Object dataObject) {
        final String className = dataObject.getClass().getName();
        log.info("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        try {
            for (final Field field : dataObject.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                log.info("- " + field.getName() + ": " + field.get(dataObject));
            }
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        log.info("");
    }

    public void setConceptMapName(final String conceptMapFileName) {
        try {
            conceptMapWriter = new BufferedWriter(new FileWriter(exportFilePath + conceptMapFileName));
            System.out.println("Writing concept map to file " + exportFilePath + conceptMapFileName);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
