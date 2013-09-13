define('EnhancementMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/enhancementMonitorJob', 'tpl!../templates/admin/statusMonitor/enhancementMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, enhancementMonitorJobTemplate, enhancementMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "enhancement",
		jobConfig : {
			url : "services/rest/enhancement/{name}?begin=0&end=20",
			template : enhancementMonitorJobTemplate,
			detailsUrl : "/services/rest/enhancement/job/{id}?type={name}",
			detailsTemplate : enhancementMonitorDetailsTemplate,
			fields : ["Status", "Label", "Enhancements", "Triggered by"],
			jobTypes : [
				{name : "active", refresh : 10000},
				{name : "queued", refresh : 10000},
				{name : "blocked", refresh : 10000},
				{name : "finished", refresh : 10000},
				{name : "failed", refresh : 10000}
			]
		},
		overviewConfig : {
			url : "/services/rest/enhancement"
		}
	};
			
	function EnhancementMonitor(options) {
		this.options = $.extend(true, {}, AbstractStatusMonitor.prototype.getDefaultOptions(), defaultOptions, options);
	}
	
	EnhancementMonitor.prototype.constructor = EnhancementMonitor;
	EnhancementMonitor.prototype = Object.create( AbstractStatusMonitor.prototype );
	
	return EnhancementMonitor;
});