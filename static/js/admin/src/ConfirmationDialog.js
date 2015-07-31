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
define('ConfirmationDialog', [ 'jquery', 'jquery-ui', 'PID', 'RemoteStateChangeMonitor'], function(
		$, ui, PID, RemoteStateChangeMonitor) {
	function ConfirmationDialog(options) {
		this._create(options);
	};
	
	var defaultOptions = {
		promptText : 'Are you sure?',
		confirmFunction : undefined,
		confirmTarget : undefined,
		confirmText : 'Yes',
		confirmMatchText : null,
		cancelTarget : undefined,
		cancelFunction : undefined,
		cancelText : 'Cancel',
		solo : true,
		additionalButtons : undefined,
		autoOpen : false,
		addClass : undefined,
		destroyOnClose : false
	};
	
	var defaultDialog = {
		modal : false,
		minHeight : 60,
		autoOpen : false,
		resizable : false,
		dialogClass : "no_titlebar confirm_dialog",
		position : {
			my : "right top",
			at : "right bottom"
		}
	};
	
	$.extend(ConfirmationDialog.prototype, {
		_create : function(options) {
			this.options = $.extend({}, defaultOptions, options);
			if ('dialogOptions' in this.options)
				this.dialogOptions = $.extend({}, defaultDialog, this.options.dialogOptions);
			else this.dialogOptions = $.extend({}, defaultDialog);
			
			var self = this;
			
			this.confirmDialog = $("<div class='confirm_dialogue'></div>");
			
			this.setPromptText(this.options.promptText);
			if (this.options.warningText) {
				this.confirmDialog.append("<div class='warning'>" + this.options.warningText + "</div>");
			}
			
			if (this.options.confirmMatchText) {
				this.confirmMatchInput = $("<input type='text' />");
				$("<p class='confirm_match_input'/>").append(this.confirmMatchInput).appendTo(this.confirmDialog);
			}
			
			$("body").append(this.confirmDialog);
			
			var buttons = this._generateButtons();
			
			$.extend(this.dialogOptions, {
				open : function() {
					if (self.options.solo) {
						$.each($('div.ui-dialog-content'), function (i, e) {
							if ($(this).dialog("isOpen") && this !== self.confirmDialog[0]) 
								$(this).dialog("close");
						});
					}
				},
				buttons : buttons
			});
			
			if (this.options.destroyOnClose)
				this.dialogOptions.close(function(){
					self.remove();
				});
			
			this.confirmDialog.dialog(this.dialogOptions);
			if (this.options.addClass)
				this.confirmDialog.addClass(this.options.addClass);
				
			if (this.options.confirmMatchText) {
				this.confirmMatchInput.keyup(function(){
					$(".confirm_dialog_confirm").button('option', 'disabled', !self.inputMatches());
				});
				$(".confirm_dialog_confirm").button('option', 'disabled', true);
			}
				
			if (this.options.autoOpen)
				this.open();
		},
		
		_generateButtons : function() {
			var buttonsObject = {};
			var self = this;
			
			var buttons = [
				{
					id : 'cancel',
					class : 'confirm_dialog_cancel',
					text : self.options.cancelText,
					click : function() {
						if (self.options.cancelFunction) {
							var result = self.options.cancelFunction.call(self.options.cancelTarget);
							if (result !== undefined && !result)
								return;
						}
						$(this).dialog("close");
					}
				}, {
					id : 'cancel',
					class : 'confirm_dialog_confirm',
					text : self.options.confirmText,
					disabled : self.options.disableConfirm,
					click : function() {
						if (self.options.confirmFunction) {
							if (!self.inputMatches())
								return;
							var result = self.options.confirmFunction.call(self.options.confirmTarget);
							if (result !== undefined && !result)
								return;
						}
						$(this).dialog("close");
					}
				}
			];
			
			// Add any additional buttons in
			if (this.options.additionalButtons) {
				for (var index in this.options.additionalButtons) {
					buttons.push({
						id : 'confirm_' + index,
						text : index,
						click : this.options.additionalButtons[index]
					});
				}
			}
			
			return buttons;
		},
		
		inputMatches : function () {
			return !this.confirmMatchInput || (this.confirmMatchInput
				&& this.confirmMatchInput.val().substring(0, this.options.confirmMatchText.length).toUpperCase() == this.options.confirmMatchText.toUpperCase());
		},
		
		open : function () {
			this.confirmDialog.dialog('open');
		},
		
		close : function () {
			this.confirmDialog.dialog('close');
		},
		
		remove : function() {
			this.confirmDialog.dialog('close');
			this.confirmDialog.remove();
		},
		
		setPromptText : function(text) {
			if (text === undefined) {
				this.confirmDialog.append("<p>Are you sure?</p>");
			} else {
				if (text instanceof jQuery)
					this.confirmDialog.append(text);
				else 
					this.confirmDialog.append("<p>" + text + "</p>");
			}
		}
	});
	return ConfirmationDialog;
});