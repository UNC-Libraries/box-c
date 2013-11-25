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
	
	$.extend(ConfirmationDialog.prototype, {
		options : {
			promptText : 'Are you sure?',
			confirmFunction : undefined,
			confirmTarget : undefined,
			confirmText : 'Yes',
			cancelTarget : undefined,
			cancelFunction : undefined,
			cancelText : 'Cancel',
			solo : true,
			additionalButtons : undefined,
			autoOpen : false,
			addClass : undefined,
			destroyOnClose : false
		},
		
		dialogOptions : {
			modal : false,
			minHeight : 60,
			autoOpen : false,
			resizable : false,
			dialogClass : "no_titlebar confirm_dialog",
			position : {
				my : "right top",
				at : "right bottom"
			}
		},
		
		_create : function(options) {
			$.extend(this.options, options);
			if ('dialogOptions' in this.options)
				$.extend(this.dialogOptions, this.options.dialogOptions);
			var self = this;
			
			this.confirmDialog = $("<div class='confirm_dialogue'></div>");
			if (this.options.promptText === undefined) {
				this.confirmDialog.append("<p>Are you sure?</p>");
			} else {
				this.confirmDialog.append("<p>" + this.options.promptText + "</p>");
			}
			$("body").append(this.confirmDialog);
			
			var buttonsObject = this._generateButtons();
			
			$.extend(this.dialogOptions, {
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
			
			if (this.options.destroyOnClose)
				this.dialogOptions.close(function(){
					self.remove();
				});
			
			this.confirmDialog.dialog(this.dialogOptions);
			if (this.options.addClass)
				this.confirmDialog.addClass(this.options.addClass);
				
			if (this.options.autoOpen)
				this.open();
		},
		
		_generateButtons : function() {
			var buttonsObject = {};
			var self = this;
			
			buttonsObject[this.options.cancelText] = function() {
				if (self.options.cancelFunction) {
					var result = self.options.cancelFunction.call(self.options.cancelTarget);
					if (result !== undefined && !result)
						return;
				}
				$(this).dialog("close");
			};
			
			buttonsObject[this.options.confirmText] = function() {
				if (self.options.confirmFunction) {
					var result = self.options.confirmFunction.call(self.options.confirmTarget);
					if (result !== undefined && !result)
						return;
				}
				$(this).dialog("close");
			};
			
			// Add any additional buttons in
			if (this.options.additionalButtons) {
				for (var index in this.options.additionalButtons)
					buttonsObject[index] = this.options.additionalButtons[index];
			}
			return buttonsObject;
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
		}
	});
	return ConfirmationDialog;
});