package be.ccb_uliege.incd.uniforext_viewer.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColorServiceTest {

    @Test
    void getColorForTypeReturnsStableDistinctPaletteColors() {
        ColorService service = new ColorService();

        String eventColor = service.getColorForType("Event");
        String sameEventColor = service.getColorForType("Event");
        String userColor = service.getColorForType("User");

        assertEquals(eventColor, sameEventColor);
        assertNotEquals(eventColor, userColor);
        assertTrue(eventColor.matches("#[0-9A-Fa-f]{6}"));
        assertTrue(userColor.matches("#[0-9A-Fa-f]{6}"));
    }

    @Test
    void lightenColorBlendsTowardWhite() {
        assertEquals("#557799", ColorService.lightenColor("#003366"));
        assertEquals("#ffffff", ColorService.lightenColor("#ffffff"));
    }

    @Test
    void lightenColorFallsBackForInvalidInput() {
        assertEquals("#cccccc", ColorService.lightenColor("not-a-color"));
        assertEquals("#cccccc", ColorService.lightenColor(null));
    }
}
