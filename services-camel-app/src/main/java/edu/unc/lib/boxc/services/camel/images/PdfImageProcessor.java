package edu.unc.lib.boxc.services.camel.images;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrImagePath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrImagePathCleanup;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Processor which generates a TIFF image of the first page of a PDF and then passes it along as the new binary
 *
 * @author bbpennel
 */
public class PdfImageProcessor implements Processor {
    private String outputPath;

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        InputStream inputStream = null;
        PDDocument document = null;
        RandomAccessReadBuffer randomAccessReadBuffer = null;

        try {
            inputStream = Files.newInputStream(Paths.get(binPath));

            randomAccessReadBuffer = new RandomAccessReadBuffer(inputStream);

            document = Loader.loadPDF(randomAccessReadBuffer);
            document.setResourceCache(null); // Reduce memory usage

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage thumbnail = renderer.renderImage(0);

            var outputImagePath = Files.createTempFile(Paths.get(outputPath), "thumbnail", ".tif");
            ImageIO.write(thumbnail, "TIF", outputImagePath.toFile());

            in.setHeader(CdrImagePath, outputImagePath.toString());
            in.setHeader(CdrBinaryMimeType, "image/tiff");
            in.setHeader(CdrImagePathCleanup, true);
        } finally {
            if (document != null) {
                document.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (randomAccessReadBuffer != null) {
                randomAccessReadBuffer.close();
            }
        }
    }

    /**
     * Returns true if the subject of the exchange is pdf binary
     * @param exchange
     * @return
     */
    public static boolean isPdfFile(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        return mimetype.equals("application/pdf");
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}
