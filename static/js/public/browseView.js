require.config({
    urlArgs: 'v=4.0-SNAPSHOT',
    baseUrl: '/static/js/',
    paths: {
        'underscore': 'lib/underscore',
        'Vue': 'lib/vue.min',
        'vue': 'lib/require-vuejs.min'
    },
    shim: {
        'underscore': {'exports': '_'},
        'Vue': {'exports': 'Vue'}
    }
});

define('browseView', ['Vue', 'underscore', 'vue!public/vueComponents/browseDisplay'], function(Vue) {
    new Vue({
        el: '#browse_view'
    });
});