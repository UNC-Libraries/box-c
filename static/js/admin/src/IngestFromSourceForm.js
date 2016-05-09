define('IngestFromSourceForm', [ 'jquery', 'AbstractFileUploadForm', 'ModalLoadingOverlay', 'StringUtilities', "tpl!../templates/admin/ingestSourceCandidates", "tpl!../templates/admin/ingestSourceMetadata"], function($, AbstractFileUploadForm, ModalLoadingOverlay, StringUtilities, candidatesTemplate, metadataTemplate) {

	var defaultOptions = {
			title : 'Ingest from file server',
			createFormTemplate : candidatesTemplate
	};

	function IngestFromSourceForm(options) {
		this.options = $.extend({}, AbstractFileUploadForm.prototype.getDefaultOptions(), defaultOptions, options);
	};
	
	IngestFromSourceForm.prototype.constructor = IngestFromSourceForm;
	IngestFromSourceForm.prototype = Object.create( AbstractFileUploadForm.prototype );
	
	IngestFromSourceForm.prototype.open = function(pid) {
		var self = this;
		
		this.dialog = $("<div class='containingDialog'></div>");
		this.dialog.dialog({
			autoOpen: true,
			width: 560,
			height: 500,
			modal: true,
			title: "Add from file server",
			resizable: false
		});
		
		this.pid = pid;
		
		this.loadCandidateList();
	};
	
	IngestFromSourceForm.prototype.loadCandidateList = function(selectedCandidates) {
		var self = this;
		
		var loadingOverlay = new ModalLoadingOverlay(this.dialog, {
			autoOpen : false,
			type : 'icon',
			dialog : self.dialog
		});
		loadingOverlay.open();
		
		$.ajax({
			url : "listSources/" + this.pid,
			type : "GET",
			cache: false
		}).done(function(data){
			loadingOverlay.remove();
			
			self.renderCandidateList(data.sources, data.candidates, selectedCandidates);
		});
	};
	
	IngestFromSourceForm.prototype.renderCandidateList = function(sources, candidates, selected) {
		var self = this;
		
		if (candidates.length == 0) {
			this.dialog.html("<p>No candidates were found for ingest to this container</p>");
			return;
		}
		
		// Sort the candidates by source and path
		var candidates = candidates.sort(function (a, b) {
			if ((a.sourceId + a.patternMatched) < (b.sourceId + b.patternMatched)) {
				return -1;
			}
			return 1;
		});
		
		// Generate index of strings to search per candidate and reformat candidate fields for display
		var candidateIndex = [];
		for (var i = 0; i < candidates.length; i++) {
			var candidate = candidates[i];
			candidateIndex.push(candidate.base + candidate.patternMatched);
			if (candidate.packagingType == "http://purl.org/net/sword/package/BagIt") {
				candidate.type = "BagIt";
			} else {
				candidate.type = "Directory";
			}
			
			if ("size" in candidate) {
				candidate.sizeFormatted = StringUtilities.readableFileSize(candidate.size);
			}
			
			var lastSlash = candidate.patternMatched.lastIndexOf("/");
			candidate.filename = lastSlash == -1? candidate.patternMatched : candidate.patternMatched.substring(lastSlash + 1);
		}
		
		// Build a lookup map of sources
		var sourceMap = {};
		for (var i = 0; i < sources.length; i++) {
			sourceMap[sources[i].id] = sources[i];
		}
		
		var candidatesForm = candidatesTemplate({sources : sourceMap, candidates : candidates});
		
		this.dialog.html(candidatesForm);
		var form = this.dialog.find("form");
		
		// If a list of selected items was provided from the user going back on the metadata page, then select them.
		if (selected) {
			for (var i = 0; i < candidates.length; i++) {
				var candidate = candidates[i];
				
				for (var j = 0; j < selected.length; j++) {
					if (selected[j].sourceId == candidate.sourceId && selected[j].patternMatched == candidate.patternMatched) {
						var entry = $(".file_browse_entry", form).eq(i);
						entry.addClass("selected").find("input").prop("checked", true);
						break;
					}
				}
			}
		}
		
		this.updateCandidateSubmitButton();
		
		// Bind selection events for result entries
		form.on("click", ".file_browse_entry", function(e){
			var $this = $(this);
			e.stopPropagation();
			if ($this.hasClass("selected")) {
				$this.removeClass("selected");
				$("input", $this).prop("checked", false);
			} else {
				$this.addClass("selected");
				$("input", $this).prop("checked", true);
			}
			
			self.updateCandidateSubmitButton();
		});
		
		// Bind select all within a source button functionality
		form.on("click", ".select_all_col input", function(e) {
			var $this = $(this);
			var sourceId = $this.data("sourceId");
			var select = $this.data("action") == "select";
			$this.data("action", select? "deselect" : "select");
			$this.attr("value", select? "Deselect All" : "Select All");
			$this.parents(".file_browse_heading").first().nextUntil(".file_browse_heading", ".file_browse_entry").each(function() {
				if (select) {
					$(this).addClass("selected");
				} else {
					$(this).removeClass("selected");
				}
				$("input", $(this)).prop("checked", select);
			});
			self.updateCandidateSubmitButton();
		});
		
		// Filter box functionality
		form.find("#candidate_filter").keyup(function() {
			var $input = $(this);
			var value = $input.val();
			
			var terms = value == null? [] : value.trim().split(" ");
			
			// Hide and show results depending on if they match the query
			self.dialog.find(".file_browse_entry").each(function(index) {
				// Check if there are any terms that don't match the index
				var matches = true;
				for (var i = 0; i < terms.length; i++) {
					if (candidateIndex[index].indexOf(terms[i]) == -1) {
						matches = false;
						break;
					}
				}
				
				// All terms match, or there were no terms, so show this entry
				if (matches) {
					$(this).show();
				} else {
					$(this).hide();
				}
				
				self.updateCandidateSubmitButton();
			});
		});
		
		form.submit(function(e) {
			e.preventDefault();
			
			// Determine which files were selected and match the current filter, to avoid hidden files getting chosen
			var selectedIndexes = [];
			self.dialog.find(".file_browse_entry.selected:visible").each(function() {
				selectedIndexes.push($(this).data("index"));
			});
			
			self.renderCandidateConfirmation(sources, candidates, selectedIndexes);
		});
	};
	
	IngestFromSourceForm.prototype.updateCandidateSubmitButton = function() {
		// Disable the submit button if no items selected
		this.dialog.find(".update_button").prop('disabled', this.dialog.find(".file_browse_entry.selected:visible").length == 0);
	};
	
	IngestFromSourceForm.prototype.renderCandidateConfirmation = function(sources, candidates, selectedIndexes) {
		var self = this;
		
		var selectedCandidates = [];
		for (var i = 0; i < selectedIndexes.length; i++) {
			selectedCandidates.push(candidates[selectedIndexes[i]]);
		}
		
		var candidatesForm = metadataTemplate({selectedCandidates : selectedCandidates});
		
		this.dialog.html(candidatesForm);
		
		this.dialog.find("#ingest_source_choose").click(function(e) {
			e.preventDefault();
			self.loadCandidateList(selectedCandidates);
		});
		
		this.$form = this.dialog.find("form");
		this.$form.submit(function(e) {
			e.preventDefault();
			
			var fileInfo = [];
			var missingFields = false;
			// Generate a map of properties to pass to controller to get this deposit started
			self.dialog.find(".file_browse_entry").each(function(index) {
				var $this = $(this);
				var candidate = selectedCandidates[index];
				
				var label = $this.find("input[name='file_label']").val();
				if (!label.trim()) {
					missingFields = true;
					return false;
				}
				
				var packagingType = (candidate.packagingType !== undefined) ? candidate.packagingType : 'http://cdr.unc.edu/DirectoryIngest';
				
				var info = {
					sourceId : candidate.sourceId,
					packagePath : candidate.patternMatched,
					packagingType : packagingType,
					label : $this.find("input[name='file_label']").val(),
					accessionNumber : $this.find("input[name='file_acc_number']").val(),
					mediaId : $this.find("input[name='file_media_id']").val()
				};
				fileInfo.push(info);
			});
			
			if (missingFields) {
				self.$form.find(".error_stack").text("A label must be provided for each deposit");
				self.$form.find(".errors").removeClass("hidden");
				return false;
			}
			
			var loadingOverlay = new ModalLoadingOverlay(self.dialog, {
				autoOpen : false,
				type : 'icon',
				dialog : self.dialog
			});
			loadingOverlay.open();
			
			// Make request to server
			$.ajax({
				url : "ingestFromSource/" + self.pid,
				type : "POST",
				contentType: "application/json",
				cache: false,
				data : JSON.stringify(fileInfo)
			}).success(function() {
				self.options.alertHandler.alertHandler("message", "The selected packages have been submitted for deposit");
				loadingOverlay.remove();
				self.dialog.remove();
			}).error(function(jqXHR, textMessage) {
				loadingOverlay.remove();
				self.options.alertHandler.alertHandler("message", "An error occurred while attempting to submit your deposit");
				self.setError(textMessage);
			});
		});
	};
	
	return IngestFromSourceForm;
});