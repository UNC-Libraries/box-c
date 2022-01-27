<template>
    <div id="set-embargo">
        <h3>Set Embargo</h3>
        <div class="embargo-details">
            <p v-if="has_embargo">Embargo expires {{ formattedEmbargoDate }} for this object</p>
            <p v-if="!has_embargo && !isBulkMode">No embargo set for this object</p>


            <button id="show-form" v-if="!has_embargo && !show_form && !isBulkMode" @click="showForm">Add embargo</button>
            <div class="form" v-if="has_embargo || show_form || isBulkMode">
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
                <button @click="removeEmbargo" :class="{'hidden': !has_embargo}" id="remove-embargo" v-if="!isBulkMode">Remove Embargo</button>
            </div>
        </div>
    </div>
</template>

<script>
    import { addYears, format, isFuture } from 'date-fns'

    export default {
        name: 'embargo',

        props: {
            currentEmbargo: String,
            isDeleted: Boolean,
            isBulkMode: Boolean
        },

        emits: ['embargo-info', 'error-msg'],

        data() {
            return {
                custom_embargo_date: '',
                embargo_ends_date: '',
                has_embargo: false,
                show_form: false,
                embargo_type: "ignore"
            }
        },

        watch: {
            currentEmbargo: {
                handler(embargo) {
                    this.has_embargo = embargo !== null;

                    if (this.has_embargo) {
                        this.embargo_ends_date = embargo;
                    } else {
                        this.embargo_ends_date = '';
                    }
                },
                deep: true
            },
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
            formattedEmbargoDate() {
                if (this.embargo_ends_date === '') {
                    return this.embargo_ends_date;
                }
                let embargo_date = this.specifiedDate(this.embargo_ends_date);
                return format(embargo_date, 'MMMM do, yyyy');
            }
        },

        methods: {
            showForm() {
                this.show_form = !this.show_form;
            },

            /**
             * Removes an embargo if one is present
             */
            removeEmbargo(confirm = true) {
                if (!confirm || window.confirm("This will clear the embargo for this object. Are you sure you'd like to continue?")) {
                    this.clearEmbargoInfo();
                    this.$emit('embargo-info', {
                        embargo: null,
                        skip_embargo: false
                    });
                }
            },

            /**
             * Embargo state should not be changed from whatever its current state is
             */
            ignoreEmbargo() {
                this.clearEmbargoInfo();
                this.$emit('embargo-info', {
                    embargo: null,
                    skip_embargo: true
                });
            },

            /**
             * Resets embargo form
             */
            clearEmbargoInfo() {
                this.has_embargo = false;
                this.show_form = false;
                this.custom_embargo_date = '';
                this.embargo_ends_date = '';

                this.clearEmbargoError();
            },

            clearEmbargoError() {
                this.error_msg = '';
                this.$emit('error-msg', this.error_msg);
            },

            /**
             * Set an embargo for the specified number of years
             * @param years
             */
            setFixedEmbargoDate(years) {
                let future_date = addYears(new Date(), years);
                this.embargo_ends_date = format(future_date, 'yyyy-LL-dd');
                this.custom_embargo_date = '';
                this.clearEmbargoError();
                this.$emit('embargo-info', {
                    embargo: this.embargo_ends_date,
                    skip_embargo: false
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
                    this.$emit('error-msg', '');
                    this.embargo_ends_date = this.custom_embargo_date;
                    this.$emit('embargo-info', {
                        embargo: this.embargo_ends_date,
                        skip_embargo: false
                    });
                } else if (date_filled && !regex_match) {
                    this.$emit('error-msg', 'Please enter a valid date in the following format YYYY-MM-DD');
                } else if (date_filled && !isFuture(date_parts)) {
                    this.$emit('error-msg', 'Please enter a future date');
                }
            },

            selectCustom() {
                this.embargo_type = 'custom';
                this.clearEmbargoError();
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
            width: 115px;
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
                margin-bottom: 0px;
            }
        }
    }
</style>