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
 * This class supports exporting ODM study data to files. For each study, there is one FileExporter object.
 * Each FileExporter creates several writers which write to files.
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
     * The name of the study.
     */
    private final String studyName;

    /**
     * The writer for writing the concept map file.
     */
    private BufferedWriter conceptMapWriter;

    /**
     * The writer for writing the columns file.
     */
    private BufferedWriter columnsWriter;

    /**
     * The writer for writing the word map file.
     */
    private BufferedWriter wordMapWriter;

    /**
     * The name of the concept map file.
     */
    private String conceptMapFileName;

    /**
     * The name of the columns file.
     */
    private String columnsFileName;

    /**
     * The name of the word map file.
     */
    private String wordMapFileName;

    /**
     * The name of the clinical data file.
     */
    private String clinicalDataFileName;

    /**
     * The writer for exporting the clinical data file.
     */
    private BufferedWriter clinicalDataWriter;

    /**
     * Whether the line with the concept map headers still has to be written to file.
     */
    private boolean writeConceptMapHeaders;

    /**
     * Whether the column with the subject ID, which should be the first, still has to be skipped.
     */
    private boolean skipSubjectId;

    /**
     * Whether the line with the word map headers still has to be written to file.
     */
    private boolean writeWordMapHeaders;

    /**
     * Whether the line with the clinical data headers still has to be written to file.
     */
    private boolean writeClinicalDataHeaders;

    /**
     * The column headers for the clinical data.
     */
    private List<String> columnHeaders;

    /**
     * The column IDs (paths) for the clinical data.
     */
    private List<String> columnIds;

    /**
     * The current column number during the processing of the study info.
     */
    private int currentColumnNumber;

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
     * @throws IOException when creating the file fails.
     */
    public FileExporter(String exportFilePath, String studyName) throws IOException {
        this.exportFilePath = exportFilePath;
        this.studyName = studyName;
        conceptMapFileName = studyName + "_concept_map.txt";
        columnsFileName = studyName + "_columns.txt";
        wordMapFileName = studyName + "_word_map.txt";
        clinicalDataFileName = studyName + "_clinical_data.txt";
        setConceptMapName(conceptMapFileName);
        setColumnsName(columnsFileName);
        setWordMapName(wordMapFileName);
        setClinicalDataName(clinicalDataFileName);
        writeConceptMapHeaders = true;
        skipSubjectId = true;
        writeWordMapHeaders = true;
        writeClinicalDataHeaders = true;
        currentColumnNumber = 1;
        columnHeaders = new ArrayList<>();
        columnIds = new ArrayList<>();
        patientData = new HashMap<>();
    }

    private void setColumnsName(String columnsFileName) {
        try {
            columnsWriter = new BufferedWriter(new FileWriter(exportFilePath + columnsFileName));
            log.info("Writing columns to file " + exportFilePath + columnsFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setWordMapName(String wordMapFileName) {
        try {
            wordMapWriter = new BufferedWriter(new FileWriter(exportFilePath + wordMapFileName));
            log.info("Writing word mappings to file " + exportFilePath + wordMapFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setConceptMapName(String conceptMapFileName) {
        try {
            conceptMapWriter = new BufferedWriter(new FileWriter(exportFilePath + conceptMapFileName));
            log.info("Writing concept map to file " + exportFilePath + conceptMapFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setClinicalDataName(String clinicalDataFileName) {
        try {
            clinicalDataWriter = new BufferedWriter(new FileWriter(exportFilePath + clinicalDataFileName));
            log.info("Writing clinical data to file " + exportFilePath + clinicalDataFileName);
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
    public void writeExportConceptMap(I2B2StudyInfo studyInfo) {
        if (writeConceptMapHeaders) {
            writeLine(conceptMapWriter, "EDC_path\ttranSMART_path\tvocabulary_term");
            writeConceptMapHeaders = false;
        }

        writeLine(conceptMapWriter, studyInfo.getNamePath() + "+" + studyInfo.getCname() + "\t"
                                  + studyInfo.getNamePath() + "+" + studyInfo.getCname() + "\t");
        columnHeaders.add(studyInfo.getCname());
        columnIds.add(studyInfo.getNamePath());
    }

    /**
     * Write the columns file: first the clinical data file name, then the path as specified in the second
     * column of the user's input concept map without the last node, then the column number and then the
     * last node of the path
     *
     * @param studyInfo the metadata study information
     */
    public void writeExportColumns(I2B2StudyInfo studyInfo) {
        if (currentColumnNumber == 1) {
            writeLine(columnsWriter, "Filename\tCategory Code\tColumn Number\tData Label\tData Label Source\tControl Vocab Cd");
            writeLine(columnsWriter, clinicalDataFileName + "\t\t1\tSUBJ_ID\t\t");
            currentColumnNumber += 1;
        }
        if (currentColumnNumber > 1 && !skipSubjectId) {
            writeLine(columnsWriter, clinicalDataFileName + "\t" + studyInfo.getNamePath() + "\t" +
                    currentColumnNumber + "\t" + studyInfo.getCname() + "\t\t" );
            currentColumnNumber += 1;
        }
        skipSubjectId = false;
    }

    /**
     * Write the word mapping file: first the clinical data file name, then the column number, then the data value,
     * and then the mapped word
     *
     * @param studyInfo the metadata study information
     */
    public void writeExportWordMap(I2B2StudyInfo studyInfo) {
        if (writeWordMapHeaders) {
            writeLine(wordMapWriter, "Filename\tColumn Number\tOriginal Data Value\tNew Data Values");
            writeWordMapHeaders = false;
        }
        writeLine(wordMapWriter, clinicalDataFileName + "\t" + currentColumnNumber + "\t" + "\t" + studyInfo.getCname());
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
            columnsWriter.close();
            wordMapWriter.close();
            conceptMapWriter.close();
            clinicalDataWriter.close();
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
