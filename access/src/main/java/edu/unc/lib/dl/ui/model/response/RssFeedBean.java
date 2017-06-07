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
package edu.unc.lib.dl.ui.model.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author count0
 *
 */
public class RssFeedBean {
    private String version;
    private String title;
    private String description;
    private String link;

    public List<RssItem> items;

    public RssFeedBean() {
        items = new ArrayList<RssItem>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<RssItem> getItems() {
        return items;
    }

    public void setItems(List<RssItem> items) {
        this.items = items;
    }

    public static class RssItem {
        private String title;
        private String description;
        private String link;
        private String pubDate;
        private String guid;
        private String encoded;
        private EnclosureObject enclosure;

        public RssItem() {
        }

        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getLink() {
            return link;
        }
        public void setLink(String link) {
            this.link = link;
        }
        public String getPubDate() {
            return pubDate;
        }
        public void setPubDate(String pubDate) {
            this.pubDate = pubDate;
        }
        public String getGuid() {
            return guid;
        }
        public void setGuid(String guid) {
            this.guid = guid;
        }

        public String getEncoded() {
            return encoded;
        }

        public void setEncoded(String encoded) {
            this.encoded = encoded;
        }

        public EnclosureObject getEnclosure() {
            return enclosure;
        }

        public void setEnclosure(EnclosureObject enclosure) {
            this.enclosure = enclosure;
        }

        public static class EnclosureObject {
            private String url;
            private String type;
            private String length;

            public EnclosureObject() {
            }

            public String getUrl() {
                return url;
            }
            public void setUrl(String url) {
                this.url = url;
            }
            public String getType() {
                return type;
            }
            public void setType(String type) {
                this.type = type;
            }
            public String getLength() {
                return length;
            }
            public void setLength(String length) {
                this.length = length;
            }

        }
    }

}
