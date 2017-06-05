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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gregory Jansen
 *
 */
public class MetsDirDiff {
    private static int fileCount = 0;
    private static int refCount = 0;
    private static File dir = null;
    private static int canonicalPathTrim = 0;

    private MetsDirDiff() {

    }

    private static void usage() {
        System.out
                .println("Usage: java -cp cdla-common.jar edu.unc.lib.dl.util.MetsDirDiff [options...]"
                        + " <mets file.xml> <directory path>");
        System.out.println(" <mets file.xml>\tfull path to a mets.xml file");
        System.out
                .println(" <directory path>\tfull path to the directory"
                        + " (within which METS file pointers are relative)");
        System.out
                .println("Options:\tMETS file and directory arguments can be in any order.\n --nopath\t match filenames"
                        + " and total numbers only (for checking http references)");
        System.exit(0);
    }

    /**
     * @param args
     */
    /**
     * @param args
     */
    public static void main(String[] args) {
    if (args.length < 2) {
        usage();
    }
    File metsFile = null;
    boolean nopath = false;
    for (int i = 0; i < args.length; i++) {
        if (args[i].endsWith(".xml") && new File(args[i]).exists()) {
            metsFile = new File(args[i]);
        } else if ("--nopath".equals(args[i].trim())) {
            nopath = true;
        } else if (new File(args[i]).exists() && new File(args[i]).isDirectory()) {
            dir = new File(args[i]);
        } else {
            System.out.println("Unrecognized option: " + args[i]);
            usage();
        }
    }
    if (metsFile == null || dir == null) {
        throw new RuntimeException("You must supply both a mets file (ending in .xml) and a directory path");
    }
    if (!metsFile.exists()) {
        throw new RuntimeException("The METS file " + metsFile.getAbsolutePath() + " does not exist.");
    }
    if (!dir.exists()) {
        throw new RuntimeException("The directory path " + dir.getAbsolutePath() + " does not exist.");
    }
    try {
        canonicalPathTrim = dir.getCanonicalPath().length() + 1;
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    Set<String> files = new HashSet<String>();
    try {
        addFolder(dir, files, nopath);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }

    Set<String> references = new HashSet<String>();
    Pattern hrefGrabber = Pattern.compile("xlink:href=\"file://(.*)\"|xlink:href=\"(.*)\"");

    if (nopath) {
        System.out.println("Running with --nopath option. Paths are not checked.");
    }

    System.out.println("Unmatched file pointers in " + metsFile.getPath() + ":");
    try (LineNumberReader fr = new LineNumberReader(new FileReader(metsFile))) {
        for (String line = fr.readLine(); line != null; line = fr.readLine()) {
        Matcher grabbed = hrefGrabber.matcher(line);
        String path = null;
        if (grabbed.find()) {
            if (grabbed.group(1) != null) {
            path = grabbed.group(1);
            } else if (grabbed.group(2) != null) {
            path = grabbed.group(2);
            }
            refCount++;
            if (nopath) {
            path = path.substring(path.lastIndexOf('/') + 1);
            }
            references.add(path);
            if (!files.contains(path)) {
            System.out.println("line " + fr.getLineNumber() + ": " + path);
            }
        }
        }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    System.out.println("Unmatched files in " + dir.getPath() + ":");
    files.removeAll(references);
    for (String f : files) {
        System.out.println(f);
    }
    if (fileCount == refCount) {
        System.out.println("There are " + refCount + " METS file pointers and " + fileCount + " files.");
    } else {
        System.out.println("WARNING: There are " + refCount + " METS file pointers and " + fileCount + " files.");
    }
    }

    private static void addFolder(File folder, Set<String> index, boolean nopath)
            throws IOException {
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                addFolder(f, index, nopath);
            } else if (f.isFile()) {
                fileCount++;
                if (nopath) {
                    // put in filename only
                    index.add(f.getName());
                } else {
                    // path with normalized separators
                    index.add(f.getCanonicalPath().substring(canonicalPathTrim)
                            .replace('\\', '/'));
                }
            }
        }
    }
}
