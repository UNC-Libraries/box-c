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
			title: "Ingest from file server"
		});
		
		this.pid = pid;
		
		this.loadCandidateList(pid);
	};
	
	IngestFromSourceForm.prototype.loadCandidateList = function() {
		var self = this;
		
		var loadingOverlay = new ModalLoadingOverlay(this.dialog, {
			autoOpen : false,
			type : 'icon',
			dialog : self.dialog
		});
		loadingOverlay.open();
		
		$.ajax({
			url : "listSources/" + this.pid,
			type : "GET"
		}).done(function(data){
			loadingOverlay.remove();
			
			self.renderCandidateList(data.sources, data.candidates);
		});
	};
	
	IngestFromSourceForm.prototype.renderCandidateList = function(sources, candidates) {
		var self = this;
		
		// Generate index of strings to search per candidate and reformat candidate fields for display
		var candidateIndex = [];
		for (var i = 0; i < candidates.length; i++) {
			var candidate = candidates[i];
			candidateIndex.push(candidate.base + candidate.patternMatched);
			if (candidate.packagingType == "http://purl.org/net/sword/package/BagIt") {
				candidate.type = "BagIt";
			}
			candidate.sizeFormatted = StringUtilities.readableFileSize(candidate.size);
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
		
		this.updateCandidateSubmitButton();
		
		// Bind selection events for result entries
		this.dialog.on("click", "#ingest_source_candidates .file_browse_entry", function(e){
			var $this = $(this);
			if (e.metaKey || e.ctrlKey) {
				$this.toggleClass("selected");
			} else {
				$this.addClass("selected").siblings().removeClass("selected");
			}
			
			self.updateCandidateSubmitButton();
		});
		
		// Filter box functionality
		this.dialog.find("#candidate_filter").keyup(function() {
			var $input = $(this);
			var value = $input.val();
			
			// Compute a regular expression for the user's search terms
			var matchRe = null;
			if (value && value.trim()) {
				var pattern = "";
				var terms = value.trim().split(" ");
				for (var i = 0; i < terms.length; i++) {
					var escaped = terms[i].replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
					pattern += ".*" + escaped + ".*";
				}
				matchRe = new RegExp(pattern);
			}
			
			// Hide and show results depending on if they match the query
			self.dialog.find(".file_browse_entry").each(function(index) {
				// No query terms, show everything
				if (!matchRe) {
					$(this).show();
				} else {
					if (candidateIndex[index].match(matchRe)) {
						$(this).show();
					} else {
						$(this).hide();
					}
				}
				
				self.updateCandidateSubmitButton();
			});
		});
		
		this.dialog.find("form").submit(function(e) {
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
			self.loadCandidateList();
		});
		
		this.$form = this.dialog.find("form");
		this.$form.submit(function(e) {
			e.preventDefault();
			
			var fileInfo = [];
			// Generate a map of properties to pass to controller to get this deposit started
			self.dialog.find(".file_browse_entry").each(function(index) {
				var $this = $(this);
				var candidate = selectedCandidates[index];
				var info = {
					sourceId : candidate.sourceId,
					packagePath : candidate.patternMatched,
					packagingType : candidate.packagingType,
					label : $this.find("input[name='file_label']").val()
				};
				fileInfo.push(info);
			});
			
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