package com.taller.portal;

import com.taller.portal.service.TallerFacade;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias de reglas deterministas del caso UC-CV-02. */
class UcCv02Test {
    /** Verifica RFC moral y físico, normalización y fechas de calendario. */
    @Test void validaRfcMexicano() {
        assertEquals("MNS260101AB1", TallerFacade.rfc(" mns260101ab1 "));
        assertEquals("ÑABC260101AB1", TallerFacade.rfc("ñabc260101ab1"));
        assertThrows(IllegalArgumentException.class, () -> TallerFacade.rfc("MNS260231AB1"));
        assertThrows(IllegalArgumentException.class, () -> TallerFacade.rfc("MNS260101A"));
        assertThrows(IllegalArgumentException.class, () -> TallerFacade.rfc("MNS260101@@@"));
    }
}
