define('StatusMonitorManager', [ 'jquery', 'jquery-ui', 'underscore', 'IngestMonitor'], 
		function($, ui, _, IngestMonitor) {
			
	function StatusMonitorManager(element, options) {
		this.element = element;
		this.tabList = $("<ul/>").attr("id", "status_monitor_tabs").appendTo(this.element);
		this.monitors = [];
		this.element.tabs();
		this.addMonitors();
		this.element.tabs("refresh");
		this.activeMonitorIndex = 0;
	};
	
	StatusMonitorManager.prototype.activate = function(index) {
		index = arguments.length > 0? index : this.activeMonitorIndex;
		this.monitors[index].activate();
	};
	
	StatusMonitorManager.prototype.addMonitors = function() {
		this.addMonitor(new IngestMonitor());
	};
	
	StatusMonitorManager.prototype.addMonitor = function(monitor) {
		this.monitors.push(monitor);
		monitor.init();
		monitor.element.appendTo(this.element);
		this.tabList.append("<li><a href='#" + monitor.monitorId + "'>" + monitor.options.name + "</a></li>");
	};
	
	return StatusMonitorManager;
});