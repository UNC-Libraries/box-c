define('PID', ['jquery'], function($) {
	function PID(pidString) {
		this.init(pidString);
	};
	
	$.extend(PID.prototype, {
		uriPrefix: "info:fedora/",
		pid: null,
		
		init: function(pidString) {
			if (pidString.indexOf(this.uriPrefix) == 0) {
				this.pid = pidString.substring(this.uriPrefix.length());
			} else {
				this.pid = pidString;
			}
		},
		
		getPid: function() {
			return this.pid;
		},
		
		getURI: function() {
			return this.urlPrefix + this.pid;
		},
		
		getPath: function() {
			return this.pid.replace(":", "/");
		}
	});
	
	return PID;
});