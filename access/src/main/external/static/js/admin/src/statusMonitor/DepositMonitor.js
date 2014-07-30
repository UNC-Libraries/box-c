define('DepositMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/depositMonitorJob', 'tpl!../templates/admin/statusMonitor/depositMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, depositMonitorJobTemplate, depositMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "deposit",
		jobConfig : {
			url : "/services/api/status/deposit/{name}",
			template : depositMonitorJobTemplate,
			detailsUrl : "/services/api/status/deposit/{id}",
			detailsTemplate : depositMonitorDetailsTemplate,
			fields : ["Status", "Submitter", "Submit time", "Ingested", "First object", "Note"],
			jobTypes : [
				{name : "running", refresh : 5000, detailsRefresh : 1000},
				{name : "queued", refresh : 10000},
				{name : "paused", refresh : 10000},
				{name : "finished", refresh : 10000},
				{name : "cancelled", refresh : 10000},
				{name : "failed", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/api/status/deposit"
		}
	};
			
	function DepositMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	DepositMonitor.prototype.constructor = DepositMonitor;
	DepositMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return DepositMonitor;
});