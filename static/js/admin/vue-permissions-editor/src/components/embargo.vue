<template>
    <div id="set-embargo">
        <h3>Set Embargo</h3>
        <div class="embargo-details">
            <p v-if="hasEmbargo">Embargo expires {{ formattedEmbargoDate }} for this object</p>
            <p v-if="!hasEmbargo && !isBulkMode">No embargo set for this object</p>


            <button id="show-form" v-if="!hasEmbargo && !show_form && !isBulkMode" @click="showForm">Add embargo</button>
            <div class="form" v-if="hasEmbargo || show_form || isBulkMode">
                <form>
                    <fieldset :disabled="isDeleted">
                        <ul class="select-type-list">
                            <li v-if="isBulkMode">
                                <input v-model="embargo_type" value="ignore" type="radio" id="embargo-ignore">
                                <label for="embargo-ignore">No Change</label>
                            </li>
                            <li v-if="isBulkMode">
                                <input v-model="embargo_type" value="clear" type="radio" id="embargo-clear">
                                <label for="embargo-clear">Clear embargoes</label>
                            </li>
                            <li>
                                <input v-model="embargo_type" value="1year" type="radio" id="embargo-1year">
                                <label for="embargo-1year">1 year</label>
                            </li>
                            <li>
                                <input v-model="embargo_type" value="2years" type="radio" id="embargo-2years">
                                <label for="embargo-2years">2 years</label>
                            </li>
                            <li>
                                <input v-model="embargo_type" value="custom" type="radio" id="embargo-custom">
                                <label for="custom-embargo">Custom Date <input id="custom-embargo" placeholder="YYYY-MM-DD"
                                       @click="selectCustom" @focusout="setCustomEmbargoDate"
                                       type="text" v-model="custom_embargo_date">
                                </label>
                            </li>
                        </ul>
                    </fieldset>
                </form>
                <button @click="removeEmbargo" :class="{'hidden': !hasEmbargo}" id="remove-embargo" v-if="!isBulkMode">Remove Embargo</button>
            </div>
        </div>
    </div>
</template>

<script>
    import { addYears, format, isFuture } from 'date-fns'
    import { mapActions, mapState } from 'pinia';
    import { usePermissionsStore } from '../stores/permissions';

    export default {
        name: 'embargo',

        props: {
            isDeleted: Boolean,
            isBulkMode: Boolean
        },

        data() {
            return {
                custom_embargo_date: '',
                error_msg: '',
                show_form: false,
                embargo_type: "ignore"
            }
        },

        watch: {
            embargo_type: {
                handler(newType) {
                    if (newType === "ignore") {
                        this.ignoreEmbargo();
                    } else if (newType === "clear") {
                        this.removeEmbargo(false);
                    } else if (newType === "1year") {
                        this.setFixedEmbargoDate(1);
                    } else if (newType === "2years") {
                        this.setFixedEmbargoDate(2);
                    }
                },
                deep: true
            }
        },

        computed: {
            ...mapState(usePermissionsStore, {
                alertHandler: store => store.alertHandler,
                embargoEndsDate: store => {
                    return (store.embargoInfo.embargo !== null) ? store.embargoInfo.embargo : '';
                },
                hasEmbargo: store => store.embargoInfo.embargo !== null
            }),
            
            

            formattedEmbargoDate() {
                if (this.embargoEndsDate === '') {
                    return this.embargoEndsDate;
                }
                let embargo_date = this.specifiedDate(this.embargoEndsDate);
                return format(embargo_date, 'MMMM do, yyyy');
            }
        },

        methods: {
            ...mapActions(usePermissionsStore, ['setEmbargoInfo']),

            showForm() {
                this.show_form = !this.show_form;
            },

            /**
             * Removes an embargo if one is present
             */
            removeEmbargo(confirm = true) {
                if (!confirm || window.confirm("This will clear the embargo for this object. Are you sure you'd like to continue?")) {
                    this.clearEmbargoInfo();
                    this.setEmbargoInfo({
                        embargo: null,
                        skipEmbargo: false
                    });
                }
            },

            /**
             * Embargo state should not be changed from whatever its current state is
             */
            ignoreEmbargo() {
                this.clearEmbargoInfo();
                this.setEmbargoInfo({
                    embargo: null,
                    skipEmbargo: true
                });
            },

            /**
             * Resets embargo form
             */
            clearEmbargoInfo() {
                this.show_form = false;
                this.custom_embargo_date = '';
                this.error_msg = '';
            },

            /**
             * Set an embargo for the specified number of years
             * @param years
             */
            setFixedEmbargoDate(years) {
                let future_date = addYears(new Date(), years);
                let fixed_embargo_date = format(future_date, 'yyyy-LL-dd');
                this.custom_embargo_date = '';
                this.setEmbargoInfo({
                    embargo: fixed_embargo_date,
                    skipEmbargo: false
                });
            },

            /**
             * Set a custom embargo date
             */
            setCustomEmbargoDate() {
                let date_parts = this.specifiedDate(this.custom_embargo_date);
                let date_filled = this.custom_embargo_date !== '';
                let regex_match = /^[1-2]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2]\d|3[0-1])$/.test(this.custom_embargo_date);

                if (date_filled && regex_match && isFuture(date_parts)) {
                    this.embargo_type = 'custom';
                    this.error_msg = '';
                    this.setEmbargoInfo({
                        embargo: this.custom_embargo_date,
                        skipEmbargo: false
                    });
                } else if (date_filled && !regex_match) {
                    this.error_msg = 'Please enter a valid date in the following format YYYY-MM-DD';
                    this.alertHandler.alertHandler('error', this.error_msg);
                } else if (date_filled && !isFuture(date_parts)) {
                    this.error_msg = 'Please enter a future date';
                    this.alertHandler.alertHandler('error', this.error_msg);
                }
            },

            selectCustom() {
                this.embargo_type = 'custom';
                this.error_msg = '';
            },

            /**
             * Turn a date string into a Date object
             * @param date_value
             * @returns {Date}
             */
            specifiedDate(date_value) {
                let parts = date_value.split('-');
                return new Date(parts[0], parts[1] - 1, parts[2]);
            }
        }
    }
</script>

<style scoped lang="scss">
    #set-embargo {
        border-top: 1px solid gray;
        margin: 25px -25px 55px -25px;

        h3 {
            color: black;
            margin: 15px auto auto 25px;
        }

        .embargo-details {
            margin: 15px 25px;
        }

        button {
            margin-left: 0;
        }

        #remove-embargo {
            margin-top: 20px;
        }

        fieldset {
            div {
                cursor: default;
                width: 100px;
            }
        }

        p {
            margin-bottom: 20px;
        }

        input {
            margin-bottom: 10px;
            margin-right: 10px;
        }

        input[type=text] {
            border: 1px solid lightgray;
            border-radius: 5px;
            padding: 5px;
        }

        .select-type-list {
            list-style-type: none;
            text-align: left;
            border-top: none;
            margin-top: 0;
            padding-top: 0;

            li {
                display: block;
                margin-left: 15px;
                margin-bottom: 0;
            }
        }
    }
</style>