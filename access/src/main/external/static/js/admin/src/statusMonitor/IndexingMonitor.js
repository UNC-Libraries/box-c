define('IndexingMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/indexingMonitorJob', 'tpl!../templates/admin/statusMonitor/indexingMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, indexingMonitorJobTemplate, indexingMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "indexing",
		jobConfig : {
			url : "/services/api/status/indexing/{name}",
			template : indexingMonitorJobTemplate,
			detailsUrl : "/services/api/status/indexing/job/{id}",
			detailsTemplate : indexingMonitorDetailsTemplate,
			fields : ["Status", "Label", "Action", "Progress"],
			jobTypes : [
				{name : "active", refresh : 2000, detailsRefresh : 2000},
				{name : "queued", refresh : 5000},
				{name : "finished", refresh : 10000},
				{name : "failed", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/api/status/indexing"
		}
	};
			
	function IndexingMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	IndexingMonitor.prototype.constructor = IndexingMonitor;
	IndexingMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return IndexingMonitor;
});