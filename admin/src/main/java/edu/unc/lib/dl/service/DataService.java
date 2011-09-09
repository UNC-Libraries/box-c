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
package edu.unc.lib.dl.service;

import edu.unc.lib.dl.schema.DataRequest;
import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.schema.FedoraDataRequest;
import edu.unc.lib.dl.schema.FedoraDataResponse;
import edu.unc.lib.dl.schema.ImageListResponse;
import edu.unc.lib.dl.schema.ImageViewRequest;
import edu.unc.lib.dl.schema.ImageViewResponseList;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.ListDatastreamsResponse;

public interface DataService {
	DataResponse getData(DataRequest dataRequest);

	DataResponse getData(String pid, String disseminator);

	ImageListResponse getImageDatastreamIds(String pid);

	ImageViewResponseList getImageViewList(IrUrlInfo irUrlInfo);

	ImageViewResponseList getImageViewList(ImageViewRequest imageViewRequest);

	ListDatastreamsResponse getDatastreams(String pid, String userid);

	FedoraDataResponse getFedoraDataUrl(FedoraDataRequest fedoraDataRequest);
}
