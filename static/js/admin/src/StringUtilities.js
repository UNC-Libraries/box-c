define('StringUtilities', ['jquery'], function($) {
	return {
		readableFileSize : function(size) {
			var fileSize = 0;
			if (size > 1024 * 1024 * 1024)
				fileSize = (Math.round(size * 100 / (1024 * 1024 * 1024)) / 100).toString() + 'gb';
			if (size > 1024 * 1024)
				fileSize = (Math.round(size * 100 / (1024 * 1024)) / 100).toString() + 'mb';
			else
				fileSize = (Math.round(size * 100 / 1024) / 100).toString() + 'kb';
			return fileSize;
		}
	};
});