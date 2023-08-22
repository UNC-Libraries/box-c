package edu.unc.lib.boxc.services.camel;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.camel.model.ModelCamelContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.UUID;

/**
 * Basic methods used in several Processor tests
 *
 * @author snluong
 */
public class ProcessorTestHelper {
    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
