require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access'
	}
});

define('sortSubmit', ['module', 'jquery'], function(module, $) {
	$("#sort_select").on("change", function() {
		$("#result_sort_form").submit();
	});
});