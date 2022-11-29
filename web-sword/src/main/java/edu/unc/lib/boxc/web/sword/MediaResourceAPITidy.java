package edu.unc.lib.boxc.web.sword;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.swordapp.server.MediaResourceAPI;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordConfiguration;

/**
 * Closes the input stream after copying since the base API does not allow for this currently.
 * @author bbpennel
 *
 */
public class MediaResourceAPITidy extends MediaResourceAPI {

    public MediaResourceAPITidy(MediaResourceManager mrm, SwordConfiguration config) {
        super(mrm, config);
    }

    @Override
    protected void copyInputToOutput(InputStream in, OutputStream out) throws IOException {
        try {
            super.copyInputToOutput(in, out);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
