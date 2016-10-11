require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-access',
		'jquery-ui' : 'cdr-access'
	},
	shim: {
		'jquery-ui' : ['jquery']
	}
});
define('studentPapers', ['jquery'], function(module, $) {
  $("#mp-dept").change(function () {
    location.href = $(this).val();
  })
});
