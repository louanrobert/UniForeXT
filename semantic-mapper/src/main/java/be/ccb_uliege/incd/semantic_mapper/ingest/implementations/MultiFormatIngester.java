package be.ccb_uliege.incd.semantic_mapper.ingest.implementations;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.BiConsumer;

import be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv.CsvIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.implementations.xlsx.XlsxIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

/**
 * Selects the proper ingester implementation based on the file extension.
 */
public class MultiFormatIngester implements SourceIngester {

    private static final Logger LOG = LoggerFactory.getLogger(MultiFormatIngester.class);

    private final Map<String, SourceIngester> ingesters;

    public MultiFormatIngester() {
        this.ingesters = Map.of(
                "csv", new CsvIngester(),
                "xlsx", new XlsxIngester());
    }

    @Override
    public void ingest(Path file, SourceMapper mapper, Character delimiter,
            BiConsumer<Integer, Integer> progressListener) {
        String extension = getExtension(file);
        SourceIngester ingester = ingesters.get(extension);

        if (ingester == null) {
            LOG.warn("No ingester registered for file type: {} ({})", extension, file);
            return;
        }

        ingester.ingest(file, mapper, delimiter, progressListener);
    }

    private String getExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
