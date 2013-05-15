define([ 'jquery', 'jquery-ui', 'PID'], function(
		$, ui, PID) {
	$.widget("cdr.structureEntry", {
		options : {
			indentSuppressed : false
		},
		
		_create : function() {
			var pid = this.element.attr("data-pid");
			if (pid)
				this.pid = new PID(pid);
			
			this.contentLoaded = false;
			
			this.$entry = this.element.children(".entry");
			if (!this.options.indentSuppressed && this.$entry.length == 0)
				this.element.addClass('suppressed');
			this.$childrenContainer = this.element.children(".children");
			if (this.$childrenContainer.children().length > 0)
				this.element.addClass("expanded");
			
			this.contentUrl = this.$entry.children(".cont_toggle").attr("data-url");
			
			this._initToggleContents();
			
			// Render indent
			this._renderIndent();
		},
		
		_initToggleContents : function() {
			var self = this;
			// Setup expand/collapse based on class
			this.$entry.children(".cont_toggle").click(function() {
				var $toggleButton = $(this);
				if ($toggleButton.hasClass('expand')) {
					if (!self.contentLoaded && self.contentUrl) {
						var loadingImage = $("<img src=\"/static/images/ajax_loader.gif\"/>");
						$(this).after(loadingImage);
						$.ajax({
							url: self.contentUrl,
							success: function(data){
								loadingImage.remove();
								if (data) {
									// Adjust existing indents if the child container already has contents
									var $existingLastSibling = self.$childrenContainer.children('.last_sib');
									if ($existingLastSibling.length > 0)
										$existingLastSibling.removeClass('last_sib').addClass('with_sib');
									
									var $newEntries = $(data).children('.children').find('.entry_wrap');
									if ($newEntries.length > 0) {
										self.$childrenContainer.append($newEntries);
										$newEntries.structureEntry(this.options);
										// Add in the new items
										self.$childrenContainer.find(".indent").show();
										self.$childrenContainer.show(100, function() {
											self.element.addClass("expanded");
										});
									}
								}
								self.contentLoaded = true;
							},
							error: function(xhr, ajaxOptions, thrownError){
								loadingImage.remove();
							}
						});
					} else {
						self.$childrenContainer.find(".indent").show();
						self.$childrenContainer.show(100, function() {
							self.element.addClass("expanded");
						});
					}
					$toggleButton.removeClass('expand').addClass('collapse');
				} else {
					self.$childrenContainer.hide(100, function() {
						self.element.removeClass("expanded");
					});
					$toggleButton.removeClass('collapse').addClass('expand');
				}
				return false;
			});
		},
	
		// Get contents, populate
		// Wrap tree
		_renderIndent : function () {
			var $entry = this.element.children('.entry'),
				$ancestors = this.element.parents(".entry_wrap:not(.suppressed)"),
				lastTier = $ancestors.length;
			if (lastTier == 0)
				return;
			$ancestors = $($ancestors.get().reverse());
			$ancestors.push(this.element);
			
			$ancestors.each(function(i){
				if (i == 0)
					return;
				var hasSiblings = $(this).next(".entry_wrap").length > 0;
				if (i == lastTier) {
					if (hasSiblings) {
						$entry.before("<div class='indent with_sib'></div>");
					} else {
						$entry.before("<div class='indent last_sib'></div>");
					}
				} else {
					if (hasSiblings) {
						$entry.before("<div class='indent'></div>");
					} else {
						$entry.before("<div class='indent empty'></div>");
					}
				}
			});
		}
	});
});