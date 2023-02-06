<template>
    <div>
        <h2>{{ $t('adv_search.advanced') }}</h2>
        <form id="advanced-search-form">
            <div class="lightest columns">
                <div class="column lightest shadowtop">
                    <h3>{{ $t('adv_search.search_for') }}</h3>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="anywhere">{{ $t('adv_search.anywhere') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="anywhere" name="anywhere" class="input is-small" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="title">{{ $t('adv_search.title') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="title" name="titleIndex" class="input is-small" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal break">
                            <label class="label" for="contributor">{{ $t('adv_search.creator_contributor') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="contributor" name="contributorIndex" class="input is-small" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="subject">{{ $t('adv_search.subject') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="subject" name="subjectIndex" class="input is-small" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label" for="subject">{{ $t('adv_search.collection_id') }}</label>
                        </div>
                        <div class="field-body">
                            <div class="field">
                                <p class="control">
                                    <input id="collection_id" name="collectionId" class="input is-small" type="text" />
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="column light shadowtop">
                    <div class="field">
                        <div class="field-label is-normal">
                            <label class="label">{{ $t('adv_search.limit_by') }}</label>
                        </div>
                        <div class="field">
                            <div class="control">
                                <div class="select is-fullwidth is-small">
                                    <select name="collection" aria-label="Collection">
                                        <option value="">Collection</option>
                                        <template v-for="collection in collections">
                                            <option :value="collection.id">{{ collection.title }}</option>
                                        </template>
                                    </select>
                                </div>
                            </div>
                        </div>
                        <div class="field">
                            <div class="control">
                                <div class="select is-fullwidth is-small">
                                    <select name="format" aria-label="Format">
                                        <option value="">Format</option>
                                        <template v-for="format in formats">
                                            <option :value="format">{{ format }}</option>
                                        </template>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="field is-horizontal dates">
                        <div class="field-label is-normal">
                            <label class="label">{{ $t('adv_search.date_deposited') }}</label>
                        </div>
                        <span>{{ $t('adv_search.date_from') }}</span>
                        <div class="field">
                            <div class="control">
                                <input aria-label="deposited-start-date" name="addedStart" placeholder="YYYY" min="0" type="number" class="input is-small" />
                            </div>
                        </div>
                        <span>{{ $t('adv_search.date_to') }}</span>
                        <div class="field">
                            <div class="control">
                                <input aria-label="deposited-end-date" name="addedEnd" placeholder="YYYY" min="1" type="number" class="input is-small" />
                            </div>
                        </div>
                        <span class="has-tooltip-arrow has-tooltip-multiline has-tooltip-right-mobile" :data-tooltip="$t('adv_search.dates_end_note')">?</span>
                    </div>
                    <div class="field is-horizontal">
                        <div class="field-label is-normal">
                            <label class="label">{{ $t('adv_search.date_created') }}</label>
                        </div>
                        <span>{{ $t('adv_search.date_from') }}</span>
                        <div class="field">
                            <div class="control">
                                <input aria-label="created-start-date" placeholder="YYYY" min="0" name="createdYearStart" type="number" class="input is-small" />
                            </div>
                        </div>
                        <span>{{ $t('adv_search.date_to') }}</span>
                        <div class="field">
                            <div class="control">
                                <input aria-label="created-end-date" placeholder="YYYY" min="1" name="createdYearEnd" type="number" class="input is-small" />
                            </div>
                        </div>
                        <span class="has-tooltip-arrow has-tooltip-multiline has-tooltip-right-mobile" :data-tooltip="$t('adv_search.dates_end_note')">?</span>
                    </div>
                </div>
            </div>
            <div class="columns">
                <div class="column align-right">
                    <input type="submit"  id="advsearch_submit" :value="$t('adv_search.search')" />
                </div>
            </div>
        </form>
    </div>
</template>

<script>
import get from 'axios';

export default {
    name: "advancedSearch",

    data() {
        return {
            collections: [],
            formats: [],
        }
    },

    methods: {
        getCollections() {
            get('/advancedSearch/collections').then((response) => {
                this.collections = response.data;
            }).catch(function (error) {
                console.log(error);
            });
        },

        getFormats() {
            get('/advancedSearch/formats').then((response) => {
                this.formats = response.data;
            }).catch(function (error) {
                console.log(error);
            });
        },
    },

    mounted() {
        this.getCollections();
        this.getFormats();
    }
}
</script>

<style scoped lang="scss">
    .dates {
        margin-top: 50px;
    }
    form, h2 {
        margin: 25px auto;
        float: none;
        width: 80%;
    }

    .break {
        word-break: break-word;
    }

    .lightest {
        float: none;
    }

    label {
        text-align: left;
    }

    .field-label {
        margin-right: 10px;
        min-width: 125px;
    }

    span {
        padding: 5px;
    }

    .align-right {
        text-align: right;
    }

    #advsearch_submit {
        color: #fff;
        padding: 5px 10px;
        width: 100px;
        font-size: 15px;
        text-align: center;
        margin-bottom: 20px;
        cursor: pointer;
        background-color: #c34d13;
        border: 1px solid #862e03;

        &:hover {
            background-color: #862e03;
        }
    }

    @media screen and (max-width: 768px) {
        .break {
            word-break: normal;
        }

        .field-label {
            margin-right: 0;
            min-width: auto;
        }
    }
</style>