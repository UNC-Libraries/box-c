package cdr.forms;

import java.io.InputStream;

public interface DepositHandler {

	public abstract DepositResult deposit(String containerPid, String modsXml, InputStream depositData);

}