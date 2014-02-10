package edu.unc.lib.workers;

import java.io.File;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;

/**
 * Orchestrates deposit operations.
 * @author count0
 *
 */
public class DepositService implements WorkerListener {

	public DepositService() {
		// TODO Auto-generated constructor stub
	}
	
	public void cancel(String depositUUID) {
		
	}
	
	public void pause(String depositUUID) {
		
	}
	
	public void resume(String depositUUID) {
		
	}
	
	public void cleanup(String depositUUID, boolean deleteExtraStagedFiles) {
		
	}
	
	public File[] getUntrackedSipFolders() {
		File[] result = null;
		return result;
	}
	
	public void registerSip(File dir) {
		
	}

	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue,
			Job job, Object runner, Object result, Exception ex) {
		// on deposit register
		// if result is a job, then run it
		// look up next job for the deposit
		// on Normalized
	}
	
	

}
