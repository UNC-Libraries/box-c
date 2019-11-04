<template>
    <div class="list-records-display">
        <div class="columns">
            <div class="column is-12" >
                <ul>
                    <li v-for="(record, index) in recordList" class="columns" :class="{stripe: index % 2 === 0}">
                        <div class="column is-2">
                            <img v-if="hasThumbnail(record.thumbnail_url)" :src="record.thumbnail_url" class="thumbnail thumbnail-size-small">
                            <i v-else class="fa" :class="recordType(record.type)"></i>
                        </div>
                        <div class="column is-10">
                            <div>
                                <a :href="recordUrl(record.id)">{{ record.title }}</a> <span>({{ countDisplay(record.counts.child) }})</span>
                            </div>
                            <div v-if="record.updated !==''">Date Deposited: {{ formatDate(record.updated) }}</div>
                            <div>Collection: </div>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script>
    import displayUtils from '../mixins/displayUtils';
    import { format } from 'date-fns';

    export default {
        name: 'listDisplay',

        mixins: [displayUtils],

        props: {
            recordList: {
                default: [],
                type: Array
            }
        },

        methods: {
            countDisplay(count) {
                let wording = 'items';

                if (count === 1) {
                    wording = 'item';
                }

                return `${count} ${wording}`;
            },

            formatDate(date_string) {
                return format(new Date(date_string), 'MMMM do, yyyy');
            }
        }

    }
</script>

<style scoped lang="scss">
    li {
        align-items: center;
        display: inline-flex;
        padding-bottom: 20px;
        padding-top: 20px;
        width: 100%;
    }

    .is-2 {
        text-align: center;
    }

    a {
        font-size: 1.5rem;
    }

    div {
        margin-bottom: 5px;
    }

    i {
        color: #007FAE;
        font-size: 5rem;
    }

    img.thumbnail {
        float: none;
        margin: auto;
    }

    span {
        color: #676777;
        margin-left: 10px;
    }

    .stripe {
        background-color: #f7f7f7;
    }
</style>