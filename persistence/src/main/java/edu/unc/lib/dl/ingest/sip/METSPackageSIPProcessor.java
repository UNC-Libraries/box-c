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
package edu.unc.lib.dl.ingest.sip;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPException;
import edu.unc.lib.dl.ingest.aip.AIPImpl;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class METSPackageSIPProcessor implements SIPProcessor {
	private static final Log log = LogFactory.getLog(METSPackageSIPProcessor.class);
	private static final String schemaPackage = "/schemas/";
	private XPath _countObjectsXpath;
	private static final Namespace METS_NS = Namespace.getNamespace("m", JDOMNamespaceUtil.METS_NS.getURI());
	private final String countObjectsXpath = "count(/m:mets/m:structMap/descendant::m:div)";
	private Templates mets2fox = null;
	private METSPackageFileValidator metsPackageFileValidator = null;
	private edu.unc.lib.dl.pidgen.PIDGenerator pidGenerator = null;
	private SchematronValidator schematronValidator = null;
	private final String stylesheetPackage = "/mets2foxml/";
	private List<String> acceptedProfiles;

	public METSPackageSIPProcessor() {
		try {
			_countObjectsXpath = XPath.newInstance(countObjectsXpath);
			_countObjectsXpath.addNamespace(METS_NS);
		} catch (JDOMException e) {
			log.error("Bad Configuration for Mets2FoxmlFilter", e);
			throw new IllegalArgumentException("Bad Configuration for Mets2FoxmlFilter", e);
		}
		Source mets2foxsrc = new StreamSource(METSPackageSIPProcessor.class.getResourceAsStream(stylesheetPackage
				+ "base-model.xsl"));
		// requires a Saxon 8 transformer factory
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			// set a Resolver that can look in the classpath
			factory.setURIResolver(new URIResolver() {
				public Source resolve(String href, String base) throws TransformerException {
					Source result = null;
					if (href.startsWith("/"))
						result = new StreamSource(METSPackageSIPProcessor.class.getResourceAsStream(href));
					else result = new StreamSource(METSPackageSIPProcessor.class.getResourceAsStream(stylesheetPackage + href));
					return result;
				}
			});

			mets2fox = factory.newTemplates(mets2foxsrc);
		} catch (TransformerFactoryConfigurationError e) {
			log.error("Error setting up transformer factory.", e);
			throw new Error("Error setting up transformer factory", e);
		} catch (TransformerConfigurationException e) {
			log.error("Error setting up transformer.", e);
			throw new Error("Error setting up transformer", e);
		}
	}

	@Override
	public ArchivalInformationPackage createAIP(SubmissionInformationPackage sip)
			throws IngestException {
		METSPackageSIP metsPack = (METSPackageSIP) sip;

		// VALIDATE METS and other schema
		this.xsdValidate(metsPack.getMetsFile());

		// PARSE THE METS DOCUMENT
		Document mets = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			mets = builder.build(metsPack.getMetsFile());
		} catch (IOException e) {
			throw new IngestException("Cannot read METS file", e);
		} catch (JDOMException e) {
			throw new IngestException("Cannot read parse METS file", e);
		}

		// VALIDATE METS AGAINST A PROFILE
		String profile = validateProfile(mets);

		// VALIDATE PACKAGED FILES AGAINST METS MANIFEST
		this.getMetsPackageFileValidator().validateFiles(mets, metsPack);

		// TODO: replace named repository with an agent object representing ingest
		// log this in the main event logger with a proper PID for the repo
		metsPack.getPreIngestEventLogger().addEvent(Type.VALIDATION, "Repository",
				"METS manifest validated against profile: " + profile, new Date(System.currentTimeMillis()));

		// CONVERT METS DOCUMENT INTO AN AIP
		ArchivalInformationPackage aip = transformMETS(metsPack, mets, metsPack.isAllowIndexing());

		// increment any duplicate slugs
		RDFAwareAIPImpl rdfaip = null;
		try {
			rdfaip = new RDFAwareAIPImpl(aip);
		} catch (AIPException e) {
			throw new Error("Could not create RDF-aware AIP.", e);
		}
		Set<String> usedSlugs = new HashSet<String>();
		for (PID p : rdfaip.getPIDs()) {
			String inslug = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), p,
					ContentModelHelper.CDRProperty.slug.getURI());
			String slug = inslug;
			while (usedSlugs.contains(slug)) {
				slug = PathUtil.incrementSlug(slug);
			}
			if (!inslug.equals(slug)) {
				JRDFGraphUtil.removeAllRelatedByPredicate(rdfaip.getGraph(), p,
						ContentModelHelper.CDRProperty.slug.getURI());
				JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), p, ContentModelHelper.CDRProperty.slug, slug);
			}
			usedSlugs.add(slug);
		}

		// extract and add the SIP creation event
		Element metsHdr = mets.getRootElement().getChild("metsHdr", METS_NS);
		String createdate = metsHdr.getAttributeValue("CREATEDATE");
		Element createEvent = metsPack.getPreIngestEventLogger().addSIPCreation(createdate, null, null);
		for (Element agent : (List<Element>) metsHdr.getChildren("agent", METS_NS)) {
			String name = agent.getChildText("name", METS_NS);
			String role = agent.getAttributeValue("ROLE");
			metsPack.getPreIngestEventLogger().addAgent(createEvent, name, "Name", role);
		}

		// move over pre-ingest events
		if (metsPack.getPreIngestEventLogger().hasEvents()) {
			for (PID p : rdfaip.getPIDs()) {
				for (Element event : metsPack.getPreIngestEventLogger().getEvents(p)) {
					rdfaip.getEventLogger().addEvent(p, event);
				}
			}
		}

		return rdfaip;
	}

	public METSPackageFileValidator getMetsPackageFileValidator() {
		return metsPackageFileValidator;
	}

	public edu.unc.lib.dl.pidgen.PIDGenerator getPidGenerator() {
		return pidGenerator;
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setMetsPackageFileValidator(METSPackageFileValidator metsPackageFileValidator) {
		this.metsPackageFileValidator = metsPackageFileValidator;
	}

	public void setPidGenerator(edu.unc.lib.dl.pidgen.PIDGenerator pidGenerator) {
		this.pidGenerator = pidGenerator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	private AIPImpl transformMETS(METSPackageSIP metsPack, Document mets, boolean allowIndexing)
			throws IngestException {

		AIPImpl aip = new AIPImpl(metsPack.getBatchPrepDir());

		// count the object divs in METS
		int num = 0;
		try {
			num = _countObjectsXpath.numberValueOf(mets).intValue();
			log.debug("GOT OBJECT COUNT: " + num);
		} catch (JDOMException e) {
			throw new IngestException("METS issue: Could not get a good count of divs in the structMap.", e);
		}
		if (num < 1) {
			throw new IngestException("METS issue: The structMap must contain at least one div.");
		}

		// generate the right number of PIDs
		StringBuffer sb = new StringBuffer("<pids>");
		for (PID pid : pidGenerator.getNextPIDs(num)) {
			sb.append("<pid>").append(pid).append("</pid>");
		}
		sb.append("</pids>");

		// get a transformer
		Transformer t = null;
		try {
			t = mets2fox.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new IngestException("There was a problem configuring the transformer.", e);
		}

		// set parameters
		t.setParameter("pids", new StreamSource(new StringReader(sb.toString())));

		String allowIndexingParam = "no";
		if (allowIndexing) {
			allowIndexingParam = "yes";
		}
		t.setParameter("allowAnyIndexing", allowIndexingParam);

		if (metsPack.getOwner() != null && metsPack.getOwner().getPID() != null) {
			t.setParameter("ownerURI", metsPack.getOwner().getPID().getURI());
		} else {
			throw new IngestException("Error setting the owner of the SIP.");
		}

		File tempFOXDir = aip.getTempFOXDir();

		t.setParameter("output.directory", tempFOXDir.getPath());
		if (log.isInfoEnabled()) {
			log.info(tempFOXDir.getPath());
		}

		Source src = new JDOMSource(mets);
		JDOMResult result = new JDOMResult();
		try {
			t.transform(src, result);
		} catch (TransformerException e) {
			throw new IngestException("METS problem: There were problems transforming METS to FOXML.", e);
		}

		if (log.isDebugEnabled()) {
			log.debug(new XMLOutputter().outputString(result.getDocument()));
		}

		// fill the pid2foxml map and top pid
		Set<PID> topPIDs = new HashSet<PID>();
		for (Object child : result.getDocument().getRootElement().getChild("objects").getChildren("object")) {
			Element e = (Element) child;
			PID pid = new PID(e.getAttributeValue("PID"));
			String output = e.getAttributeValue("OUTPUT");
			if ("yes".equals(e.getAttributeValue("TOP"))) {
				Integer designatedOrder = null;
				Integer sipOrder = null;
				topPIDs.add(pid);
				if (e.getAttributeValue("designatedOrder") != null) {
					try {
						designatedOrder = new Integer(Integer.parseInt(e.getAttributeValue("designatedOrder")));
					} catch (NumberFormatException nfe) {
						throw new IngestException("METS problem: designatedOrder attribute must be an integer.", nfe);
					}
				}
				if (e.getAttributeValue("sipOrder") != null) {
					try {
						sipOrder = new Integer(Integer.parseInt(e.getAttributeValue("sipOrder")));
					} catch (NumberFormatException nfe) {
						throw new IngestException("METS problem: sipOrder attribute must be an integer.", nfe);
					}
				}
				aip.setContainerPlacement(metsPack.getContainerPID(), pid, designatedOrder, sipOrder);
			}
			aip.setFOXMLFile(pid, new File(output));
		}
		aip.setTopPIDs(topPIDs);
		return aip;
	}

	/**
	 * Validates the METS document against a known profiles.
	 *
	 * @param mets
	 *           the METS document
	 * @param logger
	 *           the event logger
	 * @return a String indicating the name of the METS profile
	 * @throws InvalidMETSException
	 *            when the METS cannot be validated
	 */
	public String validateProfile(Document mets) throws InvalidMETSException {
		// extract the profileUrl from METS
		Attribute profileAtt = mets.getRootElement().getAttribute("PROFILE");
		if (profileAtt == null || profileAtt.getValue() == null) {
			throw new InvalidMETSException("The mets element MUST have a valid PROFILE attribute.");
		}
		String profileUrl = profileAtt.getValue();

		if (!this.acceptedProfiles.contains(profileUrl)){
			throw new InvalidMETSException("The mets element MUST provide an accepted PROFILE attribute.  Unacceptable profile: "
					+ profileUrl);
		}
		
		// is this a known schema?
		if (!this.schematronValidator.getSchemas().containsKey(profileUrl)) {
			throw new InvalidMETSException("The mets element MUST have a recognized PROFILE attribute.  Unknown profile: "
					+ profileUrl);
		}
		Source src = new JDOMSource(mets);
		Document svrl = this.schematronValidator.validate(src, profileUrl);

		if (log.isDebugEnabled()) {
			XMLOutputter dbout = new XMLOutputter();
			dbout.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
			log.debug(dbout.outputString(svrl));
		}

		// detect and report failures in output SVRL
		Filter failedAsserts = new Filter() {
			private static final long serialVersionUID = 1965854034232575078L;

			public boolean matches(Object obj) {
				if (obj instanceof Element) {
					Element e = (Element) obj;
					if ("failed-assert".equals(e.getName())) {
						return true;
					}
				}
				return false;
			}
		};
		if (svrl.getDescendants(failedAsserts).hasNext()) {
			String msg = "Validation of METS failed against submission profile: " + profileUrl;
			log.info(msg);
			if (log.isDebugEnabled()) {
				log.debug(new XMLOutputter().outputString(svrl));
			}
			throw new InvalidMETSException(msg, svrl);
		}
		log.info("Validated METS against submission profile: " + profileUrl);
		return profileUrl;
	}

	private void xsdValidate(File metsFile2) throws IngestException {
		// TODO can reuse schema object, it is thread safe
		javax.xml.validation.SchemaFactory schemaFactory = javax.xml.validation.SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		StreamSource xml = new StreamSource(getClass().getResourceAsStream(schemaPackage + "xml.xsd"));
		StreamSource xlink = new StreamSource(getClass().getResourceAsStream(schemaPackage + "xlink.xsd"));
		StreamSource mets = new StreamSource(getClass().getResourceAsStream(schemaPackage + "mets.xsd"));
		StreamSource premis = new StreamSource(getClass().getResourceAsStream(schemaPackage + "premis-v2-0.xsd"));
		StreamSource mods = new StreamSource(getClass().getResourceAsStream(schemaPackage + "mods-3-4.xsd"));
		StreamSource acl = new StreamSource(getClass().getResourceAsStream(schemaPackage + "acl.xsd"));
		Schema schema;
		try {
			Source[] sources = { xml, xlink, mets, premis, mods, acl };
			schema = schemaFactory.newSchema(sources);
		} catch (SAXException e) {
			throw new Error("Cannot locate METS schema in classpath.", e);
		}

		Validator metsValidator = schema.newValidator();
		METSParseException handler = new METSParseException("There was a problem parsing METS XML.");
		metsValidator.setErrorHandler(handler);
		// TODO get a Result document for reporting error
		try {
			metsValidator.validate(new StreamSource(metsFile2));
		} catch (SAXException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			throw handler;
		} catch (IOException e) {
			throw new IngestException("The supplied METS file is not readable.", e);
		}
	}

	public List<String> getAcceptedProfiles() {
		return acceptedProfiles;
	}

	public void setAcceptedProfiles(List<String> acceptedProfiles) {
		this.acceptedProfiles = acceptedProfiles;
	}
}
