define('ResultView', [ 'jquery', 'jquery-ui', 'ResultObjectList', 'URLUtilities', 
		'ResultObjectActionMenu', 'ResultTableActionMenu', 'ConfirmationDialog', 'ActionEventHandler', 'AlertHandler', 'ParentResultObject', 'AddMenu', 'ResultTableView', 'SearchMenu', 'detachplus', 'qtip'], 
		function($, ui, ResultObjectList, URLUtilities, ResultObjectActionMenu, ResultTableActionMenu, ConfirmationDialog, ActionEventHandler, AlertHandler, ParentResultObject, AddMenu) {
	$.widget("cdr.resultView", {
		options : {
			url : null,
			container : null,
			containerPath : null,
			filterParams : '',
			resultUrl : null, 
			selectedId : false,
			
			resultTableTemplate : "tpl!../templates/admin/resultTableView",
			resultEntryTemplate : "tpl!../templates/admin/resultEntry",
			resultTableHeaderTemplate : "tpl!../templates/admin/resultTableHeader",
			searchMenuTemplate : "tpl!../templates/admin/searchMenu",
			navBarTemplate : "tpl!../templates/admin/navigationBar",
			pathTrailTemplate : "tpl!../templates/admin/pathTrail",
			
			resultFields : undefined,
			resultActions : [
						{
							actions : [
								{action : 'PublishBatch', label : 'Publish'},
								{action : 'UnpublishBatch', label : 'Unpublish'}
							]
						}, {
							actions : [
								{action : 'RestoreBatch', label : 'Restore'},
								{action : 'DeleteBatch', label : 'Delete'}
							]
						}
					]
		},
		
		_create : function() {
			var self = this;
			
			this.$resultPage = this.element;
			this.$resultView;
			this.$columnHeaders;
			this.$containerEntry;
			this.$tableActionMenu;
			this.$resultHeader;
			this.$resultTable;
			this.$resultTableWrap;
			this.$window = $(window);
			this.menuOffset = 360;
			
			this.$alertHandler = $("<div id='alertHandler'></div>");
			this.$alertHandler.alertHandler().appendTo(document.body).hide();
			
			if (!this.options.resultFields) {
				this.options.resultFields = {
						"select" : {name : "", colClass : "narrow", dataType : "index", sortField : "collection"},
						"resourceType" : {name : "", colClass : "narrow", sortField : "resourceType"},
						"title" : {name : "Title", colClass : "itemdetails", dataType : "title", sortField : "title"},
						"creator" : {name : "Creator", colClass : "creator", sortField : "creator"},
						"dateAdded" : {name : "Added", colClass : "date_added", sortField : "dateAdded"},
						"dateModified" : {name : "Modified", colClass : "date_added", sortField : "dateUpdated"},
						"actionMenu" : {name : "", colClass : "narrow"}
					};
			}
			
			var actionHandler = new ActionEventHandler();
			
			// Setup the result table component
			self.$resultTableView = $(".result_area > div", self.element);
			self.$resultTableView.resultTableView({
				alertHandler : this.$alertHandler,
				resultFields : self.options.resultFields,
				postRender : $.proxy(self.postRender, self),
				postInit : $.proxy(self.resizeResults, self),
				actionHandler : actionHandler,
				resultActions : self.options.resultActions,
				resultTableTemplate : self.options.resultTableTemplate,
				resultEntryTemplate : self.options.resultEntryTemplate,
				resultTableHeaderTemplate : this.options.resultTableHeaderTemplate,
				navBarTemplate : this.options.navBarTemplate,
				pathTrailTemplate : this.options.pathTrailTemplate
			});
			
			self.$resultPage.on("click", ".res_link", function(e){
				var url = $(this).attr("href");
				history.pushState({}, "", url);
				self.changePage(url);
				e.preventDefault();
				e.stopPropagation();
			});

			self.$resultPage.on("submit", "#search_menu_form", function(e){
				$.ajax({
					url : $(this).attr('action'),
					type : $(this).attr('method'),
					data : $(this).serialize(),
					dataType : 'text',
					success : function(url) {
						console.log("what", url);
						history.pushState({}, "", url);
						self.changePage(url);
					},
					error : function(xhr, status){
						console.log("whoa", status);
					}
				});
				e.preventDefault();
			});
			
			window.onpopstate = function(event) {
				self.changePage(document.location);
			};
			
			this.changePage(this.options.resultUrl);
		},
		
		changePage : function(url) {
			var self = this;
			
			$.ajax({
				url : url,
				dataType : 'json',
				success : function(data) {
					if (this.addMenu) {
						$("#add_menu").remove();
					}
					self.$resultTableView.resultTableView("render", data);
					if (self.searchMenu) {
						self.searchMenu.searchMenu("changeFolder", data.container? data.container.id : "");
					}
				},
				error : function(data) {
					console.error("Failed to load results", data);
				}
			});
		},
		
		postRender : function (data) {
			this.$resultView = $('#result_view');
			this.$columnHeaders = $('.column_headers', this.element);
			this.$resultHeader = $('.result_header', this.element);
			this.$resultTable = $('.result_table', this.element);
			this.$containerEntry = $('.container_header > span > h2', this.element);
			this.$tableActionMenu = $('.result_table_action_menu', this.element);
			this.$resultTableWrap = $('.result_table_wrap', this.element);
			this.$resultArea = $('.result_area', this.element);
			
			var container = data.container;
		
			if (!this.searchMenu) {
				// Keep result area the right size when the menu is resized
				this.searchMenu = $(".search_menu", this.element).searchMenu({
					filterParams : this.options.filterParams,
					container : container,
					containerPath : this.options.containerPath,
					resultUrl : this.options.resultUrl,
					resultTableView : $(".result_area > div"),
					selectedId : container && /\w+\/uuid:[0-9a-f\-]+($|\?)/.test(document.URL)? container.id : false,
				});

				this.searchMenu.on("resize", $.proxy(function() {
					this.menuOffset = searchMenu.position().left + searchMenu.innerWidth() + 40;
					this.resizeResults();
				}, this));
			}
		
			if (container) {
				var containerObject = new ParentResultObject({metadata : container, 
						element : $(".container_entry")});
		
				if (this.addMenu) {
					this.addMenu.setContainer(container);
				} else {
					this.addMenu = new AddMenu({
						container : container,
						selector : "#add_menu",
						alertHandler : this.$alertHandler
					});
				}
			}
			
			$(document).on('mouseover', '.warning_symbol', function(event) {
				// Bind the qTip within the event handler
				$(this).qtip({
					overwrite: false,
					style: { classes: 'validity_tip' },
					show: {
						event: event.type,
						ready: true
					}
				}, event);
			})
		
			this.resizeResults();
			this.$window.resize($.proxy(this.resizeResults, this));
		},
		
		resizeResults : function() {
			var wHeight = this.$window.height(), wWidth = this.$window.width();
			var headerHeight = this.$resultHeader.outerHeight();
			this.$resultPage.height(wHeight - 105);
			this.$resultView.height(wHeight - 105);
			this.$resultHeader.width(wWidth - this.menuOffset);
			this.$resultTable.width(wWidth - this.menuOffset);
			this.$columnHeaders.width(wWidth - this.menuOffset)
					.css('top', headerHeight);
			this.$resultTableWrap.css('padding-top', headerHeight + 20);
			this.$containerEntry.css('max-width', (wWidth - this.$tableActionMenu.width() - this.menuOffset - 105));
			this.$resultArea.css('margin-left', (this.menuOffset - 45) + "px");
		}
	});
});