define('ResourceTypeUtilities', [], function() {
	return {
		getIconNameForType : function(resourceType) {
			if (resourceType == "File") {
				return "fas fa-file";
			} else if (resourceType == "Work") {
				return "fas fa-copy";
			} else if (resourceType == "Collection") {
				return "fas fa-archive";
			} else if (resourceType == "AdminUnit") {
				return "fas fa-university";
			} else if (resourceType == "Folder") {
				return "fas fa-folder";
			} else if (resourceType == "ContentRoot") {
				return "fas fa-square";
			}
			return "fas fa-question-circle";
		}
	};
});