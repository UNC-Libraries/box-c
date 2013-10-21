define('IndexingMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/indexingMonitorJob', 'tpl!../templates/admin/statusMonitor/indexingMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, indexingMonitorJobTemplate, indexingMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "indexing",
		jobConfig : {
			url : "/services/api/v1/indexing/jobs?begin=0&end=20",
			template : indexingMonitorJobTemplate,
			detailsUrl : "/services/api/v1/indexing/jobs/job/{id}",
			detailsTemplate : indexingMonitorDetailsTemplate,
			fields : ["Status", "Label", "Action", "Progress"],
			jobTypes : [
				{name : "all", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/api/v1/indexing"
		}
	};
			
	function IndexingMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	IndexingMonitor.prototype.constructor = IndexingMonitor;
	IndexingMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return IndexingMonitor;
});