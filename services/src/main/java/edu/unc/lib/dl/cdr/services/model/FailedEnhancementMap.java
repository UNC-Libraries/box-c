package edu.unc.lib.dl.cdr.services.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * Stores service enhancement failures by pid and service name. Only the most basic information for looking up failures
 * is stored in memory, with much greater detail written out to disk.
 * 
 * @author bbpennel
 * 
 */
public class FailedEnhancementMap {
	private static final Logger log = LoggerFactory.getLogger(FailedEnhancementMap.class);
	private String failureLogPath;
	private static final String MESSAGE_FILE_PREFIX = "message-";
	private static final String ERROR_FILE_PREFIX = "stack-";

	// Service to pids
	private Map<String, Map<String, WeakReference<FailedEnhancementEntry>>> serviceToPID;
	// Pid to services
	private Map<String, Map<String, WeakReference<FailedEnhancementEntry>>> pidToService;

	public FailedEnhancementMap() {
	}

	public void init() {
		this.serviceToPID = new HashMap<String, Map<String, WeakReference<FailedEnhancementEntry>>>();
		this.pidToService = new LinkedHashMap<String, Map<String, WeakReference<FailedEnhancementEntry>>>();

		this.loadFailedMap();
	}

	/**
	 * Add a new service failure
	 * 
	 * @param pid
	 * @param service
	 * @param message
	 * @param exception
	 */
	public synchronized void add(PID pid, Class<?> service, ActionMessage message, Throwable exception) {
		String serviceName = service.getName();
		Map<String, WeakReference<FailedEnhancementEntry>> pidCache = this.serviceToPID.get(serviceName);
		if (pidCache == null) {
			pidCache = new HashMap<String, WeakReference<FailedEnhancementEntry>>();
			this.serviceToPID.put(serviceName, pidCache);
		}
		pidCache.put(pid.getPid(), null);

		pidCache = this.pidToService.get(pid.getPid());
		if (pidCache == null) {
			pidCache = new HashMap<String, WeakReference<FailedEnhancementEntry>>();
			this.pidToService.put(pid.getPid(), pidCache);
		}
		pidCache.put(serviceName, null);

		try {
			serializeFailure(pid, service, message, exception);
		} catch (IOException e) {
			log.error("Failed to create failure log for " + pid.getPid() + " service " + serviceName, e);
		}
	}

	public boolean contains(String pid, String serviceName) {
		Map<String, WeakReference<FailedEnhancementEntry>> serviceCache = this.pidToService.get(pid);
		if (serviceCache == null)
			return false;
		return serviceCache.containsKey(serviceName);
	}

	/**
	 * Retrieves a list of failed enhancements for a given pid
	 * 
	 * @param pid
	 * @return
	 */
	public synchronized List<FailedEnhancementEntry> get(String pid) {
		Map<String, WeakReference<FailedEnhancementEntry>> cache = this.pidToService.get(pid);
		if (cache == null)
			return null;
		List<FailedEnhancementEntry> failedEnhancements = new ArrayList<FailedEnhancementEntry>(cache.size());
		for (String serviceName : cache.keySet()) {
			failedEnhancements.add(this.get(pid, serviceName));
		}
		return failedEnhancements;
	}

	/**
	 * Retrieves the FailedEnhancementEntry matching pid and service name, either from the cache or from disk.
	 * 
	 * @param pid
	 * @param serviceName
	 * @return
	 */
	public synchronized FailedEnhancementEntry get(String pid, String serviceName) {
		Map<String, WeakReference<FailedEnhancementEntry>> cache = this.pidToService.get(pid);
		// Either the enhancement hasn't failed or the map is out of sync, lets be optimistic and say its not there
		if (cache == null)
			return null;
		WeakReference<FailedEnhancementEntry> entryRef = cache.get(serviceName);
		if (entryRef != null && entryRef.get() != null)
			return entryRef.get();

		// Entry isn't in the cache, retrieve it from the file system
		FailedEnhancementEntry entry;
		try {
			entry = this.deserializeFailure(pid, serviceName);
		} catch (IOException e) {
			return null;
		}
		entryRef = new WeakReference<FailedEnhancementEntry>(entry);
		cache.put(serviceName, entryRef);

		this.serviceToPID.get(serviceName).put(pid, entryRef);
		return entry;
	}

	public Map<String, WeakReference<FailedEnhancementEntry>> getPIDCache(String serviceName) {
		return this.serviceToPID.get(serviceName);
	}
	
