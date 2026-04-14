package edu.unc.lib.boxc.operations.jms.machineGenerated;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MachineGenRequestSerializationHelperTest {
    private final PID pid = PIDs.get(UUID.randomUUID().toString());

    @Test
    public void testHelperSuccess() throws IOException {
        var request = new MachineGenRequest();
        request.setPidString(pid.getId());
        request.setText("This is a great machine generated description");

        var json = MachineGenRequestSerializationHelper.toJson(request);
        var helperRequest = MachineGenRequestSerializationHelper.toRequest(json);

        assertEquals(request.getPidString(), helperRequest.getPidString());
        assertEquals(request.getText(), helperRequest.getText());
    }
}
