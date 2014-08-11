/*
 * Copyright (c) 2008  Los Alamos National Security, LLC.
 *
 * Los Alamos National Laboratory
 * Research Library
 * Digital Library Research & Prototyping Team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 */

package gov.lanl.adore.djatoka.io.writer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import gov.lanl.adore.djatoka.io.FormatIOException;
import gov.lanl.adore.djatoka.io.IWriter;

import org.apache.log4j.Logger;

/**
 * JPG File Writer. Uses ImageIO to write BufferedImage as JPG
 * 
 * @author Ryan Chute
 * @author Kevin S. Clarke &lt;<a href="mailto:ksclarke@gmail.com">ksclarke@gmail.com</a>&gt;
 */
public class JPGWriter implements IWriter {
	static Logger logger = Logger.getLogger(JPGWriter.class);
	
	public static final int DEFAULT_QUALITY_LEVEL = 85;
	private int q = DEFAULT_QUALITY_LEVEL;
	
    /**
     * Write a BufferedImage instance using implementation to the provided OutputStream.
     * 
     * @param aImage BufferedImage instance to be serialized
     * @param aOutStream OutputStream to output the image to
     * @throws FormatIOException
     */
    @Override
    public void write(final BufferedImage aImage, final OutputStream aOutStream) throws FormatIOException {
        final Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName("jpeg");

        try {
            final ImageWriter jpgWriter = iterator.next();
            final ImageWriteParam iwp = jpgWriter.getDefaultWriteParam();

            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality((float) (q / 100.0));

            jpgWriter.setOutput(ImageIO.createImageOutputStream(aOutStream));
            jpgWriter.write(null, new IIOImage(aImage, null, null), iwp);
            jpgWriter.dispose();
        } catch (final IOException details) {
            throw new FormatIOException(details);
        }
    }

    /**
     * Set the Writer Implementations Serialization properties. Only JPGWriter.quality_level is supported in this
     * implementation.
     * 
     * @param aProps writer serialization properties
     */
    @Override
    public void setWriterProperties(final Properties aProps) {
        if (aProps.containsKey("JPGWriter.quality_level")) {
            q = Integer.parseInt((String) aProps.get("JPGWriter.quality_level"));
        }
    }
}
