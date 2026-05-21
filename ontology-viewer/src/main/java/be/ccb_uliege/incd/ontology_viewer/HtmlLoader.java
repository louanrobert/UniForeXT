package be.ccb_uliege.incd.ontology_viewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URL;

/**
 * Loads HTML files from the {@code /html/} resource directory on the classpath.
 */
public final class HtmlLoader {

    private HtmlLoader() {
        // utility class
    }

    /**
     * Reads an HTML resource file and returns its content as a String.
     * Logs a warning but returns a fallback error page if the resource is missing.
     *
     * @param fileName the file name inside {@code /html/}, e.g. {@code "timeline.html"}
     * @return the full HTML content, or an error page if resource is not found
     */
    public static String load(String fileName) {
        String path = "/html/" + fileName;
        try (InputStream is = HtmlLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("WARNING: HTML resource not found: " + path);
                return generateErrorPage("Resource Not Found", 
                    "The HTML resource '" + fileName + "' could not be found. " +
                    "This may indicate a packaging or deployment issue.");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read HTML resource: " + path);
            e.printStackTrace();
            return generateErrorPage("I/O Error", 
                "Failed to read the HTML resource: " + e.getMessage());
        }
    }

    /**
     * Returns the external URL of an HTML resource inside /html/.
     * This is useful for WebEngine.load(...) so relative assets resolve correctly.
     *
     * @param fileName the file name inside /html/
     * @return external URL string, or null if not found
     */
    public static String resourceUrl(String fileName) {
        String path = "/html/" + fileName;
        URL url = HtmlLoader.class.getResource(path);
        if (url == null) {
            System.err.println("WARNING: HTML resource URL not found: " + path);
            return null;
        }
        return url.toExternalForm();
    }

    /**
     * Generates a simple HTML error page when resources fail to load.
     */
    private static String generateErrorPage(String title, String message) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>" + escapeHtml(title) + "</title>\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; padding: 40px; background-color: #f0f0f0; }\n" +
               "        .error-box { background: white; border: 2px solid #d32f2f; padding: 20px; border-radius: 8px; }\n" +
               "        h1 { color: #d32f2f; }\n" +
               "        p { color: #666; line-height: 1.6; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class='error-box'>\n" +
               "        <h1>" + escapeHtml(title) + "</h1>\n" +
               "        <p>" + escapeHtml(message) + "</p>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }

    /**
     * Simple HTML entity escaping.
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
