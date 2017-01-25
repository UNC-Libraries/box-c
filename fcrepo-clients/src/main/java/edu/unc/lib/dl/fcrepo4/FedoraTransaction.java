/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.fcrepo4;

import java.net.URI;

/**
 * This class is responsible for storing a transaction id to a thread-local variable
 * @author harring
 *
 */
public class FedoraTransaction {
	
	private static ThreadLocal<URI> txUri = new ThreadLocal<>(); // initial value == null
	
	//stores txid to current thread
	public static void storeTxId(URI uri) {
		FedoraTransaction.txUri.set(uri);
	}
	
	public static boolean hasTxId() {
		return FedoraTransaction.txUri.get() != null;
	}
	
	public static void clearTxId() {
		FedoraTransaction.txUri.remove();
	}
}
