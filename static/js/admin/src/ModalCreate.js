define('ModalCreate', [ 'jquery', 'jquery-ui', 'underscore', 'ResultObject'],
	function($, ui, _, ResultObject) {

		function ModalCreate(options) {
			this.options = options;
		}

		ModalCreate.prototype.formContents = function(resultObject) {
			var self = this;
			var pid;
			var metadata;

			if (resultObject instanceof ResultObject) {
				pid = resultObject.metadata.id;
				metadata = _.mapObject(resultObject.metadata, function (value, key) {
					return self.sanitizeText(value);
				});
				this.resultObject = resultObject;
			} else {
				pid = resultObject;
			}

			return this.options.createFormTemplate({pid : pid, metadata: metadata, options : this.options});
		};

		ModalCreate.prototype.sanitizeText = function(text) {
			const doc = new DOMParser().parseFromString(text, 'text/html');
			return doc.body.textContent || '';
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