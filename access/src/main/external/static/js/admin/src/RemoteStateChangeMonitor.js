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
			'checkErrorTarget' : undefined,
			'checkStatusAjax' : {
			}
		},
		pingId : null,
		pingData : null,
		
		init: function(options) {
			this.options = $.extend({}, this.defaultOptions, options);
			this.options.checkStatusAjax.success = $.proxy(this.pingSuccessCheck, this);
			this.options.checkStatusAjax.error = $.proxy(this.pingError, this);
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
		},
		
		pingError : function() {
			this.options.checkError.apply(this.options.checkErrorTarget, arguments);
			if (this.pingId != null) {
				clearInterval(this.pingId);
				this.pingId = null;
			}
		}
	});
	
	return RemoteStateChangeMonitor;
});