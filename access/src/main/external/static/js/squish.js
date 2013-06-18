define('StructureEntry', [ 'jquery', 'jquery-ui'], function($, ui) {
	$.widget("cdr.structureEntry", {
		options : {
			indentSuppressed : false
		},
		
		_create : function() {
			this.pid = this.element.attr("data-pid");
			
			this.contentLoaded = false;
			
			this.$entry = this.element.children(".entry");
			if (this.$entry.length > 0) {
				this.contentUrl = this.$entry.children(".cont_toggle").attr("data-url");
			} else if (!this.options.indentSuppressed)
				this.element.addClass('suppressed');
			this.$childrenContainer = this.element.children(".children");
			if (this.$childrenContainer.children().length > 0)
				this.element.addClass("expanded");
			
			this.skipLastIndent = this.element.hasClass('view_all');
			
			this._initToggleContents();
			
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
									var $newEntries = $("> .children > .entry_wrap", $(data));
									if ($newEntries.length > 0) {
										// Adjust existing indents if the child container already has contents
										var $existingLastSibling = self.$childrenContainer.children('.entry_wrap').last();
										if ($existingLastSibling.length > 0)
											$existingLastSibling.children('.last_sib').removeClass('last_sib').addClass('with_sib');
										
										self.$childrenContainer.append($newEntries);
										$newEntries.structureEntry(this.options);
										// Add in the new items
										self.$childrenContainer.find(".indent").show();
										self.$childrenContainer.show(100, function() {
											self.element.addClass("expanded");
										});
									}
									if (self.$childrenContainer.children().length > 0)
										$toggleButton.removeClass('expand').addClass('collapse');
									else
										$toggleButton.removeClass('expand').addClass('leaf');
								}
								self.contentLoaded = true;
							},
							error: function(xhr, ajaxOptions, thrownError){
								loadingImage.remove();
							}
						});
					} else {
						if (self.$childrenContainer.children().length > 0) {
							self.$childrenContainer.find(".indent").show();
							self.$childrenContainer.show(100, function() {
								self.element.addClass("expanded");
							});
							$toggleButton.removeClass('expand').addClass('collapse');
						}
					}
				} else {
					if (self.$childrenContainer.children().length > 0) {
						self.$childrenContainer.hide(100, function() {
							self.element.removeClass("expanded");
						});
					}
					$toggleButton.removeClass('collapse').addClass('expand');
				}
				return false;
			});
		},
		
		refreshIndent : function() {
			this.element.children(".indent").remove();
			this._renderIndent();
		},
		
		_renderIndent : function () {
			var $entry = this.element.children('.entry'),
				$ancestors = this.element.parents(".entry_wrap:not(.suppressed)"),
				lastTier = $ancestors.length;
			if (lastTier == 0)
				return;
			$ancestors = $($ancestors.get().reverse());
			if (!this.skipLastIndent)
				$ancestors.push(this.element);
			
			$ancestors.each(function(i){
				if (i == 0)
					return;
				var hasSiblings = $(this).next(".entry_wrap:not(.view_all)").length > 0;
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
		},
		
		getParentURL : function() {
			var urlParts = this.contentUrl.replace(/&?root=false/, '').split(/\?(.+)/);
			urlParts[0] += "/parent";
			return urlParts[0] + ((urlParts.length > 1)? '?' + urlParts[1] : '');
		},
		
		insertTree : function($oldRoot) {
			var rootPid = $oldRoot.attr("data-pid");
			
			// Find the old root in the new results and remove it, while retaining an insertion point for the old root.
			var $oldRootDuplicate = this.element.find('[data-pid="' + rootPid + '"]');
			var $placeholder = $oldRootDuplicate.prev('.entry_wrap');
			$oldRootDuplicate.remove();
			
			// Insert the old root into the new results
			$oldRoot.detach();
			// Insertion point depends on if it is the first sibling
			var $refreshSet = $oldRoot.find(".entry_wrap").add($oldRoot);
			
			if ($placeholder.length == 0)
				this.element.children(".children").prepend($oldRoot);
			else {
				$placeholder.after($oldRoot);
				$refreshSet.add($placeholder);
			}
			this.element.addClass("expanded").children(".children").show();
			$refreshSet.structureEntry('refreshIndent');
		}
	});
});define('StructureView', [ 'jquery', 'jquery-ui', 'StructureEntry'], function($, ui) {
	$.widget("cdr.structureView", {
		options : {
			showResourceIcons : true,
			indentSuppressed : false,
			showParentLink : false
		},
		_create : function() {
			
			this.element.wrapInner("<div class='structure_content'/>");
			this.$content = this.element.children();
			this.element.addClass('structure');
			if (!this.options.showResourceIcons)
				this.element.addClass('no_resource_icons');
			
			if (this.options.showParentLink) {
				this._generateParentLink();
			}
			
			// Instantiate entries recursively
			this.$content.find(".entry_wrap").structureEntry({
				indentSuppressed : this.options.indentSuppressed
			});
		},
		
		_generateParentLink : function() {
			var self = this;
			var $parentLink = $("<a class='parent_link'>parent</a>");
			if (self.$content.children(".entry_wrap").hasClass('root'))
				$parentLink.addClass('disabled');
				
			$parentLink.click(function(){
				if ($parentLink.hasClass('disabled'))
					return false;
				var $oldRoot = self.$content.children(".entry_wrap");
				var parentURL = $oldRoot.structureEntry('getParentURL');
				$.ajax({
					url : parentURL,
					success : function(data) {
						var $newRoot = $(data);
						// Initialize the new results
						$newRoot.find(".entry_wrap").add($newRoot).structureEntry({
							indentSuppressed : self.options.indentSuppressed
						});
						$newRoot.structureEntry('insertTree', $oldRoot);
						self.$content.append($newRoot);
						if (self.$content.children(".entry_wrap").hasClass('root'))
							$parentLink.addClass('disabled');
					}
				});
				return false;
			});
			
			this.$content.before($parentLink);
		}
	});
});/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
/*
 * @author Ben Pennell
 */
