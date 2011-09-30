package edu.unc.lib.dl.sword.server;

import javax.servlet.ServletContext;

import org.swordapp.server.SwordConfiguration;

public class SwordConfigurationImpl implements SwordConfiguration {
	private String authType = null;
	private int maxUploadSize = 0;
	private String tempDirectory = null;

	public SwordConfigurationImpl() {
	}

	public void init(ServletContext context) {
		this.authType = context.getInitParameter("authentication-method");
		// this.tempDirectory = context.getInitParameter("");
		// this.authType = context.getInitParameter("authentication-method");
	}

	public boolean returnDepositReceipt() {
		return true;
	}

	public boolean returnStackTraceInError() {
		return true;
	}

	public boolean returnErrorBody() {
		return true;
	}

	public String generator() {
		return "http://www.swordapp.org/";
	}

	public String generatorVersion() {
		return "2.0";
	}

	public String administratorEmail() {
		return null;
	}

	public String getAuthType() {
		return this.authType;
	}

	public boolean storeAndCheckBinary() {
		return false;
	}

	public String getTempDirectory() {
		return this.tempDirectory;
	}

	public int getMaxUploadSize() {
		return this.maxUploadSize;
	}

}
