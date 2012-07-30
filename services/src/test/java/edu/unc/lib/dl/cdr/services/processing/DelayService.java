/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.PID;

public class DelayService extends AbstractFedoraEnhancementService {
	private static final long serialVersionUID = 1L;
	public List<PID> candidateList = new ArrayList<PID>();

	public DelayService(){
		this.active = true;
	}
	
	@Override
	public List<PID> findCandidateObjects(int maxResults) throws EnhancementException {
		(new DelayEnhancement(this, new PID("delaying"))).call();
		return candidateList;
	}

	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		return findCandidateObjects(maxResults);
	}

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage pid) throws EnhancementException {
		return new DelayEnhancement(this, pid.getPid());
	}

	@Override
	public boolean isApplicable(EnhancementMessage pid) throws EnhancementException {
		DelayEnhancement.incompleteServices.incrementAndGet();
		DelayEnhancement.betweenApplicableAndEnhancement.incrementAndGet();
		LOG.debug("Completed isApplicable for " + pid.getTargetID());	
		return true;
	}

	@Override
	public boolean prefilterMessage(EnhancementMessage pid) throws EnhancementException {
		return true;
	}

	@Override
	public boolean isStale(PID pid) throws EnhancementException {
		return false;
	}

	@Override
	public EnhancementApplication getLastApplied(PID pid) throws EnhancementException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int countCandidateObjects() throws EnhancementException {
		// TODO Auto-generated method stub
		return 0;
	}
}