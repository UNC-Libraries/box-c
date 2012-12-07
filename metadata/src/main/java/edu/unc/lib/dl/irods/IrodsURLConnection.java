package edu.unc.lib.dl.irods;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class IrodsURLConnection extends URLConnection {

	protected IrodsURLConnection(URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
		throw new IOException("This is a stub and not expected to work.");
	}

}
