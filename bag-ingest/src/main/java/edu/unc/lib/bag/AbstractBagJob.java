package edu.unc.lib.bag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;

/**
 * Constructed with bag directory and deposit ID.
 * Facilitates event logging with standard success/failure states.
 * @author count0
 *
 */
public abstract class AbstractBagJob implements Runnable {
	private File bagDirectory;
	private PID depositPID;
	private PremisEventLogger eventLog = new PremisEventLogger(this.getClass().getName());

	public AbstractBagJob(File bagDirectory, String depositId) {
		this.bagDirectory = bagDirectory;
		this.depositPID = new PID(depositId);
	}
	
	public AbstractBagJob() {}
	
	public PID getDepositPID() {
		return depositPID;
	}

	public void setDepositPID(PID depositPID) {
		this.depositPID = depositPID;
	}

	public File getBagDirectory() {
		return bagDirectory;
	}

	public void setBagDirectory(File bagDirectory) {
		this.bagDirectory = bagDirectory;
	}

	public PremisEventLogger getEventLog() {
		return eventLog;
	}
	
	public void recordEvent(Type type, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		Element event = getEventLog().logEvent(type, message, this.getDepositPID());
		appendDepositEvent(event);
	}
	
	public void failDeposit(Type type, String message, String details) {
		Element event = getEventLog().logEvent(type, message, this.getDepositPID());
		event = PremisEventLogger.addDetailedOutcome(event, "failed", details, null);
		appendDepositEvent(event);
		Throwable e = new DepositFailedException(message);
		throw new RuntimeException(e);
	}
	
	public void failDeposit(Throwable throwable, Type type, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		Element event = getEventLog().logException(message, throwable);
		event = PremisEventLogger.addLinkingAgentIdentifier(event, "SIP Processing Job", this.getClass().getName(), "Software");
		appendDepositEvent(event); 
		Throwable e = new DepositFailedException(message, throwable);
		throw new RuntimeException(e);
	}
	
	protected void appendDepositEvent(Element event) {
			File file = new File(bagDirectory, "events.xml");
	        FileLock lock = null;
	        PrintWriter out = null;
	        try {
	        	file.createNewFile();
	        	@SuppressWarnings("resource")
				FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
	        	// Get an exclusive lock on the whole file
	            lock = channel.lock();
			    out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			    out.append("\n");
			    new XMLOutputter(Format.getPrettyFormat()).output(event, out);
			    out.close();
	        } catch(IOException e) {
	        	throw new Error(e);
	        } finally {
	        	out.close();
	            try {
					lock.release();
				} catch (IOException e) {
					throw new Error(e);
				}
	        }
	}
	
}
