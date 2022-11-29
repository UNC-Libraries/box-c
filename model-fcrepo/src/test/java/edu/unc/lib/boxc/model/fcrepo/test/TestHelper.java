package edu.unc.lib.boxc.model.fcrepo.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;

/**
 *  Helper method for IT tests
 *
 * @author harring
 *
 */
public class TestHelper {

    public static void setContentBase(String uri) {
        try {
            Method m = FcrepoPaths.class.getDeclaredMethod("setBaseUri", String.class);
            m.setAccessible(true);
            try {
                m.invoke(null, uri);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
            Method m2 = RepositoryPaths.class.getDeclaredMethod("setContentBase", String.class);
            m2.setAccessible(true);
            try {
                m2.invoke(null, uri);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
