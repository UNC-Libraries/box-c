<template>
    <div>
        <p>I live here. Why am I blank?</p>
        <modal-metadata metadata="record_list"></modal-metadata>
        <ul v-if="record_list.length > 0">
            <li v-for="record in record_list"
                    :key="record.uuid">
                {{ record.label }}
            </li>
        </ul>
    </div>
</template>

<script>
    define(['Vue'], function(Vue) {
        Vue.component('browseDisplay', {

            props: {
                fetchPath: String,
                fetchType: String // Possible values "admin_set", "collection", "folder"
            },

            template: template,

            components: {
              modalMetadata: 'modalMetadata'
            },

            data: function() {
                return {
                    record_list: []
                }
            },

            mounted: function() {
                var self = this;

                fetch(this.fetchPath)
                    .then(function(response) {
                        self.record_list = response.json();
                    })
            }
        });
    });
</script>