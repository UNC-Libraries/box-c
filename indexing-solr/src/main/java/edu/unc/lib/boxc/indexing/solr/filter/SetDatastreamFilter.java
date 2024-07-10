package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.Jp2InfoService;
import edu.unc.lib.boxc.indexing.solr.utils.TechnicalMetadataService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.PREMIS_V3_NS;

/**
 * Extracts datastreams from an object and sets related properties concerning the default datastream for the object.
 *
 * Sets datastream, filesizeTotal, filesizeSort
 *
 * @author bbpennel
 *
 */
public class SetDatastreamFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetDatastreamFilter.class);

    private DerivativeService derivativeService;
    private TechnicalMetadataService technicalMetadataService;
    private Jp2InfoService jp2InfoService;
    private static final List<DatastreamType> THUMBNAIL_DS_TYPES = Arrays.asList(DatastreamType.THUMBNAIL_SMALL, DatastreamType.THUMBNAIL_LARGE);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing Datastream filter for object {}", dip.getPid());

        ContentObject contentObj = dip.getContentObject();
        IndexDocumentBean doc = dip.getDocument();

        List<Datastream> datastreams = new ArrayList<>();

        FileObject fileObj = getFileObject(contentObj);
        if (fileObj != null) {
            boolean ownedByOtherObject = contentObj instanceof WorkObject;

            // Add list of file datastreams associated with this object
            addDatastreams(datastreams, fileObj.getBinaryObjects(), ownedByOtherObject);
            // Set the sort file size to the size of the original file
            doc.setFilesizeSort(getFilesize(datastreams));

            // Add list of derivatives associated from the representative file
            addDerivatives(datastreams, fileObj.getPid(), ownedByOtherObject, null);
        } else {
            // Add list of derivatives associated with the object
            addDerivatives(datastreams, contentObj.getPid(), false, null);
        }

        if (contentObj instanceof WorkObject) {
            addThumbnailDerivatives((WorkObject) contentObj, datastreams);
        }

        // Add in metadata datastreams
        addDatastreams(datastreams, contentObj.listMetadata(), false);

        doc.setFilesizeTotal(getFilesizeTotal(datastreams));
        doc.setDatastream(getDatastreamStrings(datastreams));
    }

    private FileObject getFileObject(ContentObject contentObj) {
        if (contentObj instanceof FileObject) {
            return (FileObject) contentObj;
        } else if (contentObj instanceof WorkObject) {
            WorkObject workObj = (WorkObject) contentObj;
            return workObj.getPrimaryObject();
        } else {
            // object being indexed must be a work or a file object
            return null;
        }
    }

    private BinaryObject getFits(List<BinaryObject> binList) {
        return binList.stream().filter(obj -> obj.getPid().getQualifiedId().endsWith(TECHNICAL_METADATA.getId()))
                .findFirst().orElse(null);
    }

    private String getExtent(List<BinaryObject> binList) {
        BinaryObject fits = getFits(binList);

        if (fits == null) {
            return null;
        }

        String fitsId = fits.getPid().getId();

        try {
            var techMdDoc = technicalMetadataService.retrieveDocument(fits);
            Element fitsMd = techMdDoc.getRootElement().getChild("object", PREMIS_V3_NS)
                    .getChild("objectCharacteristics", PREMIS_V3_NS)
                    .getChild("objectCharacteristicsExtension", PREMIS_V3_NS)
                    .getChild("fits", FITS_NS)
                    .getChild("metadata", FITS_NS);

            if (fitsMd != null) {
                Element imgMd = fitsMd.getChild("image", FITS_NS);
                if (imgMd != null) {
                    String imgHeight = imgMd.getChildTextTrim("imageHeight", FITS_NS);
                    String imgWidth = imgMd.getChildTextTrim("imageWidth", FITS_NS);
                    return formatExtent(imgHeight, imgWidth, fits.getPid().getQualifiedId());
                }

                Element videoMd = fitsMd.getChild("video", FITS_NS);
                if (videoMd != null) {
                    var trackInfo = videoMd.getChildren("track", FITS_NS);
                    if (trackInfo != null) {
                        return formatVideoExtent(trackInfo, fits.getPid().getQualifiedId());
                    }
                }

                Element audioMd = fitsMd.getChild("audio", FITS_NS);
                if (audioMd != null) {
                    var audioTime = formatTime(audioMd);
                    return "xx" + audioTime;
                }
            }

            return null;
        } catch (RepositoryException | FedoraException e) {
            log.warn("Unable to parse FITS for {}", fitsId, e);
            return null;
        }
    }

    private String formatVideoExtent(List<Element> trackInfo, String pid) {
        var numTracks = trackInfo.size();
        var videoTrack = 0;
        var audioTrack = 0;

        if (numTracks > 1) {
            for (int i = 0; i < numTracks; i++) {
                var type = trackInfo.get(i).getAttributeValue("type");
                if (type.equals("video")) {
                    videoTrack = i;
                }
                if (type.equals("audio")) {
                    audioTrack = i;
                }
            }
        }

        var videoInfo = trackInfo.get(videoTrack);
        var videoTime = formatTime(videoInfo);
        var videoHeight = videoInfo.getChildTextTrim("width", FITS_NS);
        var videoWidth = videoInfo.getChildTextTrim("height", FITS_NS);

        // Some videos have separate tracks for the audio content
        String audioTime = null;
        if (numTracks > 1) {
            audioTime = formatTime(trackInfo.get(audioTrack));
        }

        // Audio and video don't necessarily have the same play times, so select the larger one if both are present
        String trackTime = (audioTime == null || Integer.parseInt(videoTime) >= Integer.parseInt(audioTime))
                ? videoTime : audioTime;
        var extent = formatExtent(videoHeight, videoWidth, pid);
        return (extent == null) ? "xx" + trackTime : extent + "x" + trackTime;
    }

    private String formatTime(Element durationElement) {
        var durationMilliseconds = durationElement.getChild("milliseconds", FITS_NS);
        var duration  = durationElement.getChild("duration", FITS_NS);

        if (durationMilliseconds != null) {
            return normalizeTime(durationMilliseconds.getTextTrim());
        }

        if (duration != null) {
            return normalizeTime(duration.getTextTrim());
        }

        return "-1";
    }

    private String normalizeTime(String duration) {
        String output = "";
        Pattern pattern = Pattern.compile("\\d+:\\d+:\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(duration);
        boolean matchFound = matcher.find();

        if (matchFound) {
            var durationParts = duration.split(":");
            var hoursToSeconds = Integer.parseInt(durationParts[0]) * 60 * 60;
            var minutesToSeconds = Integer.parseInt(durationParts[1]) * 60 ;
            var seconds = Integer.parseInt(durationParts[2]);

            output += hoursToSeconds + minutesToSeconds + seconds;
        } else {
            output += millisecondsToSeconds(duration);
        }

        return output;
    }

    private Integer millisecondsToSeconds(String duration) {
        var durationToSeconds = Integer.parseInt(duration) / 1000.0;
        return (int) Math.ceil(durationToSeconds);
    }

    private String formatExtent(String imgHeight, String imgWidth, String id) {
        if (!StringUtils.isBlank(imgHeight) && !StringUtils.isBlank(imgWidth)) {
            try {
                var adjustedHeight = Integer.parseInt(imgHeight.replaceAll("[\\sa-zA-Z]", ""));
                var adjustedWidth = Integer.parseInt(imgWidth.replaceAll("[\\sa-zA-Z]", ""));
                return adjustedHeight + "x" + adjustedWidth;
            } catch (NumberFormatException e) {
                log.warn("Invalid image width or height from FITS {}: {} x {}",
                        id, imgWidth, imgHeight);
                return null;
            }
        }

        return null;
    }

    /**
     * Adds a list of Datastream objects from the provided list of binaries.
     * If the datastreams are being recorded on an object  other than their owning
     * file object, the pid of the owning file object is recorded
     *
     * @param dsList
     * @param binList list of binaries
     * @param ownedByOtherObject
     */
    private void addDatastreams(List<Datastream> dsList, List<BinaryObject> binList, boolean ownedByOtherObject) {
        binList.forEach(binary -> {
                Resource binaryResc = binary.getResource();

                String name = binaryResc.getURI();
                name = name.substring(name.lastIndexOf('/') + 1);

                String mimetype = binaryResc.hasProperty(Ebucore.hasMimeType) ?
                        binaryResc.getProperty(Ebucore.hasMimeType).getString() : null;
                Long filesize = binaryResc.hasProperty(Premis.hasSize) ?
                        binaryResc.getProperty(Premis.hasSize).getLong() : null;
                // Making assumption that there is only one checksum
                String checksum = getFirstChecksum(binaryResc);

                String filename = binaryResc.hasProperty(Ebucore.filename) ?
                        binaryResc.getProperty(Ebucore.filename).getString() : null;
                int extensionIndex = filename != null ? filename.lastIndexOf('.') : -1;
                String extension = extensionIndex == -1 ? "" : filename.substring(extensionIndex + 1);

                String owner = ownedByOtherObject ? binary.getPid().getId() : null;

                String extentValue = (name.equals(ORIGINAL_FILE.getId()) &&
                        mimetype != null && (mimetype.startsWith("image") || mimetype.startsWith("video")
                        || mimetype.startsWith("audio"))) ? getExtent(binList) : null;
                dsList.add(new DatastreamImpl(owner, name, filesize, mimetype,
                        filename, extension, checksum, extentValue));
            });
    }

    private String getFirstChecksum(Resource resc) {
        Statement prop = resc.getProperty(Premis.hasMessageDigest);
        if (prop == null) {
            return null;
        }
        return prop.getResource().getURI();
    }

    private List<String> getDatastreamStrings(List<Datastream> datastreams) {
        return datastreams.stream()
                .map(Datastream::toString)
                .collect(Collectors.toList());
    }

    /**
     * Returns the sum of filesizes for all datastreams which do not belong to
     * other objects
     *
     * @param datastreams
     * @return
     */
    private long getFilesizeTotal(List<Datastream> datastreams) {
        return datastreams.stream()
            .filter(ds -> ds.getFilesize() != null && ds.getOwner() == null)
            .mapToLong(Datastream::getFilesize)
            .sum();
    }

    private long getFilesize(List<Datastream> datastreams) throws IndexingException {
        Optional<Datastream> original = datastreams.stream()
                .filter(ds -> ORIGINAL_FILE.getId().equals(ds.getName()))
                .findFirst();

        if (original.isEmpty()) {
            throw new IndexingException("File object in invalid state, cannot find original file binary");
        }

        Long size = original.get().getFilesize();
        return size != null ? size : 0l;
    }

    private void addDerivatives(List<Datastream> dsList, PID pid, boolean ownedByOtherObject, List<DatastreamType> types) {
        derivativeService.getDerivatives(pid).forEach(deriv -> {
            DatastreamType type = deriv.getType();
            // only add derivatives of types listed
            if ((types != null) && !types.contains(type)) {
                return;
            }

            String extentValue = null;
            if (type.equals(JP2_ACCESS_COPY)) {
                var dimInfo = jp2InfoService.getDimensions(deriv.getFile().toPath());
                extentValue = dimInfo.getExtent();
            }

            String owner = (ownedByOtherObject ? pid.getId() : null);
            dsList.add(createDatastream(deriv, owner, extentValue));
        });
    }

    /**
     * Used to selectively add only thumbnail datastreams
     *
     * @param workObject the work object with the thumbnail relation
     * @param datastreams work object's datastreams to add thumbnail streams to
     */
    private void addThumbnailDerivatives(WorkObject workObject, List<Datastream> datastreams) {
        FileObject thumbnailObject = workObject.getThumbnailObject();

        if (thumbnailObject != null) {
            var updatedDatastreams = clearPreviousThumbnailDatastreams(datastreams);
            addDerivatives(updatedDatastreams, thumbnailObject.getPid(), true, THUMBNAIL_DS_TYPES);
        }
    }

    /**
     *  There may be thumbnail streams from the primary object, so we'll clear those
     *  before adding the assigned thumbnail datastreams
     *
     * @param datastreams full list of datastreams to index for the work object
     * @return modified list of datastreams without thumbnail datastreams
     */
    private List<Datastream> clearPreviousThumbnailDatastreams(List<Datastream> datastreams) {
        datastreams.removeIf(ds -> THUMBNAIL_DS_TYPES.contains(DatastreamType.getByIdentifier(ds.getName())));
        return datastreams;
    }

    private DatastreamImpl createDatastream(DerivativeService.Derivative derivative, String owner, String extent) {
        DatastreamType type = derivative.getType();
        String name = type.getId();
        String mimetype = type.getMimetype();
        String extension = type.getExtension();
        File file = derivative.getFile();
        Long filesize = file.length();
        String filename = file.getName();
        return new DatastreamImpl(owner, name, filesize, mimetype, filename, extension, null, extent);
    }

    /**
     * @param derivativeService the derivativeService to set
     */
    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }

    public void setTechnicalMetadataService(TechnicalMetadataService technicalMetadataService) {
        this.technicalMetadataService = technicalMetadataService;
    }

    public void setJp2InfoService(Jp2InfoService jp2InfoService) {
        this.jp2InfoService = jp2InfoService;
    }
}
