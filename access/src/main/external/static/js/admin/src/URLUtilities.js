define('URLUtilities', ['jquery'], function($) {
	return {
		getParameter : function (name) {
			return decodeURI(
					(RegExp(name + '=' + '([^&]*?)(&|$)').exec(location.search)||[,null])[1]
			);
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
		}
	};
});