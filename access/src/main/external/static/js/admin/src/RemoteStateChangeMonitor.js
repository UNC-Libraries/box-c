define('RemoteStateChangeMonitor', ['jquery'], function($) {
	function RemoteStateChangeMonitor(options) {
		this.init(options);
	};
	
	$.extend(RemoteStateChangeMonitor.prototype, {
		defaultOptions : {
			'pingFrequency' : 1000,
			'statusChanged' : undefined,
			'statusChangedTarget' : undefined,
			'checkStatus' : undefined,
			'checkStatusTarget' : undefined,
			'checkStatusAjax' : {
			}
		},
		pingId : null,
		pingData : null,
		
		init: function(options) {
			this.options = $.extend({}, this.defaultOptions, options);
			this.options.checkStatusAjax.success = $.proxy(this.pingSuccessCheck, this);
		},
		
		performPing : function() {
			if (this.pingData)
				this.options.checkStatusAjax.data = this.pingData;
			$.ajax(this.options.checkStatusAjax);
		},
		
		pingSuccessCheck : function(data) {
			var isDone = this.options.checkStatus.call(this.options.checkStatusTarget, data);
			if (isDone) {
				if (this.pingId != null) {
					clearInterval(this.pingId);
					this.pingId = null;
				}
				this.options.statusChanged.call(this.options.statusChangedTarget, data);
			} else if (this.pingId == null) {
				this.pingId = setInterval($.proxy(this.performPing, this), this.options.pingFrequency);
			}
		}
	});
	
	return RemoteStateChangeMonitor;
});