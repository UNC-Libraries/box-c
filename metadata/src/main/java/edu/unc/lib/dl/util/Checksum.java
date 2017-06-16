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
package edu.unc.lib.dl.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

/**
 * A class to compute checksums. Expected usage: Use the default algorithm of
 * MD5 or set a different algorithm (must be supported by
 * java.security.MessageDigest). Then one can repeatedly call the various
 * "getChecksumFrom..." methods to get checksums.
 * 
 * @author count0
 */
public class Checksum {
    private String algorithm = "MD5";
    private MessageDigest messageDigest = null;

    /**
     * Constructor for Checksum class
     */
    public Checksum() {
        try {
            initializeMessageDigest();
        } catch (NoSuchAlgorithmException e) {
            throw new Error("The default algorithm should be available.");
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            File file = new File(args[i]);
            Checksum checker = new Checksum();
            System.out.print(args[i]);
            System.out.print(":");
            try {
                System.out.print(checker.getChecksum(file));
            } catch (IOException e) {
                System.out.print(e.getMessage());
            }

        }
    }

    /**
     * Initialize the internal message digest object if it is null
     */
    private void initializeMessageDigest() throws NoSuchAlgorithmException {
        messageDigest = MessageDigest.getInstance(algorithm);
    }

    /**
     * Get the checksum for the passed in file
     * 
     * @param file
     *            must not be null
     * @return byte array containing checksum
     * @throws IOException
     * @throws FileNotFoundException
     */
    public String getChecksum(File file) throws IOException,
            FileNotFoundException {
        InputStream inputStream = new FileInputStream(file);
        return getChecksum(inputStream);
    }

    /**
     * Get the checksum for the passed in String
     * 
     * @param string
     *            must not be null
     * @return byte array containing checksum
     */
    public String getChecksum(String string) {
        return getChecksum(string.getBytes());
    }

    /**
     * Get the checksum for the passed in byte array
     * 
     * @param byteArray
     *            must not be null
     * @return byte array containing checksum
     */
    public String getChecksum(byte[] byteArray) {
        messageDigest.reset();
        messageDigest.update(byteArray);
        Hex hex = new Hex();
        return new String(hex.encode(messageDigest.digest()));
    }

    /**
     * Get the checksum for the passed in byte array
     * 
     * @param byteArray
     *            must not be null
     * @return byte array containing checksum
     */
    public String getChecksum(InputStream in) throws IOException {
        messageDigest.reset();
        try (BufferedInputStream bis = new BufferedInputStream(in, 1024)) {
            byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = bis.read(buffer);
                if (numRead > 0) {
                    messageDigest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
        }
        Hex hex = new Hex();
        return new String(hex.encode(messageDigest.digest()));
    }

    /**
     * Get the message digest algorithm. Defaults to MD5.
     * 
     * @return The message digest algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the message digest algorithm and reset the internal message digest
     * object. Algorithm must be set to one of the valid algorithms supported by
     * java.security.MessageDigest . Calling this method will reset the internal
     * message digest object, which is not safe to do if on another thread this
     * same Checksum object is computing a checksum.
     * 
     * @param algorithm
     *            The message digest algorithm
     */
    public void setAlgorithm(String algorithm) throws NoSuchAlgorithmException {
        this.algorithm = algorithm;
        initializeMessageDigest();
    }
}
