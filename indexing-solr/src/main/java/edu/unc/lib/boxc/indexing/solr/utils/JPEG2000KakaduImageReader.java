package edu.unc.lib.boxc.indexing.solr.utils;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_layer_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_compressed_source;
import kdu_jni.Kdu_compressed_source_nonnative;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message;
import kdu_jni.Kdu_message_formatter;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_decompressor;
import kdu_jni.Kdu_simple_file_source;
import kdu_jni.Kdu_thread_env;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>JPEG2000 image reader using the Kakadu native library ({@literal libkdu})
 * via the Java Native Interface (JNI). Written against version 7.10.</p>
 *
 * <p><strong>Important: {@link #close()} must be called after use.</strong></p>
 *
 * <h1>Usage</h1>
 *
 * <p>The Kakadu shared library and JNI binding (two separate files) must be
 * present on the library path, or else the {@literal -Djava.library.path} VM
 * argument must be provided at launch, with a value of the pathname of the
 * directory containing the library.</p>
 *
 * <h1>License</h1>
 *
 * <p>This software was developed using a Kakadu Public Service License
 * and may not be used commercially. See the
 * <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Kakadu-Licence-Terms-Feb-2018.pdf">
 * Kakadu Software License Terms and Conditions</a> for detailed terms.</p>
 *
 * @since 4.1
 * @author Alex Dolski UIUC
 */
public final class JPEG2000KakaduImageReader implements AutoCloseable {

    /**
     * Custom {@link Kdu_compressed_source_nonnative} backed by an {@link
     * ImageInputStream}, which, in contrast to {@link java.io.InputStream}, is
     * seekable, even if it has to employ buffering or a disk cache (will
     * depend on the implementation). Seeking can offer major performance
     * advantages when the stream is capable of exploiting it fully.
     */
    private static class KduImageInputStreamSource
            extends Kdu_compressed_source_nonnative {

        private final ImageInputStream inputStream;

        KduImageInputStreamSource(ImageInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int Get_capabilities() {
            return Kdu_global.KDU_SOURCE_CAP_SEQUENTIAL |
                    Kdu_global.KDU_SOURCE_CAP_SEEKABLE;
        }

        @Override
        public long Get_pos() throws KduException {
            try {
                return inputStream.getStreamPosition();
            } catch (IOException e) {
                throw new KduException(e.getMessage());
            }
        }

        @Override
        public int Post_read(final int numBytesRequested) {
            final byte[] buffer = new byte[numBytesRequested];
            int numBytesRead = 0;
            try {
                while (numBytesRead < numBytesRequested) {
                    int numSupplied = inputStream.read(
                            buffer, numBytesRead, buffer.length - numBytesRead);
                    if (numSupplied < 0) {
                        break;
                    }
                    numBytesRead += numSupplied;
                }
                Push_data(buffer, 0, numBytesRead);
            } catch (KduException | IOException e) {
                LOGGER.error(e.getMessage());
            }
            return numBytesRead;
        }

        @Override
        public boolean Seek(long offset) throws KduException {
            try {
                inputStream.seek(offset);
                return true;
            } catch (IOException e) {
                throw new KduException(e.getMessage());
            }
        }

    }

    /**
     * N.B.: it might be better to extend {@link
     * kdu_jni.Kdu_thread_safe_message} instead, but there is apparently some
     * kind of bug up through Kakadu 8.0.3 (at least) that causes it to not
     * work right. (See
     * https://github.com/cantaloupe-project/cantaloupe/issues/396)
     */
    private static abstract class AbstractKduMessage extends Kdu_message {

        final StringBuilder builder = new StringBuilder();

        @Override
        public void Put_text(String text) {
            builder.append(text);
        }

    }

    private static class KduDebugMessage extends AbstractKduMessage {

        @Override
        public void Flush(boolean isEndOfMessage) {
            if (isEndOfMessage) {
                LOGGER.debug(builder.toString());
                builder.setLength(0);
            }
        }

    }

    private static class KduErrorMessage extends AbstractKduMessage {

        @Override
        public void Flush(boolean isEndOfMessage) throws KduException {
            if (isEndOfMessage) {
                LOGGER.error(builder.toString());
                throw new KduException(Kdu_global.KDU_ERROR_EXCEPTION,
                        "In " + this.getClass().getSimpleName());
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000KakaduImageReader.class);

    /**
     * N.B.: {@link Kdu_global#Kdu_get_num_processors()} is another way of
     * getting the CPU count. Unknown whether it's any more reliable.
     */
    private static final int NUM_THREADS =
            Math.max(1, Runtime.getRuntime().availableProcessors());

    private static final KduDebugMessage KDU_SYSOUT = new KduDebugMessage();
    private static final KduErrorMessage KDU_SYSERR = new KduErrorMessage();
    private static final Kdu_message_formatter KDU_PRETTY_SYSOUT =
            new Kdu_message_formatter(KDU_SYSOUT);
    private static final Kdu_message_formatter KDU_PRETTY_SYSERR =
            new Kdu_message_formatter(KDU_SYSERR);

    private Jpx_source jpxSrc;
    private Jp2_threadsafe_family_src familySrc;
    private Kdu_compressed_source compSrc;
    private Kdu_codestream codestream;
    private Kdu_channel_mapping channels;
    private Kdu_region_decompressor decompressor;
    private Kdu_thread_env threadEnv;
    private Kdu_quality_limiter limiter;

    /**
     * Set by {@link #setSource(Path)}. Used preferentially over {@link
     * #inputStream}.
     */
    private Path sourceFile;

    /**
     * Set by {@link #setSource(ImageInputStream)}. Used if {@link
     * #sourceFile} is not set.
     */
    private ImageInputStream inputStream;

    private boolean isOpenAttempted, haveReadInfo;

    private int width, height;

    static {
        try {
            Kdu_global.Kdu_customize_warnings(KDU_PRETTY_SYSOUT);
            Kdu_global.Kdu_customize_errors(KDU_PRETTY_SYSERR);
        } catch (KduException e) {
            LOGGER.error("Static initializer: {}", e.getMessage(), e);
        }
    }

    public JPEG2000KakaduImageReader() {
        init();
    }

    private void handle(KduException e) {
        try {
            threadEnv.Handle_exception(e.Get_kdu_exception_code());
        } catch (KduException ke) {
            LOGGER.debug("{} (code: {})",
                    ke.getMessage(),
                    Integer.toHexString(ke.Get_kdu_exception_code()),
                    ke);
        }
    }

    /**
     * Closes everything.
     */
    @Override
    public void close() {
        close(true);
    }

    /**
     * Variant of {@link #close()} with an option to leave the {@link
     * #setSource(ImageInputStream) input stream} open.
     */
    private void close(boolean alsoCloseInputStream) {
        // N.B.: see the end notes in the KduRender.java file in the Kakadu SDK
        // for explanation of how this stuff needs to be destroyed. (And it DOES
        // need to be destroyed!)
        if (isDecompressing) {
            try {
                decompressor.Finish();
            } catch (KduException e) {
                LOGGER.warn("Failed to stop the kdu_region_decompressor: {} (code: {})",
                        e.getMessage(),
                        Integer.toHexString(e.Get_kdu_exception_code()));
            }
        }
        limiter.Native_destroy();
        decompressor.Native_destroy();
        channels.Native_destroy();
        try {
            if (codestream.Exists()) {
                threadEnv.Cs_terminate(codestream);
                codestream.Destroy();
            }
        } catch (KduException e) {
            LOGGER.warn("Failed to destroy the kdu_codestream: {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
        }

        if (compSrc != null) {
            try {
                compSrc.Close();
            } catch (KduException e) {
                LOGGER.warn("Failed to close the {}: {} (code: {})",
                        compSrc.getClass().getSimpleName(),
                        e.getMessage(),
                        Integer.toHexString(e.Get_kdu_exception_code()));
            } finally {
                compSrc.Native_destroy();
            }
        }
        jpxSrc.Native_destroy();
        familySrc.Native_destroy();

        try {
            threadEnv.Destroy();
        } catch (KduException e) {
            LOGGER.warn("Failed to destroy the kdu_thread_env: {} (code: {})",
                    e.getMessage(),
                    Integer.toHexString(e.Get_kdu_exception_code()));
        }
        threadEnv.Native_destroy();

        if (inputStream != null) {
            try {
                if (alsoCloseInputStream) {
                    IOUtils.closeQuietly(inputStream);
                } else {
                    inputStream.seek(0);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to close the {}: {}",
                        inputStream.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        }

        isOpenAttempted = false;
    }

    private void init() {
        jpxSrc       = new Jpx_source();
        familySrc    = new Jp2_threadsafe_family_src();
        codestream   = new Kdu_codestream();
        channels     = new Kdu_channel_mapping();
        decompressor = new Kdu_region_decompressor();
        threadEnv    = new Kdu_thread_env();
        limiter      = new Kdu_quality_limiter(1 / 256f, false);
    }

    public int getHeight() throws IOException {
        if (height == 0) {
            openImage();
            readInfo();
        }
        return height;
    }

    public int getWidth() throws IOException {
        if (width == 0) {
            openImage();
            readInfo();
        }
        return width;
    }

    public void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setSource(Path file) {
        this.sourceFile = file;
    }

    private void openImage() throws IOException {
        if (isOpenAttempted) {
            return;
        }
        isOpenAttempted = true;

        try {
            if (sourceFile != null) {
                familySrc.Open(sourceFile.toString());
            } else {
                compSrc = new KduImageInputStreamSource(inputStream);
                familySrc.Open(compSrc);
            }

            Jpx_layer_source layerSrc           = null;
            Jpx_codestream_source codestreamSrc = null;
            int success = jpxSrc.Open(familySrc, true);
            if (success >= 0) {
                // Succeeded in opening as wrapped JP2/JPX source.
                layerSrc      = jpxSrc.Access_layer(0);
                codestreamSrc = jpxSrc.Access_codestream(layerSrc.Get_codestream_id(0));
                compSrc       = codestreamSrc.Open_stream();
            } else {
                // Must open as raw codestream.
                familySrc.Close();
                jpxSrc.Close();
                if (sourceFile != null) {
                    compSrc = new Kdu_simple_file_source(sourceFile.toString());
                } else {
                    inputStream.seek(0);
                    compSrc = new KduImageInputStreamSource(inputStream);
                }
            }

            // Tell Kakadu to use as many more threads as we have CPUs.
            threadEnv.Create();
            for (int t = 1; t < NUM_THREADS; t++) {
                threadEnv.Add_thread();
            }

            codestream.Create(compSrc, threadEnv);
            codestream.Set_resilient();

            boolean anyChannels = false;
            if (layerSrc != null) {
                channels.Configure(
                        layerSrc.Access_colour(0),
                        layerSrc.Access_channels(),
                        codestreamSrc.Get_codestream_id(),
                        codestreamSrc.Access_palette(),
                        codestreamSrc.Access_dimensions());
                anyChannels = (channels.Get_num_channels() > 0);
            }
            if (!anyChannels) {
                channels.Configure(codestream);
            }
        } catch (KduException e) {
            handle(e);
            if (e.Get_kdu_exception_code() == 1801745731) {
                throw new RuntimeException();
            } else {
                throw new IOException(e);
            }
        }
    }

    private void readInfo() throws IOException {
        if (haveReadInfo) {
            return;
        }
        haveReadInfo = true;

        try {
            // Read the image dimensions.
            {
                final int referenceComponent = channels.Get_source_component(0);
                final Kdu_dims imageDims = new Kdu_dims();
                codestream.Get_dims(referenceComponent, imageDims);
                width = imageDims.Access_size().Get_x();
                height = imageDims.Access_size().Get_y();
            }
        } catch (KduException e) {
            handle(e);
            throw new IOException(e);
        }
    }
}
