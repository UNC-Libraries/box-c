package edu.unc.lib.deposit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.UUID;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import de.svenjacobs.loremipsum.LoremIpsum;
import edu.unc.lib.dl.xml.NamespaceConstants;

public class DepositGenerator {
	private static File output = new File("/tmp/generated.xml");
	private static File testPatternTif = new File(
			"src/test/resources/test_pattern.tif");
	private static final String NS = NamespaceConstants.METS_URI;
	private static LoremIpsum li = new LoremIpsum();
	private static int lindex = 0;
	private static int divCount = 0;
	private static int modsCount = 0;
	private static XMLStreamWriter w;

	private static class TreeVisitor {
		TreeVisitor(int levels, int folderBranches, int filesPerFolder) {
			this.levels = levels;
			this.folderBranches = folderBranches;
			this.filesPerFolder = filesPerFolder;
		}

		int levels;
		int folderBranches;
		int filesPerFolder;

		void start() throws XMLStreamException {
			for (int b = 1; b <= folderBranches; b++) {
				int[] id = new int[] { b };
				visit(id, (id.length == levels));
			}
		}

		void recurse(int[] id) throws XMLStreamException {
			if (id.length == levels)
				return;
			int children = (id.length == levels - 1) ? filesPerFolder
					: folderBranches;
			for (int b = 1; b <= children; b++) {
				int[] cid = ArrayUtils.add(id, b);
				visit(cid, (cid.length == levels));
			}
		}

		void visit(int[] id, boolean file) throws XMLStreamException {
			recurse(id);
		}
	}

	public DepositGenerator() {
	}

	public static void main(String[] args) throws IOException {
		generate(2, 3, 5, "tag:count@cdr.lib.unc.edu,2014:/vagrant/"
				+ testPatternTif.getName(), testPatternTif, true);
	}

	public static long generate(int levels, int folderBranches,
			int filesPerFolder, final String dataURI, File dataCopy,
			boolean addMODS) throws IOException {
		long totalRecords = (long) Math.pow(folderBranches, levels);
		String label = MessageFormat
				.format("Generating METS into {4} with {0} files, {1} depth, {2} branching, MODS is {3}",
						totalRecords, levels, folderBranches, addMODS,
						output.getAbsolutePath());
		System.out.println(label);
		XMLOutputFactory fact = XMLOutputFactory.newInstance();
		try (OutputStream os = new FileOutputStream(output)) {
			w = new IndentingXMLStreamWriter(fact.createXMLStreamWriter(os));
			w.writeStartDocument();
			w.writeStartElement("mets");
			w.writeDefaultNamespace(NS);
			w.writeNamespace(NamespaceConstants.XLINK_PREFIX,
					NamespaceConstants.XLINK_URI);
			w.writeNamespace(NamespaceConstants.MODS_V3_PREFIX,
					NamespaceConstants.MODS_V3_URI);
			UUID uuid = UUID.randomUUID();
			w.writeAttribute("ID", "uuid_" + uuid.toString());
			w.writeAttribute("LABEL", label);
			w.writeAttribute("OBJID", "info:fedora/uuid:" + uuid.toString());
			w.writeAttribute("PROFILE",
					"http://cdr.unc.edu/METS/profiles/Simple");
			w.writeAttribute("TYPE", "WORKBENCH");
			header();
			if (addMODS) {
				new TreeVisitor(levels, folderBranches, filesPerFolder) {
					public void visit(int[] id, boolean file)
							throws XMLStreamException {
						w.writeStartElement("dmdSec");
						w.writeAttribute("ID", makeID("dmdid", id));
						w.writeAttribute("GROUPID", "foo");
						w.writeAttribute("CREATED", "2014-01-08T16:54:55.850Z");
						w.writeAttribute("STATUS", "USER_EDITED");
						w.writeStartElement("mdWrap");
						w.writeAttribute("LABEL", getRandomLIWords(2));
						w.writeAttribute("MDTYPE", "MODS");
						w.writeStartElement("xmlData");
						mods();
						w.writeEndElement();
						w.writeEndElement();
						w.writeEndElement();
						modsCount++;
						recurse(id);
					}
				}.start();
			}

			try (FileInputStream fis = new FileInputStream(dataCopy)) {
				w.writeStartElement("fileSec");
				w.writeStartElement("fileGrp");
				w.writeAttribute("ID", "OBJECTS");
				final String checksum = DigestUtils.md5Hex(fis);
				final long dataLength = dataCopy.length();
				new TreeVisitor(levels, folderBranches, filesPerFolder) {
					public void visit(int[] id, boolean file)
							throws XMLStreamException {
						if (file) {
							fileSec(id, dataURI, checksum, dataLength);
						}
						recurse(id);
					}
				}.start();
				w.writeEndElement();
				w.writeEndElement();
			}
			structMap(levels, folderBranches, filesPerFolder, addMODS);
			w.writeEndElement();
			w.writeEndDocument();
		} catch (XMLStreamException e) {
			throw new Error(e);
		}
		System.out.println("div count: " + divCount);
		System.out.println("MODS count: " + modsCount);
		return output.length();
	}

