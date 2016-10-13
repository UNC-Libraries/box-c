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

define('studentPapers', ['module', 'jquery'], function(module, $) {
  $('#mp-dept').change(function () {
    if ($(this).val() != '') {
      location.href = $(this).val();
    }
  })
});
