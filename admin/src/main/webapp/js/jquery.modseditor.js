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
 * jQuery MODS Editor
 * 
 * Dependencies:
 *   vkbeautify.0.98.01.beta.js
 *   jquery 1.7.1
 *   jquery.ui 1.7.1
 *   ajax ace editor
 *   jquery.xmlns.js
 * 
 * @author Ben Pennell
 */
;
(function($) {
	$.fn.extend({
		modsEditor : function(options) {
			return this.each(function() {
				new $.ModsEditor(this, options);
			});
		}
	});
	
	$.fn.modsChangeTabTitle = function() {
		var data = $(this).data("mods");
		var tabTitle = data.title;
		
		if (data.count != null && data.count > 0) {
			tabTitle += " (" + data.count +")";
		}
		$(this).html(tabTitle);
	};
	$.fn.modsChangeTabCount = function(count, add) {
		if (arguments.length == 2 && add) {
			$(this).data("mods").count += count;
		} else {
			$(this).data("mods").count = count;
		}
		$(this).modsChangeTabTitle();
	};
	
	var modsNS = "http://www.loc.gov/mods/v3";

	$.ModsEditor = function(target, options) {
		var elementIndex = 0;
		var xml = null;

		defaults = {
			elementRootPrefix : "root_element_",
			elementPrefix : "mods_element_",
			childrenContainerSelector : " > .mods_children",
			childrenContainerClass : "mods_children",
			attributesContainerSelector : " > .mods_attrs",
			attributesContainerClass : "mods_attrs",
			modsEditorContainerId: "mods_editor_container",
			modsWorkAreaContainerId: "mods_work_area",
			modsContentId : "mods_content",
			
			menuColumnId : "mods_menu_column",
			menuContainerClass : "mods_menu_container",
			menuHeaderClass : "menu_header",
			menuContentClass : 'menu_content',
			menuExpandDuration : 180,
			
			addTopMenuHeaderText : 'Add Top Element',
			addTopMenuId : "add_top_menu",
			addAttrMenuHeaderText : 'Add Attribute',
			addAttrMenuId : "add_attribute_menu",
			addElementMenuHeaderText : 'Add Subelement',
			addElementMenuId : "add_element_menu",
			
			modsMenuBarId : "mods_menu_bar",
			modsMenuHeaderPrefix : "mods_header_item_",
			
			submitButtonId : "send_xml",
			
			modsElementClass : 'mods_element',
			topLevelContainerClass : 'top_level_element_group',
			xmlTabId : "mods_xml_content_tab",
			xmlTabLabel : "XML",
			submissionStatusId : "mods_submit_status",
			
			confirmExitWhenUnsubmitted : true,
			enableGUIKeybindings : true,
			floatingMenu : true,
			enableMenuBar : true,
			
			ajaxOptions : {
				modsUploadPath: null,
				modsRetrievalPath: null,
				modsRetrievalParams : null
			},
			localXMLContentSelector: target,
			prettyXML : true,
			undoHistorySize: 20,
			documentTitle : null,
			nameSpaces: {
				"mods" : "http://www.loc.gov/mods/v3"
			}
		};
		
		options = $.extend({}, defaults, options);
		
		// Add namespaces into jquery
		$.each(options.nameSpaces, function (prefix, value) {
			$.xmlns[prefix] = value;
		});
		
		var modsTree = {};
		
		buildModsTree(Mods.elementTitle, Mods, "");
		
		// Retrieve the local mods content before we start populating the editor.
		var localXMLContent = $(options.localXMLContentSelector).html();
		$(target).empty();
		
		var documentHasChanged = false;
		var editorChangeState = 0;
		var editorMode = 'GUI';
		
		var xmlEditor = null;
		var modsEditorContainer = $("<div/>").attr('id', options.modsEditorContainerId).appendTo(target);
		var modsWorkAreaContainer = null;
		var modsTabContainer = null;
		var modsContent = null;
		var guiContent = null;
		var xmlContent = null;
		var menuColumn = null;
		var menuContainer = null;
		var xmlEditorDiv = null;
		
		var editorHeader = null;
		var menuBarContainer = null;
		var problemsPanel = null;
		
		var selectedTagRange = {'row': 0, 'startColumn': 0, 'endColumn': 0};
		
		var undoHistory = [];
		var undoHeadIndex = -1;
		

		var headerMenuData = [ {
			label : 'File',
			enabled : true,
			action : activateMenu, 
			items : [ {
					label : 'Submit to Server',
					enabled : (options.ajaxOptions.modsUploadPath != null),
					binding : "alt+shift+s",
					action : submitXML
				}, {
					label : 'Export',
					enabled : (getBlobBuilder() !== undefined),
					binding : "alt+shift+e",
					action : exportXML
				} ]
		}, {
			label : 'Edit',
			enabled : true,
			action : activateMenu,
			items : [ {
				label : 'Undo',
				enabled : false,
				binding : "ctrl+z or mac+z",
				action : function(){changeUndoHead(-1);}
			}, {
				label : 'Redo',
				enabled : false,
				binding : "ctrl+y or mac+shift+z",
				action : function(){changeUndoHead(1);}
			}, {
				label : 'Delete',
				enabled : true,
				binding : "del",
				action : function(){
					var selectedElement = $(".selected." + options.modsElementClass);
					if (selectedElement.length == 0) return;
					$("#" + selectedElement.attr('id') + "_del").click();
				}
			}, {
				label : 'Move Element Up',
				enabled : true,
				binding : "alt+up",
				action : function(){
					var selectedElement = $(".selected." + options.modsElementClass);
					if (selectedElement.length == 0) return;
					$("#" + selectedElement.attr('id') + "_up").click();
				}
			}, {
				label : 'Move Element Down',
				enabled : true,
				binding : "alt+down",
				action : function(){
					var selectedElement = $(".selected." + options.modsElementClass);
					if (selectedElement.length == 0) return;
					$("#" + selectedElement.attr('id') + "_down").click();
				}
			} ]
		}, {
			label : 'Select',
			enabled : true,
			action : activateMenu, 
			items : [ {
					label : 'Next Element',
					enabled : false,
					binding : "down",
					action : null
				}, {
					label : 'Previous Element',
					enabled : false,
					binding : "up",
					action : null
				}, {
					label : 'Parent',
					enabled : false,
					binding : "shift+left",
					action : null
				}, {
					label : 'First child',
					enabled : false,
					binding : "shift+right",
					action : null
				}, {
					label : 'Next Sibling',
					enabled : false,
					binding : "shift+down",
					action : null
				}, {
					label : 'Previous Sibling',
					enabled : false,
					binding : "shift+up",
					action : null
				} ]
		}, {
			label : 'View',
			enabled : true,
			action : activateMenu, 
			items : [ {
				label : 'Next Element Tab',
				enabled : false,
				binding : "right",
				action : null
			}, {
				label : 'Previous Element Tab',
				enabled : false,
				binding : "left",
				action : null
			}, {
				label : 'Switch to MODS View',
				enabled : true,
				binding : "alt+shift+m",
				action : function() {
					modsTabContainer.tabs('select', 0);
				}
			}, {
				label : 'Switch to XML View',
				enabled : true,
				binding : "alt+shift+x",
				action : function() {
					modsTabContainer.tabs('select', 1);
				}
			} ]
		}, {
			label : 'Help',
			enabled : true,
			action : activateMenu, 
			items : [ {
				label : 'MODS Outline of Elements',
				enabled : true,
				binding : null,
				action : "http://www.loc.gov/standards/mods/mods-outline.html"
			} ]
		}, {
			label : 'MODS',
			enabled : true,
			action : function() {
				modsTabContainer.tabs('select', 0);
			}, itemClass : 'header_mode_tab'
			
		}, {
			label : 'XML',
			enabled : true,
			action : function() {
				modsTabContainer.tabs('select', 1);
			}, itemClass : 'header_mode_tab'
		} ];
		if (options.enableGUIKeybindings)
			$(window).keydown(keydownCallback);
		if (options.confirmExitWhenUnsubmitted) {
			$(window).bind('beforeunload', function(e) {
				if (documentHasChanged) {
					return "The document contains unsaved changes.";
				}
			});
		}
		
		if (options.ajaxOptions.modsRetrievalPath != null) {
			$.ajax({
				type : "GET",
				url : options.ajaxOptions.modsRetrievalPath,
				data : (options.ajaxOptions.modsRetrievalParams),
				dataType : "text",
				success : function(data) {
					setupEditor(data);
				}
			});
		} else {
			setupEditor(localXMLContent);
		}
		
		
		function setupEditor(xmlString) {
			constructEditor();
			setXMLFromString(xmlString);
			// Top level element menu points to mods content as its target
			$("#" + options.addTopMenuId).data("mods", {
				'target': modsContent
			});
			populateTopLevelElements();
			if (options.floatingMenu) {
				setMenuPosition();
			}
			modsWorkAreaContainer.width(modsEditorContainer.outerWidth() - menuColumn.outerWidth());
			// Capture baseline undo state
			captureUndoSnapshot();
		}
		
		function constructEditor() {
			// Work Area
			modsWorkAreaContainer = $("<div/>").attr('id', options.modsWorkAreaContainerId).appendTo(modsEditorContainer);
			
			// Menu bar
			editorHeader = $("<div/>").attr('id', 'mods_editor_header').appendTo(modsWorkAreaContainer);
			if (options.documentTitle != null)
				$("<h2/>").html("Editing Description: " + options.documentTitle).appendTo(editorHeader);
			menuBarContainer = $("<div/>").attr('id', options.modsMenuBarId).appendTo(editorHeader);
			renderHeaderMenu();
			
			modsTabContainer = $("<div/>").attr("id", "mods_tab_area").css("padding-top", editorHeader.height() + "px").appendTo(modsWorkAreaContainer);
			problemsPanel = $("<pre/>").attr('id', 'mods_problems_panel').hide().appendTo(modsTabContainer);
			
			var modeTabs = $("<ul/>").appendTo(modsTabContainer);
			modeTabs.append("<li><a href='#gui_content'>MODS</a></li>");
			modeTabs.append("<li><a href='#xml_content' id='" + options.xmlTabId + "'>XML</a></li>");
			
			guiContent = $("<div/>").attr('id', 'gui_content').appendTo(modsTabContainer);
			xmlContent = $("<div/>").attr('id', 'xml_content').appendTo(modsTabContainer);
			xmlEditorDiv = $("<div/>").attr('id', 'xml_editor').appendTo(xmlContent);
			resizeXMLEditor();
			modsTabContainer.tabs({
				show: modeChange,
				select: modeTabSelect
			});
			
			if (options.enableMenuBar) {
				modeTabs.css("display", "none");
			}
			
			modsContent = $("<div id='" + options.modsContentId + "'/>");
			modsContent.data("mods", {});
			$(window).resize(function() {
				selected = modsTabContainer.tabs('option', 'selected');
				modsTabContainer.width(modsEditorContainer.outerWidth() - menuColumn.outerWidth());
				if (selected == 0) {
					resizeGraphicalEditor();
				} else {
					resizeXMLEditor();
				}
				editorHeader.width(modsTabContainer.width());
				if (options.floatingMenu) {
					setMenuPosition();
				}
			});
			$("<div/>").attr("class", "placeholder").html("There are no elements in this document.  Use the menu on the right to add new top level elements.").appendTo(modsContent);
			
			guiContent.append(modsContent);
			
			menuColumn = $("<div/>").attr('id', options.menuColumnId).appendTo(modsEditorContainer);
			$("<span/>").attr('id', options.submissionStatusId).html("Document is unchanged").appendTo(menuColumn);
			
			var submitButton = $("<input/>").attr({
				'id' : options.submitButtonId,
				'type' : 'button',
				'class' : 'send_xml',
				'name' : 'submit',
				'value' : 'Submit Changes'
			}).appendTo(menuColumn);
			if (options.ajaxOptions.modsUploadPath == null) {
				if (getBlobBuilder()){
					submitButton.attr("value", "Export");
				} else {
					submitButton.attr("disabled", "disabled");
				}
			}
			
			menuContainer = $("<div class='" + options.menuContainerClass + "'/>").appendTo(menuColumn);
			menuContainer.css({'max-height': $(window).height(), 'overflow-y': 'auto'});
			
			createMenu(options.addElementMenuId, options.addElementMenuHeaderText, menuContainer, true, false);
			createMenu(options.addAttrMenuId, options.addAttrMenuHeaderText, menuContainer, true, false);
			createMenu(options.addTopMenuId, options.addTopMenuHeaderText, menuContainer, true, true);
			
			populateMenu(modsContent, Mods, options.addTopMenuId);
			
			if (options.floatingMenu) {
				$(window).scroll(setMenuPosition);
			}
			
			$("#" + options.submitButtonId).click(function() {
				saveXML();
			});
			$(window).resize();
		}
		
		function selectElement(selected) {
			var selectedElement = null;
			if (selected != null){
				if (selected instanceof jQuery){
					if (selected.length > 0)
						selectedElement = selected;
				} else {
					selected.stopPropagation();
					selectedElement = $(this);
				}
			}
			
			changeSelectedElement(selectedElement);
		}
		
		function changeSelectedElement(selected) {
			$("." + options.modsElementClass + ".selected").removeClass("selected");
			if (selected == null) {
				clearMenu(options.addElementMenuId);
				clearMenu(options.addAttrMenuId);
			} else {
				selected.addClass("selected");
				refreshContextualMenus(selected, selected.data('mods').elementType);
			}
		}
		
		function createMenu(menuId, headerText, appendToMenu, expanded, enabled) {
			var menuHeader = $("<div class='" + options.menuHeaderClass + "'/>").appendTo(appendToMenu);
			if (expanded) {
				menuHeader.html(headerText + " <span>&#9660;</span>");
			} else {
				menuHeader.html(headerText + " <span>&#9654;</span>");
			}
			
			if (!enabled)
				menuHeader.addClass("disabled");
			
			var newMenu = $("<ul id='" + menuId + "' class='" + options.menuContentClass + "'/>").data('mods', {}).appendTo(appendToMenu);
			menuHeader.click(function(){
				if (newMenu.hasClass("disabled")) {
					return;
				}
				
				if (newMenu.is(":visible")){
					//newMenu.hide('slide', {direction: 'up'}, options.menuExpandDuration);
					newMenu.animate({height: 'hide'}, options.menuExpandDuration);
					menuHeader.html(headerText + " <span>&#9654;</span>");
				} else {
					//newMenu.show('slide', {direction: 'down'}, options.menuExpandDuration);
					newMenu.animate({height: 'show'}, options.menuExpandDuration);
					menuHeader.html(headerText + " <span>&#9660;</span>");
				}
			});
			
			return newMenu;
		}
		
		function clearMenu(menuId) {
			var menu = $("#" + menuId);
			var startingHeight = menu.height();
			var menuHeader = menu.prev("." + options.menuHeaderClass);
			menu.empty();
			menu.css({height: startingHeight + "px"}).stop().animate({height: "0px"}, options.menuExpandDuration);
			if (menu.data('mods') !== undefined) {
				menu.data('mods').target = null;
			}
			menuHeader.addClass('disabled');
		}
		
		function refreshContextualMenus(targetElement, elementType) {
			populateMenu(targetElement, elementType, options.addElementMenuId);
			populateAttributeMenu(targetElement, elementType, options.addAttrMenuId);
			setMenuPosition();
		}
		
		function populateMenu(guiTarget, element, menuId) {
			menu = $("#" + menuId);
			
			if (menu != null && menu.data('mods').target == guiTarget || element == null){
				return;
			}
			
			menu.css("height", "auto");
			var startingHeight = menu.height();
			menu.empty();
			
			menu.data('mods').target = guiTarget;
			
			$.each(element.elements, function(){
				var modsElement = this;
				$("<li/>").attr({
					title : 'Add ' + modsElement.title
				}).html(modsElement.title).click(addChildElementCallback).data('mods', {
					"target": guiTarget,
					"elementType": modsElement
				}).appendTo(menu);
			});
			var endingHeight = menu.height();
			if (endingHeight == 0)
				endingHeight = 1;
			menu.css({height: startingHeight + "px"}).stop().animate({height: endingHeight + "px"}, options.menuExpandDuration).show();

			if (menu.children().length == 0) {
				menu.prev("." + options.menuHeaderClass).addClass("disabled");
			} else {
				menu.prev("." + options.menuHeaderClass).removeClass("disabled");
			}
		}
		
		function populateAttributeMenu(guiTarget, elementType, menuId) {
			menu = $("#" + menuId);
			
			if (menu != null && menu.data('mods').target == guiTarget || elementType == null){
				return;
			}
			
			menu.css("height", "auto");
			var startingHeight = menu.height();
			menu.empty();
			
			menu.data('mods').target = guiTarget;
			
			var attributesArray = elementType.attributes;
			var attributesPresent = {};
			$(guiTarget.data("mods").elementNode[0].attributes).each(function() {
				var targetAttribute = this;
				$.each(attributesArray, function(){
					if (this.title == targetAttribute.nodeName) {
						attributesPresent[this.title] = $("#" + guiTarget.attr('id') + "_" + targetAttribute.nodeName);
					}
				});
			});
			
			$.each(elementType.attributes, function(){
				var attribute = this;
				var addButton = $("<li/>").attr({
					title : 'Add ' + attribute.title,
					'id' : guiTarget.attr('id') + "_" + attribute.title + "_add"
				}).html(attribute.title).click(addAttributeButtonCallback).data('mods', {
					"attributeType": attribute,
					"target": guiTarget
				}).appendTo(menu);
				
				if (attribute.title in attributesPresent) {
					addButton.addClass("disabled");
					if (attributesPresent[attribute.title].length > 0)
						attributesPresent[attribute.title].data('mods').addButton = addButton;
				}
			});
			var endingHeight = menu.height();
			if (endingHeight == 0)
				endingHeight = 1;
			menu.css({height: startingHeight + "px"}).stop().animate({height: endingHeight + "px"}, options.menuExpandDuration).show();
			
			if (menu.children().length == 0) {
				menu.prev("." + options.menuHeaderClass).addClass("disabled");
			} else {
				menu.prev("." + options.menuHeaderClass).removeClass("disabled");
			}
		}
		
		function addChildElementCallback() {
			var targetElement = $(this).data('mods').target;
			
			if (editorMode == 'XML') {
				// Refresh xml state
				if (editorChangeState == 2) {
					try {
						setXMLFromEditor();
					} catch (e) {
						addProblem("Unable to add element, please fix existing XML syntax first.", e);
						return;
					}
				}
			}
			
			//(elementType, parentNode, guiParent)
			var newElement = addElement($(this).data("mods").elementType, targetElement);
			
			if (editorMode == 'XML') {
				refreshXMLEditor();
				// Move cursor to the newly added element
				var instanceNumber = 0;
				xml.find(newElement[0].localName).each(function() {
					if (this === newElement.get(0)) {
						return false;
					}
					instanceNumber++;
				});
				var Range = require("ace/range").Range;
				var startPosition = new Range(0,0,0,0);
				var pattern = new RegExp("<(mods:)?" + newElement[0].localName +"(\\s|\\/|>|$)", "g");
				xmlEditor.find(pattern, {'regExp': true, 'start': startPosition, 'wrap': false});
				for (var i = 0; i < instanceNumber; i++) {
					xmlEditor.findNext({'needle' : pattern});
				}
				xmlEditor.clearSelection();
				xmlEditor.selection.moveCursorBy(0, -1 * newElement[0].localName.length);
			} else {
				if (targetElement.attr("id") != modsContent.attr("id")) {
					$("#" + targetElement.attr("id") + "_tab_elements_link").modsChangeTabCount(1, true);
					targetElement.tabs("select", targetElement.attr("id") + "_tab_elements");
				}
				focusElement(newElement);
				selectElement(newElement);
			}
			setEditorChangeState(3);
			setDocumentHasChanged(true);
			captureUndoSnapshot();
		}
		
		function focusElement(focusTarget) {
			if (!isCompletelyOnScreen(focusTarget)){
				var scrollHeight = focusTarget.offset().top + (focusTarget.height()/2) - ($(window).height()/2);
				if (scrollHeight > focusTarget.offset().top)
					scrollHeight = focusTarget.offset().top;
				$("html, body").stop().animate({ scrollTop: scrollHeight }, 500);
			}
		}
		
		function addAttributeButtonCallback() {
			if ($(this).hasClass("disabled"))
				return;
			if (editorChangeState == 2) {
				try {
					setXMLFromEditor();
				} catch (e) {
					alert(e.message);
					return;
				}
			}
			var data = $(this).data('mods');
			createAttribute(data.attributeType, data.target.data("mods").elementNode);
			
			if (editorMode == 'GUI') {
				var newAttribute = renderAttribute(data.attributeType, data.target);
				$("#" + data.target.attr("id") + "_tab_attr_link").modsChangeTabCount(1, true);
				data.target.tabs("select", data.target.attr("id") + "_tab_attr");
				focusElement(newAttribute);
				$(this).addClass("disabled");
				newAttribute.data('mods').addButton = $(this);
			} else {
				refreshXMLEditor();
			}
			setEditorChangeState(3);
		}
		
		function refreshXMLEditor() {
			setEditorChangeState(0);
			selectedTagRange = {'row': 0, 'startColumn': 0, 'endColumn': 0};
			var cursorPosition = xmlEditor.selection.getCursor();
			xmlEditor.getSession().setValue(xml2Str(xml));
			setEditorChangeState(1);
			xmlEditor.focus();
			xmlEditor.selection.moveCursorToPosition(cursorPosition);
		}
		
		function isCompletelyOnScreen(object) {
			var objectTop = object.offset().top;
			var objectBottom = objectTop + object.height();
			var docViewTop = $(window).scrollTop();
		    var docViewBottom = docViewTop + $(window).height();
		    
		    return (docViewTop < objectTop) && (docViewBottom > objectBottom);
		}
		
		function addElement(elementType, guiParent) {
			var parentNode = guiParent.data("mods").elementNode;
			// Create the new element in a dummy document with the mods namespace
			// It will retain its namespace after attaching to the xml document regardless of mods ns prefix
			var newElement = $($.parseXML("<wrap xmlns:mods='" + modsNS + "'><" + elementType.elementTitle + "/></wrap>")).find("wrap > *").clone();
			newElement.text(" ");
			parentNode.append(newElement);
			if (editorMode == 'GUI') {
				if (newElement.parents().length == 1){
					return renderElement(newElement, modsContent, false);
				} else {
					return renderElement(newElement, guiParent.children("." + options.childrenContainerClass), false);
				}
			}
			return newElement;
		}
		
		function clearElements(){
			$("." + options.topLevelContainerClass).remove();
		}
		
		function resizeGraphicalEditor() {
			//modsContent.width(guiContent.width() - menuContainer.width() - 30);
		}
		
		function resizeXMLEditor() {
			var xmlEditorHeight = ($(window).height() - xmlEditorDiv.offset().top);
			xmlContent.css({'height': xmlEditorHeight + 'px'});
			xmlEditorDiv.width(xmlContent.innerWidth());
			xmlEditorDiv.height(xmlEditorHeight);
			if (menuContainer != null)
				menuContainer.css({'max-height': $(modsWorkAreaContainer).height() - menuContainer.offset().top});
			if (xmlEditor != null)
				xmlEditor.resize();
		}
		
		function setMenuPosition() {
			if (menuColumn == null || menuColumn.offset() == null)
				return;
			
			var menuTop = modsWorkAreaContainer.offset().top;
			if ($(window).scrollTop() >= menuTop) {
				menuColumn.css({
					position : 'fixed',
					left : modsEditorContainer.offset().left + modsEditorContainer.outerWidth() - menuColumn.outerWidth(),
					top : 0
				});
				editorHeader.css({
					position : (editorMode == 'GUI')? 'fixed' : 'absolute',
					top : (editorMode == 'GUI')? 0 : menuTop
				});
			} else {
				menuColumn.css({
					position : 'absolute',
					left : modsEditorContainer.offset().left + modsEditorContainer.outerWidth() - menuColumn.outerWidth(),
					top : menuTop
				});
				editorHeader.css({
					position : 'absolute',
					top : menuTop
				});
			}
			
			// Adjust the menu's height so that it doesn't run out of the editor container
			
			// Gap between the top of the column and the beginning of the actual menu
			var menuOffset = menuContainer.offset().top - menuColumn.offset().top;
			// Default height matches the height of the work area
			var menuHeight = modsWorkAreaContainer.height() - menuOffset;
			
			var workAreaOffset = menuColumn.offset().top - $(window).scrollTop();
			if (workAreaOffset < 0)
				workAreaOffset = 0;
			// Prevent menu from exceeding window height
			if (menuHeight + menuOffset > $(window).height()) {
				menuHeight = $(window).height() - menuOffset;
			}
			
			// Prevent menu from exceeding editor height
			if (menuHeight + menuOffset > modsWorkAreaContainer.height() + modsWorkAreaContainer.offset().top - $(window).scrollTop()) {
				menuHeight = modsWorkAreaContainer.height() + modsWorkAreaContainer.offset().top - $(window).scrollTop() - menuOffset;
			}
			menuContainer.css({'max-height': menuHeight});
		}
		
		function modeTabSelect(event, ui) {
			if (ui.index == 0) {
				if (xmlEditor != null && editorChangeState >= 2) {
					// Try to reconstruct the xml object before changing tabs.  Cancel change if parse error to avoid losing changes.
					try {
						setXMLFromEditor();
					} catch (e) {
						addProblem("Invalid xml", e);
						return false;
					}
					captureUndoSnapshot();
				}
			}
		}
		
		function setXMLFromEditor() {
			var xmlString = xmlEditor.getValue();
			setXMLFromString(xmlString);
		}
		
		function setXMLFromString(xmlString) {
			// parseXML doesn't return any info on why a document is invalid, so do it the old fashion way.
			if (window.DOMParser) {
				parser = new DOMParser();
				if (options.prettyXML) {
					xmlString = vkbeautify.xml(xmlString);
				}
				xmlDoc = parser.parseFromString(xmlString, "application/xml");
				
				var parseError = xmlDoc.getElementsByTagName("parsererror");
				if (parseError.length > 0){
					throw new Error($(parseError).text());
				}
			} else {
				// Internet Explorer
				xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
				xmlDoc.async = false;
				if (options.prettyXML) {
					xmlString = vkbeautify.xml(xmlString);
				}
				xmlDoc.loadXML(xmlString);
				if (xmlDoc.parseError.errorCode != 0) {
					throw new Error("Error in line " + xmlDoc.parseError.line + " position " + xmlDoc.parseError.linePos
							+ "\nError Code: " + xmlDoc.parseError.errorCode + "\nError Reason: "
							+ xmlDoc.parseError.reason + "Error Line: " + xmlDoc.parseError.srcText);
				}
			}
			xml = $(xmlDoc);
			modsContent.data("mods").elementNode = xml.children().first();
			clearProblemPanel();
		}
		
		function modeChange(event, ui) {
			if (ui.index == 0) {
				editorMode = 'GUI';
				changeSelectedElement(null);
				selectedTagRange = {'row': 0, 'startColumn': 0, 'endColumn': 0};
				if (xmlEditor != null && editorChangeState >= 2) {
					refreshDisplay();
				}
				
				$("#" + options.addAttrMenuId).empty();
				$("#" + options.addElementMenuId).empty();
				setEditorChangeState(0);
				

				$(".active_mode_tab").removeClass("active_mode_tab");
				$("#" + options.modsMenuHeaderPrefix + "MODS").addClass("active_mode_tab");
			} else {
				editorMode = 'XML';
				$(".active_mode_tab").removeClass("active_mode_tab");
				$("#" + options.modsMenuHeaderPrefix + "XML").addClass("active_mode_tab");
				var firstLoad = xmlEditor == null;
				
				if (firstLoad){
					xmlEditor = ace.edit("xml_editor");
					xmlEditor.setTheme("ace/theme/textmate");
					xmlEditor.getSession().setMode("ace/mode/xml");
					xmlEditor.setShowPrintMargin(false);
					xmlEditor.getSession().setUseSoftTabs(true);
					xmlEditor.getSession().on('change', function(){
						if (editorChangeState == 1 || editorChangeState == 3){
							setDocumentHasChanged(true);
							setEditorChangeState(2);
						}
					});
				}
				
				resizeXMLEditor();
				
				clearMenu(options.addElementMenuId);
				clearMenu(options.addAttrMenuId);
				refreshDisplay();
				setMenuPosition();
				
				$("#" + options.addAttrMenuId).empty();
				$("#" + options.addElementMenuId).empty();
				if (firstLoad) {
					// Delay setting this so it won't trigger until the document is loaded
					xmlEditor.getSession().selection.on('changeCursor', changeXMLEditorCursorCallback);
				}
			}
		}
		
		function refreshDisplay() {
			if (editorMode == 'GUI') {
				elementIndex = 0;
				clearElements();
				populateTopLevelElements();
			} else {
				var markers = xmlEditor.session.getMarkers();
				$.each(markers, function(index) {
					xmlEditor.session.removeMarker(index);
				});
				
				setEditorChangeState(0);
				$("#" + options.xmlTabId).html(options.xmlTabLabel);
				var xmlString = xml2Str(xml);
				try {
					xmlEditor.getSession().setValue(xmlString);
				} catch (e) {
					alert(e);
				}
				
				xmlEditor.clearSelection();
				setEditorChangeState(1);
				
				changeXMLEditorCursorCallback();
			}
		}
		
		function changeXMLEditorCursorCallback(e) {
			if (editorChangeState == 0)
				return;
			var currentLine = xmlEditor.getSession().getDocument().getLine(xmlEditor.selection.getCursor().row);
			var openingIndex = currentLine.lastIndexOf("<", xmlEditor.selection.getCursor().column);
			var preceedingClosingIndex = currentLine.lastIndexOf(">", xmlEditor.selection.getCursor().column);
			
			// Not inside a tag
			if (openingIndex <= preceedingClosingIndex)
				return;
			
			var currentRow = xmlEditor.selection.getCursor().row;
			var closingIndex = currentLine.indexOf(">", xmlEditor.selection.getCursor().column);
			if (closingIndex == -1)
				closingIndex = currentLine.length - 1;
			
			var tagRegex = /<((mods:)?[a-zA-Z]+)( |\/|>|$)/;
			var match = tagRegex.exec(currentLine.substring(openingIndex));
			
			// Check to see if the tag being selected is already selected.  If it is and the document hasn't been changed, then quit.
			if (match != null && !(editorChangeState != 2 && currentRow == selectedTagRange.row 
					&& openingIndex == selectedTagRange.startColumn && closingIndex == selectedTagRange.endColumn)){
				var tagTitle = match[1];
				var prefixedTitle = tagTitle;
				var unprefixedTitle = tagTitle;
				if (tagTitle.indexOf("mods:") == -1){
					prefixedTitle = "mods:" + prefixedTitle;
				} else {
					unprefixedTitle = tagTitle.substring(tagTitle.indexOf(":") + 1);
				}
				
				// No element type or is the root node, done.
				if (!(prefixedTitle in modsTree) || prefixedTitle == "mods:mods")
					return;
				var elementType = modsTree[prefixedTitle];
				
				if (editorChangeState == 2) {
					//Refresh the xml if it has changed
					try {
						setXMLFromEditor();
						editorChangeState = 3;
					} catch (e) {
						// XML isn't valid, so can't continue
						return;
					}
				}
				
				var Range = require("ace/range").Range;
				var range = new Range(0, 0, xmlEditor.selection.getCursor().row, openingIndex);
				var preceedingLines = xmlEditor.getSession().getDocument().getTextRange(range);
				
				var instanceNumber = tagOccurrences(preceedingLines, tagTitle);
				// Get xpath to this object using jquery.
				var elementNode = $("*", xml).filter(function() {
			        return modsEquals(this, unprefixedTitle);
			      })[instanceNumber];
				if (elementNode == null)
					return;
				
				var dummyTargetNode = $("<div/>").attr('id', prefixedTitle + "_" + instanceNumber).data("mods", {"elementNode" : $(elementNode)});
				
				// Attempt to disambiguate by selecting by parent tag name.
				if (elementNode.parentNode == null || elementNode.parentNode.tagName == null)
					return;
				var tagName = elementNode.parentNode.tagName;
				if (tagName.indexOf("mods:") == -1){
					tagName = "mods:" + tagName;
				}
				elementType = elementType[tagName];
					
				if (elementType == null) {
					clearMenu(options.addElementMenuId);
					clearMenu(options.addAttrMenuId);
					return;
				}
				
				refreshContextualMenus(dummyTargetNode, elementType);
				
				setMenuPosition();
				
				selectedTagRange.row = currentRow;
				selectedTagRange.startColumn = openingIndex;
				selectedTagRange.endColumn = closingIndex;
				
				var Range = require("ace/range").Range;
				var markers = xmlEditor.session.getMarkers();
				
				$.each(markers, function(index) {
					xmlEditor.session.removeMarker(index);
				});
				xmlEditor.session.addMarker(new Range(selectedTagRange.row, selectedTagRange.startColumn, selectedTagRange.row, selectedTagRange.endColumn + 1), "highlighted", "line", false);
			}
				
			return;
		}
		
		function setEditorChangeState(value) {
			editorChangeState = value;
			if (editorChangeState == 2 || editorChangeState == 3){
				setDocumentHasChanged(true);
			}
		}
		
		function setDocumentHasChanged(value) {
			documentHasChanged = value;
			if (documentHasChanged) {
				$("#" + options.submissionStatusId).html("Unsaved changes");
			} else {
				$("#" + options.submissionStatusId).html("All changes saved");
			}
		}

		function populateTopLevelElements() {
			// order child nodes as they are in XML
			xml.children("mods|mods").children().each(function(){
				if (this.namespaceURI != modsNS) {
					return;
				}
				var element = modsTree["mods:" + this.localName]["mods:mods"];
				if (element == null)
					return;
				renderElement($(this), modsContent, true);
				//addElement(element, modsContent);
				
			});
		}
		
		function saveXML() {
			if (options.ajaxOptions.modsUploadPath != null) {
				submitXML();
			} else {
				// Implement later when there is more browser support for html5 File API
				exportXML();
			}
		}
		
		function getBlobBuilder() {
			return window.BlobBuilder || window.WebKitBlobBuilder || window.MozBlobBuilder || window.MSBlobBuilder;
		}
		
		function exportXML() {
			window.URL = window.webkitURL || window.URL;
			window.BlobBuilder = getBlobBuilder();
			
			if (window.BlobBuilder === undefined) {
				addProblem("Browser does not support saving files via this editor.  To save, copy and paste the document from the XML view.");
				return false;
			}
			
			if (editorMode == 'XML') {
				try {
					setXMLFromEditor();
				} catch (e) {
					setDocumentHasChanged(true);
					$("#" + options.submissionStatusId).html("Failed to save<br/>See errors at top").css("background-color", "#ffbbbb").animate({backgroundColor: "#ffffff"}, 1000);
					addProblem("Cannot save due to invalid xml", e);
					return false;
				}
			}
			
			var xmlString = xml2Str(xml);
			var blobBuilder = new BlobBuilder();
			blobBuilder.append(xmlString);
			
			var mimeType = "text/xml";
			
			var a = document.createElement('a');
			a.download = "mods.xml";
			a.href = window.URL.createObjectURL(blobBuilder.getBlob(mimeType));
			
			a.dataset.downloadurl = [mimeType, a.download, a.href].join(':');
			a.target = "exportMODS";
			
			var event = document.createEvent("MouseEvents");
			event.initMouseEvent(
				"click", true, false, window, 0, 0, 0, 0, 0
				, false, false, false, false, 0, null
			);
			a.dispatchEvent(event);
		}

		function submitXML() {
			if (editorMode == 'XML') {
				try {
					setXMLFromEditor();
				} catch (e) {
					setDocumentHasChanged(true);
					$("#" + options.submissionStatusId).html("Failed to submit<br/>See errors at top").css("background-color", "#ffbbbb").animate({backgroundColor: "#ffffff"}, 1000);
					addProblem("Cannot submit due to invalid xml", e);
					return false;
				}
			}
			
			// convert XML DOM to string
			var xmlString = xml2Str(xml);

			$("#" + options.submissionStatusId).html("Submitting...");
			
			$.ajax({
				'url' : options.ajaxOptions.modsUploadPath,
				'contentType' : "application/xml",
				'type' : "POST",
				'data' : xmlString,
				success : function(response) {
					var responseObject = $(response);
					if (responseObject.length > 0 && responseObject[responseObject.length - 1].localName == "sword:error") {
						setDocumentHasChanged(true);
						$("#" + options.submissionStatusId).html("Failed to submit<br/>See errors at top").css("background-color", "#ffbbbb").animate({backgroundColor: "#ffffff"}, 1000);
						addProblem("Failed to submit MODS document", responseObject.find("atom\\:summary").html());
						return;
					}
					
					setEditorChangeState(0);
					setDocumentHasChanged(false);
					clearProblemPanel();
				},
				error : function(jqXHR, exception) {
					if (jqXHR.status === 0) {
						alert('Not connect.\n Verify Network.');
					} else if (jqXHR.status == 404) {
						alert('Requested page not found. [404]');
					} else if (jqXHR.status == 500) {
						alert('Internal Server Error [500].');
					} else if (exception === 'parsererror') {
						alert('Requested JSON parse failed.');
					} else if (exception === 'timeout') {
						alert('Time out error.');
					} else if (exception === 'abort') {
						alert('Ajax request aborted.');
					} else {
						alert('Uncaught Error.\n' + jqXHR.responseText);
					}
				}
			});
		}

		// convert xml DOM to string
		function xml2Str(xmlNodeObject) {
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
					addProblem('Xmlserializer not supported', e);
					return false;
				}
			}
			xmlStr = vkbeautify.xml(xmlStr);
			return xmlStr;
		}

		function nextIndex() {
			return ++elementIndex;
		}
		
		function renderElement(elementNode, container, recursive) {
			var isTopLevel = (elementNode.parents().length == 1);
			
			var elementId = options.elementPrefix + nextIndex();
			var elementType = modsTree["mods:" + elementNode[0].localName]["mods:" + elementNode.parent()[0].localName];
			if (elementType == null)
				return;
			
			// Create the element and add it to the container
			var newElement = $('<div/>').attr({
				'id' : elementId,
				'class' : elementType.title + 'Instance mods_element'
			}).appendTo(container);
			if (isTopLevel) {
				newElement.addClass(options.topLevelContainerClass);
			}
			
			newElement.data('mods', {
				'elementType': elementType,
				'elementNode': elementNode
			});
			
			// Begin building contents
			var elementHeader = $("<ul/>").attr({
				'class' : 'element_header'
			}).appendTo(newElement);
			var elementNameContainer = $("<li class='element_name'/>").appendTo(elementHeader);

			// set up element title and entry field if appropriate
			$('<span/>').text(elementType.title).appendTo(elementNameContainer);

			// Tabs go in next
			addElementTabs(newElement, newElement, elementHeader, recursive);

			// Action buttons
			elementHeader.append(addTopActions(elementId));

			// Activate the tabs
			newElement.tabs({
				select: function(){
					changeSelectedElement(newElement);
				}
			});

			newElement.click(selectElement);
			
			return newElement;
		}

		function countChildren(element, container) {
			var num = container.children("." + element.title + 'Instance').length;
			return ((num == undefined) ? 0 : num);
		}

		function addTopActions(guiElementId) {
			var topActionSpan = $("<li class='top_actions'/>");

			// create move up button and callback for element
			$('<input>').attr({
				'type' : 'button',
				'value' : '\u2193',
				'id' : guiElementId + '_down'
			}).appendTo(topActionSpan).click(moveDownElementButtonCallback);

			// create move up button and callback for element
			$('<input>').attr({
				'type' : 'button',
				'value' : '\u2191',
				'id' : guiElementId + '_up'
			}).appendTo(topActionSpan).click(moveUpElementButtonCallback);

			// create delete button and callback for element
			$('<input>').attr({
				'type' : 'button',
				'value' : 'X',
				'id' : guiElementId + '_del'
			}).appendTo(topActionSpan).click(deleteElementButtonCallback);
			
			return topActionSpan;
		}

		function addElementTabs(guiElement, contentContainer, listContainer, recursive) {
			var elementType = guiElement.data("mods").elementType;
			var attributesArray = elementType.attributes;
			var elementsArray = elementType.elements;

			if (elementType.type != 'none') {
				addTextTab(guiElement, contentContainer, listContainer);
			}

			if (elementsArray.length > 0) {
				addSubelementTab(guiElement, contentContainer, listContainer, recursive);
			}

			if (attributesArray.length > 0) {
				addAttributeTab(guiElement, contentContainer, listContainer);
			}
		}
		
		function addTextTab(guiElement, contentContainer, listContainer) {
			var elementNode = guiElement.data("mods").elementNode;
			
			var tabContent = addElementTab(guiElement.attr("id") + "_tab_text", contentContainer, listContainer, "Text");
			var textContainsChildren = elementNode.children().length > 0;
			
			var textValue = "";
			if (textContainsChildren) {
				textValue = xml2Str(elementNode.children());
			} else {
				textValue = elementNode.text();
			}
			
			var input = createElementInput(guiElement.data("mods").elementType, guiElement.attr("id") + "_text", 
					textValue, tabContent);
			if (textContainsChildren)
				input.attr("disabled", "disabled");
			input.change(changeTextCallback);
		}
		
		function addSubelementTab(guiElement, contentContainer, listContainer, recursive) {
			var elementsArray = guiElement.data("mods").elementType.elements;
			var tabContent = addElementTab(guiElement.attr("id") + "_tab_elements", contentContainer, listContainer, "Subelements");
			tabContent.addClass(options.childrenContainerClass);
			
			$("<div/>").addClass("placeholder").html("Use the menu on the right to add subelements.").appendTo(tabContent);
			
			var count = 0;
			// Add all the subchildren
			guiElement.data("mods").elementNode.children().each(function() {
				for ( var i = 0; i < elementsArray.length; i++) {
					if (modsEquals(this, elementsArray[i])) {
						if (recursive)
							renderElement($(this), tabContent, true);
						count++;
					}
				}
			});
			
			$("#" + guiElement.attr("id") + "_tab_elements_link", listContainer).modsChangeTabCount(count);
		}
		
		function addAttributeTab(guiElement, contentContainer, listContainer) {
			var attributesArray = guiElement.data("mods").elementType.attributes;
			
			// Generate the tab
			var tabContent = addElementTab(guiElement.attr("id") + "_tab_attr", contentContainer, listContainer, "Attributes");
			tabContent.addClass(options.attributesContainerClass);
			
			$("<div/>").addClass("placeholder").html("Use the menu on the right to add attributes.").appendTo(tabContent);
			
			var count = 0;
			$(guiElement.data("mods").elementNode[0].attributes).each(function(){
				for ( var i = 0; i < attributesArray.length; i++) {
					if (attributesArray[i].title == this.nodeName) {
						renderAttribute(attributesArray[i], guiElement);
						count++;
					}
				}
			});
			
			$("#" + guiElement.attr("id") + "_tab_attr_link", listContainer).modsChangeTabCount(count);
		}

		function addElementTab(tabContentID, contentContainer, listContainer, label) {
			var tabEntry = $("<li/>");
			$("<a/>").attr("href", "#" + tabContentID).attr('id', tabContentID + "_link").data("mods", {
				"count" : 0,
				"title" : label
			}).html(label).appendTo(tabEntry);
			listContainer.append(tabEntry);
			var tabContent = $("<div/>").attr({
				'id' : tabContentID
			});
			contentContainer.append(tabContent);
			return tabContent;
		}
		
		function createElementInput(elementType, inputID, startingValue, appendTarget) {
			var input = null;
			if (elementType.type == 'text') {
				input = $('<input/>').attr({
					'id' : inputID,
					'type' : 'text',
					'value' : startingValue
				}).appendTo(appendTarget);
			} else if (elementType.type == 'textarea') {
				input = $('<textarea/>').attr({
					'id' : inputID,
					'value' : startingValue
				}).appendTo(appendTarget);
			} else if (elementType.type == 'selection') {
				var selectionValues = elementType.values;
				input = $('<select />').attr({
					'id' : inputID
				}).appendTo(appendTarget);

				$.each(selectionValues, function() {
					$('<option />', {
						value : this,
						text : this.toString(),
						selected : (startingValue == this)
					}).appendTo(input);
				});
			}
			return input;
		}

		

		function addAttributeButton(attribute, parentXPath, parentId, buttonContainer) {
			var button = $('<span/>').attr({
				'id' : parentId + "_" + attribute.title + "_btn"
			}).data('mods', {
				'parentId': parentId,
				'attribute': attribute,
				'parentXPath': parentXPath
			}).html(attribute.title).appendTo(buttonContainer);
			button.click(addAttributeButtonCallback);
			return button;
		}

		function renderAttribute(attributeType, guiElement) {
			var parentID = guiElement.attr("id");
			var attributeId = parentID + "_" + attributeType.title;
			var elementNode = guiElement.data("mods").elementNode;
			
			var attributeContainer = $("<div/>").attr({
				'id' : attributeId + "_cont",
				'class' : 'attribute_container'
			}).appendTo(guiElement.children("." + options.attributesContainerClass));
			
			$('<label/>').attr({
				'for' : attributeId
			}).text(attributeType.title).appendTo(attributeContainer);
			
			$("<a/>").html("(x)").css("cursor", "pointer").on('click', function() {
				if ($("#" + attributeId).length > 0) {
					var addButton = $("#" + attributeId).data("mods").addButton;
					if (addButton != null){
						addButton.removeClass("disabled");
					}
				}
				removeAttribute(attributeType, elementNode);
				attributeContainer.remove();
				$("#" + parentID + "_tab_attr_link").modsChangeTabCount(-1, true);
			}).appendTo(attributeContainer);
			
			var attributeValue = elementNode.attr(attributeType.title);
			if (attributeValue == '' && attributeType.defaultValue != null) {
				attributeValue = attributeType.defaultValue;
			}
			
			var attributeInput = createElementInput(attributeType, attributeId, attributeValue, attributeContainer);
			
			attributeInput.data('mods', {
				'elementNode': elementNode,
				'attributeType': attributeType
			}).change(changeAttributeCallback);
			
			return attributeInput;
		}
		
		function createAttribute(attributeType, elementNode) {
			var attributeValue = "";
			if (attributeType.defaultValue) {
				attributeValue = attributeType.defaultValue;
			}
			elementNode.attr(attributeType.title, attributeValue);
			return attributeValue;
		}
		
		function removeAttribute(attributeType, elementNode) {
			elementNode.removeAttr(attributeType.title);
		}

		function changeAttributeCallback() {
			var attributeObject = $(this);
			var data = attributeObject.data('mods');
			data.elementNode.attr(data.attributeType.title, attributeObject.val());
		}

		function deleteElementButtonCallback() {
			var elementObject = getParentObject(this, "_del");
			if (elementObject == undefined)
				return;
			
			// Remove the element from the xml doc
			elementObject.data('mods').elementNode.remove(); 
			
			// Remove element from view
			if (!elementObject.hasClass(options.topLevelContainerClass))
				$("#" + elementObject.parents("." + options.modsElementClass).first().attr("id") + "_tab_elements_link").modsChangeTabCount(-1, true);
			elementObject.remove();
			
			captureUndoSnapshot();
			setDocumentHasChanged(true);
		}

		function reinitializeElement(elementObject) {
			if (elementObject.hasClass(options.modsElementClass))
				elementObject.tabs();
			elementObject.find("." + options.modsElementClass).each(function(){
				$(this).tabs();
			});
		}
		
		
		function swapElements(firstElement, secondElement) {
			if (firstElement == undefined || firstElement.lenght == 0 || secondElement.lenght == 0)
				return;
			
			var movedData = firstElement.data('mods');
			var swapData = secondElement.data('mods');
			
			// Swap the xml nodes
			swapData.elementNode.detach().insertAfter(movedData.elementNode);
			// Swap the gui nodes
			secondElement.detach().insertAfter(firstElement);
			
			// Some things, like tabs, need to be reinitialized
			reinitializeElement(secondElement);
			setDocumentHasChanged(true);
			captureUndoSnapshot();
			
			if (!isCompletelyOnScreen(firstElement)) {
				focusElement(firstElement);
			}
		}

		function moveUpElementButtonCallback() {
			var movedObject = getParentObject(this, "_up");
			var previousSibling = movedObject.prev("." + options.modsElementClass);
			swapElements(movedObject, previousSibling);
		}
		
		function moveDownElementButtonCallback() {
			var movedObject = getParentObject(this, "_down");
			var nextSibling = movedObject.next("." + options.modsElementClass);
			swapElements(nextSibling, movedObject);
		}

		function changeTextCallback() {
			var elementObject = getParentObject(this, "_text");
			if (elementObject === undefined)
				return;
			
			elementObject.data("mods").elementNode.text($(this).val());
			
			setDocumentHasChanged(true);
			captureUndoSnapshot();
		}
		
		function getParentObject(object, suffix) {
			var objectId = $(object).attr('id');
			var parentId = objectId.substring(0, objectId.indexOf(suffix));
			
			var parentObject = $("#" + parentId);
			if (parentObject.length == 0)
				return;
			
			return parentObject;
		}
		
		function addProblem(message, problem) {
			problemsPanel.html(message + "<br/>");
			if (problem !== undefined) {
				if (problem.substring) {
					problemsPanel.append(problem.replace(/</g, "&lt;").replace(/>/g, "&gt;"));
				} else {
					problemsPanel.append(problem.message.replace(/</g, "&lt;").replace(/>/g, "&gt;"));
				}
			}
			refreshProblemPanel();
		}
		
		function clearProblemPanel() {
			problemsPanel.hide();
		}
		
		function refreshProblemPanel() {
			if (problemsPanel.html() == "") {
				problemsPanel.hide("fast");
			} else {
				problemsPanel.show("fast");
			}
		}

		function modsEquals(node, element) {
			return (((element.substring && element == node.localName) || (!element.substring && element.title == node.localName)) 
					&& node.namespaceURI == "http://www.loc.gov/mods/v3");
		}

		function tagOccurrences(string, tagTitle) {
			if (string == null || tagTitle == null)
				return 0;
			var matches = string.match(new RegExp("<" + tagTitle + "( |>|$)", "g"));
			return matches ? matches.length : 0;
		}
		
		function buildModsTree(elementTitle, elementObject, parentTitle){
			if (elementTitle in modsTree) {
				if (!(elementObject in modsTree[elementTitle]))
					modsTree[elementTitle][parentTitle] = elementObject;
			} else {
				modsTree[elementTitle] = {};
				modsTree[elementTitle][parentTitle] = elementObject;
			}
			$.each(elementObject.elements, function() {
				buildModsTree(this.elementTitle, this, elementObject.elementTitle);
			});
		}
		
		function getXPath(element) {
		    var xpath = '';
		    for ( ; element && element.nodeType == 1; element = element.parentNode ) {
		        var id = $(element.parentNode).children(element.tagName.replace(":", "\\:")).index(element) + 1;
		        id = ('[' + id + ']');
		        if (element.tagName.indexOf("mods:") == -1)
		        	xpath = '/mods:' + element.tagName + id + xpath;
		        else xpath = '/' + element.tagName + id + xpath;
		    }
		    return xpath;
		}
		
		function captureUndoSnapshot() {
			if (options.undoHistorySize <= 0)
				return;
			
			if (undoHeadIndex < undoHistory.length - 1) {
				undoHistory = undoHistory.slice(0, undoHeadIndex + 1);
			}
			
			if (undoHistory.length >= options.undoHistorySize) {
				undoHistory = undoHistory.slice(1, undoHistory.length);
			}

			undoHeadIndex = undoHistory.length;
			undoHistory.push(xml.clone());
			refreshMenuUndo();
		}
					
		function keydownCallback(e) {
			if (editorMode == 'GUI') {
				var focused = $("input:focus, textarea:focus, select:focus");
				
				// Escape key, blur the currently selected input
				if (e.keyCode == 27 && focused.length > 0) {
					focused.blur();
					return false;
				}
				
				var selectedElement = $(".selected." + options.modsElementClass);
				
				// Enter, focus the first visible input
				if (e.keyCode == 13 && selectedElement.length > 0 && focused.length == 0) {
					selectedElement.find("input[type=text]:visible, textarea:visible, select:visible").first().focus();
					return false;
				}
				
				// Tab, select the next input
				if (e.keyCode == 9 && selectedElement.length > 0) {
					if (focused.length > 0) {
						// When an input is already focused, tabbing selects the next sibling input
						var foundFocus = false;
						var siblingInputs = selectedElement.find("input[type=text]:visible, textarea:visible, select:visible");
						// Shift tab selects the previous sibling
						if (e.shiftKey) {
							siblingInputs = $(siblingInputs.get().reverse());
						}
						siblingInputs.each(function(){
							if (foundFocus) {
								$(this).focus();
								return false;
							} else if (this.id == focused.attr('id')) {
								foundFocus = true;
							}
						});
						return false;
					} else {
						if (!e.shiftKey) {
							// If nothing is focused, than always select the first visible child input.
							selectedElement.find("input[type=text]:visible, textarea:visible, select:visible").first().focus();
						}
						return false;
					}
				}
				
				// Delete key press while item selected but nothing is focused.
				if (e.keyCode == 46 && focused.length == 0 && selectedElement.length > 0) {
					// After delete, select next sibling, previous sibling, or parent, as available.
					var afterDeleteSelection = selectedElement.next("." + options.modsElementClass);
					if (afterDeleteSelection.length == 0)
						afterDeleteSelection = selectedElement.prev("." + options.modsElementClass);
					if (afterDeleteSelection.length == 0)
						afterDeleteSelection = $("#" + selectedElement.data("mods" + "." + options.modsElementClass).parentId);
					
					$("#" + selectedElement.attr('id') + "_del").click();
					
					selectElement(afterDeleteSelection);
					return false;
				}
				
				if (e.keyCode > 36 && e.keyCode < 41 && focused.length == 0){
					var newSelection = null;
					
					if (e.altKey) {
						// Alt + up or down move the element up and down in the document
						if (e.keyCode == 40) {
							$("#" + selectedElement.attr('id') + "_down").click();
							return false;
						} else if (e.keyCode == 38) {
							$("#" + selectedElement.attr('id') + "_up").click();
							return false;
						}
					}
					
					if (e.shiftKey) {
						// If holding shift while pressing up or down, then jump to the next/prev sibling
						if (e.keyCode == 40) {
							if (selectedElement.length > 0) {
								newSelection = selectedElement.next("." + options.modsElementClass);
								if (newSelection.length == 0 && !selectedElement.hasClass(options.topLevelContainerClass)) {
									// If there is no next sibling but the parent has one, then go to parents sibling
									newSelection = $("#" + selectedElement.data('mods').parentId).next("." + options.modsElementClass);
								}
							} else {
								newSelection = $("." + options.modsElementClass).first();
							}
						} else if (e.keyCode == 38) {
							newSelection = selectedElement.prev("." + options.modsElementClass);
							if (newSelection.length == 0 && !selectedElement.hasClass(options.topLevelContainerClass)) {
								// If there is no next sibling but the parent has one, then go to parents sibling
								newSelection = $("#" + selectedElement.data('mods').parentId);
							}
						} else if (e.keyCode == 37) {
							newSelection = selectedElement.parents("." + options.modsElementClass).first();
							if (newSelection.lenght == 0)
								return false;
						} else if (e.keyCode == 39) {
							newSelection = selectedElement.find("." + options.modsElementClass).first();
							if (newSelection.lenght == 0)
								return false;
						}
					} else {
						// If not holding shift while hitting up or down, go to the next/prev element
						if (e.keyCode == 40 || e.keyCode == 38){
							if (selectedElement.length == 0) {
								if (e.keyCode == 40)
									newSelection = $("." + options.modsElementClass).first();
							} else {
								var found = false;
								var allElements = $("." + options.modsElementClass + ":visible", modsContent);
								
								if (e.keyCode == 38)
									allElements = $(allElements.get().reverse());
								
								allElements.each(function(){
									if (found) {
										newSelection = $(this);
										return false;
									} else if (this.id == selectedElement.attr('id')) {
										found = true;
									}
								});
							}
						}
						
						if (e.keyCode == 39 || e.keyCode == 37) {
							// Right arrow, move selection to first child.  Or if alt is held, change tabs
							var currentTab = selectedElement.tabs('option', 'selected') + (e.keyCode == 39? 1: -1);
							if (currentTab < selectedElement.tabs('length') && currentTab >= 0) {
								selectedElement.tabs('option', 'selected', currentTab);
							}
						}
					}
					
					if (newSelection != null && newSelection.length > 0){
						changeSelectedElement(newSelection.first());
						focusElement(newSelection);
						return false;
					} else return true;
				}
				
				if (e.metaKey && focused.length == 0 && e.keyCode == 'Z'.charCodeAt(0)) {
					// Undo
					changeUndoHead(e.shiftKey? 1: -1);
					return false;
				} else if (e.metaKey && focused.length == 0 && e.keyCode == 'Y'.charCodeAt(0)){
					// Redo
					changeUndoHead(1);
					return false;
				}
			}
			
			// Save, on either tab.
			if (e.altKey && e.shiftKey && e.keyCode == 'S'.charCodeAt(0)) {
				$("#" + options.submitButtonId).click();
				return false;
			}
			
			if (e.altKey && e.shiftKey && e.keyCode == 'E'.charCodeAt(0)) {
				exportXML();
				return false;
			}
			
			if (e.altKey && e.shiftKey && e.keyCode == 'M'.charCodeAt(0)) {
				modsTabContainer.tabs('select', 0);
				return false;
			}
			
			if (e.altKey && e.shiftKey && e.keyCode == 'X'.charCodeAt(0)) {
				modsTabContainer.tabs('select', 1);
				return false;
			}
			
			return true;
		}
		
		function changeUndoHead(step){
			if ((step < 0 && undoHeadIndex + step < 0) || (step > 0 && undoHeadIndex + step >= undoHistory.length
					||  undoHeadIndex + step >= options.undoHistorySize))
				return;
			
			undoHeadIndex += step;
			xml = undoHistory[undoHeadIndex].clone();
			
			changeSelectedElement(null);
			refreshMenuUndo();
			refreshDisplay();
		}
		
		function activateMenu(event) {
			if (menuBarContainer.hasClass("active")) {
				menuBarContainer.removeClass("active");
				return;
			}
			menuBarContainer.addClass("active");
			menuBarContainer.children("ul").children("li").click(function (event) {
				event.stopPropagation();
			});
			$('html').one("click" ,function() {
				menuBarContainer.removeClass("active");
			});
			event.stopPropagation();
		}
		
		function refreshMenuUndo() {
			if (undoHeadIndex > 0) {
				$("#" + options.modsMenuHeaderPrefix + "Undo").removeClass("disabled").data("menuItemData").enabled = true;
			} else {
				$("#" + options.modsMenuHeaderPrefix + "Undo").addClass("disabled").data("menuItemData").enabled = false;
			}
			if (undoHeadIndex < undoHistory.length - 1) {
				$("#" + options.modsMenuHeaderPrefix + "Redo").removeClass("disabled").data("menuItemData").enabled = true;
			} else {
				$("#" + options.modsMenuHeaderPrefix + "Redo").addClass("disabled").data("menuItemData").enabled = false;
			}
		}
		
		function generateMenuItem(menuItemData, parentMenu) {
			var menuItem = $("<li/>").appendTo(parentMenu);
			var menuItemLink = $("<a/>").appendTo(menuItem).html("<span>" + menuItemData.label + "</span>");
			if (menuItemData.binding) {
				menuItemLink.append("<span class='binding'>" + menuItemData.binding + "</span>");
			}
			if (menuItemData.action != null) {
				if (Object.prototype.toString.call(menuItemData.action) == '[object Function]'){
					menuItem.click(menuItemData.action);
				} else {
					menuItemLink.attr({"href": menuItemData.action, "target" : "_blank"});
				}
			}
			if (!menuItemData.enabled) {
				menuItem.addClass("disabled");
			}
			if (menuItemData.itemClass) {
				menuItem.addClass(menuItemData.itemClass);
			}
			menuItem.data("menuItemData", menuItemData).attr("id", options.modsMenuHeaderPrefix + menuItemData.label.replace(" ", "_"));
			if (menuItemData.items !== undefined && menuItemData.items.length > 0) {
				var subMenu = $("<ul/>").addClass('sub_menu').appendTo(menuItem);
				$.each(menuItemData.items, function() {
					generateMenuItem(this, subMenu);
				});
			}
		}
		
		function renderHeaderMenu() {
			var headerMenu = $("<ul/>");
			menuBarContainer.empty().append(headerMenu);
			$.each(headerMenuData, function() {
				generateMenuItem(this, headerMenu);
			});
		}
	};
})(jQuery);
