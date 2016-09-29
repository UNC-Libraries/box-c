package edu.unc.lib.dl.fcrepo4;

import java.util.List;

public class DirectPCDMObject implements PCDMObject {
	private String path;
	
	public DirectPCDMObject(String path) {
		this.path = path;
	}
	
	@Override
	public String getMemberPath(String memberUuid) {
		return path + "/members/" + memberUuid;
	}

	@Override
	public String getMembersPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addRelatedObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addMember() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move() {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<PCDMObject> getMembers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<?> getFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<?> getRelatedObjects() {
		// TODO Auto-generated method stub
		return null;
	}

}
