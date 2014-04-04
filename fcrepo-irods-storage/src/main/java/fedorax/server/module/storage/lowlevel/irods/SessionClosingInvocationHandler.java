package fedorax.server.module.storage.lowlevel.irods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.fcrepo.server.proxy.AbstractInvocationHandler;
import org.irods.jargon.core.connection.IRODSCommands;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.exception.JargonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionClosingInvocationHandler extends AbstractInvocationHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(SessionClosingInvocationHandler.class);

	public SessionClosingInvocationHandler() {
	}

	public void closeIRODSSessions() {
		Map<String, IRODSCommands> map = IRODSSession.sessionMap.get();
		if (map != null) {
			for (IRODSCommands com : map.values()) {
				try {
					LOG.debug("Trying to close session for Request worker thread.");
					com.getIrodsSession().closeSession();
				} catch (JargonException e) {
					LOG.warn(
							"Error closing a session for Request worker thread.",
							e);
				}
			}
		}
	}

	@Override
	public Object invoke(Object arg0, Method method, Object[] args)
			throws Throwable {
		Object returnValue = null;
		try {
			returnValue = method.invoke(target, args);
		} catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}
		this.closeIRODSSessions();
		return returnValue;
	}

}