	private static void mods() throws XMLStreamException {
		w.writeStartElement(NamespaceConstants.MODS_V3_URI, "mods");

		w.writeStartElement(NamespaceConstants.MODS_V3_URI, "titleInfo");
		w.writeStartElement(NamespaceConstants.MODS_V3_URI, "title");
		w.writeCharacters(getRandomLIWords(6));
		w.writeEndElement();
		w.writeStartElement(NamespaceConstants.MODS_V3_URI, "subTitle");
		w.writeCharacters(getRandomLIWords(5));
		w.writeEndElement();
		w.writeEndElement();

		w.writeStartElement(NamespaceConstants.MODS_V3_URI, "abstract");
		w.writeCharacters(li.getParagraphs());
		w.writeEndElement();

		w.writeEndElement();
	}

	private static void rights(XMLStreamWriter w, int depth, int branching) {
		// TODO
	}

	private static void structMap(int levels, int branching,
			final int filesPerFolder, final boolean addMODS)
			throws XMLStreamException {
		w.writeStartElement("structMap");
		w.writeStartElement("div");
		w.writeAttribute("TYPE", "Bag");
		new TreeVisitor(levels, branching, filesPerFolder) {

			@Override
			public void visit(int[] id, boolean file) throws XMLStreamException {
				UUID uuid = UUID.randomUUID();
				w.writeStartElement("div");
				w.writeAttribute("CONTENTIDS",
						"info:fedora/uuid:" + uuid.toString());
				w.writeAttribute("ID", "uuid_" + uuid.toString());
				if (addMODS) {
					w.writeAttribute("DMDID", makeID("dmdid", id));
				}
				if (file) {
					w.writeAttribute("TYPE", "File");
					w.writeAttribute("LABEL", getRandomLIWords(1) + ".tif");
					w.writeStartElement("fptr");
					w.writeAttribute("FILEID", makeID("file_", id));
					w.writeEndElement();
				} else {
					w.writeAttribute("TYPE", "Folder");
					w.writeAttribute("LABEL", getRandomLIWords(4));
				}
				divCount++;
				recurse(id);
				w.writeEndElement();
			}

		}.start();
		w.writeEndElement();
		w.writeEndElement();
	}

	private static String getRandomLIWords(int i) {
		int offset = (int) (Math.random() * (50 - i));
		String result = li.getWords(i, offset);
		lindex = lindex + i;
		if (lindex > 1000)
			lindex = 0;
		return result;
	}

	private static void fileSec(int[] id, String dataURI, String checksum,
			long dataLength) throws XMLStreamException {
		w.writeStartElement("file");
		w.writeAttribute("CHECKSUM", checksum);
		w.writeAttribute("ID", makeID("file_", id));
		w.writeAttribute("CHECKSUMTYPE", "MD5");
		w.writeAttribute("CREATED", "2013-01-09T10:12:21.000-05:00");
		w.writeAttribute("MIMETYPE", "application/octet-stream");
		w.writeAttribute("SIZE", String.valueOf(dataLength));
		w.writeStartElement("FLocat");
		w.writeAttribute(NamespaceConstants.XLINK_URI, "href", dataURI);
		w.writeAttribute("LOCTYPE", "OTHER");
		w.writeAttribute("OTHERLOCTYPE", "tag");
		w.writeAttribute("USE", "STAGE");
		w.writeEndElement();
		w.writeEndElement();
	}

	private static void header() throws XMLStreamException {
		w.writeStartElement("metsHdr");
		w.writeAttribute("CREATEDATE", "2014-04-03T13:26:41.509Z");
		w.writeAttribute("LASTMODDATE", "2014-04-03T13:26:41.509Z");
		w.writeStartElement("agent");
		w.writeAttribute("ROLE", "CREATOR");
		w.writeAttribute("TYPE", "OTHER");
		w.writeStartElement("name");
		w.writeCharacters("CDR Workbench");
		w.writeEndElement();
		w.writeEndElement();
		w.writeStartElement("agent");
		w.writeAttribute("ROLE", "CREATOR");
		w.writeAttribute("TYPE", "INDIVIDUAL");
		w.writeStartElement("name");
		w.writeCharacters("count0");
		w.writeEndElement();
		w.writeEndElement();
		w.writeEndElement();
	}

	private static String makeID(String prefix, int[] id) {
		StringBuilder result = new StringBuilder();
		result.append(prefix);
		for (int i : id)
			result.append("_").append(i);
		return result.toString();
	}

}
