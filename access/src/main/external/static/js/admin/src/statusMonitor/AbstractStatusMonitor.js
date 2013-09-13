define('AbstractStatusMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'tpl!../templates/admin/statusMonitor/overview', 'tpl!../templates/admin/statusMonitor/details', 'moment'], 
		function($, ui, _, overviewTemplate, detailsTemplate) {

	var defaultOptions = {
		name : undefined,
		jobConfig : {
			url : undefined,
			detailsUrl : undefined,
		},
		overviewConfig : {
			url : undefined,
			refresh : 10000,
			render : undefined,
			template : overviewTemplate
		}
	};

	function AbstractStatusMonitor() {
	};
	
	AbstractStatusMonitor.prototype.init = function() {
		this.monitorId = "status_monitor_" + this.options.name.replace(" ", "_");
		this.element = $("<div></div>").attr("id", this.monitorId);
		this.jobConfig = $.extend({}, this.options.jobConfig);
		this.overviewConfig = $.extend({}, this.options.overviewConfig);
		this.initializeOverview();
		this.createJobTable();
		for (var index in this.options.jobConfig.jobTypes) {
			var jobType = this.options.jobConfig.jobTypes[index];
			this.initializeJobType(jobType);
		}
		this.createDetailsView();
		return this;
	};
	
	AbstractStatusMonitor.prototype.createJobTable = function() {
		var jobContainer = $("<div/>").addClass("status_monitor_job_container").appendTo(this.element);
		this.jobTable = $("<table id='" + this.options.name + "_monitor_jobs' class='status_monitor_jobs'></table>").appendTo(jobContainer);
		var $colgroup = $("<colgroup/>").appendTo(this.jobTable);
		var $headerRow = $("<tr class='column_headers'/>").appendTo(this.jobTable);
		for (var index in this.jobConfig.fields) {
			$colgroup.append("<col class='col_" + this.jobConfig.fields[index].replace(" ", "_") + "'></col>")
			$headerRow.append("<th>" + this.jobConfig.fields[index] + "</th>");
		}
		// Box shadow for first row to support webkit
		$("<div/>").addClass("column_headers_shadow").appendTo(jobContainer);
	};
	
	AbstractStatusMonitor.prototype.createDetailsView = function() {
		this.detailsView = $(detailsTemplate(this.options)).appendTo(this.element);
		this.detailsContent = this.detailsView.find(".status_details_content");
		this.detailsView.find(".hide_status_details").click($.proxy(this.deactivateDetails, this));
	};
	
	AbstractStatusMonitor.prototype.deactivateDetails = function() {
		if (this.detailsType)
			clearTimeout(this.detailsType.repeatId);
		this.element.removeClass("show_details");
	};
	
	// Deactivate this status monitor, preventing it from refreshing any further
	AbstractStatusMonitor.prototype.deactivate = function() {
		this.active = false;
		// Cancel all the refresh timeouts
		for (var index in this.options.jobConfig) {
			var jobType = this.jobConfig[index];
			clearTimeout(jobType.repeatId);
		}
		return this;
	};
	
	// Activate this status monitor, and begin refreshing status
	AbstractStatusMonitor.prototype.activate = function() {
		this.active = true;
		// Start refreshing of overview panel
		this.refreshType(this.overviewConfig, true);
		// Start refreshing of job types
		for (var index in this.jobConfig.jobTypes) {
			var jobType = this.jobConfig.jobTypes[index];
			this.refreshType(jobType, true);
		}
		return this;
	};
	
	// Setup the overview panel for this monitor
	AbstractStatusMonitor.prototype.initializeOverview = function() {
		this.overviewPanel = $("<div></div>").appendTo(this.element);
		if (!this.overviewConfig.render)
			this.overviewConfig.render = this.renderOverview;
	};
	
	// Render the overview panel
	AbstractStatusMonitor.prototype.renderOverview = function(typeConfig) {
		this.overviewPanel.html(typeConfig.template({data : typeConfig.results, type : typeConfig, dateFormat : this.dateFormat}));
	};
	
	// Instantiate the configuration and placement for the provided job type
	AbstractStatusMonitor.prototype.initializeJobType = function(jobBase) {
		var jobType = $.extend({}, this.options.jobConfig, jobBase);
		jobType.url = jobType.url.replace("{name}", jobType.name);
		jobType.detailsUrl = jobType.detailsUrl.replace("{name}", jobType.name);
		if (!('render' in jobType))
			jobType.render = this.renderJobType;
		
			// Instantiate the placeholder where these types of jobs will be located and store reference in config
		var placeholder = $("<tr id='" + this.options.name + "_monitor_ph_" + jobType.name + "'></tr>");
		jobType.placeholder = placeholder;
		this.jobTable.append(placeholder);
		// Register click events for viewing details
		var self = this;
		this.jobTable.on("click", ".monitor_job." + jobType.name, function(){
			// Cleanup previous details callback if there is one
			if (self.detailsType)
				clearTimeout(self.detailsType.repeatId);
			var $this = $(this); 
			$this.siblings(".selected").removeClass("selected");
			$this.addClass("selected");
			var jobId = this.getAttribute("data-id");
			var detailsType = $.extend({}, jobType);
			// Store the currently active details definition
			self.detailsType = detailsType;
			detailsType.url = jobType.detailsUrl.replace("{id}", jobId);
			detailsType.id = jobId;
			detailsType.template = jobType.detailsTemplate;
			detailsType.render = self.renderJobDetails;
			self.refreshType(detailsType, true);
			self.element.addClass("show_details");
		});
		// Add job type to config
		this.jobConfig.jobTypes.push(jobType);
	};
	
	// Default job type rendering, calls the job type's template for each job result
	AbstractStatusMonitor.prototype.renderJobType = function(typeConfig) {
		$(".monitor_job." + typeConfig.name).remove();
		for (var index in typeConfig.results.jobs) {
			var selected = this.detailsType && typeConfig.results.jobs[index].id == this.detailsType.id;
			typeConfig.placeholder.after(typeConfig.template({data : typeConfig.results.jobs[index], type : typeConfig, dateFormat : this.dateFormat, selected : selected}));
		}
	};
	
	AbstractStatusMonitor.prototype.renderJobDetails = function(typeConfig) {
		this.detailsContent.html(typeConfig.template({data : typeConfig.results, type : typeConfig, dateFormat : this.dateFormat}));
	};
	
	// Standard date format function to be use in templates
	AbstractStatusMonitor.prototype.dateFormat = function(dateObject) {
		return moment(dateObject).format("MM/DD/YYYY h:mma");
	};
	
	// Refresh results from a type configuration.  This applies to both overview types and job types.
	// If repeat is true, then the refresh will repeat until interrupted
	AbstractStatusMonitor.prototype.refreshType = function(typeConfig, repeat) {
		var self = this;
		$.getJSON(typeConfig.url, {}, function(json) {
			// Store results in this job type
			typeConfig.results = json;
			// Update display
			typeConfig.render.call(self, typeConfig);
			// If this a repeating refresh, start the next repeat
			if (repeat) {
				typeConfig.repeatId = setTimeout(function() {
					if (self.active)
						self.refreshType(typeConfig, repeat);
				}, typeConfig.refresh);
			}
		});
	};
	
	AbstractStatusMonitor.prototype.getDefaultOptions = function() {
		return defaultOptions;
	};
	
	return AbstractStatusMonitor;
});