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
				disabled: true,
			}).on('hidden', function(e, reason) {
				if (reason === 'cancel') {
					$(".add_embargo", this.element).editable('setValue', null);
					self.removeAttribute(self.accessControlModel, 'embargo-until', self.aclPrefix);
					return;
				}
			});

			$(".roles_granted .remove_group", this.element).hide();
			
			$('.add_group_name').one('focus', function(){
				var addGroup = $(this);
				$.getJSON(self.options.groupSuggestionsURL, function(data){
					addGroup.autocomplete({
						source : data
					});
				});
			});

			var containing = this.options.containingDialog;
			
			if (this.options.containingDialog) {
				containing.data('can-close', false);
				var confirmationDialog = new ConfirmationDialog({
					'promptText' : 'Are you sure you would like to close the access control panel?',
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