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
package edu.unc.lib.dl.service.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.BasicQueryResponse;
import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.schema.ConferenceMetadata;
import edu.unc.lib.dl.schema.ConferenceMetadataComplexType;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.ImageViewResponseList;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.ItemInfoRequest;
import edu.unc.lib.dl.schema.ItemInfoResponse;
import edu.unc.lib.dl.schema.JournalArticleMetadata;
import edu.unc.lib.dl.schema.JournalArticleMetadataComplexType;
import edu.unc.lib.dl.schema.JournalCollectionMetadata;
import edu.unc.lib.dl.schema.JournalCollectionMetadataComplexType;
import edu.unc.lib.dl.schema.ResearchItemMetadata;
import edu.unc.lib.dl.schema.ResearchItemMetadataComplexType;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.service.GatherRelsExtInformationService;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.service.ItemInfoService;
import edu.unc.lib.dl.service.SearchService;
import edu.unc.lib.dl.service.ViewSelectionService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.UtilityMethods;

public class ItemInfoServiceImpl implements ItemInfoService {
	protected final Log logger = LogFactory.getLog(getClass());
	private UtilityMethods utilityMethods;
	private String baseInstUrl;
	private IdService idService;
	private DataService dataService;
	private SearchService searchService;
	private ViewSelectionService viewSelectionService;
	private GatherRelsExtInformationService gatherRelsExtInformationService;
	private TripleStoreQueryService tripleStoreQueryService;

