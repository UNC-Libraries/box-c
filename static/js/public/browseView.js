require.config({
    urlArgs: 'v=4.0-SNAPSHOT',
    baseUrl: '/static/js/',
    paths: {
        'Vue': 'lib/vue.min',
        'vue': 'lib/require-vuejs.min'
    },
    shim: {
        'Vue': {'exports': 'Vue'}
    }
});

define('browseView', ['Vue', 'vue!public/vueComponents/browseDisplay'], function(Vue){
    new Vue({
        el: '#browse_view'
    });
});