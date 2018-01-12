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
<link rel="stylesheet" type="text/css" href="/static/css/admin/performance_visualizations.css" />
<div class="row">
	<div class="col-md-12">
		<p id="loader" class="text-center">Loading...</p>
		<h3 class="text-center dim">CDR Performance Metrics</h3>
	</div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Throughput by Deposit</h4>
        <div id="throughput-uuid-stats"></div>
        <svg id="throughput-deposit"></svg>
    </div>
    <div class="col-md-12">
        <h4 class="text-center">Throughput by Date</h4>
        <div id="throughput-stats"></div>
        <svg id="throughput-date"></svg>
        <svg id="throughput-date-brush"></svg>
    </div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Files Processed By Ingest (<span id="files-by-ingest-text">Total Files Ingested</span>)</h4>
        <div id="files-by-ingest-stats"></div>
        <div class="text-center">
            <div class="btn-group files-uuid" role="group">
                <button type="button" class="btn btn-default" id="throughput_files">Total Files Ingested</button>
                <button type="button" class="btn btn-default" id="avg_filesize">Avg. Filesize Ingested (MB)</button>
            </div>
        </div>
        <svg id="files-by-ingest"></svg>
    </div>
    <div class="col-md-12">
        <h4 class="text-center">Files Processed By Day (<span id="files-by-day-text">Total Files Ingested</span>)</h4>
        <div id="files-by-day-stats"></div>
        <div class="text-center">
            <div class="btn-group files" role="group">
                <button type="button" class="btn btn-default" id="all_throughput_files">Total Files Ingested</button>
                <button type="button" class="btn btn-default" id="all_avg_filesize">Avg. Filesize Ingested (MB)</button>
            </div>
        </div>
        <svg id="files-by-day"></svg>
        <svg id="files-by-day-brush"></svg>
    </div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Deposit Duration By Deposit (<span id="duration-date-text">Total Time</span> Seconds)</h4>
        <div id="duration-date-stats"></div>
        <div class="text-center">
            <div class="btn-group time-uuid" role="group">
                <button type="button" class="btn btn-default" id="total_time">Total Time</button>
                <button type="button" class="btn btn-default" id="queued_duration">Queued Time</button>
                <button type="button" class="btn btn-default" id="ingest_duration">Ingest Time</button>
            </div>
        </div>
        <svg id="duration-date"></svg>
        
    </div>
    <div class="col-md-12">
        <h4 class="text-center">Deposit Duration By Total (<span id="duration-total-date-text">Total Time</span> Seconds)</h4>
        <div id="duration-total-date-stats"></div>
        <div class="text-center">
            <div class="btn-group time" role="group">
                <button type="button" class="btn btn-default" id="all_total_time">Total Time</button>
                <button type="button" class="btn btn-default" id="all_queued_duration">Queued Time</button>
                <button type="button" class="btn btn-default" id="all_ingest_duration">Ingest Time</button>
            </div>
        </div>
        <svg id="duration-total-date"></svg>
        <svg id="duration-total-date-brush"></svg>
    </div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Total Deposits (<span id="total-deposits-date-text">Completed Deposits</span>)</h4>
        <div id="failed-deposits-stats"></div>
        <div class="text-center">
            <div class="btn-group total-deposits" role="group">
                <button type="button" class="btn btn-default" id="dep_finished">Completed Deposits</button>
                <button type="button" class="btn btn-default" id="dep_failed_deposit">Failed Deposits</button>
            </div>
        </div>
        <svg id="total-deposits-date"></svg>
        <svg id="total-deposits-date-brush"></svg>
    </div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Move Operations</h4>
        <div id="moves-date-stats"></div>
        <svg id="moves-date"></svg>
        <svg id="moves-date-brush"></svg>
    </div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Finished Enhancements (<span id="enh-date-text">All</span>)</h4>
         <div class="text-center">
            <div class="btn-group enh" role="group">
                <button type="button" class="btn btn-default" id="enh_finished_all_enh">All</button>
                <button type="button" class="btn btn-default" id="enh_image_enh">Image</button>
                <button type="button" class="btn btn-default" id="enh_metadata_enh">Metadata</button>
                <button type="button" class="btn btn-default" id="enh_solr_enh">Solr</button>
                <button type="button" class="btn btn-default" id="enh_fulltext_enh">Fulltext</button>
                <button type="button" class="btn btn-default" id="enh_thumbnail_enh">Thumbnail</button>
            </div>
        </div>
        <div id="enh-date-stats"></div>
        <svg id="enh-date"></svg>
        <svg id="enh-date-brush"></svg>
    </div>
</div>
<div class="row dim">
    <div class="col-md-12">
        <h4 class="text-center">Failed Enhancements (<span id="failed-enh-date-text">All</span>)</h4>
        <div id="failed-enh-stats"></div>
         <div class="text-center">
            <div class="btn-group failed-enh" role="group">
                <button type="button" class="btn btn-default" id="failed_all_enh">All</button>
                <button type="button" class="btn btn-default" id="failed_image_enh">Image</button>
                <button type="button" class="btn btn-default" id="failed_metadata_enh">Metadata</button>
                <button type="button" class="btn btn-default" id="failed_solr_enh">Solr</button>
                <button type="button" class="btn btn-default" id="failed_fulltext_enh">Fulltext</button>
                <button type="button" class="btn btn-default" id="failed_thumbnail_enh">Thumbnail</button>
            </div>
        </div>
        <svg id="failed-enh-date"></svg>
        <svg id="failed-enh-date-brush"></svg>
    </div>
</div>
<script src="/static/js/admin/performance_visualizations/assets/d3/d3.min.js" charset="utf-8"></script>
<script src="/static/js/admin/performance_visualizations/assets/d3-queue/d3-queue.min.js"></script>
<script src="/static/js/admin/performance_visualizations/assets/d3-jetpack/d3-jetpack.min.js"></script>
<script src="/static/js/admin/performance_visualizations/assets/simple-statistics/simple-statistics-regression-only.js"></script>
<script src="/static/js/admin/performance_visualizations/assets/ramda/ramda-merge-with-key-only.js"></script>
<script src="/static/js/admin/performance_visualizations/helpers.js"></script>
<script src="/static/js/admin/performance_visualizations/draw.js"></script>
<script src="/static/js/admin/performance_visualizations/load.js"></script>