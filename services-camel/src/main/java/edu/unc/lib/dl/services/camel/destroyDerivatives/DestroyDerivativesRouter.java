package edu.unc.lib.dl.services.camel.destroyDerivatives;

import static org.apache.camel.LoggingLevel.DEBUG;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

public class DestroyDerivativesRouter extends RouteBuilder {
    @BeanInject(value = "binaryInfoProcessor")
    private BinaryInfoProcessor binaryInfoProcessor;

    @BeanInject(value = "destroySmallThumbnailProcessor")
    private DestroyDerivativesProcessor destroySmallThumbnailProcessor;

    @BeanInject(value = "destroyLargeThumbnailProcessor")
    private DestroyDerivativesProcessor destroyLargeThumbnailProcessor;

    @BeanInject(value = "destroyAccessCopyProcessor")
    private DestroyDerivativesProcessor destroyAccessCopyProcessor;

    @BeanInject(value = "destroyFulltextProcessor")
    private DestroyDerivativesProcessor destroyFulltextProcessor;

    private static final String IMAGE_MIMETYPE_PATTERN = "^(image.*$|application.*?(photoshop|psd)$)";
    private static final String TEXT_MIMETYPE_PATTERN = "^(text/|application/pdf|application/msword"
            + "|application/vnd\\.|application/rtf|application/powerpoint"
            + "|application/postscript).*$";

    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.destroy.derivatives.stream.camel}}")
                .routeId("CdrDestroyDerivatives")
                .log(DEBUG, "Received destroy derivatives message")
                .process(binaryInfoProcessor)
                .choice()
                    .when(simple("${headers[CdrMimeType]} regex '" + IMAGE_MIMETYPE_PATTERN + "'"))
                        .to("direct:image.derivatives.destroy")
                    .when(simple("${headers[CdrMimeType]} regex '" + TEXT_MIMETYPE_PATTERN + "'"))
                        .to("direct:fulltext.derivatives.destroy")
                .end();

        from("direct:fulltext.derivatives.destroy")
                .bean(destroyFulltextProcessor);

        from("direct:image.derivatives.destroy")
                .bean(destroySmallThumbnailProcessor)
                .bean(destroyLargeThumbnailProcessor)
                .bean(destroyAccessCopyProcessor);
    }
}
