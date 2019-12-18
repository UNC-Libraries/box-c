<template>
    <div id="set-embargo">
        <div class="embargo-details">
            <p v-if="has_embargo">Embargo expires {{ formattedEmbargoDate }} for this object</p>
            <p v-else>No embargo set for this object</p>


            <button id="show-form" v-if="!has_embargo && !show_form" @click="showForm">Add embargo</button>
            <div class="form" v-if="has_embargo || show_form">
                <h3>Set Embargo</h3>
                <form>
                    <fieldset :disabled="isDeleted">
                        <div @click="setFixedEmbargoDate(1)"><input v-model="fixed_embargo_date" value="1" type="radio"> 1 year</div>
                        <div @click="setFixedEmbargoDate(2)"><input v-model="fixed_embargo_date" value="2" type="radio"> 2 years</div>
                        <input id="custom-embargo" placeholder="YYYY-MM-DD"
                               @click="clearEmbargoError" @focusout="setCustomEmbargoDate" type="text" v-model="custom_embargo_date"> Custom Date
                    </fieldset>
                </form>
                <button @click="removeEmbargo" :class="{'hidden': !has_embargo}" id="remove-embargo">Remove Embargo</button>
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
            isDeleted: Boolean
        },

        data() {
            return {
                custom_embargo_date: '',
                embargo_ends_date: '',
                fixed_embargo_date: '',
                has_embargo: false,
                show_form: false
            }
        },

        watch: {
            currentEmbargo(embargo) {
                this.has_embargo = embargo !== null;

                if (this.has_embargo) {
                    this.embargo_ends_date = embargo;
                } else {
                    this.embargo_ends_date = '';
                }
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
             * Adds an embargo if none is already present
             * Removes an embargo if one is present
             */
            removeEmbargo() {
                if (window.confirm("This will clear the embargo for this object. Are you sure you'd like to continue?")) {
                    this.clearEmbargoInfo();
                    this.$emit('embargo-info', null);
                }
            },

            /**
             * Resets embargo form
             */
            clearEmbargoInfo() {
                this.has_embargo = false;
                this.show_form = false;
                this.custom_embargo_date = '';
                this.embargo_ends_date = '';
                this.fixed_embargo_date = '';

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
                this.fixed_embargo_date = years;
                this.custom_embargo_date = '';
                this.clearEmbargoError();
                this.$emit('embargo-info', this.embargo_ends_date);
            },

            /**
             * Set a custom embargo date
             */
            setCustomEmbargoDate() {
                let date_parts = this.specifiedDate(this.custom_embargo_date);
                let date_filled = this.custom_embargo_date !== '';
                let regex_match = /^[1-2]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2]\d|3[0-1])$/.test(this.custom_embargo_date);

                if (date_filled && regex_match && isFuture(date_parts)) {
                    this.$emit('error-msg', '');
                    this.fixed_embargo_date = '';
                    this.embargo_ends_date = this.custom_embargo_date;
                    this.$emit('embargo-info', this.embargo_ends_date);
                } else if (date_filled && !regex_match) {
                    this.$emit('error-msg', 'Please enter a valid date in the following format YYYY-MM-DD');
                } else if (date_filled && !isFuture(date_parts)) {
                    this.$emit('error-msg', 'Please enter a future date');
                }
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
        margin: 25px -25px 0 -25px;

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
                width: 80px;
            }
        }

        p {
            margin-bottom: 20px;
        }

        input {
            margin-bottom: 10px;
        }

        input[type=text] {
            border: 1px solid lightgray;
            border-radius: 5px;
            padding: 5px;
        }
    }
</style>