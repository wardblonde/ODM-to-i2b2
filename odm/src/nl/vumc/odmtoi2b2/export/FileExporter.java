/**
 * Copyright(c) 2014 VU University Medical Center.
 * Licensed under the Apache License version 2.0 (see http://opensource.org/licenses/Apache-2.0).
 */

package nl.vumc.odmtoi2b2.export;

import com.recomdata.i2b2.entity.I2B2ClinicalDataInfo;
import com.recomdata.i2b2.entity.I2B2StudyInfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * This class supports exporting ODM data to files.
 *
 * @author <a href="mailto:w.blonde@vumc.nl">Ward Blondé</a>
 * @author <a href="mailto:f.debruijn@vumc.nl">Freek de Bruijn</a>
 */
public class FileExporter {
    /**
     * The writer for exporting to file.
     */
    private final BufferedWriter exportWriter;

    /**
     * Construct an export file.
     *
     * @param exportFilePath the path to the export file.
     * @throws IOException when creating the file fails.
     */
    public FileExporter(String exportFilePath, String exportFileName) throws IOException  {
        String exportFile = exportFilePath + exportFileName;
        exportWriter = new BufferedWriter(new FileWriter(exportFile));
        System.out.println("Writing export data to file " + exportFile);
    }

    /**
     * Write all the data fields that would be written to the database in text-form.
     *
     * @param dataObject the data object that is prepared for loading to the database
     */
    @SuppressWarnings("UnusedDeclaration")
    public void writeExportDataObject(final Object dataObject) {
        final String className = dataObject.getClass().getName();
        writeExportLine("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        try {
            for (final Field field : dataObject.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                writeExportLine("- " + field.getName() + ": " + field.get(dataObject));
            }
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        writeExportLine("");
    }

    /**
     * Write the metadata about study information to a columns file and a word map file.
     *
     * @param studyInfo the metadata study information
     */
    public void writeExportStudyInfo(final I2B2StudyInfo studyInfo) {
        final String className = studyInfo.getClass().getName();
        writeExportLine("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        writeExportLine("+ " + studyInfo.getCfullname());
        writeExportLine("+ " + studyInfo.getNamePath());
        writeExportLine("+ " + studyInfo.getCname());
        writeExportLine("+ " + studyInfo.getCbasecode());
        writeExportLine("");
    }

    /**
     * Write the clinical data to a tab-delimited text file.
     *
     * @param clinicalDataInfo the clinical data to be written to the file
     */
    public void writeExportClinicalDataInfo(final I2B2ClinicalDataInfo clinicalDataInfo) {
        final String className = clinicalDataInfo.getClass().getName();
        writeExportLine("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        writeExportLine("+ " + clinicalDataInfo.getPatientNum());
        writeExportLine("+ " + clinicalDataInfo.getConceptCd());
        writeExportLine("+ " + clinicalDataInfo.getTvalChar());
        writeExportLine("");
    }

    /**
     * Write a line to the export file.
     *
     * @param line the line to be written
     */
    private void writeExportLine(final String line) {
        try {
            exportWriter.write(line);
            exportWriter.newLine();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the export file.
     */
    public void closeExportWriter() {
        try {
            exportWriter.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
