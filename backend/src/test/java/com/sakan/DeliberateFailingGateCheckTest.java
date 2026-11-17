package com.sakan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeliberateFailingGateCheckTest {

    @Test
    void deliberateFailingTestToVerifyCiGate() {
        assertEquals(1, 2, "SCRUM-9 acceptance test: deliberate failing assertion to verify CI gate blocks PR");
    }
}
