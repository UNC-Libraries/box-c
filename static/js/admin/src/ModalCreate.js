define('ModalCreate', [ 'jquery', 'jquery-ui', 'underscore', 'dompurify', 'ResultObject'],
	function($, ui, _, DOMPurify, ResultObject) {

		function ModalCreate(options) {
			this.options = options;
		}

		ModalCreate.prototype.formContents = function(resultObject) {
			var pid;
			var metadata;

			if (resultObject instanceof ResultObject) {
				pid = resultObject.metadata.id;
				metadata = _.mapObject(resultObject.metadata, function (value, key) {
					if (resultObject.metadata[key] instanceof String) {
						return DOMPurify.sanitize(value, { ALLOWED_TAGS: ['#text'] });
					}
					return value;
				});
				this.resultObject = resultObject;
			} else {
				pid = resultObject;
			}

			return this.options.createFormTemplate({pid : pid, metadata: metadata, options : this.options});
		};

		ModalCreate.prototype.modalDialog = function(dialog, self) {
			return dialog.dialog({
				autoOpen: true,
				width: 'auto',
				minWidth: '400',
				height: 'auto',
				modal: true,
				title: self.options.title,
				beforeClose: $.proxy(self.close, self)
			});
		};

		return ModalCreate;
	});