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

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

public class DummyMessageConductor implements MessageConductor {

	String identifier = null;
	ArrayList<PIDMessage> messageList = null; 
	
	public DummyMessageConductor(){
		messageList = new ArrayList<PIDMessage>();
	}
	
	@Override
	public void add(PIDMessage message) {
		messageList.add(message);
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier){
		this.identifier = identifier;
	}

	public ArrayList<PIDMessage> getMessageList() {
		return messageList;
	}

	public void setMessageList(ArrayList<PIDMessage> messageList) {
		this.messageList = messageList;
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getQueueSize() {
		// TODO Auto-generated method stub
		return 0;
	}
}

