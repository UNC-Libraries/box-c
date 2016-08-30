package edu.unc.lib.dl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RDFModelUtil {
	
	public static void serializeModel(Model model, File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			RDFDataMgr.write(fos, model, RDFFormat.TURTLE_PRETTY);
		}
	}
	
	/**
	 * Serializes and streams the provided model as serialized turtle
	 * 
	 * @param model
	 * @return
	 * @throws IOException
	 */
	public static InputStream streamModel(Model model) throws IOException {
		return streamModel(model, RDFFormat.TURTLE_PRETTY);
	}

	/**
	 * Serializes and streams the provided model, using the specified format
	 * 
	 * @param model
	 * @param format
	 * @return
	 * @throws IOException
	 */
	public static InputStream streamModel(Model model, RDFFormat format) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			RDFDataMgr.write(bos, model, format);
			return new ByteArrayInputStream(bos.toByteArray());
		}
	}
	
	/**
	 * Returns a model built from the given turtle input stream
	 * 
	 * @param inStream
	 * @return
	 */
	public static Model createModel(InputStream inStream) {
		Model model = ModelFactory.createDefaultModel();
		model.read(inStream, null, "TURTLE");
		return model;
	}
}
