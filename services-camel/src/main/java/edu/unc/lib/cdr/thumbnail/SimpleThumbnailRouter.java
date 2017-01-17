package edu.unc.lib.cdr.thumbnail;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.BinaryMetadataProcessor;
import edu.unc.lib.dl.fcrepo4.Repository;

public class SimpleThumbnailRouter extends RouteBuilder {

	@BeanInject(value = "repository")
	private Repository repository;
	
	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;
	
//	@PropertyInject(value = "fcrepo.binaryBase")
//	private String baseBinaryPath;
	
	public void configure() throws Exception {
		from("activemq:topic:fedora2")
		.routeId("SCdrThumbnailEnhancement")
		.bean(mdProcessor).id("simpleBinaryMetadataProcessor")
		.to("fcrepo:localhost:8080/fcrepo/rest");
	}
}