	public Set<String> getOrCreateServicePIDSet(ObjectEnhancementService service) {
		Map<String, WeakReference<FailedEnhancementEntry>> pidCache = this.serviceToPID.get(service.getClass().getName());
		if (pidCache == null) {
			pidCache = new HashMap<String, WeakReference<FailedEnhancementEntry>>();
			serviceToPID.put(service.getClass().getName(), pidCache);
		}
		return pidCache.keySet();
	}

	public Set<String> getFailedServices(String pid) {
		Map<String, WeakReference<FailedEnhancementEntry>> serviceCache = this.pidToService.get(pid);
		if (serviceCache == null)
			return null;
		return serviceCache.keySet();
	}

	/**
	 * Clears out the state information for all failures related to the provided pid
	 * 
	 * @param pid
	 */
	public synchronized void remove(String pid) {
		Map<String, WeakReference<FailedEnhancementEntry>> serviceCache = this.pidToService.get(pid);
		if (serviceCache != null) {
			Iterator<String> serviceIt = serviceCache.keySet().iterator();
			while (serviceIt.hasNext()) {
				String serviceName = serviceIt.next();
				Map<String, WeakReference<FailedEnhancementEntry>> pidCache = this.serviceToPID.get(serviceName);
				if (pidCache != null) {
					pidCache.remove(pid);
				}
			}
			this.pidToService.remove(pid);
		}

		File baseFolder = new File(this.failureLogPath);
		File pidFolder = new File(baseFolder, pid);
		if (pidFolder.exists()) {
			try {
				FileUtils.deleteDirectory(pidFolder);
			} catch (IOException e) {
				log.error("Failed to delete directory " + pidFolder.getName(), e);
			}
		}
	}

	/**
	 * Constructs a FailedEnhancementEntry for the failure identified by pid and service name using data retrieved from
	 * disk.
	 * 
	 * @param pid
	 * @param serviceName
	 * @return
	 * @throws IOException
	 */
	public synchronized FailedEnhancementEntry deserializeFailure(String pid, String serviceName) throws IOException {
		File pidFolder = new File(this.failureLogPath, pid);
		if (!pidFolder.exists()) {
			return null;
		}
		FailedEnhancementEntry entry = new FailedEnhancementEntry(pid, serviceName);

		File stackFile = new File(pidFolder, ERROR_FILE_PREFIX + serviceName);
		entry.timeFailed = stackFile.lastModified();
		if (stackFile.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(stackFile));
				StringBuilder content = new StringBuilder((int) stackFile.length());
				String line = null;
				while ((line = reader.readLine()) != null) {
					content.append(line).append("\n");
				}
				entry.stackTrace = content.toString();
			} catch (FileNotFoundException e) {
				return null;
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		}

		File messageFile = new File(pidFolder, MESSAGE_FILE_PREFIX + serviceName);
		if (messageFile.exists()) {
			ObjectInputStream objectInputStream = null;
			try {
				FileInputStream fileInputStream = new FileInputStream(messageFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				Object obj = objectInputStream.readObject();
				entry.message = (ActionMessage) obj;
			} catch (ClassNotFoundException e) {
				log.error("Failed to deserialize message object for " + pid + " " + serviceName, e);
			} finally {
				objectInputStream.close();
			}
		}

		return entry;
	}

