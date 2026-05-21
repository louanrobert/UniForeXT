package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.xlsx;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

/**
 * Implements ingestion logic for XLSX files.
 *
 * The first row is interpreted as headers and each following row is mapped to a
 * SourceRecord.
 */
public class XlsxIngester implements SourceIngester {

    private static final Logger LOG = Logger.getLogger(XlsxIngester.class.getName());

    @Override
    public void ingest(Path file, SourceMapper mapper, Character delimiter) {
        try (InputStream inputStream = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                LOG.log(Level.WARNING, "XLSX file has no sheets: {0}", file);
                return;
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                LOG.log(Level.WARNING, "XLSX file has no header row: {0}", file);
                return;
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            String[] headers = extractHeaders(headerRow, formatter, evaluator);

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                boolean hasAnyValue = false;

                for (int colIndex = 0; colIndex < headers.length; colIndex++) {
                    String header = headers[colIndex];
                    if (header.isBlank()) {
                        continue;
                    }

                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
                    values.put(header, value);

                    if (!value.isBlank()) {
                        hasAnyValue = true;
                    }
                }

                if (hasAnyValue) {
                    mapper.map(new XlsxRecord(values));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error ingesting XLSX file: " + file + " - " + e.getMessage(), e);
        }
    }

    private String[] extractHeaders(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        int lastCellNum = Math.max(headerRow.getLastCellNum(), 0);
        String[] headers = new String[lastCellNum];

        for (int colIndex = 0; colIndex < lastCellNum; colIndex++) {
            Cell cell = headerRow.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            headers[colIndex] = cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
        }

        return headers;
    }
}
