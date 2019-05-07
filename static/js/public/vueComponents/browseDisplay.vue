<template>
    <div class="browse-records-display">
        <div class="columns spacing">
            <div class="column is-10">
                There are <strong>{{ record_list.length }}</strong> {{ noResultsText }} in this {{ resultText }}
            </div>
            <div class="column is-2">
                <modal-metadata v-if="record_list.length > 0" :metadata="container_metadata"></modal-metadata>
            </div>
        </div>
        <div class="columns">
            <div class="column is-12">
                <ul v-if="record_list.length > 0">
                    <li class="column is-3" v-for="record in record_list"
                        :key="record.id">
                        <a :href="record.uri">
                            <div>
                                <i class="fa fa-archive"></i>
                                <div class="record-count">{{ recordCount(39841) }}</div>
                            </div>
                            <div>{{ record.title }}</div>
                            <div class="spacing">({{ recordCount(3242) }})</div>
                        </a>
                    </li>
                </ul>
                <p v-else>There are no {{ noResultsText }} to display</p>
            </div>
        </div>
    </div>
</template>

<script>
    define(['Vue', 'vue!public/vueComponents/modalMetadata'], function(Vue) {
        Vue.component('browseDisplay', {
            props: {
                path: String,
                type: String
            },

            template: template,

            data: function() {
                return {
                    container_metadata: {},
                    record_list: []
                }
            },

            computed: {
                noResultsText: function() {
                    if (this.type === 'AdminUnit') {
                        return 'collections';
                    } else if (this.type === 'Collection') {
                        return 'folders and files';
                    } else {
                        return 'files';
                    }
                },

                resultText: function() {
                    if (this.type === 'AdminUnit') {
                        return 'administrative unit';
                    } else {
                        return this.type.toLowerCase();
                    }
                }
            },

            methods: {
                recordCount: function(number) {
                    return new Intl.NumberFormat().format(number);
                }
            },

            mounted: function() {
                let self = this;
                fetch(this.path)
                    .then(function(response) {
                        return response.json();
                    }).then(function(data) {
                    self.container_metadata = data.container;
                    self.record_list = data.metadata;
                });
            }
        });
    });
</script>

<style>
    .browse-records-display .columns {
        display: inline-flex;
        width: 100%;
    }

    .browse-records-display ul {
        display: inline-flex;
        text-align: center;
        width: 100%;
    }

    .browse-records-display div {
        font-size: 1.4rem;
    }

    .browse-records-display i.fa-archive {
        font-size: 10rem;
    }

    .browse-records-display .spacing {
        margin-top: 12px;
    }

    .browse-records-display button {
        color: white;
        background-color: #007FAE;
    }

    .browse-records-display button:hover {
        color: white;
        opacity: .9;
    }

    .browse-records-display .record-count {
        margin-top: -45px;
        color: white;
        margin-bottom: 30px;
        margin-left: -15px;
        font-size: 40px;
    }
</style>