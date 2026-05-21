package be.ccb_uliege.incd.ontology_viewer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for assigning and managing colors for ontology types.
 * Uses the ColorBrewer "Paired" qualitative palette for high
 * distinguishability on both light and dark backgrounds.
 *
 */
public class ColorService {

    /**
     * ColorBrewer "Paired" qualitative palette.
     */
    private static final String[] COLORBREWER_PAIRED_12 = { 
      "#5F4690","#1D6996","#38A6A5","#0F8554","#73AF48","#EDAD08","#E17C05",
      "#1f78b4", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#6a3d9a", "#b15928",
      "#7F3C8D","#11A579","#3969AC","#F2B701","#E73F74","#80BA5A","#E68310",
      "#E58606","#5D69B1","#52BCA3","#99C945","#CC61B0","#24796C","#DAA51B"
    };

    /** Per-instance type-to-color mapping */
    private final Map<String, String> typeColors = new ConcurrentHashMap<>();
    private final AtomicInteger paletteIdx = new AtomicInteger(0);

    /**
     * Returns a stable color for the given type name. The same type always
     * receives the same color within this instance. Colors are assigned from
     * the palette in order of first occurrence.
     */
    public String getColorForType(String type) {
        return typeColors.computeIfAbsent(type,
                t -> COLORBREWER_PAIRED_12[paletteIdx.getAndIncrement() % COLORBREWER_PAIRED_12.length]);
    }

    /**
     * Returns a lighter variant of the given hex color by blending it toward white.
     *
     * @param hex a hex color string such as {@code "#1f78b4"}
     * @return a lightened hex color string
     */
    public static String lightenColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = Math.min(255, r + (255 - r) / 3);
            g = Math.min(255, g + (255 - g) / 3);
            b = Math.min(255, b + (255 - b) / 3);
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return "#cccccc";
        }
    }
}
