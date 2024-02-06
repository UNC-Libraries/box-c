package edu.unc.lib.boxc.operations.jms.views;


/**
 * Request object for updating the view behavior of the UV
 * @author sharonluong
 */
public class UpdateViewBehaviorRequest {
    private String objectPidString;
    private ViewBehavior viewBehavior;
    public enum ViewBehavior {
        INDIVIDUALS,
        PAGED,
        CONTINUOUS
    }

    public String getObjectPidString() {
        return objectPidString;
    }

    public void setObjectPidString(String objectPidString) {
        this.objectPidString = objectPidString;
    }

    public ViewBehavior getViewBehavior() {
        return viewBehavior;
    }

    public void setViewBehavior(ViewBehavior viewBehavior) {
        this.viewBehavior = viewBehavior;
    }
}
