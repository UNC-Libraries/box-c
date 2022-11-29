<div id="status_monitor">
	
</div>

<link rel="stylesheet" type="text/css" href="/static/css/admin/status_monitor.css" />
<script>
	var require = {
		config: {
		    statusMonitor : {
		    	'username' : '<%= request.getRemoteUser() %>'
		    }
		}
	};
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/admin/statusMonitor"></script>