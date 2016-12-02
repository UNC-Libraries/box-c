define('ModalCreate', [ 'jquery', 'jquery-ui', 'ResultObject'],
	function($, ui, ResultObject) {

		function ModalCreate(options) {
			this.options = options;
		}

		ModalCreate.prototype.formContents = function(resultObject) {
			var pid;
			var metadata;
			if (resultObject instanceof ResultObject) {
				pid = resultObject.metadata.id;
				metadata = resultObject.metadata;
				this.resultObject = resultObject;
			} else {
				pid = resultObject;
			}

			return this.options.createFormTemplate({pid : pid, metadata: metadata});
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