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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.core.io.ClassPathResource;

/**
 * LOC6392BExtractor
 * @author count0
 *
 */
public class LOC6392BExtractor {

    private LOC6392BExtractor() {

    }

    public static void main(String[] args) {
        ClassPathResource isoFile = new ClassPathResource(
                "/edu/unc/lib/dl/standards/ISO-639-2_utf-8.txt");
        BufferedReader br = null;
        Document xml = new Document().setRootElement(new Element("option-set"));
        xml.getRootElement().setAttribute("label", "ISO 639-2 Languages");
        xml.getRootElement().setAttribute("authority-term", "iso639-2b");
        xml.getRootElement().setAttribute("type", "code");

        xml.getRootElement().setAttribute("url",
                "http://www.loc.gov/standards/iso639-2/");
        xml.getRootElement().setAttribute("contextElement", "languageTerm");
        xml.getRootElement().setAttribute("contextNamespace",
                "http://www.loc.gov/mods/v3");

        try {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
            Date now = new java.util.Date(isoFile.getFile().lastModified());
            xml.getRootElement().setAttribute("date", df.format(now));

            br = new BufferedReader(new InputStreamReader(
                    isoFile.getInputStream(), "utf-8"));
            for (String s = br.readLine(); s != null; s = br.readLine()) {
                String[] m = s.split("\\|");
                if (m.length > 1) {
                    String value = m[0];
                    String label = m[3].split(";")[0];
                    Element option = new Element("option");
                    option.addContent(new Element("value").setText(value));
                    option.addContent(new Element("label").setText(label));
                    xml.getRootElement().addContent(option);
                    System.err.println(label + " -> " + value);
                }
            }

            File outFile = new File("/tmp/ISO-639-2_utf-8.xml");
            XMLOutputter out = new XMLOutputter();
            out.setFormat(Format.getPrettyFormat());
            out.output(xml, new FileOutputStream(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
