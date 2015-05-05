define('EditAccessControlForm', [ 'jquery', 'jquery-ui', 'ModalLoadingOverlay', 'ConfirmationDialog', 'AlertHandler', 
         'editable', 'moment', 'qtip'], function($, ui, ModalLoadingOverlay, ConfirmationDialog) {
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
				onblur:'submit',
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
			}).on('hidden', function(e, reason) {
				if (reason === 'cancel') {
					$(".add_embargo", this.element).editable('setValue', null);
					self.removeAttribute(self.accessControlModel, 'embargo-until', self.aclPrefix);
					return;
			    } 
			    
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
				setTimeout(function() {
					var container = ((self.options.containingDialog)? self.options.containingDialog : $(body));
					var overlay = new ModalLoadingOverlay(container);
					$.ajax({
						url : self.options.updateUrl,
						type : 'PUT',
						data : self.xml2Str(self.accessControlModel),
						success : function(data) {
							containing.data('can-close', true);
							overlay.remove();
							if (self.options.containingDialog != null) {
								self.options.containingDialog.dialog('close');
							}
							self.alertHandler.alertHandler('success', 'Access control changes saved');
						},
						error : function(data) {
							overlay.remove();
							self.alertHandler.alertHandler('error', 'Failed to save changes: ' + data);
						}
					});
				}, 0);
				
			});
			
			if (this.options.containingDialog) {
				containing.data('can-close', false);
				var confirmationDialog = new ConfirmationDialog({
					'promptText' : 'There are unsaved access control changes, close without saving?',
					'confirmFunction' : function() {
						containing.data('can-close', true);
						containing.dialog('close');
					},
					'solo' : false,
					'dialogOptions' : {
						modal : true,
						minWidth : 200,
						maxWidth : 400,
						position : {
							at : "center center"
						}
					}
				});
				
				containing.on('dialogbeforeclose', function(){
					if (!containing.data('can-close') && self.isDocumentChanged()) {
						confirmationDialog.open();
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
});