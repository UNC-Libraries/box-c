<template>
    <div class="list-records-display">
        <div class="columns">
            <div class="column is-12" >
                <ul v-for="record in recordList">
                    <li class="">
                        <div class="column is-2">
                            <a :href="recordUrl(record.id)">

                                <div class="record-title">{{ record.title }}</div>
                            </a>
                        </div>
                        <div class="column is-10">
                            <a :href="recordUrl(record.id)">
                                <div class="record-title">{{ record.title }}</div>
                            </a>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
    import displayUtils from '../mixins/displayUtils';

    export default {
        name: 'listDisplay',

        mixins: [displayUtils],

        props: {
            recordList: {
                default: [],
                type: Array
            }
        },

    }
</script>

<style scoped lang="scss">
</style>