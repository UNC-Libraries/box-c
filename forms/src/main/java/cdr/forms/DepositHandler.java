package cdr.forms;

import java.io.File;

public interface DepositHandler {
	DepositResult deposit(String containerPid, String modsXml, String title, File depositData);
}