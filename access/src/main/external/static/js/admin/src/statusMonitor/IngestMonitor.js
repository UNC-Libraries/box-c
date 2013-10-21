define('IngestMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/ingestMonitorJob', 'tpl!../templates/admin/statusMonitor/ingestMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, ingestMonitorJobTemplate, ingestMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "ingest",
		jobConfig : {
			url : "/services/api/v1/ingest/{name}/",
			template : ingestMonitorJobTemplate,
			detailsUrl : "/services/api/v1/ingest/job/{id}",
			detailsTemplate : ingestMonitorDetailsTemplate,
			fields : ["Status", "Submitter", "Submit time", "Ingested", "First object", "Note"],
			jobTypes : [
				{name : "active", refresh : 5000, detailsRefresh : 1000},
				{name : "queued", refresh : 10000},
				{name : "finished", refresh : 10000},
				{name : "failed", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/api/v1/ingest/"
		}
	};
			
	function IngestMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	IngestMonitor.prototype.constructor = IngestMonitor;
	IngestMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return IngestMonitor;
});