define('AjaxCallbackButton', [ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor', 'ModalLoadingOverlay', 'ConfirmationDialog'], function(
		$, ui, PID, RemoteStateChangeMonitor) {
	$.widget("cdr.ajaxCallbackButton", {
		options : {
			pid : null,
			defaultLabel : undefined,
			workLabel : undefined,
			workPath : "",
			workDone : undefined,
			workDoneTarget : undefined,
			followup : undefined,
			followupTarget : undefined,
			followupPath : "",
			followupLabel : undefined,
			followupFrequency : 1000,
			completeTarget : undefined,
			parentElement : undefined,
			animateSpeed : 80,
			confirm : false,
			confirmMessage : "Are you sure?",
			confirmPositionElement : undefined,
			alertHandler : "#alertHandler"
		},

		_create : function() {
			if (!this.options.defaultLabel)
				this.options.defaultLabel = this.element.text();
			if (!this.options.workLabel)
				this.options.workLabel = this.element.text();
			if (this.options.workDoneTarget == undefined)
				this.options.workDoneTarget = this;
			if (this.options.completeTarget == undefined)
				this.options.completeTarget = this;
			if (this.options.followupTarget == undefined)
				this.options.followupTarget = this;
			if (this.options.setText == undefined)
				this.options.setText = this.setText;

			this.element.addClass("ajaxCallbackButton");

			this.alertHandler = $(this.options.alertHandler);
			
			if (this.options.pid !== undefined && this.options.pid != null) {
				if (this.options.pid instanceof PID)
					this.pid = this.options.pid;
				else
					this.pid = new PID(this.options.pid);
			}
			this.setWorkURL(this.options.workPath);
			
			this.followupId = null;
		},
		
		_init : function() {
			var op = this;
			
			if (this.options.followup) {
				this.setFollowupURL(this.options.followupPath);

				this.followupMonitor = new RemoteStateChangeMonitor({
					'checkStatus' : this.options.followup,
					'checkStatusTarget' : this.options.followupTarget,
					'statusChanged' : this.completeState,
					'statusChangedTarget' : this.options.completeTarget, 
					'checkStatusAjax' : {
						url : this.followupURL,
						dataType : 'json'
					}
				});
			}
			
			if (this.options.confirm) {
				var dialogOptions = {
						width : 200,
						modal : true
					};
				if (this.options.parentObject)
					dialogOptions['close'] = function() {
						op.options.parentObject.unhighlight();
					};
				if (this.options.confirmAnchor) {
					dialogOptions['position'] = {};
					dialogOptions['position']['of'] = this.options.confirmAnchor; 
				}
				
				this.element.confirmationDialog({
					'promptText' : this.options.confirmMessage,
					'confirmFunction' : this.doWork,
					'confirmTarget' : this,
					'dialogOptions' : dialogOptions
				});
			}

			this.element.text(this.options.defaultLabel);
			this.element.click(function() {
				op.activate.call(op);
				return false;
			});
			
			if (this.options.disabled){
				this.disable();
			} else {
				this.enable();
			}
		},
		
		activate : function() {
			if (this.options.disabled)
				return;
			if (this.options.confirm) {
				if (this.options.parentObject)
					this.options.parentObject.highlight();
				this.element.confirmationDialog("open");
			} else {
				this.doWork();
			}
		},

		doWork : function(workMethod, workData) {
			if (this.options.disabled)
				return;
			this.performWork($.get, null);
		},

		workState : function() {
			this.disable();
			if (this.options.parentObject) {
				this.options.parentObject.setState("working");
				this.options.parentObject.setStatusText(this.options.workLabel);
			} else {
				this.element.text(this.options.workLabel);
			}
		},

		performWork : function(workMethod, workData) {
			this.workState();
			var op = this;
			workMethod(this.workURL, workData, function(data, textStatus, jqXHR) {
				if (op.options.followup) {
					if (op.options.workDone) {
						try {
							var workSuccessful = op.options.workDone.call(op.options.workDoneTarget, data);
							if (!workSuccessful)
								throw "Operation was unsuccessful";
						} catch (e) {
							op.alertHandler.alertHandler('error', e);
							if (op.options.parentObject)
								op.options.parentObject.setState("failed");
						}
					}
					if (op.options.parentObject)
						op.options.parentObject.setState("followup");
					op.followupMonitor.performPing();
				} else {
					if (op.options.parentObject)
						op.options.parentObject.setState("idle");
					op.options.complete.call(op.options.completeTarget, data);
					op.enable();
				}
			}).fail(function(jqxhr, textStatus, error) {
				op.alertHandler.alertHandler('error', textStatus + ", " + error);
			});
		},

		disable : function() {
			this.options.disabled = true;
			this.element.css("cursor", "default");
			this.element.addClass("disabled");
			this.element.attr('disabled', 'disabled');
		},

		enable : function() {
			this.options.disabled = false;
			this.element.css("cursor", "pointer");
			this.element.removeClass("disabled");
			this.element.removeAttr('disabled');
		},

		setWorkURL : function(url) {
			this.workURL = url;
			this.workURL = this.resolveParameters(this.workURL);
		},

		setFollowupURL : function(url) {
			this.followupURL = url;
			this.followupURL = this.resolveParameters(this.followupURL);
		},

		resolveParameters : function(url) {
			if (!url || !this.pid)
				return url;
			return url.replace("{idPath}", this.pid.getPath());
		},

		destroy : function() {
			this.element.unbind("click");
		},

		followupState : function() {
			if (this.options.followupLabel != null) {
				if (this.options.parentObject)
					this.options.parentObject.setStatusText(this.options.followupLabel);
				else 
					this.element.text(this.options.followupLabel);

			}
		},

		completeState : function(data) {
			if (this.options.parentObject) {
				this.options.parentObject.setState("idle");
			}
			this.enable();
			this.element.text(this.options.defaultLabel);
		}
	});
});define('AlertHandler', ['jquery', 'jquery-ui', 'qtip'], function($) {
	$.widget("cdr.alertHandler", {
		_create: function() {
		    // Utilise delegate so we don't have to rebind for every qTip!
		    $(document).delegate('.qtip.jgrowl', 'mouseover mouseout', this.timer);
		},
		
		error: function(message) {
			this.showAlert(message, "error");
		},
		
		message: function(message) {
			this.showAlert(message, "info");
		},
		
		success: function(message) {
			this.showAlert(message, "success");
		},
		
		showAlert: function(message, type) {
			var target = $('.qtip.jgrowl:visible:last');
			var self = this;
			
			$(document.body).qtip({
	            content: {
	                text: message,
	                title: {
	                    text: "",
	                    button: true
	                }
	            },
	            position: {
	                my: 'top right',
	                at: (target.length ? 'bottom' : 'top') + ' right',
	                target: target.length ? target : $(window),
	                adjust: { 'y': 10, 'x' : target.length ? 0 : -5 },
	                effect: function(api, newPos) {
	                    $(this).animate(newPos, {
	                        duration: 200,
	                        queue: false
	                    });
	                    api.cache.finalPos = newPos; 
	                }
	            },
	            show: {
	                event: false,
	                // Don't show it on a regular event
	                ready: true,
	                // Show it when ready (rendered)
	                effect: function() {
	                    $(this).stop(0, 1).fadeIn(400);
	                },
	                // Matches the hide effect
	                delay: 0,
	                persistent: false
	            },
	            hide: {
	                event: false,
	                // Don't hide it on a regular event
	                effect: function(api) {
	                    // Do a regular fadeOut, but add some spice!
	                    $(this).stop(0, 1).fadeOut(400).queue(function() {
	                        // Destroy this tooltip after fading out
	                        api.destroy();
	 
	                        // Update positions
	                        self.updateGrowls();
	                    });
	                }
	            },
	            style: {
	                classes: 'jgrowl qtip-admin qtip-rounded alert-' + type,
	                tip: false
	            },
	            events: {
	                render: function(event, api) {
	                    // Trigger the timer (below) on render
	                    self.timer.call(api.elements.tooltip, event);
	                }
	            }
			}).removeData('qtip');
		},
		
		updateGrowls : function() {
	        // Loop over each jGrowl qTip
	        var each = $('.qtip.jgrowl'),
	            width = each.outerWidth(),
	            height = each.outerHeight(),
	            gap = each.eq(0).qtip('option', 'position.adjust.y'),
	            pos;
	 
	        each.each(function(i) {
	            var api = $(this).data('qtip');
	 
	            // Set target to window for first or calculate manually for subsequent growls
	            api.options.position.target = !i ? $(window) : [
	                pos.left + width, pos.top + (height * i) + Math.abs(gap * (i-1))
	            ];
	            api.set('position.at', 'top right');
	            
	            // If this is the first element, store its finak animation position
	            // so we can calculate the position of subsequent growls above
	            if(!i) { pos = api.cache.finalPos; }
	        });
	    },
		
		// Setup our timer function
	    timer : function(event) {
	        var api = $(this).data('qtip'),
	            lifespan = 5000;
	        
	        // If persistent is set to true, don't do anything.
	        if (api.get('show.persistent') === true) { return; }
	 
	        // Otherwise, start/clear the timer depending on event type
	        clearTimeout(api.timer);
	        if (event.type !== 'mouseover') {
	            api.timer = setTimeout(api.hide, lifespan);
	        }
	    }
	});
});define('BatchCallbackButton', [ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObjectList' ], function($, ui, ResultObjectList) {
	$.widget("cdr.batchCallbackButton", $.cdr.ajaxCallbackButton, {
		options : {
			resultObjectList : undefined,
			followupPath: "services/rest/item/solrRecord/version",
			childWorkLinkName : undefined,
			workFunction : undefined,
			followupFunction : undefined,
			completeFunction : undefined 
		},

		_create : function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);

			this.options.workDone = this.workDone;
			this.options.followup = this.followup;
			this.options.completeTarget = this;
		},
		
		_init : function() {
			$.cdr.ajaxCallbackButton.prototype._init.apply(this, arguments);
			
			this.followupMonitor.options.checkStatusAjax.type = 'POST';
		},

		doWork : function() {
			this.disable();
			this.targetIds = this.getTargetIds();
			
			for (var index in this.targetIds) {
				var resultObject = this.options.resultObjectList.resultObjects[this.targetIds[index]];
				resultObject.disable();
				if (this.options.workFunction)
					if ($.isFunction(this.options.workFunction))
						this.options.workFunction.call(resultObject);
					else
						resultObject[this.options.workFunction]();
			}
			
			var self = this;
			if (this.targetIds.length > 0) {
				this.performWork($.post, {
					'ids' : self.targetIds.join('\n')
				});
			} else {
				this.enable();
			}
		},

		workDone : function(data) {
			if ($.isArray(data)) {
				this.followupObjects = [];
				for (var index in data) {
					var id = data[index].pid;
					this.followupObjects.push(id);
					if (this.options.workFunction) {
						var resultObject = this.options.resultObjectList.resultObjects[id];
						if ($.isFunction(this.options.followupFunction))
							this.options.followupFunction.call(resultObject);
						else
							resultObject[this.options.followupFunction]();
					}
				}
				this.followupMonitor.pingData = {
						'ids' : this.followupObjects.join('\n')
				}; 
				return true;
			} else
				alert("Error while attempting to perform action: " + data);
			return false;
		},

		followup : function(data) {
			for (var id in data) {
				if (this.options.resultObjectList.resultObjects[id].updateVersion(data[id])) {
					var index = $.inArray(id, this.followupObjects);
					if (index != -1) {
						this.followupObjects.splice(index, 1);
						
						var resultObject = this.options.resultObjectList.resultObjects[id];
						resultObject.setState("idle");
						
						if (this.options.completeFunction) {
							if ($.isFunction(this.options.completeFunction))
								this.options.completeFunction.call(resultObject);
							else
								resultObject.resultObject(this.options.completeFunction);
						}
					}
				}
			}
			this.followupMonitor.pingData = {
					'ids' : this.followupObjects.join('\n')
			}; 
			return this.followupObjects.length == 0;
		},
		
		completeState : function(id) {
			this.targetIds = null;
			this.enable();
		},

		getTargetIds : function() {
			var targetIds = [];

			$.each(this.options.resultObjects, function() {
				var resultObject = this;
				if (this.isSelected()) {
					targetIds.push(resultObject.getPid());
				}
			});

			return targetIds;
		}
	});
});
/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
/*
 * @author Ben Pennell
 */
