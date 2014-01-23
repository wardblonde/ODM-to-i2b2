package com.recomdata.i2b2.dao;

import com.recomdata.i2b2.entity.I2B2ClinicalDataInfo;
import com.recomdata.i2b2.entity.I2B2StudyInfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA.
 * User: PA-NB101
 * Date: 23-1-14
 * Time: 11:07
 * To change this template use File | Settings | File Templates.
 */
public class ExportFile {

    // TODO: testing other export format.
    public final BufferedWriter exportWriter;


    public ExportFile (String exportFilePath) throws IOException  {
        exportWriter = new BufferedWriter(new FileWriter(exportFilePath));
        System.out.println("Writing export data to file " + exportFilePath);

    }


    // TODO: testing other export format.
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

    // TODO: testing other export format.
    public void writeExportStudyInfo(final I2B2StudyInfo studyInfo) {
        final String className = studyInfo.getClass().getName();
        writeExportLine("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        writeExportLine("+ " + studyInfo.getCfullname());
        writeExportLine("+ " + studyInfo.getNamePath());
        writeExportLine("+ " + studyInfo.getCname());
        writeExportLine("+ " + studyInfo.getCbasecode());
        writeExportLine("");
    }

    // TODO: testing other export format.
    public void writeExportClinicalDataInfo(final I2B2ClinicalDataInfo clinicalDataInfo) {
        final String className = clinicalDataInfo.getClass().getName();
        writeExportLine("[I2B2ODMStudyHandler] " + className.substring(className.lastIndexOf('.') + 1) + ":");
        writeExportLine("+ " + clinicalDataInfo.getPatientNum());
        writeExportLine("+ " + clinicalDataInfo.getConceptCd());
        writeExportLine("+ " + clinicalDataInfo.getTvalChar());
        writeExportLine("");
    }

    // TODO: testing other export format.
    private void writeExportLine(final String line) {
        try {
            exportWriter.write(line);
            exportWriter.newLine();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }




}
