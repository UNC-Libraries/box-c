package org.purl.sword.server.fedora.fileHandlers;

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
  * This is the catch all for files which are not recognised by the other 
  * file handlers. You should extends this class if you are writing new 
  * file handlers. It allows you to only extend the methods which you need to.
  */

import org.apache.log4j.Logger;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.Collection;
import org.purl.sword.base.SWORDException;

import org.purl.sword.atom.Source;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.Author;
import org.purl.sword.atom.Contributor;
import org.purl.sword.atom.Rights;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.atom.InvalidMediaTypeException;

import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.baseExtensions.XMLServiceDocument;

import org.purl.sword.server.fedora.utils.XMLProperties;

import org.purl.sword.server.fedora.FedoraServer;

import org.purl.sword.server.fedora.fedoraObjects.FedoraObject;
import org.purl.sword.server.fedora.fedoraObjects.DublinCore;
import org.purl.sword.server.fedora.fedoraObjects.Relationship;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.Disseminator;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;
import org.purl.sword.server.fedora.fedoraObjects.Property;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.TimeZone;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DefaultFileHandler implements FileHandler {
	private static final Logger LOG = Logger.getLogger(DefaultFileHandler.class);
	/** The mime type of the deposit */
	protected String _contentType = "";
	/** The packaging of the deposit */
	protected String _packaging = "";
	/** Access to the properties file */
	protected XMLProperties _props = null;

	/**
	 * Call this from child classes as it initates the Properties file, the content type and packaging
	 * @param String the mime type of the deposit
	 * @param String the packaging 
	 */ 
	public DefaultFileHandler(final String pContentType, final String pPackaging) {
		_props = new XMLProperties();
		this.setContentType(pContentType);
		this.setPackaging(pPackaging);
	}

	/**
	 * This decides whether the File handler can handle the current deposit. Child classes 
	 * must override this method
	 * @param String the mime type
	 * @param String packaging
	 * @return boolean if this handler can handle the current deposit
	 */
	public boolean isHandled(final String pContentType, final String pPackaging) {
		return true; // catch all for deposits so can handle anything
	}

	/**
	 * This is the main method that is called to ingest a deposit. Override this if
	 * you want complete control over the ingest. This method calls all the other methods.
	 * @param DepositCollection the deposit and its associated collection
	 * @param ServiceDocument the service document which this request applies to
	 * @throws SWORDException if any problems occured during ingest
	 */
	public SWORDEntry ingestDepost(final DepositCollection pDeposit, final ServiceDocument pServiceDocument) throws SWORDException {

		FedoraObject tNewObject = new FedoraObject(pDeposit.getUsername(), pDeposit.getPassword());

		tNewObject.setIdentifiers(this.getIdentifiers(pDeposit));
		tNewObject.setDC(this.getDublinCore(pDeposit));
		tNewObject.setRelationships(this.getRelationships(pDeposit));
		try {
			List<Datastream> tDatastreamList = this.getDatastreams(pDeposit);
			this.ensureValidDSIds(tDatastreamList);
			tNewObject.setDatastreams(tDatastreamList);
		} catch (IOException tIOExcpt) {
			tIOExcpt.printStackTrace();
			LOG.debug("Excpetion");
			LOG.error("Couldn't access uploaded file" + tIOExcpt.toString());
			throw new SWORDException("Couldn't access uploaded file", tIOExcpt);
		}
		tNewObject.setDisseminators(this.getDisseminators(pDeposit, tNewObject.getDatastreams()));

		if (!pDeposit.isNoOp()){ // Don't ingest if no op is set
			tNewObject.ingest();
		}
		
		return this.getSWORDEntry(pDeposit, pServiceDocument, tNewObject);
	}

	/** 
	 * This returns the Properties associated with the fedora object
	 * some of these properties are manditory so it is a good idea to call this method
	 * from any method that overrides it.
	 * @param DepositCollection the deposit and its associated collection
	 * @return List<Property> a list of properties for this object
	 */
	protected List<Property> getIdentifiers(final DepositCollection pDeposit) {
		List<Property> tIdentifiers = new ArrayList<Property>();

		tIdentifiers.add(new Property("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "FedoraObject"));
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#state", "Active"));
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#label", "Object created through the SWORD deposit system"));
		if (pDeposit.getOnBehalfOf() != null) {
			tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#ownerId", pDeposit.getOnBehalfOf()));
		} else {
			tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#ownerId", pDeposit.getUsername()));
		}
			
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/model#createdDate", this.getCurrDateAsFedora()));
		tIdentifiers.add(new Property("info:fedora/fedora-system:def/view#lastModifiedDate", this.getCurrDateAsFedora()));
		if (pDeposit.getSlug() != null) {
			tIdentifiers.add(new Property("org.purl.sword.slug", pDeposit.getSlug(), Property.TYPE.EXTERNAL));
		}

		return tIdentifiers;
	}

	/**
	 * This returns the dublin core from the deposit. This method guesses at 
	 * dublin core values so you don't need to call it from child classes if you
	 * override it. All fedora objects have a dublin core record.
	 * @param DepositCollection the deposit and its associated collection
	 * @return DublinCore the dublin core from the deposit
	 */
	protected DublinCore getDublinCore(final DepositCollection pDeposit) {
		DublinCore tDC = new DublinCore();
		
		tDC.getTitle().add("Uploaded by the JISC funded SWORD project");

		if (pDeposit.getSlug() != null) {
			tDC.getIdentifier().add(pDeposit.getSlug());
		}

		if (pDeposit.getOnBehalfOf() != null) {
			tDC.getCreator().add(pDeposit.getOnBehalfOf());
		} else {
			tDC.getCreator().add(pDeposit.getUsername());
		}

		tDC.getFormat().add(pDeposit.getContentType());

		return tDC;
	}

	/**
	 * This returns the relationships for a deposit. This implementation
	 * assigns it to the collection specified in the service document.
	 *
	 * @param DepositCollection the deposit and its associated collection
	 * @return Relationship the relationships
	 */
	protected Relationship getRelationships(final DepositCollection pDeposit) {
		Relationship tRelation = new Relationship();
		tRelation.add("isMemberOf", pDeposit.getCollectionPid());

		return tRelation;
	}

	/**
	 * This method ensures the datastream names are unique. If a duplicate is found then it will append
	 * a number to it to make it unique. 
	 * 
	 * @param List<Datastream> the list of datastreams to be added
	 */
	protected void ensureValidDSIds(final List<Datastream> pDatastreamList) {
		Map<String,Integer> tDatastreamNames = new HashMap<String,Integer>(pDatastreamList.size());

		for (Datastream tDatastream : pDatastreamList) {
			LOG.debug("Checking " + tDatastream.getId());
			tDatastream.setId(this.getValidFileName(tDatastream.getId()));
	
			if (tDatastreamNames.get(tDatastream.getId()) == null) {
				tDatastreamNames.put(tDatastream.getId(), 1);
			} else {
				tDatastreamNames.put(tDatastream.getId(), tDatastreamNames.get(tDatastream.getId()) + 1);
				tDatastream.setId(tDatastream.getId() + "-" + tDatastreamNames.get(tDatastream.getId()));
			}
		}
	}

	/**
	 * This is the method that is most commonly overridden to provide new file handlers. Ensure you remove temp
	 * files unless you use LocalDatastream which cleans up after its self.
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @return List<Datastream> a list of datastreams to add
	 * @throws IOException if can't access a datastream
	 * @throws SWORDException if there are any other problems
	 */
	protected List<Datastream> getDatastreams(final DepositCollection pDeposit) throws IOException, SWORDException {
		LOG.debug("copying file");

		String tTempFileName = this.getTempDir() + "uploaded-file.tmp";
		IOUtils.copy(pDeposit.getFile(), new FileOutputStream(tTempFileName));
		Datastream tDatastream = new LocalDatastream(this.getGenericFileName(pDeposit), this.getContentType(), tTempFileName);
	
		List<Datastream> tDatastreams = new ArrayList<Datastream>();
		tDatastreams.add(tDatastream);

		return tDatastreams;
	}

	/**
	 * Override this method if you want to add disseminators to an object. This implementation does not add any disseminators.
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param List<Datastream> a list of the datastreams for the object
	 */
	protected List<Disseminator> getDisseminators(final DepositCollection pDeposit, final List<Datastream> pDatastream) {
		LOG.debug("No disseminators are added");
		return new ArrayList<Disseminator>();
	}

	/** 
	 * This method is the general method that converts the service document and deposit into a SWORD entry. This is the overall method
	 * so if you want complete control on how the SWORDEntry is created overried this method otherwise overside the other SWORD Entry methods.
    * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param ServiceDocument the service document associated with this request
	 * @param FedoraObject the object that has been ingested
	 */ 
	protected SWORDEntry getSWORDEntry(final DepositCollection pDeposit, final ServiceDocument pServiceDocument, final FedoraObject pFedoraObj) throws SWORDException {
		SWORDEntry tEntry = new SWORDEntry();

		this.addServiceDocEntries(tEntry, ((XMLServiceDocument)pServiceDocument).getCollection(pDeposit.getCollectionPid()));
		this.addDepositEntries(tEntry, pDeposit);
		this.addIngestEntries(pDeposit, tEntry, pFedoraObj);
		this.addDCEntries(tEntry, pFedoraObj.getDC());
				
		tEntry.setPublished(this.getCurrDateAsAtom());
		tEntry.setUpdated(this.getCurrDateAsAtom());

		//Source tSource = new Source();
		Generator tGenerator = new Generator();
		//tSource.setGenerator(tGenerator);
		tGenerator.setUri(_props.getRepositoryUri());
		tGenerator.setVersion(FedoraServer.VERSION);
		//tEntry.setSource(tSource);
		tEntry.setGenerator(tGenerator);
		if (pDeposit.isVerbose()) {
			tEntry.setVerboseDescription("Your deposit was added to the repository with identifier " + pFedoraObj.getPid() + ". Thank you for depositing.");
		}	

		return tEntry;
	}

	/**
	 * This just sets the treatment entry from the Collection found in the service document
	 * @param SWORDEntry the entry to add the treatment value to
	 * @param Collection the service document collection
	 */ 
	protected void addServiceDocEntries(final SWORDEntry pEntry, final Collection pCollection) {
		pEntry.setTreatment(pCollection.getTreatment());
	}
	
	/** 
	 * This just sets the no op option and Packaging from the deposit.
	 * @param SWORDEntry the entry to add the no op value to
	 * @param DepositCollection the deposit object
	 */
	protected void addDepositEntries(final SWORDEntry pEntry, final DepositCollection pDeposit) {
		if (this.getPackaging() != null && this.getPackaging().trim().length() != 0) {
			pEntry.setPackaging(this.getPackaging());
		}	
		pEntry.setNoOp(pDeposit.isNoOp());
		
		Author tAuthor = new Author();
		tAuthor.setName(pDeposit.getUsername());
		pEntry.addAuthors(tAuthor);
		if (pDeposit.getOnBehalfOf() != null) {
			Contributor tContributor = new Contributor();
			tContributor.setName(pDeposit.getOnBehalfOf());
			pEntry.addContributor(tContributor);
		}
	}

	/**
	 * This methods adds entries about the ingest e.g. The URL to the deposit and links. The entries added here are Content and Link.
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param SWORDEntry the entry to add the values to
	 * @param FedoraObject the ignested object
	 * @throws SWORDException if there is a problem accessing the properties file to find the URL to the deposit
	 */
	protected void addIngestEntries(final DepositCollection pDeposit, final SWORDEntry pEntry, final FedoraObject pFedoraObj) throws SWORDException {
		Content tContent = new Content();
		try {
			tContent.setType(this.getContentType());
		} catch (InvalidMediaTypeException tInvalidMedia) {
			LOG.error("Invalid media type '" + this.getContentType() + "':" + tInvalidMedia.toString());
		}
		tContent.setSource(pFedoraObj.getURLToDS(this.getLinkName(pDeposit)));
		pEntry.setContent(tContent);

		pEntry.setId(pFedoraObj.getPid());

		// Upload link
		//Link tEditMedia = new Link();
		//tEditMedia.setHref(pFedoraObj.getURLToDS(this.getLinkName(pDeposit)));
		//tEditMedia.setHreflang("en");
		//tEditMedia.setRel("edit-media");
		//pEntry.addLink(tEditMedia);

		Link tEdit = new Link();
		tEdit.setHref(_props.getRepositoryUri() + "/" + pDeposit.getCollectionPid() + "/" + pFedoraObj.getPid());
		tEdit.setHreflang("en");
		tEdit.setRel("edit");

		pEntry.addLink(tEdit);

	}

	/** 
	 * This specifies the datastream name for the object that was deposited. For example if a zip
	 * file was deposited the this would return the datastream that contains the zip file in Fedora not 
	 * a link to the files that were in the zip file. This is used for creating the links.
	 *
	 * @param DepositCollection the deposit
	 * @return String the datastream name for the actual file deposited
	 */
	protected String getLinkName(final DepositCollection pDeposit) {
		return this.getGenericFileName(pDeposit);
	}

	/** 
	 * This method tries to copy some of the dublin core elmenets into the SWORD entry object. The following
	 * entries are added; Author, Contributor, Rights, Summary and Title.
    *
	 * @param SWORDEntry the entry to add to
	 * @param DublinCore the dublin core datastream to retrieve the data from
	 */
	protected void addDCEntries(final SWORDEntry pEntry, final DublinCore pDC) {
		/*Iterator<String> tAuthorsIter = pDC.getCreator().iterator();
		Author tAuthor = null;
		while (tAuthorsIter.hasNext()) {
			tAuthor = new Author();
			tAuthor.setName(tAuthorsIter.next());
			pEntry.addAuthors(tAuthor);
		}*/

		Iterator<String> tCategoryIter = pDC.getSubject().iterator();
		while (tCategoryIter.hasNext()) {
			pEntry.addCategory(tCategoryIter.next());
		}

		// This includes the on behalf of
		/*Iterator<String> tContributorsIter = pDC.getContributor().iterator();
		Contributor tContributor = null;
		while (tContributorsIter.hasNext()) {
			tContributor = new Contributor();
			tContributor.setName(tContributorsIter.next());
			pEntry.addContributor(tContributor);
		}*/

		Iterator<String> tRightsIter = pDC.getRights().iterator();
		StringBuffer tRightsStr = new StringBuffer();
		while (tRightsIter.hasNext()) {
			tRightsStr.append(tRightsIter.next());
		}
		Rights tRights = new Rights();
		tRights.setContent(tRightsStr.toString());
		pEntry.setRights(tRights);

		Iterator<String> tDescriptionIter = pDC.getDescription().iterator();
		StringBuffer tDescriptionStr = new StringBuffer();
		while (tDescriptionIter.hasNext()) {
			tDescriptionStr.append(tDescriptionIter.next());
		}
		Summary tSummary = new Summary();
		tSummary.setContent(tDescriptionStr.toString());
		pEntry.setSummary(tSummary);


		Iterator<String> tTitleIter = pDC.getTitle().iterator();
		StringBuffer tTitleStr = new StringBuffer();
		while (tTitleIter.hasNext()) {
			tTitleStr.append(tTitleIter.next());
		}
		Title tTitle = new Title();
		tTitle.setContent(tTitleStr.toString());
		pEntry.setTitle(tTitle);


	}

	/** 
	 * Utility method to convert the current date to a date Fedora understands
	 * @return String the date in Fedora's requried format
	 */
	protected String getCurrDateAsFedora() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(new Date());
	}

	/** 
	 * Utility method to convert the current date to a date from the Atom spec
	 * @return String the date in Atom's requried format
	 */
	protected String getCurrDateAsAtom() {
       DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(new Date());
	}
	
	/**
	 * Get _contentType.
	 *
	 * @return _contentType as String.
	 */
	public String getContentType() {
		LOG.debug("Returning " + _contentType);
	    return _contentType;
	}
	
	/**
	 * Set _contentType.
	 *
	 * @param _contentType the value to set.
	 */
	public void setContentType(final String pContentType) {
	     _contentType = pContentType;
	}
	
	/**
	 * Get _packaging.
	 *
	 * @return _packaging as String.
	 */
	public String getPackaging() {
	    return _packaging;
	}
	
	/**
	 * Set _packaging.
	 *
	 * @param _packaging the value to set.
	 */
	public void setPackaging(final String pPackaging) {
	     _packaging = pPackaging;
	}

	/** 
	 * Returns the directory where deposited files can be stored before upload to fedora
	 * 
	 * @return String the tempory directory
	 * @throws SWORDException if there is a problem retrieving the temp dir value from the properties file
	 */
	public String getTempDir() throws SWORDException {
		String tTempDir = _props.getTempDir(); 
		if (!tTempDir.endsWith(System.getProperty("file.separator"))) {
			tTempDir += System.getProperty("file.separator");
		}
		return tTempDir;
	}

	/**
	 * If the file name is unknown this method tries to get it from the deposit object getFilename method
	 * but if this is null it sets it to uploaded
	 *
	 * @param DepositCollection the deposit and its associated collection
	 * @param String the datastream name to use for the deposit.
	 */
	protected String getGenericFileName(final DepositCollection pDeposit) {
		LOG.debug("Filename '" + pDeposit.getFilename() + "'");
		String tFilename = "";
		if (pDeposit.getFilename() == null && pDeposit.getContentDisposition() == null) {
			tFilename = "upload";
		} else {
			if (pDeposit.getFilename() != null) {
				tFilename = this.getValidFileName(pDeposit.getFilename());
			} else {	
				tFilename = this.getValidFileName(pDeposit.getContentDisposition());
			}	
		}
		return tFilename;
	}

	// Needs to return not just a valid file name but specifically an "NCName" (a "non-colonized XML name")	in order to produce valid FoxML
	protected String getValidFileName(final String pName) {
		String tFilename;
		StringBuffer tFilenameBuffer = new StringBuffer();
		char initial = pName.charAt(0);
		if ( !(initial == '_') && !isLetter(initial) ) {
			// initial does not start with either an underscore or a letter (as it must), so prepend a valid prefix.
			LOG.debug("datastream name must begin with a letter or _");
			tFilenameBuffer.append("Uploaded-");
		}
		tFilenameBuffer.append(initial);
		for (int i=1; i < pName.length(); i++) {
			char c = pName.charAt(i);
			if (isNCNameChar(c)) {
				tFilenameBuffer.append(c);
			}
		}

		if (tFilenameBuffer.indexOf(".") == -1) {
			// Filename contains no extension so return it unchanged
			tFilename = tFilenameBuffer.toString();
		} else {
			// Remove extension
			tFilename = tFilenameBuffer.substring(0, tFilenameBuffer.indexOf("."));
		}	

		LOG.debug("Replacing filename: " + pName + " with " + tFilename);
		return tFilename;
	}
	
	/**
	 * Determine if a character is an <a href="http://www.w3.org/TR/1999/REC-xml-names-19990114/#NT-NCNameChar">"NCNameChar"</a>
	 *
	 *@param  c  The putative NCNameChar
	 *@return   Whether c is an NCNameChar
	 */
	private static boolean isNCNameChar(char c) {
		return
			isLetter(c) ||
			isDigit(c) ||
			c == '.' ||
			c == '-' ||
			c == '_' ||
			isCombiningChar(c) ||
			isExtender(c);
	}
	
	/**
	 * Determine if a character is a <a href="http://www.w3.org/TR/REC-xml/#NT-Letter">"Letter"</a>
	 *
	 *@param  c  The putative Letter
	 *@return   Whether c is a Letter
	 */
	private static boolean isLetter(char c) {
		return	
			isBaseChar(c) || isIdeographic(c);
	}
	
	/**
	 * Determine if a character is a <a href="http://www.w3.org/TR/REC-xml/#NT-BaseChar">"BaseChar"</a>
	 *
	 *@param  c  The putative BaseChar
	 *@return   Whether c is a BaseChar
	 */
	private static boolean isBaseChar(char c) {
		return
				charIn(c, 0x0041, 0x005A) ||
				charIn(c, 0x0061, 0x007A) ||
				charIn(c, 0x00C0, 0x00D6) ||
				charIn(c, 0x00D8, 0x00F6) ||
				charIn(c, 0x00F8, 0x00FF) ||
				charIn(c, 0x0100, 0x0131) ||
				charIn(c, 0x0134, 0x013E) ||
				charIn(c, 0x0141, 0x0148) ||
				charIn(c, 0x014A, 0x017E) ||
				charIn(c, 0x0180, 0x01C3) ||
				charIn(c, 0x01CD, 0x01F0) ||
				charIn(c, 0x01F4, 0x01F5) ||
				charIn(c, 0x01FA, 0x0217) ||
				charIn(c, 0x0250, 0x02A8) ||
				charIn(c, 0x02BB, 0x02C1) ||
				c == 0x0386 ||
				charIn(c, 0x0388, 0x038A) ||
				c == 0x038C ||
				charIn(c, 0x038E, 0x03A1) ||
				charIn(c, 0x03A3, 0x03CE) ||
				charIn(c, 0x03D0, 0x03D6) ||
				c == 0x03DA ||
				c == 0x03DC ||
				c == 0x03DE ||
				c == 0x03E0 ||
				charIn(c, 0x03E2, 0x03F3) ||
				charIn(c, 0x0401, 0x040C) ||
				charIn(c, 0x040E, 0x044F) ||
				charIn(c, 0x0451, 0x045C) ||
				charIn(c, 0x045E, 0x0481) ||
				charIn(c, 0x0490, 0x04C4) ||
				charIn(c, 0x04C7, 0x04C8) ||
				charIn(c, 0x04CB, 0x04CC) ||
				charIn(c, 0x04D0, 0x04EB) ||
				charIn(c, 0x04EE, 0x04F5) ||
				charIn(c, 0x04F8, 0x04F9) ||
				charIn(c, 0x0531, 0x0556) ||
				c == 0x0559 ||
				charIn(c, 0x0561, 0x0586) ||
				charIn(c, 0x05D0, 0x05EA) ||
				charIn(c, 0x05F0, 0x05F2) ||
				charIn(c, 0x0621, 0x063A) ||
				charIn(c, 0x0641, 0x064A) ||
				charIn(c, 0x0671, 0x06B7) ||
				charIn(c, 0x06BA, 0x06BE) ||
				charIn(c, 0x06C0, 0x06CE) ||
				charIn(c, 0x06D0, 0x06D3) ||
				c == 0x06D5 ||
				charIn(c, 0x06E5, 0x06E6) ||
				charIn(c, 0x0905, 0x0939) ||
				c == 0x093D ||
				charIn(c, 0x0958, 0x0961) ||
				charIn(c, 0x0985, 0x098C) ||
				charIn(c, 0x098F, 0x0990) ||
				charIn(c, 0x0993, 0x09A8) ||
				charIn(c, 0x09AA, 0x09B0) ||
				c == 0x09B2 ||
				charIn(c, 0x09B6, 0x09B9) ||
				charIn(c, 0x09DC, 0x09DD) ||
				charIn(c, 0x09DF, 0x09E1) ||
				charIn(c, 0x09F0, 0x09F1) ||
				charIn(c, 0x0A05, 0x0A0A) ||
				charIn(c, 0x0A0F, 0x0A10) ||
				charIn(c, 0x0A13, 0x0A28) ||
				charIn(c, 0x0A2A, 0x0A30) ||
				charIn(c, 0x0A32, 0x0A33) ||
				charIn(c, 0x0A35, 0x0A36) ||
				charIn(c, 0x0A38, 0x0A39) ||
				charIn(c, 0x0A59, 0x0A5C) ||
				c == 0x0A5E ||
				charIn(c, 0x0A72, 0x0A74) ||
				charIn(c, 0x0A85, 0x0A8B) ||
				c == 0x0A8D ||
				charIn(c, 0x0A8F, 0x0A91) ||
				charIn(c, 0x0A93, 0x0AA8) ||
				charIn(c, 0x0AAA, 0x0AB0) ||
				charIn(c, 0x0AB2, 0x0AB3) ||
				charIn(c, 0x0AB5, 0x0AB9) ||
				c == 0x0ABD ||
				c == 0x0AE0 ||
				charIn(c, 0x0B05, 0x0B0C) ||
				charIn(c, 0x0B0F, 0x0B10) ||
				charIn(c, 0x0B13, 0x0B28) ||
				charIn(c, 0x0B2A, 0x0B30) ||
				charIn(c, 0x0B32, 0x0B33) ||
				charIn(c, 0x0B36, 0x0B39) ||
				c == 0x0B3D ||
				charIn(c, 0x0B5C, 0x0B5D) ||
				charIn(c, 0x0B5F, 0x0B61) ||
				charIn(c, 0x0B85, 0x0B8A) ||
				charIn(c, 0x0B8E, 0x0B90) ||
				charIn(c, 0x0B92, 0x0B95) ||
				charIn(c, 0x0B99, 0x0B9A) ||
				c == 0x0B9C ||
				charIn(c, 0x0B9E, 0x0B9F) ||
				charIn(c, 0x0BA3, 0x0BA4) ||
				charIn(c, 0x0BA8, 0x0BAA) ||
				charIn(c, 0x0BAE, 0x0BB5) ||
				charIn(c, 0x0BB7, 0x0BB9) ||
				charIn(c, 0x0C05, 0x0C0C) ||
				charIn(c, 0x0C0E, 0x0C10) ||
				charIn(c, 0x0C12, 0x0C28) ||
				charIn(c, 0x0C2A, 0x0C33) ||
				charIn(c, 0x0C35, 0x0C39) ||
				charIn(c, 0x0C60, 0x0C61) ||
				charIn(c, 0x0C85, 0x0C8C) ||
				charIn(c, 0x0C8E, 0x0C90) ||
				charIn(c, 0x0C92, 0x0CA8) ||
				charIn(c, 0x0CAA, 0x0CB3) ||
				charIn(c, 0x0CB5, 0x0CB9) ||
				c == 0x0CDE ||
				charIn(c, 0x0CE0, 0x0CE1) ||
				charIn(c, 0x0D05, 0x0D0C) ||
				charIn(c, 0x0D0E, 0x0D10) ||
				charIn(c, 0x0D12, 0x0D28) ||
				charIn(c, 0x0D2A, 0x0D39) ||
				charIn(c, 0x0D60, 0x0D61) ||
				charIn(c, 0x0E01, 0x0E2E) ||
				c == 0x0E30 ||
				charIn(c, 0x0E32, 0x0E33) ||
				charIn(c, 0x0E40, 0x0E45) ||
				charIn(c, 0x0E81, 0x0E82) ||
				c == 0x0E84 ||
				charIn(c, 0x0E87, 0x0E88) ||
				c == 0x0E8A ||
				c == 0x0E8D ||
				charIn(c, 0x0E94, 0x0E97) ||
				charIn(c, 0x0E99, 0x0E9F) ||
				charIn(c, 0x0EA1, 0x0EA3) ||
				c == 0x0EA5 ||
				c == 0x0EA7 ||
				charIn(c, 0x0EAA, 0x0EAB) ||
				charIn(c, 0x0EAD, 0x0EAE) ||
				c == 0x0EB0 ||
				charIn(c, 0x0EB2, 0x0EB3) ||
				c == 0x0EBD ||
				charIn(c, 0x0EC0, 0x0EC4) ||
				charIn(c, 0x0F40, 0x0F47) ||
				charIn(c, 0x0F49, 0x0F69) ||
				charIn(c, 0x10A0, 0x10C5) ||
				charIn(c, 0x10D0, 0x10F6) ||
				c == 0x1100 ||
				charIn(c, 0x1102, 0x1103) ||
				charIn(c, 0x1105, 0x1107) ||
				c == 0x1109 ||
				charIn(c, 0x110B, 0x110C) ||
				charIn(c, 0x110E, 0x1112) ||
				c == 0x113C ||
				c == 0x113E ||
				c == 0x1140 ||
				c == 0x114C ||
				c == 0x114E ||
				c == 0x1150 ||
				charIn(c, 0x1154, 0x1155) ||
				c == 0x1159 ||
				charIn(c, 0x115F, 0x1161) ||
				c == 0x1163 ||
				c == 0x1165 ||
				c == 0x1167 ||
				c == 0x1169 ||
				charIn(c, 0x116D, 0x116E) ||
				charIn(c, 0x1172, 0x1173) ||
				c == 0x1175 ||
				c == 0x119E ||
				c == 0x11A8 ||
				c == 0x11AB ||
				charIn(c, 0x11AE, 0x11AF) ||
				charIn(c, 0x11B7, 0x11B8) ||
				c == 0x11BA ||
				charIn(c, 0x11BC, 0x11C2) ||
				c == 0x11EB ||
				c == 0x11F0 ||
				c == 0x11F9 ||
				charIn(c, 0x1E00, 0x1E9B) ||
				charIn(c, 0x1EA0, 0x1EF9) ||
				charIn(c, 0x1F00, 0x1F15) ||
				charIn(c, 0x1F18, 0x1F1D) ||
				charIn(c, 0x1F20, 0x1F45) ||
				charIn(c, 0x1F48, 0x1F4D) ||
				charIn(c, 0x1F50, 0x1F57) ||
				c == 0x1F59 ||
				c == 0x1F5B ||
				c == 0x1F5D ||
				charIn(c, 0x1F5F, 0x1F7D) ||
				charIn(c, 0x1F80, 0x1FB4) ||
				charIn(c, 0x1FB6, 0x1FBC) ||
				c == 0x1FBE ||
				charIn(c, 0x1FC2, 0x1FC4) ||
				charIn(c, 0x1FC6, 0x1FCC) ||
				charIn(c, 0x1FD0, 0x1FD3) ||
				charIn(c, 0x1FD6, 0x1FDB) ||
				charIn(c, 0x1FE0, 0x1FEC) ||
				charIn(c, 0x1FF2, 0x1FF4) ||
				charIn(c, 0x1FF6, 0x1FFC) ||
				c == 0x2126 ||
				charIn(c, 0x212A, 0x212B) ||
				c == 0x212E ||
				charIn(c, 0x2180, 0x2182) ||
				charIn(c, 0x3041, 0x3094) ||
				charIn(c, 0x30A1, 0x30FA) ||
				charIn(c, 0x3105, 0x312C) ||
				charIn(c, 0xAC00, 0xD7A3);
	}

	/**
	 * Determine if a character is <a href="http://www.w3.org/TR/REC-xml/#NT-BaseChar">Ideographic</a>
	 *
	 *@param  c  The putative ideographic character
	 *@return  Whether c is ideographic
	 */
	private static boolean isIdeographic(char c) {
		return
				charIn(c, 0x4E00, 0x9FA5) ||
				c == 0x3007 ||
				charIn(c, 0x3021, 0x3029);
	}

	/**
	 *  Determine if a character is a <a href="http://www.w3.org/TR/REC-xml/#NT-CombiningChar">"CombiningChar"</a>
	 *
	 *@param  c  The putative CombiningChar
	 *@return  Whether the character is a CombiningChar
	 */
	private static boolean isCombiningChar(char c) {
		return
				charIn(c, 0x0300, 0x0345) ||
				charIn(c, 0x0360, 0x0361) ||
				charIn(c, 0x0483, 0x0486) ||
				charIn(c, 0x0591, 0x05A1) ||
				charIn(c, 0x05A3, 0x05B9) ||
				charIn(c, 0x05BB, 0x05BD) ||
				c == 0x05BF ||
				charIn(c, 0x05C1, 0x05C2) ||
				c == 0x05C4 ||
				charIn(c, 0x064B, 0x0652) ||
				c == 0x0670 ||
				charIn(c, 0x06D6, 0x06DC) ||
				charIn(c, 0x06DD, 0x06DF) ||
				charIn(c, 0x06E0, 0x06E4) ||
				charIn(c, 0x06E7, 0x06E8) ||
				charIn(c, 0x06EA, 0x06ED) ||
				charIn(c, 0x0901, 0x0903) ||
				c == 0x093C ||
				charIn(c, 0x093E, 0x094C) ||
				c == 0x094D ||
				charIn(c, 0x0951, 0x0954) ||
				charIn(c, 0x0962, 0x0963) ||
				charIn(c, 0x0981, 0x0983) ||
				c == 0x09BC ||
				c == 0x09BE ||
				c == 0x09BF ||
				charIn(c, 0x09C0, 0x09C4) ||
				charIn(c, 0x09C7, 0x09C8) ||
				charIn(c, 0x09CB, 0x09CD) ||
				c == 0x09D7 ||
				charIn(c, 0x09E2, 0x09E3) ||
				c == 0x0A02 ||
				c == 0x0A3C ||
				c == 0x0A3E ||
				c == 0x0A3F ||
				charIn(c, 0x0A40, 0x0A42) ||
				charIn(c, 0x0A47, 0x0A48) ||
				charIn(c, 0x0A4B, 0x0A4D) ||
				charIn(c, 0x0A70, 0x0A71) ||
				charIn(c, 0x0A81, 0x0A83) ||
				c == 0x0ABC ||
				charIn(c, 0x0ABE, 0x0AC5) ||
				charIn(c, 0x0AC7, 0x0AC9) ||
				charIn(c, 0x0ACB, 0x0ACD) ||
				charIn(c, 0x0B01, 0x0B03) ||
				c == 0x0B3C ||
				charIn(c, 0x0B3E, 0x0B43) ||
				charIn(c, 0x0B47, 0x0B48) ||
				charIn(c, 0x0B4B, 0x0B4D) ||
				charIn(c, 0x0B56, 0x0B57) ||
				charIn(c, 0x0B82, 0x0B83) ||
				charIn(c, 0x0BBE, 0x0BC2) ||
				charIn(c, 0x0BC6, 0x0BC8) ||
				charIn(c, 0x0BCA, 0x0BCD) ||
				c == 0x0BD7 ||
				charIn(c, 0x0C01, 0x0C03) ||
				charIn(c, 0x0C3E, 0x0C44) ||
				charIn(c, 0x0C46, 0x0C48) ||
				charIn(c, 0x0C4A, 0x0C4D) ||
				charIn(c, 0x0C55, 0x0C56) ||
				charIn(c, 0x0C82, 0x0C83) ||
				charIn(c, 0x0CBE, 0x0CC4) ||
				charIn(c, 0x0CC6, 0x0CC8) ||
				charIn(c, 0x0CCA, 0x0CCD) ||
				charIn(c, 0x0CD5, 0x0CD6) ||
				charIn(c, 0x0D02, 0x0D03) ||
				charIn(c, 0x0D3E, 0x0D43) ||
				charIn(c, 0x0D46, 0x0D48) ||
				charIn(c, 0x0D4A, 0x0D4D) ||
				c == 0x0D57 ||
				c == 0x0E31 ||
				charIn(c, 0x0E34, 0x0E3A) ||
				charIn(c, 0x0E47, 0x0E4E) ||
				c == 0x0EB1 ||
				charIn(c, 0x0EB4, 0x0EB9) ||
				charIn(c, 0x0EBB, 0x0EBC) ||
				charIn(c, 0x0EC8, 0x0ECD) ||
				charIn(c, 0x0F18, 0x0F19) ||
				c == 0x0F35 ||
				c == 0x0F37 ||
				c == 0x0F39 ||
				c == 0x0F3E ||
				c == 0x0F3F ||
				charIn(c, 0x0F71, 0x0F84) ||
				charIn(c, 0x0F86, 0x0F8B) ||
				charIn(c, 0x0F90, 0x0F95) ||
				c == 0x0F97 ||
				charIn(c, 0x0F99, 0x0FAD) ||
				charIn(c, 0x0FB1, 0x0FB7) ||
				c == 0x0FB9 ||
				charIn(c, 0x20D0, 0x20DC) ||
				c == 0x20E1 ||
				charIn(c, 0x302A, 0x302F) ||
				c == 0x3099 ||
				c == 0x309A;
	}

	/**
	 *  Determine if a character is a digit
	 *
	 *@param  c  The putative digit
	 *@return  Whether the character is a digit
	 */
	private static boolean isDigit(char c) {
		return
				charIn(c, 0x0030, 0x0039) ||
				charIn(c, 0x0660, 0x0669) ||
				charIn(c, 0x06F0, 0x06F9) ||
				charIn(c, 0x0966, 0x096F) ||
				charIn(c, 0x09E6, 0x09EF) ||
				charIn(c, 0x0A66, 0x0A6F) ||
				charIn(c, 0x0AE6, 0x0AEF) ||
				charIn(c, 0x0B66, 0x0B6F) ||
				charIn(c, 0x0BE7, 0x0BEF) ||
				charIn(c, 0x0C66, 0x0C6F) ||
				charIn(c, 0x0CE6, 0x0CEF) ||
				charIn(c, 0x0D66, 0x0D6F) ||
				charIn(c, 0x0E50, 0x0E59) ||
				charIn(c, 0x0ED0, 0x0ED9) ||
				charIn(c, 0x0F20, 0x0F29);
	}

	/**
	 *  Determine if a character is an <a href="http://www.w3.org/TR/REC-xml/#NT-Extender">"Extender"</a>
	 *
	 *@param  c  The putative extender character
	 *@return  Whether the character is an extender
	 */
	private static boolean isExtender(char c) {
		return
				c == 0x00B7 ||
				c == 0x02D0 ||
				c == 0x02D1 ||
				c == 0x0387 ||
				c == 0x0640 ||
				c == 0x0E46 ||
				c == 0x0EC6 ||
				c == 0x3005 ||
				charIn(c, 0x3031, 0x3035) ||
				charIn(c, 0x309D, 0x309E) ||
				charIn(c, 0x30FC, 0x30FE);
	}
	
	/**
	 *  Determine if a character falls within a range of unicode values
	 *
	 *@param  c  The character which may fall into the range
	 *@param  start  The unicode codepoint of the first character of the range
	 *@param  end  The unicode codepoint of the last character of the range
	 *@return  Whether the character is in the range
	 */
	private static boolean charIn(char c, int start, int end) {	
		return c >= start && c <= end;
	}
}
