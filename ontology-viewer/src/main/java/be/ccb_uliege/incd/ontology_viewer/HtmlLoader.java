package be.ccb_uliege.incd.ontology_viewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads HTML files from the {@code /html/} resource directory on the classpath.
 */
public final class HtmlLoader {

    private HtmlLoader() {
        // utility class
    }

    /**
     * Reads an HTML resource file and returns its content as a String.
     *
     * @param fileName the file name inside {@code /html/}, e.g. {@code "timeline.html"}
     * @return the full HTML content
     * @throws IllegalStateException if the resource cannot be found or read
     */
    public static String load(String fileName) {
        String path = "/html/" + fileName;
        try (InputStream is = HtmlLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("HTML resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read HTML resource: " + path, e);
        }
    }
}
