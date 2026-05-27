package be.ccb_uliege.incd.semantic_mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void shouldSkipShaclValidationAcceptsPrimaryFlag() {
        assertTrue(App.shouldSkipShaclValidation(new String[] {"--skip-shacl-validation"}));
    }

    @Test
    void shouldSkipShaclValidationAcceptsAliasFlag() {
        assertTrue(App.shouldSkipShaclValidation(new String[] {"--skip-shacl"}));
    }

    @Test
    void shouldSkipShaclValidationReturnsFalseWhenFlagIsMissing() {
        assertFalse(App.shouldSkipShaclValidation(new String[] {"--other-flag"}));
    }
}