package edu.unc.lib.boxc.operations.jms.machineGenerated;

/**
 * Request object for updating machine generated descriptions
 *
 * @author snluong
 */
public class MachineGenRequest {
    private String pidString;
    private String text;

    public String getPidString() {
        return pidString;
    }

    public void setPidString(String pidString) {
        this.pidString = pidString;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
