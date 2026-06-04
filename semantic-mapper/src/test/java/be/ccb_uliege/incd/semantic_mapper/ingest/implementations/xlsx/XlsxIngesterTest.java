package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;

class XlsxIngesterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDiscardHeaderRowsAndMapOnlyDataRows() throws Exception {
        Path xlsxFile = tempDir.resolve("sample.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(xlsxFile)) {
            var sheet = workbook.createSheet("Events");

            sheet.createRow(0).createCell(0).setCellValue("Process report");

            var headerRow = sheet.createRow(1);
            headerRow.createCell(0).setCellValue("EventTime");
            headerRow.createCell(1).setCellValue("Computer");

            var duplicateHeaderRow = sheet.createRow(2);
            duplicateHeaderRow.createCell(0).setCellValue("EventTime");
            duplicateHeaderRow.createCell(1).setCellValue("Computer");

            var dataRow = sheet.createRow(3);
            dataRow.createCell(0).setCellValue("2026-06-04T12:34:56Z");
            dataRow.createCell(1).setCellValue("host-01");

            workbook.write(outputStream);
        }

        List<SourceRecord> mappedRecords = new ArrayList<>();
        SourceMapper mapper = mappedRecords::add;

        new XlsxIngester().ingest(xlsxFile, mapper, null);

        assertEquals(1, mappedRecords.size());
        XlsxRecord record = (XlsxRecord) mappedRecords.get(0);
        assertNotNull(record);
        assertEquals("2026-06-04T12:34:56Z", record.get("EventTime"));
        assertEquals("host-01", record.get("Computer"));
    }
}