package edu.unc.lib.dl.cdr.services.rest.modify;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.SingleFolderSIP;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.services.IngestResult;
import edu.unc.lib.dl.util.DepositMethod;

@Controller
public class AddContainerController {
	private static final Logger log = LoggerFactory.getLogger(AddContainerController.class);

	@Autowired
	protected AgentFactory agentFactory;
	@Autowired
	private DigitalObjectManager digitalObjectManager;

	@RequestMapping(value = "edit/create_container/{id}", method = RequestMethod.POST)
	public @ResponseBody
	String createContainer(@PathVariable("id") String id, @RequestParam("name") String filename, 
			@RequestParam(value = "type", required=false) String type, @RequestParam(value="description", required=false) MultipartFile description, 
			Model model, HttpServletRequest request, HttpServletResponse response) {
		PID pid = new PID(id);
		
		try {
			String user = request.getHeader("On-Behalf-Of");
			if (user == null) 
				user = request.getRemoteUser();
			Agent owner = agentFactory.findPersonByOnyen(user, false);
			if (owner == null) {
				response.setStatus(400);
				return "{\"error\": \"Owner was not specified\"}";
			}
				
			
			SingleFolderSIP sip = new SingleFolderSIP();
			sip.setContainerPID(pid);
			sip.setSlug(filename);
			if (description != null && !description.isEmpty()) {
				File modsFile = File.createTempFile("mods", "xml");
				description.transferTo(modsFile);
				sip.setModsXML(modsFile);
			}
			sip.setCollection(type != null && type.equals("collection"));

			DepositRecord record = new DepositRecord(owner, owner, DepositMethod.CDRAPI1);
			IngestResult ingestResult = digitalObjectManager.addWhileBlocking(sip, record);
			
			response.setStatus(201);
			return "{\"pid\": \"" + ingestResult.originalDepositID.getPid() + "\"}";
		} catch (IOException e) {
			log.error("Unexpected IO exception", e);
		} catch (IngestException e) {
			log.error("Ingest exception", e);
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		}
		response.setStatus(500);
		return "{\"error\": \"An error occurred while attempting to create a container in " + pid + "\"}";
	}
	
	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
}
