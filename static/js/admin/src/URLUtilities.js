define('URLUtilities', ['jquery'], function($) {
	return {
		uriEncodeParameters : function(url) {
			var newParameterString = "", tempArray = url.split("?");
			if (tempArray.length < 2)
				return url;
			var baseURL = tempArray[0], parameterString = tempArray[1];
			if (parameterString) {
				tempArray = parameterString.split("&");
				for (var i=0; i<tempArray.length; i++){
					if (newParameterString.length > 0)
						newParameterString += '&';
					var paramPair = tempArray[i].split('=');
					newParameterString += paramPair[0] + "=" + encodeURIComponent(paramPair[1]);
				}
			}
			return baseURL + "?" + newParameterString;
		},
		
		getParameter : function (name) {
			var value = RegExp(name + '=' + '([^&]*?)(&|$)').exec(location.search);
			if (value == null)
				return null;
			return decodeURI(value[1]);
		},

		getAllParameters: function(url) {
			var parameters = url.split('?');

			if (parameters.length > 0) {
				return '?' + parameters[1];
			}

			return null;
		},
		
		setParameter : function(url, key, paramVal){
			var baseURL = this.removeParameter(url, key);
			if (baseURL.indexOf('?') == -1)
				baseURL += '?';
			else baseURL += '&';
			return baseURL + key + "=" + paramVal;
		},
		
		removeParameter : function (url, key) {
			var newParameterString = "", tempArray = url.split("?");
			var baseURL = tempArray[0], parameterString = tempArray[1];
			if (parameterString) {
				tempArray = parameterString.split("&");
				for (var i=0; i<tempArray.length; i++){
					if(tempArray[i].split('=')[0] != key){
						if (newParameterString.length > 0)
							newParameterString += '&';
						newParameterString += tempArray[i];
					}
				}
			}
			if (newParameterString)
				return baseURL + "?" + newParameterString;
			return baseURL;
		},

		getAdminUrl: function() {
			return this.getServerUrl() + 'admin/';
		},

		getAccessUrl: function() {
			return this.getServerUrl();
		},

		getServerUrl: function(url) {
			return document.location.origin + "/";
		}
	};
});