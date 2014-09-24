<%--

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<div id="status_monitor">
	
</div>

<link rel="stylesheet" type="text/css" href="/static/css/admin/status_monitor.css" />
<script>
	var require = {
		config: {
		    statusMonitor : {
		    	'username' : '<%= request.getRemoteUser() %>',
		    	'isAdmin' : <%= edu.unc.lib.dl.acl.util.GroupsThreadStore.getGroups().contains(edu.unc.lib.dl.acl.util.AccessGroupConstants.ADMIN_GROUP) %>
		    }
		}
	};
</script>
<script type="text/javascript" src="/static/js/lib/require.js" data-main="/static/js/admin/statusMonitor"></script>