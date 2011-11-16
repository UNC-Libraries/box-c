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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ZipFileUtil {
	private static final Log log = LogFactory.getLog(ZipFileUtil.class);
	private static Pattern filePathPattern = null;

	static {
		filePathPattern = Pattern.compile("^(file:)?([/\\\\]{0,3})?(.+)$");
	}

	/**
	 * Create a temporary directory, unzip the contents of the given zip file to it, and return the directory.
	 *
	 * If anything goes wrong during this process, clean up the temporary directory and throw an exception.
	 */
	public static File unzipToTemp(File zipFile) throws IOException {
		// get a temporary directory to work with
		File tempDir = File.createTempFile("ingest", null);
		tempDir.delete();
		tempDir.mkdir();
		tempDir.deleteOnExit();
		log.info("Unzipping to temporary directory: " + tempDir.getPath());
		try {
			unzip(new FileInputStream(zipFile), tempDir);
			return tempDir;
		} catch (IOException e) {
			// attempt cleanup, then re-throw
			deleteDir(tempDir);
			throw e;
		}
	}

	public static void copyFolder(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			// if directory not exists, create it
			if (!dest.exists()) {
				dest.mkdir();
			}
			// list all the directory contents
			String files[] = src.list();
			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive copy
				copyFolder(srcFile, destFile);
			}
		} else {
			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			// copy the file content in bytes
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
		}
	}

	/**
	 * Create a temporary directory, unzip the contents of the given zip file to it, and return the directory.
	 *
	 * If anything goes wrong during this process, clean up the temporary directory and throw an exception.
	 */
	public static File unzipToTemp(InputStream zipStream) throws IOException {
		// get a temporary directory to work with
		File tempDir = File.createTempFile("ingest", null);
		tempDir.delete();
		tempDir.mkdir();
		tempDir.deleteOnExit();
		log.debug("Unzipping to temporary directory: " + tempDir.getPath());
		try {
			unzip(zipStream, tempDir);
			return tempDir;
		} catch (IOException e) {
			// attempt cleanup, then re-throw
			deleteDir(tempDir);
			throw e;
		}
	}

	/**
	 * Unzips the contents of the zip file to the directory.
	 *
	 * If anything goes wrong during this process, clean up the temporary directory and throw an exception.
	 */
	public static void unzipToDir(File zipFile, File destDir) throws IOException {
		log.debug("Unzipping to directory: " + destDir.getPath());
		unzip(new FileInputStream(zipFile), destDir);
	}

	/**
	 * Unzip to the given directory, creating subdirectories as needed, and ignoring empty directories.
	 */
	public static void unzip(InputStream is, File destDir) throws IOException {
		BufferedOutputStream dest = null;
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			if (!entry.isDirectory()) {
				log.debug("Extracting: " + entry);
				File f = new File(destDir, entry.getName());
				f.getParentFile().mkdirs();
				int count;
				byte data[] = new byte[8192];
				// write the files to the disk
				FileOutputStream fos = new FileOutputStream(f);
				dest = new BufferedOutputStream(fos, 8192);
				while ((count = zis.read(data, 0, 8192)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
		}
		zis.close();
	}

	/**
	 * Delete the given directory and all contents, recursively.
	 */
	public static void deleteDir(File dir) {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				deleteDir(files[i]);
			} else {
				if (!files[i].delete()) {
					log.warn("Unable to delete file: " + files[i].getPath());
				}
			}
		}
		if (!dir.delete()) {
			log.warn("Unable to delete directory: " + dir.getPath());
		}
	}

	public static List<File> getFilesInDir(File dir) {
		List<File> result = new ArrayList<File>();
		File[] contents = dir.listFiles();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isDirectory()) {
				result.addAll(getFilesInDir(contents[i]));
			} else {
				result.add(contents[i]);
			}
		}
		return result;
	}

	/**
	 * Safely returns a File object for a "file:" URL. The base and root directories are both considered to be the
	 * zipDir. URLs that point outside of that directory will throw an IOException.
	 *
	 * @param url
	 *           the URL
	 * @param zipDir
	 *           the directory within which to resolve the "file:" URL.
	 * @return a File object
	 * @throws IOException
	 *            when URL improperly formatted or points outside of the ZIP dir.
	 */
	public static File getFileForUrl(String url, File zipDir) throws IOException {
		File result = null;

		// remove any file: prefix and beginning slashes or backslashes
		Matcher m = filePathPattern.matcher(url);

		if (m.find()) {
			String path = m.group(3); // grab the path group
			path = path.replaceAll("\\\\", File.pathSeparator);
			result = new File(zipDir, path);
			if (result.getCanonicalPath().startsWith(zipDir.getCanonicalPath())) {
				return result;
			} else {
				throw new IOException("Bad locator for a file in SIP:" + url);
			}
		} else {
			throw new IOException("Bad locator for a file in SIP:" + url);
		}
	}

}
