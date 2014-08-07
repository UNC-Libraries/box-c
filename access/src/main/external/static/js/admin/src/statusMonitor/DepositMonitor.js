define('DepositMonitor', [ 'jquery', 'jquery-ui', 'underscore', 'AbstractStatusMonitor', 'tpl!../templates/admin/statusMonitor/depositMonitorJob', 'tpl!../templates/admin/statusMonitor/depositMonitorJobDetails'],
		function($, ui, _, AbstractStatusMonitor, depositMonitorJobTemplate, depositMonitorDetailsTemplate) {
			
	var defaultOptions = {
		name : "deposit",
		jobConfig : {
			url : "/services/api/status/deposit/{name}",
			template : depositMonitorJobTemplate,
			detailsUrl : "/services/api/status/deposit/{id}",
			detailsTemplate : depositMonitorDetailsTemplate,
			fields : ["Status", "Submitter", "Submit time", "Progress", "First object", "Note"],
			jobTypes : [
				{name : "running", refresh : 1000, detailsRefresh : 1000},
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
	
	DepositMonitor.prototype.init = function() {
		AbstractStatusMonitor.prototype.init.call(this);
		
		$(this.element).on("click", ".monitor_action", function(){
			var $this = $(this);
			$this.text($this.text() + "...");
			$this.addClass("disabled");
			
			$.post($this.attr("href"), function(){
				
			});
			return false;
		});
	};
	
	DepositMonitor.prototype.renderJobType = function(typeConfig) {
		$(".monitor_job." + typeConfig.name).remove();
		var results = typeConfig.results;
		for (var index in results) {
			var selected = this.detailsType && results[index].uuid == this.detailsType.id;
			var result = results[index];
			if ("currentJob" in result) {
				var currentJob = result.currentJob;
				
				var jobName = currentJob.name.substring(currentJob.name.lastIndexOf(".") + 1);
				result["shortName"] = jobName;
				
				if (jobName == "IngestDeposit" && "total" in currentJob) {
					var completion = result.ingestedObjects? result.ingestedObjects : 0;
					completion += " / " + currentJob.total;
					result["completion"] = completion;
				}
			}
			
			if (result.state == "finished") {
				result["completion"] = result.ingestedObjects + " / " + result.ingestedObjects;
			}
			typeConfig.placeholder.after(typeConfig.template({data : result, type : typeConfig, dateFormat : this.dateFormat, selected : selected}));
		}
	};
		
	DepositMonitor.prototype.renderJobDetails = function(typeConfig) {
		var results = typeConfig.results;
		
		if ("currentJob" in results) {
			var currentJob = results.jobs[results.currentJob];
		}
		
		if ("jobs" in results) {
			for (var index in results.jobs) {
				var job = results.jobs[index];
				var jobName = job.name.substring(job.name.lastIndexOf(".") + 1);
				
				job["shortName"] = jobName;
				
				if (jobName == "IngestDeposit" && "total" in job) {
					var completion = results.ingestedObjects? results.ingestedObjects : 0;
					completion += " / " + job.total;
					job["completion"] = completion;
				}
			}
		}
		
		this.detailsContent.html(typeConfig.template({data : typeConfig.results, type : typeConfig, dateFormat : this.dateFormat}));
	};
		
	DepositMonitor.prototype.dateFormat = function(dateObject) {
		return moment(parseInt(dateObject)).format("MM/DD/YYYY h:mma");
	};
	
	return DepositMonitor;
});