package edu.unc.lib.dl.util;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import edu.unc.lib.dl.irods.Handler;

public class IRODSURLStreamHandlerFactory implements URLStreamHandlerFactory {

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if("irods".equals(protocol)) {
			return new Handler();
		} else {
			return null;
		}
	}

}