define('ConfirmationDialog', [ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor', 'ModalLoadingOverlay'], function(
		$, ui, PID, RemoteStateChangeMonitor) {
	$.widget("cdr.confirmationDialog", {
		options : {
			'promptText' : 'Are you sure?',
			'confirmFunction' : undefined,
			'confirmTarget' : undefined,
			'confirmText' : 'Yes',
			'cancelText' : 'Cancel',
			'dialogOptions' : {
				modal : false,
				minHeight : 60,
				autoOpen : false,
				resizable : false,
				dialogClass : "no_titlebar confirm_dialog",
				position : {
					my : "right top",
					at : "right bottom"
				},
			},
			'solo' : true
		},
		
		_create : function() {
			var self = this;
			
			if (this.options.dialogOptions.position.of == undefined)
				this.options.dialogOptions.position.of = this.element;
			
			this.confirmDialog = $("<div class='confirm_dialogue'></div>");
			if (this.options.promptText === undefined) {
				this.confirmDialog.append("<p>Are you sure?</p>");
			} else {
				this.confirmDialog.append("<p>" + this.options.promptText + "</p>");
			}
			$("body").append(this.confirmDialog);
			
			var buttonsObject = {};
			
			buttonsObject[self.options.cancelText] = function() {
				$(this).dialog("close");
			};
			
			buttonsObject[self.options.confirmText] = function() {
				if (self.options.confirmFunction) {
					self.options.confirmFunction.call(self.options.confirmTarget);
				}
				$(this).dialog("close");
			};
			
			var dialogOptions = $.extend({}, this.options.dialogOptions, {
				open : function() {
					if (self.options.solo) {
						$.each($('div.ui-dialog-content'), function (i, e) {
							if ($(this).dialog("isOpen") && this !== self.confirmDialog[0]) 
								$(this).dialog("close");
						});
					}
				},
				buttons : buttonsObject
			});
			this.confirmDialog.dialog(dialogOptions);
		},
		
		open : function () {
			this.confirmDialog.dialog('open');
		},
		
		close : function () {
			this.confirmDialog.dialog('close');
		}
	});
});define('DeleteBatchButton', [ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.deleteBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "delete",
			childWorkLinkName : "delete",
			confirm: true,
			confirmMessage: "Delete selected object(s)?",
			animateSpeed: 'fast'
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.isSelected() && resultObject.isEnabled()) {
					targetIds.push(resultObject.getPid());
				}
			}
			return targetIds;
		},
		
		followup : function(data) {
			var removedIds;
			var emptyData = jQuery.isEmptyObject(data.length);
			if (emptyData){
				removedIds = this.followupObjects;
			} else {
				removedIds = [];
				for (var index in this.followupObjects) {
					var id = this.followupObjects[index];
					if (!(id in data)) {
						removedIds.push(id);
					}
				}
			}
			
			if (removedIds.length > 0) {
				if (emptyData)
					this.followupObjects = null;
				for (var index in removedIds) {
					var id = removedIds[index];
					// Don't bother trimming out followup objects if all ids are complete
					if (!emptyData) {
						var followupIndex = $.inArray(id, this.followupObjects);
						this.followupObjects.splice(followupIndex, 1);
					}
					
					var resultObject = this.options.resultObjectList.resultObjects[id];
					// Trigger the complete function on targeted child callback buttons
					if (this.options.completeFunction) {
						if ($.isFunction(this.options.completeFunction))
							this.options.completeFunction.call(resultObject);
						else
							resultObject[this.options.completeFunction]();
					} else {
						resultObject.setState("idle");
					}
				}
			}
			
			return !this.followupObjects;
		}
	});
});define('DeleteObjectButton', [ 'jquery', 'jquery-ui', 'AjaxCallbackButton'], function($) {
	$.widget("cdr.deleteObjectButton", $.cdr.ajaxCallbackButton, {
		options : {
			workLabel: "Deleting...",
			workPath: "delete/{idPath}",
			followupLabel: "Cleaning up...",
			followupPath: "services/rest/item/{idPath}/solrRecord/version",
			confirm: true,
			confirmMessage: "Delete this object?",
			animateSpeed: 'fast'
		},
		
		_create: function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);
			
			this.options.workDone = this.deleteWorkDone;
			this.options.followup = this.deleteFollowup;
			if (this.options.parentObject)
				this.options.confirmAnchor = this.options.parentObject.element; 
			
			this.element.data("callbackButtonClass", "deleteObjectButton");
		},

		deleteFollowup: function(data) {
			if (data == null) {
				return true;
			}
			return false;
		},

		completeState: function() {
			if (this.options.parentObject != null)
				this.options.parentObject.deleteElement();
			this.destroy();
		},

		deleteWorkDone: function(data) {
			var jsonData;
			if ($.type(data) === "string") {
				try {
					jsonData = $.parseJSON(data);
				} catch (e) {
					throw "An error occurred while attempting to delete object " + this.pid.pid;
				}
			} else jsonData = data;
			
			this.completeTimestamp = jsonData.timestamp;
			return true;
		}
	});
});define('EditAccessControlForm', [ 'jquery', 'jquery-ui', 'ModalLoadingOverlay', 'AlertHandler', 'PID', 
         'editable', 'moment', 'qtip', 'ConfirmationDialog'], function($, PID) {
	$.widget("cdr.editAccessControlForm", {
		_create : function() {
			var self = this;
			self.aclNS = this.options.namespace;
			
			this.alertHandler = $("#alertHandler");
			
			this.accessControlModel = $($(this.options.xml).children()[0]).clone();
			this.originalDocument = this.xml2Str(this.accessControlModel);
			this.aclPrefix = this.getNamespacePrefix(this.accessControlModel, self.aclNS);
			
			$.fn.editable.defaults.mode = 'inline';
			this.addEmbargo = $(".add_embargo", this.element).editable({
				emptytext: 'Add embargo',
				format: 'MM/DD/YYYY',
				viewformat: 'MM/DD/YYYY',
				template: 'MM/DD/YYYY',
				clear: true,
				combodate: {
					minYear: moment().year(),
					maxYear: moment().add('years', 75).year(),
					minuteStep: 1,
					yearDescending: true
				}
			}).on('save', function(e, params) {
				if (params.newValue == null || params.newValue == "") {
					self.removeAttribute(self.accessControlModel, 'embargo-until', self.aclPrefix);
					return;
				}
				var formattedDate = moment(params.newValue).format('YYYY-MM-DD[T]HH:mm:ss');
				self.addAttribute(self.accessControlModel, 'embargo-until', formattedDate, self.aclNS, self.aclPrefix);
			});
			
			$(".roles_granted .remove_group", this.element).hide();
			
			$(".boolean_toggle", this.element).click(function(){
				$.proxy(self.toggleField(this), self);
				return false;
			});
			
			$(".inherit_toggle", this.element).click(function(){
				$.proxy(self.toggleField(this), self);
				var rolesGranted = $('.roles_granted', self.element);
				rolesGranted.toggleClass('inheritance_disabled');
			});
			
			$(".edit_role_granted a", this.element).click(function(){
				$(".roles_granted a", self.element).show();
				$(".edit_role_granted", self.element).hide();
				$(".add_role_granted", self.element).show();
				return false;
			});
			
			$(".add_group_name, .add_role_name", this.element).keypress(function(e){
				var code = (e.keyCode ? e.keyCode : e.which);
				if (code == 13) {
					$(".add_role_button", self.element).click();
					e.preventDefault();
				}
			});
			
			$('.add_group_name').one('focus', function(){
				var addGroup = $(this);
				$.getJSON(self.options.groupSuggestionsURL, function(data){
					addGroup.autocomplete({
						source : data
					});
				});
			});
			
			$(".add_role_button", this.element).click(function(){
				var roleValue = $(".add_role_name", self.element).val();
				var groupName = $.trim($(".add_group_name", self.element).val());
				if (roleValue == "" || groupName == "" || self.groupRoleExists(self.accessControlModel, roleValue, groupName, self.aclPrefix))
					return false;
				
				var roleRow = $("tr.role_groups[data-value='" + roleValue +"']", self.element);
				if (roleRow.length == 0) {
					roleRow = $("<tr class='role_groups' data-value='" + roleValue + "'><td class='role'>" + 
							roleValue + "</td><td class='groups'></td></tr>");
					$(".edit_role_granted", self.element).before(roleRow);
				}
				
				var grantElement = $(self.addElement(self.accessControlModel, 'grant', self.aclNS, self.aclPrefix));
				self.addAttribute(grantElement, 'role', roleValue, self.aclNS, self.aclPrefix);
				self.addAttribute(grantElement, 'group', groupName, self.aclNS, self.aclPrefix);
				
				$(".groups", roleRow).append("<span>" + groupName + "</span><a class='remove_group'>x</a><br/>");
				$('.add_group_name').autocomplete('search');
			});
			
			$(this.element).on("click", ".roles_granted .remove_group", function(){
				var groupName = $(this).prev("span").html();
				var roleValue = $(this).parents('.role_groups')[0].getAttribute('data-value');
				self.accessControlModel.children().each(function(){
					var group = self.getAttribute($(this), 'group', self.aclNS);
					var role = self.getAttribute($(this), 'role', self.aclNS);
					if (group == groupName && role == roleValue) {
						$(this).remove();
						return false;
					}
				});
				
				$(this).prev("span").remove();
				$(this).next("br").remove();
				var parentTd = $(this).parent();
				if (parentTd.children("span").length == 0){
					parentTd.parent().remove();
				}
				$(this).remove();
			});
			
			var containing = this.options.containingDialog;
			$('.update_button').click(function(){
				var container = ((self.options.containingDialog)? self.options.containingDialog : $(body));
				container.modalLoadingOverlay();
				$.ajax({
					url : self.options.updateUrl,
					type : 'PUT',
					data : self.xml2Str(self.accessControlModel),
					success : function(data) {
						containing.data('can-close', true);
						container.modalLoadingOverlay('close');
						if (self.options.containingDialog != null) {
							self.options.containingDialog.dialog('close');
						}
						self.alertHandler.alertHandler('success', 'Access control changes saved');
						$(".entry[data-pid='" + self.options.pid + "']").data('resultObject').refresh();
					},
					error : function(data) {
						container.modalLoadingOverlay('close');
						self.alertHandler.alertHandler('error', 'Failed to save changes: ' + data);
					}
				});
			});
			
			if (this.options.containingDialog) {
				
				containing.data('can-close', false);
				var closeButton = $(containing.prev().find(".ui-dialog-titlebar-close")[0]);
				closeButton.confirmationDialog({
					'promptText' : 'There are unsaved access control changes, close without saving?',
					'confirmFunction' : function() {
						containing.data('can-close', true);
						containing.dialog('close');
					},
					'solo' : false,
					'dialogOptions' : {
						modal : true,
						minWidth : 200,
						maxWidth : 400
					}
				});
				
				containing.on('dialogbeforeclose', function(){
					if (!containing.data('can-close') && self.isDocumentChanged()) {
						closeButton.confirmationDialog('open');
						return false;
					} else {
						return true;
					}
				});
			}
		},
		
		toggleField: function(fieldElement) {
			var fieldName = $(fieldElement).attr("data-field");
			if ($.trim($(fieldElement).html()) == "Yes") {
				$(fieldElement).html("No");
				this.addAttribute(this.accessControlModel, fieldName, 'false', this.aclNS, this.aclPrefix);
				return false;
			} else { 
				$(fieldElement).html("Yes");
				this.addAttribute(this.accessControlModel, fieldName, 'true', this.aclNS, this.aclPrefix);
				return true;
			}
		},
		
		getNamespacePrefix: function(node, namespace) {
			var prefix = null;
			var attributes = node[0].attributes;
			$.each(attributes, function(key, value) {
				if (value.value == namespace) {
					var index = value.nodeName.indexOf(":");
					if (index == -1)
						prefix = "";
					else prefix = value.localName;
					return false;
				}
			});
			
			return prefix;
		},
		
		isDocumentChanged : function() {
			return this.originalDocument != this.xml2Str(this.accessControlModel);
		},
		
		groupRoleExists: function(xmlNode, roleName, groupName, namespacePrefix) {
			var prefix = "";
			if (namespacePrefix)
				prefix = namespacePrefix + "\\:";
			return $(prefix + "grant[" + prefix + "role='" + roleName + "'][" + prefix + "group='" + groupName + "']", xmlNode).length > 0;
		},
		
		addElement: function(xmlNode, localName, namespace, namespacePrefix) {
			var nodeName = localName;
			if (namespacePrefix != null && namespacePrefix != "") 
				nodeName = namespacePrefix + ":" + localName;
			var newElement = xmlNode[0].ownerDocument.createElementNS(namespace, nodeName);
			$(newElement).text("");
			xmlNode.append(newElement);
			return newElement;
		},
		
		removeAttribute: function(xmlNode, attrName, namespacePrefix) {
			if (namespacePrefix != null && namespacePrefix != "")
				xmlNode.removeAttr(namespacePrefix + ":" + attrName);
			else xmlNode.removeAttr(attrName);
		},
		
		getAttribute: function(xmlNode, attrName, namespace) {
			var attributes = xmlNode[0].attributes;
			for (var index in attributes) {
				if (attributes[index].localName == attrName && attributes[index].namespaceURI == namespace)
					return attributes[index].nodeValue;
			}
			return null;
		},
		
		addAttribute: function(xmlNode, attrName, attrValue, namespace, namespacePrefix) {
			if (namespacePrefix != null) {
				if (namespacePrefix == "")
					xmlNode.attr(attrName, attrValue);
				else xmlNode.attr(namespacePrefix + ":" + attrName, attrValue);
				return;
			}
			xmlNode = xmlNode[0];
			
		    var attr;
		    if (xmlNode.ownerDocument.createAttributeNS)
		       attr = xmlNode.ownerDocument.createAttributeNS(namespace, attrName);
		    else
		       attr = xmlNode.ownerDocument.createNode(2, attrName, namespace);

		    attr.nodeValue = attrValue;

		    //Set the new attribute into the xmlNode
		    if (xmlNode.setAttributeNodeNS)
		    	xmlNode.setAttributeNodeNS(attr);  
		    else
		    	xmlNode.setAttributeNode(attr);  
		    
		    return attr;
		},
		
		xml2Str: function(xmlNodeObject) {
			if (xmlNodeObject == null)
				return;
			var xmlNode = (xmlNodeObject instanceof jQuery? xmlNodeObject[0]: xmlNodeObject);
			var xmlStr = "";
			try {
				// Gecko-based browsers, Safari, Opera.
				xmlStr = (new XMLSerializer()).serializeToString(xmlNode);
			} catch (e) {
				try {
					// Internet Explorer.
					xmlStr = xmlNode.xml;
				} catch (e) {
					return false;
				}
			}
			return xmlStr;
		}
	});
});define('MetadataObject', [ 'jquery', 'PID' ], function($, PID) {
	function MetadataObject(metadata) {
		this.init(metadata);
	};
	
	$.extend(MetadataObject.prototype, {
		data: null,
		
		init: function(metadata) {
			this.data = metadata;
			if (this.data === undefined || this.data == null) {
				this.data = {};
			}
			//this.setPID(this.data.id);
		},
		
		setPID: function(pid) {
			this.pid = new PID(pid);
		},
		
		isPublished: function() {
			if (!$.isArray(this.data.status)){
				return true;
			}
			return $.inArray("Unpublished", this.data.status) == -1;
		},
		
		publish: function () {
			if (!$.isArray(this.data.status)){
				this.data.status = [];
			} else {
				this.data.status.splice($.inArray("Unpublished", this.data.status), 1);
			}
			this.data.status.push("Published");
		},
		
		unpublish: function () {
			if (!$.isArray(this.data.status)){
				this.data.status = [];
			} else {
				this.data.status.splice($.inArray("Published", this.data.status), 1);
			}
			this.data.status.push("Unpublished");
		}
	});
	
	return MetadataObject;
});define('ModalLoadingOverlay', [ 'jquery', 'jquery-ui', 'editable', 'moment', 'qtip'], function($) {
	$.widget("cdr.modalLoadingOverlay", {
		options : {
			'text' : null,
			'iconSize' : 'large',
			'iconPath' : '/static/images/admin/loading-small.gif',
			'autoOpen' : true
		},
		
		_create : function() {
			if (this.options.text == null)
				this.overlay = $('<div class="load_modal icon_' + (this.options.iconSize) + '"></div>');
			else {
				this.overlay = $('<div class="load_modal"></div>');
				this.textSpan = $('<span>' + this.options.text + '</span>');
				this.overlay.append(this.textSpan);
				if (this.options.iconPath)
					this.textIcon = $('<img src="' + this.options.iconPath + '" />').appendTo(this.overlay);
			}
			
			this.overlay.appendTo(document.body);
			
			$(window).resize($.proxy(this.resize, this));
			
			if (this.options.autoOpen)
				this.show();
			else this.hide();
		},
		
		close : function() {
			this.overlay.remove();
		},
		
		show : function() {
			this.overlay.css({'visibility': 'hidden', 'display' : 'block'});
			if (this.element != $(document)) {
				this.resize();
				if (this.textSpan) {
					var topOffset = (this.element.innerHeight() - this.textSpan.outerHeight()) / 2;
					this.textSpan.css('top', topOffset);
					this.textIcon.css('top', topOffset);
				}
			}
			this.overlay.css('visibility', 'visible');
		},
		
		resize : function() {
			this.overlay.css({'width' : this.element.innerWidth(), 'height' : this.element.innerHeight(),
				'top' : this.element.offset().top, 'left' : this.element.offset().left});
		},
		
		hide : function() {
			this.overlay.hide();
		},
		
		setText : function(text) {
			this.textSpan.html(text);
		}
	});
});define('PID', ['jquery'], function($) {
	function PID(pidString) {
		this.init(pidString);
	};
	
	$.extend(PID.prototype, {
		uriPrefix: "info:fedora/",
		pid: null,
		
		init: function(pidString) {
			if (pidString.indexOf(this.uriPrefix) == 0) {
				this.pid = pidString.substring(this.uriPrefix.length());
			} else {
				this.pid = pidString;
			}
		},
		
		getPid: function() {
			return this.pid;
		},
		
		getURI: function() {
			return this.urlPrefix + this.pid;
		},
		
		getPath: function() {
			return this.pid.replace(":", "/");
		}
	});
	
	return PID;
});define('PublishBatchButton', [ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.publishBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "services/rest/edit/publish",
			childWorkLinkName : 'publish'
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.isSelected() && !resultObject.getMetadata().isPublished()
						&& resultObject.isEnabled()) {
					targetIds.push(resultObject.getPid().getPid());
				}
			}
			return targetIds;
		}
	});
});define('PublishObjectButton', [ 'jquery', 'jquery-ui', 'AjaxCallbackButton', 'ResultObject'], function($) {
	$.widget("cdr.publishObjectButton", $.cdr.ajaxCallbackButton, {
		options : {
			defaultPublish: false,
			followupPath: "services/rest/item/{idPath}/solrRecord/version"
		},
		
		_create: function() {
			$.cdr.ajaxCallbackButton.prototype._create.apply(this, arguments);
			
			this.options.workDone = this.publishWorkDone;
			this.options.followup = this.publishFollowup;
			
			this.element.data("callbackButtonClass", "publishObjectButton");
			
			this.published = this.options.defaultPublish;
			if (this.published) {
				this.publishedState();
			} else {
				this.unpublishedState();
			}
		},

		publishFollowup : function(data) {
			if (data) {
				return this.options.parentObject.updateVersion(data);
			}
			return false;
		},
		
		completeState : function() {
			if (this.options.parentObject) {
				this.options.parentObject.refresh(true);
			} else {
				this.toggleState();
			}
			this.enable();
		},
		
		toggleState : function() {
			if (this.published) {
				this.unpublishedState();
			} else {
				this.publishedState();
			}
		},

		publishedState : function() {
			this.published = true;
			this.element.text("Unpublish");
			this.setWorkURL("services/rest/edit/unpublish/{idPath}");
			this.options.workLabel = "Unpublishing...";
			this.options.followupLabel = "Unpublishing....";
		},

		unpublishedState : function() {
			this.published = false;
			this.element.text("Publish");
			this.setWorkURL("services/rest/edit/publish/{idPath}");
			this.options.workLabel = "Publishing...";
			this.options.followupLabel = "Publishing....";
		},

		publishWorkDone : function(data) {
			var jsonData;
			if ($.type(data) === "string") {
				try {
					jsonData = $.parseJSON(data);
				} catch (e) {
					throw "Failed to change publication status for " + this.pid.pid;
				}
			} else {
				jsonData = data;
			}
			
			
			this.completeTimestamp = jsonData.timestamp;
			return true;
		}
	});
});define('RemoteStateChangeMonitor', ['jquery'], function($) {
	function RemoteStateChangeMonitor(options) {
		this.init(options);
	};
	
	$.extend(RemoteStateChangeMonitor.prototype, {
		defaultOptions : {
			'pingFrequency' : 1000,
			'statusChanged' : undefined,
			'statusChangedTarget' : undefined,
			'checkStatus' : undefined,
			'checkStatusTarget' : undefined,
			'checkStatusAjax' : {
			}
		},
		pingId : null,
		pingData : null,
		
		init: function(options) {
			this.options = $.extend({}, this.defaultOptions, options);
			this.options.checkStatusAjax.success = $.proxy(this.pingSuccessCheck, this);
		},
		
		performPing : function() {
			if (this.pingData)
				this.options.checkStatusAjax.data = this.pingData;
			$.ajax(this.options.checkStatusAjax);
		},
		
		pingSuccessCheck : function(data) {
			var isDone = this.options.checkStatus.call(this.options.checkStatusTarget, data);
			if (isDone) {
				if (this.pingId != null) {
					clearInterval(this.pingId);
					this.pingId = null;
				}
				this.options.statusChanged.call(this.options.statusChangedTarget, data);
			} else if (this.pingId == null) {
				this.pingId = setInterval($.proxy(this.performPing, this), this.options.pingFrequency);
			}
		}
	});
	
	return RemoteStateChangeMonitor;
});/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
define('ResultObject', [ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor', 'DeleteObjectButton',
		'PublishObjectButton', 'EditAccessControlForm', 'ModalLoadingOverlay'], function($, ui, RemoteStateChangeMonitor) {
	var defaultOptions = {
			animateSpeed : 100,
			metadata : null,
			selected : false,
			selectable : true,
			selectCheckboxInitialState : false
		};
	
	function ResultObject(element, options) {
		this.init(element, options);
	};
	
	ResultObject.prototype.init = function(element, options) {
		this.element = element;
		this.element.data('resultObject', this);
		this.options = $.extend({}, defaultOptions, options);
		this.metadata = this.options.metadata;
		this.links = [];
		this.pid = this.options.id;
		this.overlayInitialized = false;
		if (this.options.selected)
			this.select();
	};
	
	ResultObject.prototype.activateActionMenu = function() {
		var $menuIcon = $(".menu_box img", this.element);
		if (!this.actionMenuInitialized) {
			this.initializeActionMenu();
			$menuIcon.click();
			return;
		}
		if (this.actionMenu.children().length == 0)
			return;
		$menuIcon.parent().css("background-color", "#7BAABF");
		return;
	};
	
	ResultObject.prototype.initializeActionMenu = function() {
		var self = this;
		
		this.actionMenuInitialized = true;
		
		this.actionMenu = $(".menu_box ul", this.element);
		if (this.actionMenu.children().length == 0)
			return;
		
		var menuIcon = $(".menu_box img", this.element);
		
		// Set up the dropdown menu
		menuIcon.qtip({
			content: self.actionMenu,
			position: {
				at: "bottom right",
				my: "top right"
			},
			style: {
				classes: 'qtip-light',
				tip: false
			},
			show: {
				event: 'click',
				delay: 0
			},
			hide: {
				delay: 2000,
				event: 'unfocus mouseleave click',
				fixed: true, // Make sure we can interact with the qTip by setting it as fixed
				effect: function(offset) {
					menuIcon.parent().css("background-color", "transparent");
					$(this).fadeOut(100);
				}
			},
			events: {
				render: function(event, api) {
					self.initializePublishLinks($(this));
					self.initializeDeleteLinks($(this));
				}
			}
		});
		
		self.actionMenu.children().click(function(){
			menuIcon.qtip('hide');
		});
		
		this.actionMenu.children(".edit_access").click(function(){
			menuIcon.qtip('hide');
			self.highlight();
			var dialog = $("<div class='containingDialog'><img src='/static/images/admin/loading-large.gif'/></div>");
			dialog.dialog({
				autoOpen: true,
				width: 500,
				height: 'auto',
				maxHeight: 800,
				minWidth: 500,
				modal: true,
				title: 'Access Control Settings',
				close: function() {
					dialog.remove();
					self.unhighlight();
				}
			});
			dialog.load("acl/" + self.pid, function(responseText, textStatus, xmlHttpRequest){
				dialog.dialog('option', 'position', 'center');
			});
		});
	};
	
	ResultObject.prototype._destroy = function () {
		if (this.overlayInitialized) {
			this.element.modalLoadingOverlay('close');
		}
	};

	ResultObject.prototype.initializePublishLinks = function(baseElement) {
		var links = baseElement.find(".publish_link");
		if (!links)
			return;
		this.links['publish'] = links;
		var obj = this;
		$(links).publishObjectButton({
			pid : obj.pid,
			parentObject : obj,
			defaultPublish : $.inArray("Unpublished", this.metadata.status) == -1
		});
	};

	ResultObject.prototype.initializeDeleteLinks = function(baseElement) {
		var links = baseElement.find(".delete_link");
		if (!links)
			return;
		this.links['delete'] = links;
		var obj = this;
		$(links).deleteObjectButton({
			pid : obj.pid,
			parentObject : obj
		});
	};

	ResultObject.prototype.disable = function() {
		this.options.disabled = true;
		this.element.css("cursor", "default");
		this.element.find(".ajaxCallbackButton").each(function(){
			$(this)[$(this).data("callbackButtonClass")].call($(this), "disable");
		});
	};

	ResultObject.prototype.enable = function() {
		this.options.disabled = false;
		this.element.css("cursor", "pointer");
		this.element.find(".ajaxCallbackButton").each(function(){
			$(this)[$(this).data("callbackButtonClass")].call($(this), "enable");
		});
	};
	
	ResultObject.prototype.isEnabled = function() {
		return !this.options.disabled;
	};

	ResultObject.prototype.toggleSelect = function() {
		if (this.element.hasClass("selected")) {
			this.unselect();
		} else {
			this.select();
		}
	};
	
	ResultObject.prototype.getElement = function () {
		return this.element;
	};
	
	ResultObject.prototype.getPid = function () {
		return this.pid;
	};
	
	ResultObject.prototype.getMetadata = function () {
		return this.metadata;
	};

	ResultObject.prototype.select = function() {
		if (!this.options.selectable)
			return;
		this.element.addClass("selected");
		if (!this.checkbox)
			this.checkbox = this.element.find("input[type='checkbox']");
		this.checkbox.prop("checked", true);
	};

	ResultObject.prototype.unselect = function() {
		if (!this.options.selectable)
			return;
		this.element.removeClass("selected");
		if (!this.checkbox)
			this.checkbox = this.element.find("input[type='checkbox']");
		this.checkbox.prop("checked", false);
	};
	
	ResultObject.prototype.highlight = function() {
		this.element.addClass("highlighted");
	};
	
	ResultObject.prototype.unhighlight = function() {
		this.element.removeClass("highlighted");
	};

	ResultObject.prototype.isSelected = function() {
		return this.element.hasClass("selected");
	};

	ResultObject.prototype.setState = function(state) {
		if ("idle" == state || "failed" == state) {
			this.enable();
			this.element.removeClass("followup working").addClass("idle");
			this.updateOverlay('hide');
		} else if ("working" == state) {
			this.updateOverlay('show');
			this.disable();
			this.element.switchClass("idle followup", "working", this.options.animateSpeed);
		} else if ("followup" == state) {
			this.element.removeClass("idle").addClass("followup", this.options.animateSpeed);
		}
	};

	ResultObject.prototype.getActionLinks = function(linkNames) {
		return this.links[linkNames];
	};
	
	ResultObject.prototype.isPublished = function() {
		if (!$.isArray(this.metadata.status)){
			return true;
		}
		return $.inArray("Unpublished", this.metadata.status) == -1;
	};
	
	ResultObject.prototype.publish = function() {
		var links = this.links['publish'];
		if (links.length == 0)
			return;
		$(links[0]).publishObjectButton('activate');
	};
	
	ResultObject.prototype['delete'] = function() {
		var links = this.links['delete'];
		if (links.length == 0)
			return;
		$(links[0]).deleteObjectButton('activate');
	};

	ResultObject.prototype.deleteElement = function() {
		var obj = this;
		obj.element.hide(obj.options.animateSpeed, function() {
			obj.element.remove();
			if (obj.options.resultObjectList) {
				obj.options.resultObjectList.removeResultObject(obj.pid.getPid());
			}
		});
	};
	
	ResultObject.prototype.updateVersion = function(newVersion) {
		if (newVersion != this.metadata._version_) {
			this.metadata._version_ = newVersion;
			return true;
		}
		return false;
	};
	
	ResultObject.prototype.setStatusText = function(text) {
		this.updateOverlay('setText', [text]);
	};
	
	ResultObject.prototype.updateOverlay = function(fnName, fnArgs) {
		// Check to see if overlay is initialized
		if (!this.overlayInitialized) {
			this.overlayInitialized = true;
			this.element.modalLoadingOverlay({'text' : 'Working...', 'autoOpen' : false});
		}
		var overlay = this.element.data("modalLoadingOverlay");
		overlay[fnName].apply(overlay, fnArgs);
	};
	
	ResultObject.prototype.refresh = function(immediately) {
		this.updateOverlay('show');
		this.setStatusText('Refreshing...');
		if (immediately) {
			this.options.resultObjectList.refreshObject(this.pid.getPid());
			return;
		}
		var self = this;
		var followupMonitor = new RemoteStateChangeMonitor({
			'checkStatus' : function(data) {
				return (data != self.metadata._version_);
			},
			'checkStatusTarget' : this,
			'statusChanged' : function(data) {
				self.options.resultObjectList.refreshObject(self.pid.getPid());
			},
			'statusChangedTarget' : this, 
			'checkStatusAjax' : {
				url : "services/rest/item/" + self.pid.getPath() + "/solrRecord/version",
				dataType : 'json'
			}
		});
		
		followupMonitor.performPing();
	};
	
	return ResultObject;
});define('ResultObjectList', ['jquery', 'MetadataObject', 'ResultObject' ], function($, MetadataObject, ResultObject) {
	function ResultObjectList(options) {
		this.init(options);
	};
	
	$.extend(ResultObjectList.prototype, {
		options: {
			resultIdPrefix : "entry_",
			metadataObjects : undefined,
			refreshEntryUrl : "entry/"
		},
		resultObjects: {},
		
		init: function(options) {
			this.options = $.extend({}, this.options, options);
			var self = this;
			console.time("Initialize entries");
			
			console.time("Get entries");
			var $entries = $(".res_entry", this.element);

			console.timeEnd("Get entries");
			var metadataObjects = self.options.metadataObjects;
			for (var i = 0; i < $entries.length; i++) {
				var id = $entries[i].id;
				id = 'uuid' + id.substring(id.indexOf(':') + 1);
				self.resultObjects[id] = new ResultObject($entries.eq(i), {id : id, metadata : metadataObjects[id], 
					resultObjectList : self});
			}
			console.timeEnd("Initialize entries");
		},
		
		getResultObject: function(id) {
			return this.resultObjects[id];
		},
		
		removeResultObject: function(id) {
			if (id in this.resultObjects) {
				delete this.resultObjects[id];
			}
		},
		
		refreshObject: function(id) {
			var self = this;
			var resultObject = this.getResultObject(id);
			$.ajax({
				url : this.options.refreshEntryUrl + resultObject.getPid(),
				dataType : 'json',
				success : function(data, textStatus, jqXHR) {
					var newContent = $(data.content);
					resultObject.replaceWith(newContent);
					self.resultObjects[id] = newContent.resultObject({'id' : id, "metadata" : data.data.metadata, "resultObjectList" : self});
				}
			});
		}
		
	});
	
	return ResultObjectList;
});define('ResultTableView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'PublishBatchButton', 'UnpublishBatchButton', 'DeleteBatchButton', 'detachplus'], 
		function($, ui, ResultObjectList) {
	$.widget("cdr.resultTableView", {
		options : {
			enableSort : true,
			ajaxSort : false,
			metadataObjects : undefined,
			enableArrange : false,
			enableMove : false
		},
		
		_create : function() {
			this.resultObjectList = new ResultObjectList({'metadataObjects' : this.options.metadataObjects});
			
			if (this.options.enableSort)
				this._initSort();
			this._initBatchOperations();
			this._initEventHandlers();
			this._initReordering();
		},

		_initSort : function() {
			var $resultTable = this.element;
			var self = this;
			$("th.sort_col", $resultTable).wrapInner('<span/>').each(function(){
				var $th = $(this),
				thIndex = $th.index(),
				dataType = $th.attr("data-type");
				$th.addClass('sorting');
				
				$th.click(function(){
					if (!$th.hasClass('sorting')) return;
					console.time("Sort total");
					var inverse = $th.hasClass('desc');
					$('.sorting', $resultTable).removeClass('asc desc');
					if (inverse)
						$th.addClass('asc');
					else 
						$th.addClass('desc');
					
					// Apply sort function based on data-type
					if (dataType == 'index') {
						self._originalOrderSort(inverse);
					} else if (dataType == 'title') {
						self._titleSort(inverse);
					} else {
						self._alphabeticSort(thIndex, inverse);
					}
					inverse = !inverse;
					console.timeEnd("Sort total");
				});
			});
		},
		
		_sortEntries : function($entries, matchMap, getSortable) {
			console.time("Reordering elements");
			var $resultTable = this.element;
			
			$resultTable.detach(function(){
				var fragment = document.createDocumentFragment();
				if (matchMap) {
					if ($.isFunction(getSortable)) {
						for (var i = 0, length = matchMap.length; i < length; i++) {
							fragment.appendChild(getSortable.call($entries[matchMap[i].index]));
						}
					} else {
						for (var i = 0, length = matchMap.length; i < length; i++) {
							fragment.appendChild($entries[matchMap[i].index].parentNode);
						}
					}
				} else {
					if ($.isFunction(getSortable)) {
						for (var i = 0, length = $entries.length; i < length; i++) {
							fragment.appendChild(getSortable.call($entries[i]));
						}
					} else {
						for (var i = 0, length = $entries.length; i < length; i++) {
							fragment.appendChild($entries[i].parentNode);
						}
					}
				}
				var resultTable = $resultTable[0];
				resultTable.appendChild(fragment);
			});
			
			console.timeEnd("Reordering elements");
		},
		
		_alphabeticSort : function(thIndex, inverse) {
			var $resultTable = this.element;
			var matchMap = [];
			console.time("Finding elements");
			var $entries = $resultTable.find('tr.res_entry').map(function() {
				return this.children[thIndex];
			});
			console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				matchMap.push({
					index : i,
					value : $entries[i].innerHTML.toUpperCase()
				});
			}
			console.time("Sorting");
			matchMap.sort(function(a, b){
				if(a.value == b.value)
					return 0;
				return a.value > b.value ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		_originalOrderSort : function(inverse) {
			console.time("Finding elements");
			var $entries = [];
			for (var index in this.resultObjectList.resultObjects) {
				var resultObject = this.resultObjectList.resultObjects[index];
				$entries.push(resultObject.getElement()[0]);
			}
			if (inverse)
				$entries = $entries.reverse();
			
			console.timeEnd("Finding elements");

			this._sortEntries($entries, null, function(){
				return this;
			});
		},
		
		_titleSort : function(inverse) {
			var $resultTable = this.element;
			var titleRegex = new RegExp('(\\d+|[^\\d]+)', 'g');
			var matchMap = [];
			console.time("Finding elements");
			var $entries = $resultTable.find('.itemdetails');
			console.timeEnd("Finding elements");
			for (var i = 0, length = $entries.length; i < length; i++) {
				var text = $entries[i].children[0].innerHTML.toUpperCase();
				matchMap.push({
					index : i,
					text : text,
					value : text.match(titleRegex)
				});
			}
			console.time("Sorting");
			matchMap.sort(function(a, b) {
				if (a.text == b.text)
					return 0;
				var i = 0;
				for (; i < a.value.length && i < b.value.length && a.value[i] == b.value[i]; i++);
				
				// Whoever ran out of entries first, loses
				if (i == a.value.length)
					if (i == b.value.length)
						return 0;
					else return inverse ? 1 : -1;
				if (i == b.value.length)
					return inverse ? -1 : 1;
				
				// Do int comparison of unmatched elements
				var aInt = parseInt(a.value[i]);
				if (!isNaN(aInt)) {
						var bInt = parseInt(b.value[i]);
						if (!isNaN(bInt))
							return aInt > bInt ?
									inverse ? -1 : 1
									: inverse ? 1 : -1;
				}
				return a.text > b.text ?
						inverse ? -1 : 1
						: inverse ? 1 : -1;
			});
			console.timeEnd("Sorting");
			this._sortEntries($entries, matchMap);
		},
		
		_initBatchOperations : function() {
			var self = this;
			
			$(".select_all", self.element).click(function(){
				var resultObjects = self.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index].select();
				}
			});
			
			
			$(".deselect_all", self.element).click(function(){
				var resultObjects = self.resultObjectList.resultObjects;
				for (var index in resultObjects) {
					resultObjects[index].unselect();
				}
			});
			
			$(".publish_selected", self.element).publishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						var resultObject = this.data('resultObject');
						resultObject.setStatusText('Publishing...');
						resultObject.updateOverlay('show');
					}, 
				'followupFunction' : function() {
					this.data('resultObject').setStatusText('Publishing....');
				}, 
				'completeFunction' : function(){
					this.data('resultObject').refresh(true);
				}
			});
			$(".unpublish_selected", self.element).unpublishBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						var resultObject = this.data('resultObject');
						resultObject.setStatusText('Unpublishing...');
						resultObject.updateOverlay('show');
					}, 
				'followupFunction' : function() {
					this.data('resultObject').setStatusText('Unpublishing....');
				}, 
				'completeFunction' : function(){
					this.data('resultObject').refresh(true);
				}
			});
			$(".delete_selected", self.element).deleteBatchButton({
				'resultObjectList' : this.resultObjectList, 
				'workFunction' : function() {
						var resultObject = this.data('resultObject');
						resultObject.setStatusText('Deleting...');
						resultObject.updateOverlay('show');
					}, 
				'followupFunction' : function() {
						this.data('resultObject').setStatusText('Cleaning up...');
					}, 
				'completeFunction' : 'deleteElement'
			});
		},
		
		_initEventHandlers : function() {
			this.element.on('click', ".menu_box img", function(e){
				$(this).parents(".res_entry").data('resultObject').activateActionMenu();
				e.stopPropagation();
			});
			this.element.on('click', ".res_entry", function(e){
				$(this).data('resultObject').toggleSelect();
				e.stopPropagation();
			});
			this.element.on('click', ".res_entry a", function(e){
				e.stopPropagation();
			});
		},
		
		_initReordering : function() {
			var arrangeMode = true;
			var $resultTable = this.element;
			$resultTable.sortable({
				delay : 200,
				items: '.res_entry',
				cursorAt : { top: -2, left: -5 },
				forceHelperSize : false,
				scrollSpeed: 100,
				/*connectWith: '.hier_entry, .entry.container',*/
				placeholder : 'arrange_placeholder',
				helper: function(e, element){
					var title = $($('.itemdetails a', element)[0]).html();
					if ($(element).hasClass('selected')) {
						this.selected = element.parent().children(".selected");
						if (this.selected.length > 1) {
							return $("<div class='move_helper'><img src='/static/images/admin/type_folder.png'/><span>" + title + "</span> (and " + (this.selected.length - 1) + " others)</div>");
						}
					}
					return $("<div class='move_helper'><span><img src='/static/images/admin/type_folder.png'/>" + title + "</span></div>");
				},
				appendTo: document.body,
				start: function(e, ui) {
					moving = false;
					ui.item.show();
					var self = this;
					if (this.selected) {
						$.each(this.selected, function(index){
							if (self.selected[index] === ui.item[0]) {
								self.itemSelectedIndex = index;
								return false;
							}
						});
					}
				},
				stop: function(e, ui) {
					if (!moving && !arrangeMode)
						return false;
					var self = this;
					if (this.selected) {
						$.each(this.selected, function(index){
							if (index < self.itemSelectedIndex)
								ui.item.before(self.selected[index]);
							else if (index > self.itemSelectedIndex)
								$(self.selected[index - 1]).after(self.selected[index]);
						});
					}
				},
				update: function (e, ui) {
					if (!moving && !arrangeMode)
						return false;
					if (ui.item.hasClass('selected') && this.selected.length > 0)
						this.selected.hide().show(300);
					else ui.item.hide().show(300);
				}
			});
		},
		
		setEnableSort : function(value) {
			this.options.enableSort = value;
			if (value) {
				$("th.sort_col").removeClass("sorting");
			} else {
				$("th.sort_col").addClass("sorting");
			}
		}
	});
});
	define('SearchMenu', [ 'jquery', 'jquery-ui', 'StructureView'], function(
		$, ui) {
	$.widget("cdr.searchMenu", {
		_create : function() {
			this.element.children('.query_menu').accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true
			});
			this.element.children('.filter_menu').accordion({
				header: "> div > h3",
				heightStyle: "content",
				collapsible: true,
				active: false,
				activate: function(event, ui) {
					if (ui.newPanel.attr('data-href') != null && !ui.newPanel.data('contentLoaded')) {
						$.ajax({
							url : ui.newPanel.attr('data-href'),
							success : function(data) {
								if (ui.newPanel.attr('id') == "structure_facet") {
									var $structureView = $('<div/>').html(data);
									$structureView.structureView({
										showResourceIcons : true,
										showParentLink : true
									});
									$structureView.addClass('inset facet');
									data = $structureView;
								}
								ui.newPanel.html(data);
								ui.newPanel.data('contentLoaded', true);
							}
						});
						
					}
				}
			}).accordion('activate', 0);
			
			this.element.resizable({
				handles: 'e',
				alsoResize : ".structure.inset.facet",
				minWidth: 300,
				maxWidth: 600
			}).css('visibility', 'visible');
		}
	});
});define('UnpublishBatchButton', [ 'jquery', 'jquery-ui', 'BatchCallbackButton' ], function($) {
	$.widget("cdr.unpublishBatchButton", $.cdr.batchCallbackButton, {
		options : {
			resultObjectList : undefined,
			workPath: "services/rest/edit/unpublish",
			childWorkLinkName : 'publish'
		},

		getTargetIds : function() {
			var targetIds = [];
			for (var id in this.options.resultObjectList.resultObjects) {
				var resultObject = this.options.resultObjectList.resultObjects[id];
				if (resultObject.isSelected() && resultObject.isPublished()
						&& resultObject.isEnabled()) {
					targetIds.push(resultObject.getPid());
				}
			}
			return targetIds;
		}
	});
});