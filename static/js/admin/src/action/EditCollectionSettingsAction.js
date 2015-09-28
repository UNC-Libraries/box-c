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
			width: '600',
			height: '700',
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
			
			self.$form.on("change", ".selectable_multi input[type='checkbox']", function(e){
				var $this = $(this);
				if ($this.is(":checked")) {
					$this.parent().addClass("selected");
				} else {
					$this.parent().removeClass("selected default");
				}
			});
			
			self.$form.on("click", ".selectable_multi li", function(e){
				$(this).addClass("highlighted").siblings("li").removeClass("highlighted");
			}).on("dblclick", ".selectable_multi li", function(e){
				$(this).find("input").trigger('click');
			});
			
			$(".help", self.$form).qtip({
				style : {
					classes : "qtip-admin qtip-rounded qtip-light"
				}
			});
			
			$("#set_default_view").click(function(e){
				$(".highlighted", self.$form).addClass("default").siblings("li").removeClass("default");
			});
			
			// Select the already selected values
			if (collectionSettings.defaultView) {
				$("#full_record_views_select li[data-viewid='" + collectionSettings.defaultView + "']").addClass("default");
			}
			
			if (collectionSettings.views) {
				for (var i = 0; i < collectionSettings.views.length; i++) {
					$("#full_record_views_select li[data-viewid='" + collectionSettings.views[i] + "']").find("input").click();
				}
			}
			
			self.$form.submit(function(e){
				var selectedViews = $("#full_record_views_select .selected");
				var views = [];
				for (var i = 0; i < selectedViews.length; i++) {
					views.push(selectedViews.eq(i).data("viewid"));
				}
				var defaultView = $("#full_record_views_select .default").data("viewid");
		
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