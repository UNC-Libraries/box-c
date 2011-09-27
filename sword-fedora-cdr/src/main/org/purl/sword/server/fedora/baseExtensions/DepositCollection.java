package org.purl.sword.server.fedora.baseExtensions;

/**
  * Copyright (c) 2007, Aberystwyth University
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *  - Redistributions of source code must retain the above
  *    copyright notice, this list of conditions and the
  *    following disclaimer.
  *
  *  - Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  *  - Neither the name of the Centre for Advanced Software and
  *    Intelligent Systems (CASIS) nor the names of its
  *    contributors may be used to endorse or promote products derived
  *    from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
  * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * @author Glen Robson
  * @version 1.0
  * Date: 26th February 2009
  *
  * This class extends the deposit clas but adds in the collection pid
  * so that it is easily accessible with the deposit
  *
  */

import org.purl.sword.base.Deposit;

public class DepositCollection extends Deposit {
	protected String _collectionPid = "";

	public DepositCollection(final Deposit pOriginalDeposit, final String pCollectionPid) {
		super();
	
		super.setFile(pOriginalDeposit.getFile());
		super.setContentDisposition(pOriginalDeposit.getContentDisposition());
		super.setContentType(pOriginalDeposit.getContentType());
		super.setContentLength(pOriginalDeposit.getContentLength());
		super.setUsername(pOriginalDeposit.getUsername());
		super.setPassword(pOriginalDeposit.getPassword());
		super.setOnBehalfOf(pOriginalDeposit.getOnBehalfOf());
		super.setSlug(pOriginalDeposit.getSlug());
		super.setMd5(pOriginalDeposit.getMd5());
		super.setVerbose(pOriginalDeposit.isVerbose());
		super.setNoOp(pOriginalDeposit.isNoOp());
		super.setPackaging(pOriginalDeposit.getPackaging());
		super.setDepositID(pOriginalDeposit.getDepositID());
		super.setIPAddress(pOriginalDeposit.getIPAddress());
		super.setLocation(pOriginalDeposit.getLocation());

		this.setCollectionPid(pCollectionPid);
	}

	public String getCollectionPid() {
		return _collectionPid;
	}

	public void setCollectionPid(final String pCollectionPid) {
		_collectionPid = pCollectionPid;
	}
}
