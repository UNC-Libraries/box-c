<template>
    <div id="set-embargo">
        <div class="embargo-details">
            <p v-if="has_embargo">Embargo expires {{ formattedEmbargoDate }}</p>
            <p v-else>No embargo set for this object</p>

            <h3>Set Embargo</h3>
                <form>
                    <input @click="setFixedEmbargoDate(1)" v-model="fixed_embargo_date" value="1" type="radio"> 1 year<br/>
                    <input @click="setFixedEmbargoDate(2)" v-model="fixed_embargo_date" value="2" type="radio"> 2 years<br/>
                    <input @focusin="error_msg=''" :min="minDate" id="custom-embargo" placeholder="YYYY-MM-DD"
                           @change="setCustomEmbargoDate" type="date" v-model="custom_embargo_date"> Custom Date
                </form>
                <p class="error">{{ error_msg }}</p>

            <button @click="changeEmbargo" id="add-embargo">{{ embargoText }}</button>
        </div>
    </div>
</template>

<script>
    import { addYears, format, isFuture, startOfTomorrow } from 'date-fns'

    export default {
        name: 'embargo',

        props: {
            currentEmbargo: Number
        },

        data() {
            return {
                custom_embargo_date: '',
                embargo_ends_date: '',
                fixed_embargo_date: '',
                error_msg: '',
                has_embargo: false,
            }
        },

        watch: {
            currentEmbargo(embargo) {
                this.has_embargo = embargo > 0;

                if (this.has_embargo) {
                    this.embargo_ends_date = format(new Date(embargo), 'yyyy-LL-dd');
                }
            }
        },

        computed: {
            embargoText() {
                return this.has_embargo ? 'Remove Embargo' : 'Add Embargo';
            },

            minDate() {
                let tomorrow = new Date(startOfTomorrow());
                return  format(tomorrow, 'yyyy-LL-dd');
            },

            formattedEmbargoDate() {
                let embargo_date = this.specifiedDate(this.embargo_ends_date);
                return format(embargo_date, 'MMMM do, yyyy');
            }
        },

        methods: {
            changeEmbargo() {
                this.has_embargo = !this.has_embargo;

                if (!this.has_embargo) {
                    if(window.confirm("This will clear the embargo for this object. Are you sure you'd like to continue?")) {
                        this.clearEmbargoInfo();
                        this.$emit('embargo-info', null);
                    } else {
                        this.has_embargo = true;
                    }
                } else if (this.custom_embargo_date !== '' && !isFuture(this.specifiedDate(this.custom_embargo_date))) {
                    this.error_msg = 'Please enter a future date';
                } else if (this.fixed_embargo_date === '' && this.custom_embargo_date === '') {
                    this.has_embargo = false;
                    this.error_msg = 'No embargo is set. Please choose an option from the form above.';
                } else {
                    this.$emit('embargo-info', this.embargo_ends_date);
                }
            },

            clearEmbargoInfo() {
                this.custom_embargo_date = '';
                this.embargo_ends_date = '';
                this.fixed_embargo_date = '';
            },

            updateCurrentEmbargo() {
                if (this.has_embargo && this.custom_embargo_date !== '' && this.fixed_embargo_date !== '') {
                    this.$emit('embargo-info', this.embargo_ends_date);
                }
            },

            setFixedEmbargoDate(years) {
                this.error_msg = '';
                this.custom_embargo_date = '';
                let future_date = addYears(new Date(), years);
                this.embargo_ends_date = format(future_date, 'yyyy-LL-dd');
                this.updateCurrentEmbargo();
            },

            setCustomEmbargoDate() {
                let date_parts = this.specifiedDate(this.custom_embargo_date);

                if (date_parts !== null && !isFuture(date_parts)) {
                    this.error_msg = 'Please enter a future date';
                } else {
                    this.error_msg = '';
                    this.fixed_embargo_date = '';
                    this.embargo_ends_date = this.custom_embargo_date;
                    this.updateCurrentEmbargo();
                }
            },

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

        p.error {
            color: red;
            height: 20px;
            margin-bottom: 0;
        }
    }
</style>