	public boolean isContainer(IrUrlInfo irUrlInfo) {
		Id id = new Id();
		id.setUid("unknown");
		id.setType(id.getUid());
		
		PID pid = tripleStoreQueryService.fetchByRepositoryPath(irUrlInfo
				.getFedoraUrl());
		
		return tripleStoreQueryService.isContainer(pid);
	}

	
	public ItemInfoResponse getItemInfo(ItemInfoRequest itemInfoRequest) {
		ItemInfoResponse itemResponse = new ItemInfoResponse();
		String stableUrl = itemInfoRequest.getIrUrlInfo().getDecodedUrl();

		Map map = gatherRelsExtInformationService
				.getAllFromIrUrlInfo(itemInfoRequest.getIrUrlInfo());

		itemResponse.setBaseInstUrl(baseInstUrl);
		itemResponse.setCreator(getString(map.get(Constants.RI_CREATOR)));
		itemResponse.setDate(getDate((String) map.get(Constants.RI_DATE)));
		itemResponse
				.setDescription(getString(map.get(Constants.RI_DESCRIPTION)));
		itemResponse.setIrUrlInfo(itemInfoRequest.getIrUrlInfo());
		itemResponse.setTitle(getTitle(map));
		itemResponse.setText(getString(map.get(Constants.RI_TEXT)));
		itemResponse.setType(getString(map.get(Constants.RI_TYPE)));
		itemResponse.setContentModel(getString(map
				.get(Constants.RI_CONTENT_MODEL)));
		itemResponse
				.setThumbnail(getThumbnail(map.get(Constants.RI_THUMBNAIL)));
		itemResponse
				.setDescription(getString(map.get(Constants.RI_DESCRIPTION)));
		itemResponse.setCollection(getString(map.get(Constants.RI_COLLECTION)));
		itemResponse.getSubjects().addAll((List) map.get(Constants.RI_SUBJECT));
		itemResponse.getBreadcrumbs().addAll(
				getBreadcrumbs(stableUrl, itemResponse.getTitle()));
		itemResponse.setStableUrl(stableUrl);
		itemResponse.getDatastreams().addAll(
				getDatastreams((List) map.get(Constants.RI_DATASTREAM),
						stableUrl, (Map<String, String>) map
								.get(Constants.RI_DATASTREAM_LABEL)));

		itemResponse.setRights(getString(map.get(Constants.RI_RIGHTS)));

		itemResponse.setNextConstituent(getInfoUrl(map
				.get(Constants.RI_NEXT_CONSTITUENT)));
		itemResponse.setPrevConstituent(getInfoUrl(map
				.get(Constants.RI_PREV_CONSTITUENT)));

		itemResponse.setPublisher(getString(map.get(Constants.RI_PUBLISHER)));
		itemResponse.setIssued(getString(map.get(Constants.RI_ISSUED)));
		itemResponse.setTextSurrogate(getString(map
				.get(Constants.RI_DESCRIPTION)));

		BasicQueryResponseList basicQueryResponseList = searchService
				.getChildren(stableUrl);

		itemResponse.getBasicQueryResponse().addAll(
				basicQueryResponseList.getBasicQueryResponse());

		List<BasicQueryResponse> list = itemResponse.getBasicQueryResponse();

		BasicQueryResponseList imageViewResponseList = searchService
				.getImageViewUrl(stableUrl);

//		if (imageViewResponseList.getBasicQueryResponse().size() > 0) {
//			itemResponse.setImageView(getString(imageViewResponseList
//					.getBasicQueryResponse().get(0).getImageView()));
//		} else {
//			itemResponse.setImageView("");
//		}

		if ((itemResponse.getType() != null)
				&& ((itemResponse.getType()
						.equals(Constants.RESOURCE_TYPE_JOURNAL_ARTICLE))
						|| (itemResponse.getType()
								.equals(Constants.RESOURCE_TYPE_JOURNAL_FRONT_MATTER))
						|| (itemResponse.getType()
								.equals(Constants.RESOURCE_TYPE_JOURNAL_BACK_MATTER)) || (itemResponse
						.getType()
						.equals(Constants.RESOURCE_TYPE_JOURNAL_CONTENTS)))) {
			ImageViewResponseList imageViewResponse = dataService
					.getImageViewList(itemInfoRequest.getIrUrlInfo());
			itemResponse.getImages().addAll(imageViewResponse.getImages());
		}

		JournalCollectionMetadata journalCollectionMetadata = null;
		JournalCollectionMetadataComplexType journalCollectionMetadataComplexType = new JournalCollectionMetadataComplexType();

		if ((itemResponse.getType() != null)
				&& (itemResponse.getType()
						.equals(Constants.RESOURCE_TYPE_JOURNAL_COLLECTION))) {
			Id id = idService.getId(itemInfoRequest.getIrUrlInfo());
			DataResponse dataResponse = dataService.getData(id.getPid(),
					"COLL_MD");

			try {
				JAXBContext jContext = JAXBContext
						.newInstance("edu.unc.lib.dl.schema");

				Unmarshaller unmarshaller = jContext.createUnmarshaller();

				journalCollectionMetadata = (JournalCollectionMetadata) unmarshaller
						.unmarshal(new ByteArrayInputStream(dataResponse
								.getDissemination().getStream()));

				journalCollectionMetadataComplexType
						.setCountry(journalCollectionMetadata.getCountry());
				journalCollectionMetadataComplexType
						.setCoverage(journalCollectionMetadata.getCoverage());
				journalCollectionMetadataComplexType
						.setIssn(journalCollectionMetadata.getIssn());
				journalCollectionMetadataComplexType
						.setLanguage(journalCollectionMetadata.getLanguage());
				journalCollectionMetadataComplexType
						.setOclc(journalCollectionMetadata.getOclc());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		itemResponse
				.setJournalCollectionMetadata(journalCollectionMetadataComplexType);

		if (journalCollectionMetadata == null) {
			JournalCollectionMetadataComplexType temp = new JournalCollectionMetadataComplexType();
			temp.setCountry("");
			temp.setCoverage("");
			temp.setIssn("");
			temp.setLanguage("");
			temp.setOclc("");

			itemResponse.setJournalCollectionMetadata(temp);
		}

		JournalArticleMetadata journalArticleMetadata = null;
		JournalArticleMetadataComplexType journalArticleMetadataComplexType = new JournalArticleMetadataComplexType();

		if ((itemResponse.getType() != null)
				&& ((itemResponse.getType()
						.equals(Constants.RESOURCE_TYPE_JOURNAL_ARTICLE))
						|| (itemResponse.getType()
								.equals(Constants.RESOURCE_TYPE_JOURNAL_CONTENTS))
						|| (itemResponse.getType()
								.equals(Constants.RESOURCE_TYPE_JOURNAL_FRONT_MATTER)) || (itemResponse
						.getType()
						.equals(Constants.RESOURCE_TYPE_JOURNAL_BACK_MATTER)))) {
			Id id = idService.getId(itemInfoRequest.getIrUrlInfo());
			DataResponse dataResponse = dataService.getData(id.getPid(), "XML"
					+ (String) map.get(Constants.RI_ORDER));

			try {
				JAXBContext jContext = JAXBContext
						.newInstance("edu.unc.lib.dl.schema");

				Unmarshaller unmarshaller = jContext.createUnmarshaller();

				journalArticleMetadata = (JournalArticleMetadata) unmarshaller
						.unmarshal(new ByteArrayInputStream(dataResponse
								.getDissemination().getStream()));

				journalArticleMetadataComplexType
						.setPages(journalArticleMetadata.getPages());
				journalArticleMetadataComplexType
						.setLanguage(journalArticleMetadata.getLanguage());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		itemResponse
				.setJournalArticleMetadata(journalArticleMetadataComplexType);

		if (journalArticleMetadata == null) {
			JournalArticleMetadataComplexType temp = new JournalArticleMetadataComplexType();
			temp.setLanguage("");
			temp.setPages("");

			itemResponse.setJournalArticleMetadata(temp);
		}

		ConferenceMetadata conferenceMetadata = null;
		ConferenceMetadataComplexType conferenceMetadataComplexType = new ConferenceMetadataComplexType();

		if ((itemResponse.getType() != null)
				&& (itemResponse.getType()
						.equals(Constants.RESOURCE_TYPE_CONFERENCE_COLLECTION))) {
			Id id = idService.getId(itemInfoRequest.getIrUrlInfo());
			DataResponse dataResponse = dataService.getData(id.getPid(),
					"CONF_MD");

			try {
				JAXBContext jContext = JAXBContext
						.newInstance("edu.unc.lib.dl.schema");

				Unmarshaller unmarshaller = jContext.createUnmarshaller();

				conferenceMetadata = (ConferenceMetadata) unmarshaller
						.unmarshal(new ByteArrayInputStream(dataResponse
								.getDissemination().getStream()));

				conferenceMetadataComplexType.setDateRange(conferenceMetadata
						.getDateRange());
				conferenceMetadataComplexType.setLocation(conferenceMetadata
						.getLocation());
				conferenceMetadataComplexType.setSponsor(conferenceMetadata
						.getSponsor());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		itemResponse.setConferenceMetadata(conferenceMetadataComplexType);

		if (conferenceMetadata == null) {
			ConferenceMetadataComplexType temp = new ConferenceMetadataComplexType();
			temp.setDateRange("");
			temp.setLocation("");
			temp.setSponsor("");

			itemResponse.setConferenceMetadata(temp);
		}

		ResearchItemMetadata researchItemMetadata = null;
		ResearchItemMetadataComplexType researchItemMetadataComplexType = new ResearchItemMetadataComplexType();

		if ((itemResponse.getType() != null)
				&& ((itemResponse.getType()
						.equals(Constants.RESOURCE_TYPE_RESEARCH_ITEM)) || (itemResponse
						.getType()
						.equals(Constants.RESOURCE_TYPE_CONFERENCE_ITEM)))) {
			Id id = idService.getId(itemInfoRequest.getIrUrlInfo());
			DataResponse dataResponse = dataService.getData(id.getPid(), "XML"
					+ (String) map.get(Constants.RI_ORDER));

			try {
				JAXBContext jContext = JAXBContext
						.newInstance("edu.unc.lib.dl.schema");

				Unmarshaller unmarshaller = jContext.createUnmarshaller();

				researchItemMetadata = (ResearchItemMetadata) unmarshaller
						.unmarshal(new ByteArrayInputStream(dataResponse
								.getDissemination().getStream()));

				researchItemMetadataComplexType.setSize(researchItemMetadata
						.getSize());
				researchItemMetadataComplexType.setPages(researchItemMetadata
						.getPages());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		itemResponse.setResearchItemMetadata(researchItemMetadataComplexType);

		if (researchItemMetadata == null) {
			ResearchItemMetadataComplexType temp = new ResearchItemMetadataComplexType();
			temp.setSize("");
			temp.setPages("");

			itemResponse.setResearchItemMetadata(temp);
		}

		itemResponse.setView(viewSelectionService
				.getViewForResourceType(itemResponse.getType()));

		return itemResponse;
	}

	private String getThumbnail(Object thumbnail) {
		if (thumbnail == null) {
			return "";
		}

		return utilityMethods.getThumbnailUrl((String) thumbnail);
	}

	// Get the labels for datastreams if they exist
	private List<String> getDatastreamLabels(Map datastreamLabels,
			List datastreams) {
		List<String> result = new ArrayList(datastreams.size());

		Object[] streams = datastreams.toArray();

		for (int i = 0; i < streams.length; i++) {
			String ds = (String) streams[i];
			String temp = ds.substring(ds.lastIndexOf('/') + 1);

			String value = (String) datastreamLabels.get(temp);

			if (value == null) {
				result.add(temp);
			} else {
				result.add(value);
			}
		}

		return result;
	}

	private String getTitle(Map map) {
		if (map.get(Constants.RI_TITLE) != null) {
			return getString(map.get(Constants.RI_TITLE));
		}

		return getString(map.get(Constants.RI_LABEL));
	}

	private String getUrl(Object object) {
		if (object == null)
			return "";

		return idService.getUrlFromPid((String) object);
	}

	private String getInfoUrl(Object object) {
		if (object == null)
			return "";

		StringBuilder sb = new StringBuilder(256);

		sb.append(Constants.IR_PREFIX).append(
				idService.getUrlFromPid((String) object));

		return sb.toString();
	}

	// This should format date at some point
	private String getDate(String dateString) {
		if (dateString == null) {

			if (logger.isDebugEnabled())
				logger.debug("getString value was null");

			return "";
			// return Constants.SEARCH_UNKNOWN_DATE;
		}

		StringBuilder sb = new StringBuilder(16);

		// assuming date is of the following format:
		// yyyy-mm-ddTHH:mm:ssZ
		// and for now we only care about date, not time

		// Get date
		int tindex = dateString.indexOf('T');
		if (tindex < 8) {
			return "";
		}

		String temp = dateString.substring(0, tindex);

		logger.debug("operating on date: " + temp);

		// Should be 0 - year, 1 - month, 2 - day in month
		String[] dateArray = temp.split("-");

		int year = Integer.parseInt(dateArray[0]);
		int month = Integer.parseInt(dateArray[1]);
		int day = Integer.parseInt(dateArray[2]);

		if (year > 0) {
			sb.append(year);
			if (month > 0) {
				sb.append("-").append(month);
				if (day > 0) {
					sb.append("-").append(day);
				}
			}

			return sb.toString();
		}

		return "";
	}

	private String getString(Object object) {
		if (object == null) {
			return "";
		}

		if (logger.isDebugEnabled())
			logger.debug("getString value was " + object);

		return (String) object;
	}

	private List getDatastreams(List datastreams, String stableUrl,
			Map datastreamLabels) {
		List result = new ArrayList(datastreams.size());

		String dataUrl = stableUrl.replaceAll(Constants.IR_PREFIX,
				Constants.DATA_PREFIX);

		Object[] ds = datastreams.toArray();

		if (logger.isDebugEnabled())
			logger.debug("getDatastreams ds.length: " + ds.length);

		for (int i = 0; i < ds.length; i++) {
			StringBuffer temp = new StringBuffer(256);
			String datastream = (String) ds[i];
			String dsName = datastream
					.substring(datastream.lastIndexOf('/') + 1);
			String dsLabel = (String) datastreamLabels.get(dsName);

			if (logger.isDebugEnabled())
				logger.debug("dsName: " + dsName);

			// if (("DC".equals(dsName)) || ("IRMD".equals(dsName))
			// || ("RELS-EXT".equals(dsName))
			// || ("MD_DESCRIPTIVE".equals(dsName))
			// || ("MD_ADMINISTRATIVE".equals(dsName))) {
			// continue;
			// }

			if (dsLabel == null) {
				dsLabel = dsName;
			}

			if (logger.isDebugEnabled())
				logger.debug("dsLabel: " + dsLabel);

			temp.append("<a href=\"").append(dataUrl).append("?ds=").append(
					dsName).append("\">").append(dsLabel).append("</a>");
			result.add(temp.toString());

			if (logger.isDebugEnabled())
				logger.debug(temp.toString());
		}

		return result;
	}

	private List getBreadcrumbs(String stableUrl, String title) {
		List result = new ArrayList();
		String prefix = Constants.IR_PREFIX.substring(0, Constants.IR_PREFIX
				.lastIndexOf('/'));
		String temp = stableUrl;

		result.add(title);

		int idx = 0;

		while ((idx = temp.lastIndexOf('/')) > 0) {
			temp = temp.substring(0, idx);

			if (temp.endsWith(prefix)) {
				break;
			} else {
				StringBuffer sb = new StringBuffer(256);
				String name = searchService.getTitleByUri(temp);

				sb.append("<a href=\"").append(temp).append("\">").append(name)
						.append("</a>");

				result.add(sb.toString());
			}
		}

		Collections.reverse(result);

		return result;
	}
	

	public void setIdService(IdService idService) {
		this.idService = idService;
	}

	public void setGatherRelsExtInformationService(
			GatherRelsExtInformationService gatherRelsExtInformationService) {
		this.gatherRelsExtInformationService = gatherRelsExtInformationService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}

	public void setViewSelectionService(
			ViewSelectionService viewSelectionService) {
		this.viewSelectionService = viewSelectionService;
	}

	public void setUtilityMethods(UtilityMethods utilityMethods) {
		this.utilityMethods = utilityMethods;
	}

	public void setBaseInstUrl(String baseInstUrl) {
		this.baseInstUrl = baseInstUrl;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

}
