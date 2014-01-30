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
     * The directory where the export files will be written to.
     */
    private final String exportFilePath;

    /**
     * The writer for exporting the clinical data.
     */
    private final BufferedWriter clinicalDataWriter;

    /**
     * Whether the line with the clinical data headers still has to be written to file.
     */
    private boolean writeClinicalDataHeaders;

    /**
     * The writer for writing the concept map.
     */
    private BufferedWriter conceptMapWriter;

    /**
     * The column headers for the clinical data.
     */
    private List<String> columnHeaders;

    /**
     * The column IDs (paths) for the clinical data.
     */
    private List<String> columnIds;

    /**
     * The patient number that clinical data info records are being processed for. All data for a patient is collected
     * and written on one line.
     */
    private String currentPatientNumber;

    /**
     * Mapping of column ID to values for the current patient.
     */
    private Map<String, String> patientData;

    /**
     * Construct a file exporter.
     *
     * @param exportFilePath the directory for the export files.
     * @param exportFileName the name of the export file.
     * @throws IOException when creating the file fails.
     */
    public FileExporter(String exportFilePath, String exportFileName) throws IOException {
        this.exportFilePath = exportFilePath;
        String exportFile = exportFilePath + exportFileName;
        clinicalDataWriter = new BufferedWriter(new FileWriter(exportFile));
        writeClinicalDataHeaders = true;
        log.info("Writing export data to file " + exportFile);
        columnHeaders = new ArrayList<>();
        columnIds = new ArrayList<>();
        patientData = new HashMap<>();
    }

    /**
     * Write the metadata about study information to a columns file and a word map file.
     *
     * @param studyInfo the metadata study information
     */
    public void writeExportStudyInfo(I2B2StudyInfo studyInfo) {
        writeExportStudyInfo(studyInfo, false);
    }

    /**
     * Write the metadata about study information to a columns file and a word map file.
     *
     * @param studyInfo the metadata study information
     * @param addToConceptMap whether to add this record to the concept map
     */
    public void writeExportStudyInfo(I2B2StudyInfo studyInfo, boolean addToConceptMap) {
        if (addToConceptMap) {
            writeConceptMap(studyInfo);
        }

        String className = studyInfo.getClass().getName();
        log.info("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        log.info("+ " + studyInfo.getCfullname());
        log.info("+ " + studyInfo.getNamePath());
        log.info("+ " + studyInfo.getCname());
        log.info("+ " + studyInfo.getCbasecode());
        log.info("");
    }

    public void setConceptMapName(String conceptMapFileName) {
        try {
            conceptMapWriter = new BufferedWriter(new FileWriter(exportFilePath + conceptMapFileName));
            log.info("Writing concept map to file " + exportFilePath + conceptMapFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the concept mapping in two columns. The first column represents the tree structure as it will
     * appear in i2b2, the second column is the original tree structure from the ODM. Concepts are separated
     * by + symbols.
     *
     * @param studyInfo the metadata study information
     */
    private void writeConceptMap(I2B2StudyInfo studyInfo) {
        writeLine(conceptMapWriter, studyInfo.getNamePath() + "\t" + studyInfo.getNamePath());
        columnHeaders.add(studyInfo.getCname());
        columnIds.add(studyInfo.getNamePath());
    }

    /**
     * Write the clinical data to a tab-delimited text file.
     *
     * @param clinicalDataInfo the clinical data to be written to the file
     */
    public void writeExportClinicalDataInfo(I2B2ClinicalDataInfo clinicalDataInfo) {
        if (!clinicalDataInfo.getPatientNum().equals(currentPatientNumber)) {
            writePatientData();
            currentPatientNumber = clinicalDataInfo.getPatientNum();
        }
        patientData.put(clinicalDataInfo.getConceptCd(), clinicalDataInfo.getTvalChar());

        String className = clinicalDataInfo.getClass().getName();
        log.info("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        log.info("+ " + clinicalDataInfo.getPatientNum());
        log.info("+ " + clinicalDataInfo.getConceptCd());
        log.info("+ " + clinicalDataInfo.getTvalChar());
        log.info("");
    }

    private void writePatientData() {
        if (writeClinicalDataHeaders) {
            StringBuilder headers = new StringBuilder();
            for (String columnHeader : columnHeaders) {
                if (headers.length() > 0)
                    headers.append("\t");
                headers.append(columnHeader);
            }
            writeLine(clinicalDataWriter, headers.toString());
            writeClinicalDataHeaders = false;
        }

        if (!patientData.isEmpty()) {
            StringBuilder line = new StringBuilder();
            for (String columnId : columnIds) {
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
    private void writeLine(BufferedWriter writer, String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * For debugging: write all the data fields that would be written to the database in text-form.
     *
     * @param dataObject the data object that is prepared for loading to the database
     */
    @SuppressWarnings("UnusedDeclaration")
    public void writeExportDataObject(Object dataObject) {
        String className = dataObject.getClass().getName();
        log.info("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        try {
            for (Field field : dataObject.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                log.info("- " + field.getName() + ": " + field.get(dataObject));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        log.info("");
    }
}
