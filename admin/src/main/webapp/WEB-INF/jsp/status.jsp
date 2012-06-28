<%--

	Copyright 2008 The University of North Carolina at Chapel Hill

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

			http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ include file="include.jsp"%>
<%@ include file="../../html/head.html"%>
<link rel="stylesheet" href="../../css/jquery/ui/jquery-ui.css" type="text/css" media="all" />
<link rel="stylesheet" type="text/css" href="../../css/statusMonitor.css"/>
<script type="text/javascript" src="../../js/jquery/jquery.min.js"></script>
<script type="text/javascript" src="../../js/jquery/ui/jquery-ui.min.js"></script>
<script type="text/javascript" src="../../js/statusMonitor.js"></script>

<title><fmt:message key="admin.status.heading" /></title>
<%@ include file="../../html/admincontents.html"%>
<div id="content">
	<p class="breadcrumbs">
		<a href="<c:url value='/index.jsp'/>">Home</a> &gt; Status Monitors
	</p>
	<h2 class="fontface">Status Monitors</h2>

	<div id="serviceInfo"></div>
	<div id="servicetabs">
		<ul style="height: 37px">
			<li><a href="#servicetabs-1">Ingest</a>
			</li>
			<li><a href="#servicetabs-2">Indexing</a>
			</li>
			<li><a href="#servicetabs-3">Enhancement</a>
			</li>
			<li><a href="#servicetabs-4">Catchup</a>
			</li>
		</ul>
		<div id="servicetabs-1">
			<table>
				<tr class="narrow">
					<th>Active</th>
					<th>Idle</th>
					<th>Queued</th>
					<th>Active</th>
					<th>Failed</th>
					<th>Finished<sup>*</sup>
					</th>
					<th>Refreshed</th>
				</tr>
				<tr>
					<td><span id="ingestActive"></span>
					</td>
					<td><span id="ingestIdle"></span>
					</td>
					<td><span id="ingestQueuedJobs"></span>
					</td>
					<td><span id="ingestActiveJobs"></span>
					</td>
					<td><span id="ingestFailedJobs"></span>
					</td>
					<td><span id="ingestFinishedJobs"></span>
					</td>
					<td><span id="ingestRefreshed"></span>
					</td>
				</tr>
			</table>
			<p>Batch Ingest Jobs by Status</p>
			<table>
				<thead>
					<tr class="narrow">
						<th>status</th>
						<th>submitter</th>
						<th>submit time &uarr;</th>
						<th>ingested</th>
						<th>first object</th>
						<th>tracking note</th>
					</tr>
				</thead>
				<tbody id="ingestJobs">
					<tr class="queued-end" style="display: none">
						<td></td>
					</tr>
					<tr class="active-end" style="display: none">
						<td></td>
					</tr>
					<tr class="finished-end" style="display: none">
						<td></td>
					</tr>
					<tr class="failed-end" style="display: none">
						<td></td>
					</tr>
				</tbody>
			</table>
			<p>* Finished ingest jobs are removed after two days.</p>
		</div>
		<div id="servicetabs-2">
			<table>
				<tr class="narrow">
					<th>Active</th>
					<th>Idle</th>
					<th>Queued</th>
					<th>Active</th>
					<th>Refreshed</th>
				</tr>
				<tr>
					<td><span id="indexingActive"></span>
					</td>
					<td><span id="indexingIdle"></span>
					</td>
					<td><span id="indexingQueuedJobs"></span>
					</td>
					<td><span id="indexingActiveJobs"></span>
					</td>
					<td><span id="indexingRefreshed"></span>
					</td>
				</tr>
			</table>
			<p>Indexing Jobs</p>
			<div id="indexingData">
				<div class="detailsContainer">
					<div class="detailsColumn">
						<div class="detailsView">
							<h2>Details <a class="expandDetails">(expand)</a><img title="Refresh details" class="refreshDetailsButton" src="../../images/arrow_refresh.png"/></h2>
							<div class="detailsContent">
								<div id="indexingDetails" class="jobDetails"></div>
							</div>
						</div>
					</div>
				</div>
				<table class="statusList">
					<thead>
						<tr class="narrow">
							<th>status</th>
							<th>label</th>
							<th>action</th>
							<th>progress</th>
						</tr>
					</thead>
					<tbody id="indexingJobs">
						<tr class="jobs-end" style="display: none">
							<td></td>
						</tr>
					</tbody>
				</table>
			</div>
			<div style="clear:both;"></div>
		</div>
		<div id="servicetabs-3">
			<table>
				<tr class="narrow">
					<th>Active</th>
					<th>Idle</th>
					<th>Queued</th>
					<th>Active</th>
					<th>Failed</th>
					<th>Refreshed</th>
				</tr>
				<tr>
					<td><span id="enhancementActive"></span>
					</td>
					<td><span id="enhancementIdle"></span>
					</td>
					<td><span id="enhancementQueuedJobs"></span>
					</td>
					<td><span id="enhancementActiveJobs"></span>
					</td>
					<td><span id="enhancementFailedJobs"></span>
					</td>
					<td><span id="enhancementRefreshed"></span>
					</td>
				</tr>
			</table>
			<p>Enhancement Jobs by Status</p>
			<div id="enhancementData">
				<div class="detailsContainer">
					<div class="detailsColumn">
						<div class="detailsView">
							<h2>Details <a class="expandDetails">(expand)</a><img title="Refresh details" class="refreshDetailsButton" src="../../images/arrow_refresh.png"/></h2>
							<div class="detailsContent">
								<div id="enhancementDetails" class="jobDetails"></div>
							</div>
						</div>
					</div>
				</div>
				<table class="statusList">
					<thead>
						<tr class="narrow">
							<th>status</th>
							<th>label</th>
							<th>service(s)</th>
						</tr>
					</thead>
					<tbody id="enhancementJobs">
						<tr class="active-end" style="display: none">
							<td></td>
						</tr>
						<tr class="blocked-end" style="display: none">
							<td></td>
						</tr>
						<tr class="queued-end" style="display: none">
							<td></td>
						</tr>
						<tr class="finished-end" style="display: none">
							<td></td>
						</tr>
						<tr class="failed-end" style="display: none">
							<td></td>
						</tr>
					</tbody>
				</table>
			</div>
			
			<div style="clear:both;"></div>
		</div>
		<div id="servicetabs-4">
			<table id="catchupStatus">
				<tr class="narrow">
					<th>Enabled</th>
					<th>Active</th>
					<th>Items Processed</th>
					<th>This Session</th>
					<th>Services</th>
					<th>Refreshed</th>
				</tr>
				<tr class="loadJobs" style="display: none">
					<td colspan="6"><img src="../../images/load.gif"/></td>
				</tr>
				<tr>
					<td><span id="catchupEnabled"></span>
					</td>
					<td><span id="catchupActive"></span>
					</td>
					<td><span id="catchupItemsProcessed"></span>
					</td>
					<td><span id="catchupItemsProcessedThisSession"></span>
					</td>
					<td><span id="catchupServices"></span>
					</td>
					<td><span id="catchupRefreshed"></span>
					</td>
				</tr>
			</table>
			<p>Catchup candidates</p>
			<table>
				<thead>
					<tr class="narrow">
						<th>Service</th>
						<th>label</th>
						<th>pid</th>
					</tr>
				</thead>
				<tbody id="catchupJobs">
					<tr class="loadJobs" style="display: none">
						<td colspan="3"><img src="../../images/load.gif"/></td>
					</tr>
					<tr class="TechnicalMetadataEnhancementService-end" style="display: none">
						<td></td>
					</tr>
					<tr class="ImageEnhancementService-end" style="display: none">
						<td></td>
					</tr>
					<tr class="ThumbnailEnhancementService-end" style="display: none">
						<td></td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
	<%@ include file="../../html/footer.html"%>