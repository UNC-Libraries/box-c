define('EditCollectionSettingsAction', ['jquery', 'underscore', 'RemoteStateChangeMonitor', 'AlertHandler', 'tpl!../templates/admin/editCollectionSettings'], 
	function($, _, RemoteStateChangeMonitor, alertHandler, editSettingsTemplate) {
			
	function EditCollectionSettingsAction(context) {
		this.context = context;
	};
		
	EditCollectionSettingsAction.prototype.execute = function() {
		var self = this;
		
		this.dialog = $("<div class='containingDialog'>Loading...</div>");
		this.dialog.dialog({
			autoOpen: true,
			width: '560',
			height: 'auto',
			modal: true,
			title: "Collection Settings"
		});
		
		$.ajax({
			url : "editCollection/" + this.context.target.pid,
			type : "GET"
		}).done(function(collectionSettings){
			var editSettingsForm = editSettingsTemplate({settings : collectionSettings});
			
			self.dialog.html(editSettingsForm);
			self.$form = self.dialog.first();
			var defaultViewSelect = $("#full_record_default_view", self.$form);
			
			function selectEntry(checkbox) {
				var listEntry = checkbox.parent();
				if (checkbox.is(":checked")) {
					listEntry.addClass("selected");
					var defaultEntry = defaultViewSelect.children("option[value='" + listEntry.data("viewid") + "']");
					defaultEntry.prop("disabled", false);
					if (defaultViewSelect.children("option:enabled").length == 1) {
						defaultEntry.prop("selected", true);
					}
				} else {
					listEntry.removeClass("selected");
					var defaultEntry = defaultViewSelect.children("option[value='" + listEntry.data("viewid") + "']");
					defaultEntry.prop("disabled", true);
					if (defaultEntry.prop("selected")) {
						defaultViewSelect.children("option:enabled:not(:selected)").first().prop("selected", true);
					}
				}
			}
			
			function toggleChecked(checkbox, checked) {
				if (checked === undefined) {
					checked = !checkbox.prop("checked");
				}
				selectEntry(checkbox.prop("checked", checked));
			}
			
			// Event to select entire entry when checkbox checked
			self.$form.on("change", ".selectable_multi input[type='checkbox']", function(e){
				var $this = $(this);
				selectEntry($this);
			});
			
			// Event to select entry when row clicked
			self.$form.on("click", ".selectable_multi li", function(e){
				toggleChecked($(this).find("input"));
			});
			
			// Select starting defaultView
			if (collectionSettings.defaultView) {
				defaultViewSelect.children("option[value='" + collectionSettings.defaultView + "']").prop("selected", true);
			}
			
			// Mark starting selected views
			$.each(collectionSettings.viewInfo, function(viewKey) {
				var checkbox = $("#full_record_views_select li[data-viewid='" + viewKey + "']", self.$form).find("input");
				toggleChecked(checkbox, $.inArray(viewKey, collectionSettings.views) != -1);
			});
			
			// Enable help text
			$(".help", self.$form).qtip({
				style : {
					classes : "qtip-admin qtip-rounded qtip-light"
				}
			});
			
			self.$form.submit(function(e){
				var selectedViews = $("#full_record_views_select .selected", self.$form);
				var views = [];
				for (var i = 0; i < selectedViews.length; i++) {
					views.push(selectedViews.eq(i).data("viewid"));
				}
				var defaultView = $("#full_record_default_view", self.$form).val();
		
				$.ajax({
					url : "editCollection/" + self.context.target.pid,
					type : "POST",
					contentType: "application/json; charset=utf-8",
					dataType: "json",
					data : JSON.stringify({
						views : views,
						defaultView : defaultView
					})
				}).done(function(reponse) {
					self.context.view.$alertHandler.alertHandler("message", "Updated collection settings.");
					self.dialog.remove();
				}).fail(function() {
					self.context.view.$alertHandler.alertHandler("error", "Failed to update collection settings");
				});
		
				e.preventDefault();
			});
		});
	}
	
	return EditCollectionSettingsAction;
});