	/**
	 * Serializes the description of a failed service to disk
	 * 
	 * @param pid
	 * @param service
	 * @param message
	 * @param exception
	 * @throws IOException
	 */
	public void serializeFailure(PID pid, Class<?> service, ActionMessage message, Throwable exception)
			throws IOException {
		String serviceName = service.getName();

		// Serialize this failure
		File pidFolder = new File(this.failureLogPath, pid.getPid());
		// Create the folder if it doesn't exist
		if (!pidFolder.exists() && !pidFolder.mkdir()) {
			throw new RuntimeException("Failed to create folder " + this.failureLogPath + "/" + pid.getPid());
		}

		File stackFile = new File(pidFolder, ERROR_FILE_PREFIX + serviceName);
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileOutputStream(stackFile));
			if (exception != null)
				exception.printStackTrace(writer);
		} finally {
			if (writer != null) {
				try {
					writer.flush();
					writer.close();
				} catch (Exception ignored) {
				}
			}
		}

		if (message != null) {
			File messageFile = new File(pidFolder, MESSAGE_FILE_PREFIX + serviceName);
			FileOutputStream fileOutputStream = null;
			ObjectOutputStream objectOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(messageFile);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(message);
			} catch (FileNotFoundException e) {
				throw new IOException("Failed to serialize message for " + pid, e);
			} finally {
				if (objectOutputStream != null)
					objectOutputStream.close();
			}
		}
	}

	/**
	 * Recreates the failed services map from the structure on disk, but does not load the actual contents
	 */
	public synchronized void loadFailedMap() {
		File baseFolder = new File(this.failureLogPath);
		String[] directories = baseFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		});

		// Create pid entries for each directory
		for (String directory : directories) {
			File pidFolder = new File(baseFolder, directory);
			String[] pidFiles = pidFolder.list();
			Map<String, WeakReference<FailedEnhancementEntry>> serviceCache = new HashMap<String, WeakReference<FailedEnhancementEntry>>();
			this.pidToService.put(directory, serviceCache);
			// Scan the subfolders of the pid directories for which particular services failed
			for (String pidFile : pidFiles) {
				if (pidFile.startsWith(ERROR_FILE_PREFIX)) {
					String serviceName = pidFile.substring(ERROR_FILE_PREFIX.length());
					// Store a stub for the pid -> service mapping
					serviceCache.put(serviceName, null);

					Map<String, WeakReference<FailedEnhancementEntry>> pidCache = this.serviceToPID.get(serviceName);
					if (pidCache == null) {
						pidCache = new HashMap<String, WeakReference<FailedEnhancementEntry>>();
						this.serviceToPID.put(serviceName, pidCache);
					}
					// Store a stub for the service to pid mapping
					pidCache.put(directory, null);
				}
			}
		}
	}

	public synchronized void clear() {
		this.serviceToPID.clear();
		this.pidToService.clear();

		if (this.failureLogPath != null) {
			// Clear out the trace files
			File logDirectory = new File(this.failureLogPath);
			File[] failFolderContents = logDirectory.listFiles();
			if (failFolderContents != null) {
				for (File pidFolder : failFolderContents) {
					if (pidFolder.isDirectory()) {
						try {
							FileUtils.deleteDirectory(pidFolder);
						} catch (IOException e) {
							log.error("Failed to delete pid directory " + pidFolder.getName(), e);
						}
					}
				}
			}
		}
	}

	public void setFailureLogPath(String failureLogPath) {
		this.failureLogPath = failureLogPath;
	}

	/**
	 * Returns the total number of failed services
	 * 
	 * @return
	 */
	public int size() {
		if (this.serviceToPID == null)
			return 0;
		int count = 0;
		Iterator<Map<String, WeakReference<FailedEnhancementEntry>>> serviceIt = this.serviceToPID.values().iterator();
		while (serviceIt.hasNext()) {
			count += serviceIt.next().size();
		}
		return count;
	}

	public Map<String, Map<String, WeakReference<FailedEnhancementEntry>>> getServiceToPID() {
		return serviceToPID;
	}

	public Map<String, Map<String, WeakReference<FailedEnhancementEntry>>> getPidToService() {
		return pidToService;
	}

	public class FailedEnhancementEntry implements ActionMessage {
		private static final long serialVersionUID = 1L;
		
		public PID pid;
		public String serviceName;
		public long timeFailed = System.currentTimeMillis();
		public String stackTrace;
		public ActionMessage message;

		public FailedEnhancementEntry(String pid, String serviceName) {
			this.pid = new PID(pid);
			this.serviceName = serviceName;
		}

		@Override
		public String getMessageID() {
			if (this.message == null)
				return null;
			return this.message.getMessageID();
		}

		@Override
		public String getTargetID() {
			return this.pid.getPid();
		}

		@Override
		public String getTargetLabel() {
			if (this.message == null)
				return null;
			return this.message.getTargetLabel();
		}

		@Override
		public void setTargetLabel(String targetLabel) {
			if (this.message == null)
				return;
			this.message.setTargetLabel(targetLabel);
		}

		@Override
		public String getAction() {
			if (this.message == null)
				return null;
			return this.message.getAction();
		}

		@Override
		public String getNamespace() {
			if (this.message == null)
				return null;
			return this.message.getNamespace();
		}

		@Override
		public String getQualifiedAction() {
			if (this.message == null)
				return null;
			return this.message.getQualifiedAction();
		}

		@Override
		public long getTimeCreated() {
			if (this.message == null)
				return -1;
			return this.message.getTimeCreated();
		}
	}
}
