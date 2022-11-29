define('ParentResultObject', [ 'jquery', 'ResultObject'],
		function($, ResultObject) {
	
	function ParentResultObject(options) {
		ResultObject.call(this, options);
	};
	
	ParentResultObject.prototype.constructor = ParentResultObject;
	ParentResultObject.prototype = Object.create( ResultObject.prototype );
	
	ParentResultObject.prototype.init = function(metadata) {
		this.metadata = metadata;
		this.pid = metadata.id;
		this.isDeleted = $.inArray("Marked For Deletion", this.metadata.status) != -1;
		this.actionMenuInitialized = false;
		this.element = this.options.element;
		this.element.data('resultObject', this);
		this.links = [];
		$(".container_title span", this.element).text(this.metadata.title)
	};
	
	return ParentResultObject;
});