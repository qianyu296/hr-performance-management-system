package com.hrpm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HrpmApplicationTests {
    @Test
    void applicationMetadataIsDefined() {
        assertEquals("com.hrpm", HrpmApplication.class.getPackageName());
    }
}
