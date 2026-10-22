package com.onimeku.app

import com.onimeku.app.ui.WidthClass
import com.onimeku.app.ui.widthClassFor
import org.junit.Assert.assertEquals
import org.junit.Test

class ResponsiveTest {
    @Test fun phonesAreCompact() {
        assertEquals(WidthClass.Compact, widthClassFor(320))
        assertEquals(WidthClass.Compact, widthClassFor(411))
        assertEquals(WidthClass.Compact, widthClassFor(599))
    }

    @Test fun foldablesAndSmallTabletsAreMedium() {
        assertEquals(WidthClass.Medium, widthClassFor(600))
        assertEquals(WidthClass.Medium, widthClassFor(720))
        assertEquals(WidthClass.Medium, widthClassFor(839))
    }

    @Test fun largeTabletsAndLandscapeAreExpanded() {
        assertEquals(WidthClass.Expanded, widthClassFor(840))
        assertEquals(WidthClass.Expanded, widthClassFor(1280))
        assertEquals(WidthClass.Expanded, widthClassFor(1920))
    }
}
