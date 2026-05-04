package edu.unc.lib.boxc.web.services.utils;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MachineUpdateServiceTestHelper {

    public static void assertResponse(MvcResult result, String action, PID datastreamPid) throws Exception {
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(action, respMap.get("action"));
        assertEquals(datastreamPid.getComponentId(), respMap.get("pid"));
    }
}
