package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.xlsx;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.util.IOUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParserFactory;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

/**
 * Implements ingestion logic for XLSX files.
 *
 * The first non-empty row with multiple populated cells is interpreted as the
 * header row and each following row is mapped to a SourceRecord.
 */
public class XlsxIngester implements SourceIngester {

    private static final Logger LOG = Logger.getLogger(XlsxIngester.class.getName());

    @Override
    public void ingest(Path file, SourceMapper mapper, Character delimiter,
            BiConsumer<Integer, Integer> progressListener) {
        try {
            IOUtils.setByteArrayMaxOverride(10000000); // override security limit
            ZipSecureFile.setMinInflateRatio(0.000001);
            int totalRows = countMappedRows(file);
            if (progressListener != null) {
                progressListener.accept(0, totalRows);
            }

            try (OPCPackage packageFile = OPCPackage.open(file.toFile(), PackageAccess.READ)) {
                XSSFReader reader = new XSSFReader(packageFile, true);
                Iterator<InputStream> sheets = reader.getSheetsData();
                if (!sheets.hasNext()) {
                    LOG.log(Level.WARNING, "XLSX file has no sheets: " + file);
                    return;
                }

                StylesTable styles = reader.getStylesTable();
                SharedStrings sharedStrings = reader.getSharedStringsTable();
                DataFormatter formatter = new DataFormatter();
                StreamingSheetHandler handler = new StreamingSheetHandler(mapper, progressListener, totalRows);
                XMLReader parser = createXmlReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(styles, sharedStrings, handler, formatter, false));

                try (InputStream sheetData = sheets.next()) {
                    parser.parse(new InputSource(sheetData));
                }

                if (!handler.hasHeaderRow()) {
                    LOG.log(Level.WARNING, "XLSX file has no header row: " + file);
                }
            }
        } catch (FileNotFoundException e) {
            LOG.log(Level.SEVERE, "XLSX file not found: " + file);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error ingesting XLSX file: " + file + " - " + e.getMessage(), e);
        }
    }

    // Counts the number of data rows that would be mapped, without fully parsing
    // the file into memory.
    private int countMappedRows(Path file) throws Exception {
        CountingSheetHandler handler = new CountingSheetHandler();
        parseFirstSheet(file, handler);
        return handler.getMappedRows();
    }

    // Creates an XMLReader for parsing XLSX sheet data
    private XMLReader createXmlReader() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newSAXParser().getXMLReader();
    }

    // Parses only the first sheet of the XLSX file using the provided
    // SheetContentsHandler.
    private void parseFirstSheet(Path file, XSSFSheetXMLHandler.SheetContentsHandler handler) throws Exception {
        IOUtils.setByteArrayMaxOverride(10000000); // override security limit
        ZipSecureFile.setMinInflateRatio(0.000001);

        try (OPCPackage packageFile = OPCPackage.open(file.toFile(), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(packageFile, true);
            Iterator<InputStream> sheets = reader.getSheetsData();
            if (!sheets.hasNext()) {
                LOG.log(Level.WARNING, "XLSX file has no sheets: " + file);
                return;
            }

            StylesTable styles = reader.getStylesTable();
            SharedStrings sharedStrings = reader.getSharedStringsTable();
            DataFormatter formatter = new DataFormatter();
            XMLReader parser = createXmlReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(styles, sharedStrings, handler, formatter, false));

            try (InputStream sheetData = sheets.next()) {
                parser.parse(new InputSource(sheetData));
            }
        }
    }

    /*
     * Abstract base class for handling sheet content parsing.
     * Implements logic for detecting header row and mapping subsequent rows to
     * SourceRecords.
     * The header row is determined as the first non-empty row with multiple
     * populated cells.
     * Subclasses implement the onDataRow method to define how to map data rows to
     * SourceRecords.
     */
    private abstract static class AbstractSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final List<String> currentRowValues = new ArrayList<>();
        private String[] headers;
        private boolean headerRowSeen;

        // Start of row: clear current row values
        @Override
        public void startRow(int rowNum) {
            currentRowValues.clear();
        }

        // detect header row and map data rows to SourceRecords
        @Override
        public void endRow(int rowNum) {
            if (!headerRowSeen) {
                if (countNonBlankValues(currentRowValues) < 2) {
                    return;
                }

                headers = currentRowValues.toArray(new String[0]);
                headerRowSeen = true;
                return;
            }

            if (headers == null || headers.length == 0) {
                return;
            }

            if (isHeaderRow(currentRowValues)) {
                return;
            }

            Map<String, String> values = new LinkedHashMap<>();
            boolean hasAnyValue = false;

            for (int colIndex = 0; colIndex < headers.length; colIndex++) {
                String header = headers[colIndex] == null ? "" : headers[colIndex].trim();
                if (header.isBlank()) {
                    continue;
                }

                String value = colIndex < currentRowValues.size() && currentRowValues.get(colIndex) != null
                        ? currentRowValues.get(colIndex).trim()
                        : "";
                values.put(header, value);

                if (!value.isBlank()) {
                    hasAnyValue = true;
                }
            }

            if (hasAnyValue) {
                onDataRow(values);
            }
        }

        // Cell value: store in current row values list at the appropriate column index
        @Override
        public void cell(String cellReference, String formattedValue,
                org.apache.poi.xssf.usermodel.XSSFComment comment) {
            int columnIndex = cellReference == null ? currentRowValues.size()
                    : new CellReference(cellReference).getCol();
            ensureCapacity(columnIndex + 1);
            currentRowValues.set(columnIndex, formattedValue == null ? "" : formattedValue.trim());
        }

        // Header/footer: ignored
        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
        }

        protected abstract void onDataRow(Map<String, String> values);

        protected boolean hasHeaderRow() {
            return headerRowSeen;
        }

        // Ensure the currentRowValues list has enough capacity to store a value at the
        // given index
        private void ensureCapacity(int size) {
            while (currentRowValues.size() < size) {
                currentRowValues.add(null);
            }
        }

        // Determines if the given row values match the header row
        private boolean isHeaderRow(List<String> rowValues) {
            if (headers == null || rowValues == null) {
                return false;
            }

            int columnCount = Math.max(headers.length, rowValues.size());
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                String header = colIndex < headers.length && headers[colIndex] != null ? headers[colIndex].trim() : "";
                String value = colIndex < rowValues.size() && rowValues.get(colIndex) != null
                        ? rowValues.get(colIndex).trim()
                        : "";

                if (!header.equals(value)) {
                    return false;
                }
            }

            return countNonBlankValues(rowValues) > 0;
        }

        // Counts the number of non-blank values in the given list of row values
        private int countNonBlankValues(List<String> rowValues) {
            int count = 0;
            for (String value : rowValues) {
                if (value != null && !value.trim().isBlank()) {
                    count++;
                }
            }
            return count;
        }
    }

    // Sheet handler for counting mapped rows without mapping to SourceRecords
    private static final class CountingSheetHandler extends AbstractSheetHandler {

        private int mappedRows;

        @Override
        protected void onDataRow(Map<String, String> values) {
            mappedRows++;
        }

        int getMappedRows() {
            return mappedRows;
        }
    }

    // Sheet handler for streaming mapping of data rows to SourceRecords using the
    // provided SourceMapper and progress listener
    private static final class StreamingSheetHandler extends AbstractSheetHandler {

        private final SourceMapper mapper;
        private final BiConsumer<Integer, Integer> progressListener;
        private final int totalRows;
        private int processedRows;

        private StreamingSheetHandler(SourceMapper mapper, BiConsumer<Integer, Integer> progressListener,
                int totalRows) {
            this.mapper = mapper;
            this.progressListener = progressListener;
            this.totalRows = totalRows;
        }

        @Override
        protected void onDataRow(Map<String, String> values) {
            mapper.map(new XlsxRecord(values));
            processedRows++;

            if (progressListener != null) {
                progressListener.accept(processedRows, totalRows);
            }
        }
    }
}
