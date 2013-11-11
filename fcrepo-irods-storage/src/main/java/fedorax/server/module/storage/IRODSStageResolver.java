package fedorax.server.module.storage;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.staging.FileResolver;

public class IRODSStageResolver extends FileResolver {
	Logger log = LoggerFactory.getLogger(IRODSStageResolver.class);
	private IRODSAccount irodsAccount;

	public IRODSStageResolver(IRODSAccount irodsAccount) {
		super();
		this.irodsAccount = irodsAccount;
	}

	@Override
	public boolean exists(URI locationURI) {
		try {
			if ("irods".equalsIgnoreCase(locationURI.getScheme())) {
				IRODSFileFactory ff = IRODSFileSystem.instance()
						.getIRODSFileFactory(irodsAccount);
				IRODSFile file = ff.instanceIRODSFile(URLDecoder.decode(
						locationURI.getRawPath(), "UTF-8"));
				return file.exists();
			} else {
				return super.exists(locationURI);
			}
		} catch (JargonException e) {
			log.warn("Trouble checking existence of path in irods: {}",
					locationURI, e);
			return false;
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		}
	}

}
