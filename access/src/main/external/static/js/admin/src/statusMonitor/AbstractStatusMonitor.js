define('AbstractStatusMonitor', [ 'jquery', 'jquery-ui', 'underscore'], 
		function($, ui, _) {

	var defaultOptions = {
		jobConfig : [],
		jobUrl : undefined,
		jobDetailsUrl : undefined,
		overviewUrl : undefined,
		overviewRefresh : 10000
	};

	function AbstractStatusMonitor(options) {
		this.jobConfiguration = {
			fields : ["status", "submitter", "submit time", "ingested", "first object"],
			jobTypes : [
			{name : "active", refresh : 10000},
			{name : "queued", refresh : 10000},
			{name : "failed", refresh : 10000}
		]};
		this.jobUrl = "/services/rest/ingest/{name}/";
		this.jobDetailsUrl = "/services/rest/ingest/{name}/{id}";
		this.overviewRefreshRate;
		this.jobUrl; // with keyword replacement
		// overview url
	};
	
	AbstractStatusMonitor.prototype.init = function() {
		this.initializeOverview();
		for (var index in this.options.jobConfig) {
			var jobType = this.jobConfig[index];
			this.initializeJobType(jobType);
		}
		
	};
	
	AbstractStatusMonitor.prototype.deactivate = function() {
		this.active = false;
		// Cancel all the refresh timeouts
		for (var index in this.options.jobConfig) {
			var jobType = this.jobConfig[index];
			clearTimeout(jobType.repeatId);
		}
	};
	
	AbstractStatusMonitor.prototype.activate = function() {
		this.active = true;
		
		this.refreshType(this.overviewConfig, true);
		this.startRefresh();
	};
	
	AbstractStatusMonitor.prototype.initializeOverview = function() {
		this.overviewConfig = {
			url : this.options.overviewUrl,
			refresh : this.options.overviewRefresh,
			results : undefined
		};
	};
	
	AbstractStatusMonitor.prototype.initializeJobType = function(jobBase) {
		var jobType = $.extend({}, jobBase);
		if (!('url' in jobType))
			jobType = this.options.jobUrl.replace("{name}", jobType.name);
		this.jobConfig.jobTypes.push(jobType);
	};
	
	AbstractStatusMonitor.prototype.startRefresh = function() {
		var self = this;
		for (var index in this.jobConfig.jobTypes) {
			var jobType = this.jobConfig[index];
			this.refreshJobType(jobType, true);
		}
	};
	
	AbstractStatusMonitor.prototype.refreshType = function(typeConfig, repeat) {
		var self = this;
		$.getJSON(typeConfig.url, {}, function(json) {
			// Store results in this job type
			typeConfig.results = json;
			// Update display
			
			// If this a repeating refresh, start the next repeat
			if (repeat) {
				typeConfig.repeatId = setTimeout(function() {
					if (self.active)
						self.refreshJobType(typeConfig, repeat);
				}, typeConfig.refresh);
			}
		});
	};
	
	AbstractStatusMonitor.prototype.defaultOptions = function() {
		return defaultOptions;
	};
			
});