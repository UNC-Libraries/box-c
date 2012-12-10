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
(function($) {
	$.widget("cdr.resultObject", {
		options : {
			animateSpeed : 80,
			metadata : null,
			selected : false,
			selectable : true,
			selectCheckboxInitialState : false
		},

		_create : function() {
			if (this.options.metadata instanceof MetadataObject) {
				this.metadata = this.options.metadata;
			} else {
				this.metadata = new MetadataObject(this.options.metadata);
			}
			
			this.pid = this.metadata.pid;
			
			if (this.options.selected)
				this.select();

			var obj = this;
			if (this.options.selectable) {
				this.checkbox = this.element.find("input[type='checkbox']");
				if (this.checkbox) {
					this.checkbox = $(this.checkbox[0]).click(function(event) {
						$.proxy(obj.toggleSelect(), obj);
						event.stopPropagation();
					}).prop("checked", obj.options.selectCheckboxInitialState);
				}
				this.element.click($.proxy(obj.toggleSelect(), obj)).find('a').click(function(event) {
					event.stopPropagation();
				});
			}
		},
		
		initializePublishLinks: function() {
			var links = this.element.find(".publish_link");
			if (!links)
				return;
			
			links.ajaxCallbackButton({
				pid: obj.pid,
				parentObject: obj,
				defaultPublish: $.inArray("Unpublished", this.metadata.status) != -1
			});
		},
		
		initializeDeleteLinks: function() {
			var obj = this;
			
			function deleteFollowup(data) {
				if (data == null) {
					return true;
				}
				return false;
			}

			function deleteComplete() {
				obj.deleteObject();
				this.destroy();
			}

			function deleteWorkDone(data) {
				if (data == null) {
					alert("Unable to delete object " + this.pid.pid);
					return false;
				}
				this.completeTimestamp = data.timestamp;
				return true;
			}
			
			this.element.find(".delete_link").ajaxCallbackButton({
				pid: obj.pid,
				workLabel: "Deleting...",
				workPath: "delete/{idPath}",
				workDone: deleteWorkDone,
				followupLabel: "Cleaning up...",
				followupPath: "services/rest/item/{idPath}/solrRecord/lastIndexed",
				followup: deleteFollowup,
				complete: deleteComplete,
				parentObject: obj,
				confirm: true,
				confirmMessage: "Delete this object?"
			});
		},

		disable : function() {
			this.options.disabled = true;
			this.element.css("cursor", "default");
		},

		enable : function() {
			this.options.disabled = false;
			this.element.css("cursor", "pointer");
		},

		toggleSelect : function() {
			if (this.element.hasClass("selected")) {
				this.unselect();
			} else {
				this.select();
			}
		},

		select : function() {
			this.element.addClass("selected");
			if (this.checkbox) {
				this.checkbox.prop("checked", true);
			}
		},

		unselect : function() {
			this.element.removeClass("selected");
			if (this.checkbox) {
				this.checkbox.prop("checked", false);
			}
		},

		publish : function() {
			this.metadata.publish();
			if (!$.inArray("Parent Unpublished", this.metadata.data.status)) {
				this.element.switchClass("unpublished", "published", op.options.animateSpeed);
			}
		},

		unpublish : function() {
			this.metadata.unpublish();
			if (!$.inArray("Parent Unpublished", this.metadata.data.status)) {
				this.element.switchClass("published", "unpublished", op.options.animateSpeed);
			}
		},

		deleteObject : function() {
			var element = this.element;
			this.element.hide(op.options.animateSpeed, function() {
				element.remove();
			});
		}
	});
})(jQuery);