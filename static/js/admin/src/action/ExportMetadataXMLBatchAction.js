define('ExportMetadataXMLBatchAction', [ 'jquery', 'AbstractBatchAction', "tpl!templates/admin/exportMetadataForm"], function($, AbstractBatchAction, exportMetadataTemplate) {

	function ExportMetadataXMLBatchAction(context) {
		this._create(context);
	};
	
	ExportMetadataXMLBatchAction.prototype.constructor = ExportMetadataXMLBatchAction;
	ExportMetadataXMLBatchAction.prototype = Object.create( AbstractBatchAction.prototype );
	
	ExportMetadataXMLBatchAction.prototype.isValidTarget = function(target) {
		return target.isSelected() && target.isEnabled() && $.inArray("editDescription", target.metadata.permissions) != -1;
	};
	
	ExportMetadataXMLBatchAction.prototype.getTargets = function(targets) {
		if (this.context.targets) {
			return this.context.targets;
		} 
		return AbstractBatchAction.prototype.getTargets.call(this);
	};
	
	ExportMetadataXMLBatchAction.prototype.execute = function() {
		var self = this;

		this.targets = this.getTargets();
		var title;
		var defaultType;
		if (this.targets.length == 1) {
			title = "Export XML metadata for " + this.targets[0].metadata.title.substring(0, 30);
		} else {
			title = "Export XML metadata for " + this.targets.length + " objects";
		}
		
		// Retrieve the last email address used by this user
		var onyen = this.context.view.resultData.onyen;
		
		var emailAddress = localStorage.getItem("send_to_address_" + onyen);
		if (!emailAddress) {
			emailAddress = this.context.view.resultData.email;
			if (!emailAddress && onyen) {
				// No email, so generate one
				emailAddress = onyen + "@email.unc.edu";
			}
		}
		
		var exportMetadataForm = exportMetadataTemplate({email : emailAddress});
		this.dialog = $("<div class='containingDialog'>" + exportMetadataForm + "</div>");
		this.dialog.dialog({
			autoOpen: true,
			width: 'auto',
			minWidth: '500',
			height: 'auto',
			modal: true,
			title: title
		});
		this.$form = this.dialog.first();
		
		this.$form.submit(function(e){
			var email = $("#xml_recipient_email", self.$form).val();
			var includeChildren = $("#export_xml_include_children", self.$form).prop("checked");
			var excludeNoDs = $("#export_xml_exclude_no_datastreams", self.$form).prop("checked");
			var datastreamTypes = $.map($("input[name='export_xml_metadata_types']:checkbox:checked"), function(e,i) {
			    return e.value;
			});
			
			if (!email || !$.trim(email)) {
				return false;
			}
			localStorage.setItem("send_to_address_" + onyen, email);

			if (datastreamTypes.length == 0 && excludeNoDs) {
				self.context.view.$alertHandler.alertHandler("error", "Must select at least one metadata type for export, or include objects with no returned datastreams.");
				return false;
			}

			var pids = [];
			for (var index in self.targets) {
				pids.push(self.targets[index].pid);
			}
			
			$.ajax({
				url : "/services/api/edit/exportXML",
				type : "POST",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				data : JSON.stringify({
					email : email,
					pids : pids,
					exportChildren: includeChildren || false,
					onlyIncludeValidDatastreams: excludeNoDs || false,
					datastreams: datastreamTypes || []
				})
			}).done(function(response) {
				self.context.view.$alertHandler.alertHandler("message", response.message);
				self.dialog.remove();
			}).fail(function() {
				self.context.view.$alertHandler.alertHandler("error", "Failed to export " + self.targets.length + " objects");
			});
			
			e.preventDefault();
		});
	}
	
	return ExportMetadataXMLBatchAction;
});