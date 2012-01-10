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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Gregory Jansen
 *
 */
public class FileUtils {
	private static final Log log = LogFactory.getLog(FileUtils.class);
	private static Pattern filePathPattern = null;

	static {
		filePathPattern = Pattern.compile("^(file:)?([/\\\\]{0,3})?(.+)$");
	}

	public static void copyFile(File in, File out) throws IOException {
		FileChannel inChannel = new FileInputStream(in).getChannel();
		FileChannel outChannel = new FileOutputStream(out).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} catch (IOException e) {
			throw e;
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
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
	 * Creates a temporary copy of the original file or folder.
	 *
	 * @param file
	 *           the original file
	 * @return the temporary file
	 */
	public static File tempCopy(File file) {
		try {
			File result = File.createTempFile("tempCopy", "");
			result.deleteOnExit();
			copyFolder(file, result);
			return result;
		} catch (IOException e) {
			throw new Error("Unexpected", e);
		}
	}

	/**
	 * Moves a file or directory. Uses rename operation if possible. This operation may not be atomic.
	 *
	 * @param src
	 * @param dest
	 * @return
	 * @throws IOException
	 */
	public static void renameOrMoveTo(File src, File dest) throws IOException {
		if (!src.renameTo(dest)) {
			// cannot rename, try copy and delete
			log.warn("Cannot rename directory " + src + " to queue location " + dest
					+ ", forced to perform slower copy and delete operations.");
			FileUtils.copyFolder(src, dest);
			if (!src.delete()) {
				log.warn("Cannot delete original file in move operation.");
			}
		}
	}

	public static File createTempDirectory(String prefix) throws IOException {
		final File temp;
		temp = File.createTempFile(prefix, Long.toString(System.nanoTime()));
		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}
		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return (temp);
	}

	/**
	 * Safely returns a File object for a "file:" URL. The base and root directories are both considered to be the dir.
	 * URLs that point outside of that directory will throw an IOException.
	 *
	 * @param url
	 *           the URL
	 * @param dir
	 *           the directory within which to resolve the "file:" URL.
	 * @return a File object
	 * @throws IOException
	 *            when URL improperly formatted or points outside of the dir.
	 */
	public static File getFileForUrl(String url, File dir) throws IOException {
		File result = null;

		// remove any file: prefix and beginning slashes or backslashes
		Matcher m = filePathPattern.matcher(url);

		if (m.find()) {
			String path = m.group(3); // grab the path group
			path = path.replaceAll("\\\\", File.pathSeparator);
			result = new File(dir, path);
			if (result.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
				return result;
			} else {
				throw new IOException("Bad locator for a file in SIP:" + url);
			}
		} else {
			throw new IOException("Bad locator for a file in SIP:" + url);
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
}
