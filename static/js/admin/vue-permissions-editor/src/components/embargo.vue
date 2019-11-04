<template>
    <div id="set-embargo">
        <div class="embargo-details">
            <p v-if="has_embargo">Embargo expires {{ formattedEmbargoDate }} for this object</p>
            <p v-else>No embargo set for this object</p>

            <h3>Set Embargo</h3>
            <form @click="clearEmbargoError">
                <fieldset :disabled="isDeleted">
                    <input @click="setFixedEmbargoDate(1)" v-model="fixed_embargo_date" value="1" type="radio"> 1 year<br/>
                    <input @click="setFixedEmbargoDate(2)" v-model="fixed_embargo_date" value="2" type="radio"> 2 years<br/>
                    <input :min="minDate" id="custom-embargo" placeholder="YYYY-MM-DD"
                           @change="setCustomEmbargoDate" type="date" v-model="custom_embargo_date"> Custom Date
                </fieldset>
            </form>

            <button @click="changeEmbargo" :disabled="isDeleted" :class="{'is-disabled': isDeleted}" id="add-embargo">{{ embargoText }}</button>
        </div>
    </div>
</template>

<script>
    import { addYears, format, isFuture, startOfTomorrow } from 'date-fns'

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
                has_embargo: false
            }
        },

        watch: {
            currentEmbargo(embargo) {
                this.has_embargo = embargo !== null;

                if (this.has_embargo) {
                    this.embargo_ends_date = embargo;
                    this.custom_embargo_date = embargo;
                } else {
                    this.embargo_ends_date = '';
                    this.custom_embargo_date = '';
                }
            }
        },

        computed: {
            embargoText() {
                return this.has_embargo ? 'Remove Embargo' : 'Add Embargo';
            },

            minDate() {
                let tomorrow = new Date(startOfTomorrow());
                return format(tomorrow, 'yyyy-LL-dd');
            },

            formattedEmbargoDate() {
                if (this.embargo_ends_date === '') {
                    return this.embargo_ends_date;
                }
                let embargo_date = this.specifiedDate(this.embargo_ends_date);
                return format(embargo_date, 'MMMM do, yyyy');
            }
        },

        methods: {
            /**
             * Adds an embargo if none is already present
             * Removes an embargo if one is present
             */
            changeEmbargo() {
                this.has_embargo = !this.has_embargo;

                if (!this.has_embargo) {
                    if (window.confirm("This will clear the embargo for this object. Are you sure you'd like to continue?")) {
                        this.clearEmbargoInfo();
                        this.$emit('embargo-info', null);
                    } else {
                        this.has_embargo = true;
                    }
                } else if (this.custom_embargo_date !== '' && !isFuture(this.specifiedDate(this.custom_embargo_date))) {
                    this.$emit('error-msg', 'Please enter a future date');
                } else if (this.fixed_embargo_date === '' && this.custom_embargo_date === '') {
                    this.has_embargo = false;
                    this.$emit('error-msg', 'No embargo is set. Please choose an option from the form above.');
                } else {
                    this.$emit('embargo-info', this.embargo_ends_date);
                }
            },

            /**
             * Resets embargo form
             */
            clearEmbargoInfo() {
                this.custom_embargo_date = '';
                this.embargo_ends_date = '';
                this.fixed_embargo_date = '';
            },

            clearEmbargoError() {
                this.error_msg = '';
                this.$emit('error-msg', this.error_msg);
            },

            /**
             * If an embargo is already present and the user changes the embargo length
             */
            updateCurrentEmbargo() {
                if (this.has_embargo) {
                    this.$emit('embargo-info', this.embargo_ends_date);
                }
            },

            /**
             * Set an embargo for the specified number of years
             * @param years
             */
            setFixedEmbargoDate(years) {
                let future_date = addYears(new Date(), years);
                this.embargo_ends_date = format(future_date, 'yyyy-LL-dd');
                this.custom_embargo_date = '';
                this.updateCurrentEmbargo();
            },

            /**
             * Set a custom embargo date
             */
            setCustomEmbargoDate() {
                let date_parts = this.specifiedDate(this.custom_embargo_date);

                if (date_parts !== null && !isFuture(date_parts)) {
                    this.$emit('error-msg', 'Please enter a future date');
                } else {
                    this.$emit('error-msg', '');
                    this.fixed_embargo_date = '';
                    this.embargo_ends_date = this.custom_embargo_date;
                    this.updateCurrentEmbargo();
                }
            },

            /**
             * Turn a date string into a Date object
             * @param date_value
             * @returns {null|Date}
             */
            specifiedDate(date_value) {
                let parts = date_value.split('-');

                if (parts.length > 0) {
                    return new Date(parts[0], parts[1] - 1, parts[2]);
                }

                return null;
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

        #add-embargo {
            margin-left: 0;
            margin-top: 20px;
            width: 115px;
        }

        p {
            margin-bottom: 20px;
        }

        input {
            margin-bottom: 10px;
        }

        input[type=date] {
            border: 1px solid lightgray;
            border-radius: 5px;
            padding: 5px;
        }
    }
</style>