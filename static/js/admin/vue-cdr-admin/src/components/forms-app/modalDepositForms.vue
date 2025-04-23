<template>
    <div id="dcr-forms-app">
        <link href="https://cdn.jsdelivr.net/npm/bulma@1.0.2/css/versions/bulma-prefixed.min.css" rel="stylesheet">
        <div class="bulma-columns bulma-is-centered">
            <div class="bulma-column bulma-is-12">
                <div class="bulma-modal" :class="{'bulma-is-active': showFormsModal}">
                    <div @click="closeModal()" class="bulma-modal-background"></div>
                    <div class="bulma-modal-content">
                        <div v-if="form === ''">
                            <h1 class="has-text-centered">Add a work to the current collection</h1>
                            <div class="bulma-column has-text-centered">
                                <div class="bulma-select">
                                    <select v-model="form" @change="getSchema()">
                                        <option value="">-- Please select a form --</option>
                                        <option value="generic_work">Generic Work</option>
                                        <option value="continuing_resource_item">Continuing Resource</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                        <Vueform v-else
                                 :schema="schema"
                                 :endpoint="async (FormData, form$) => submitForm(FormData, form$)"
                                 @response="handleResponse"/>
                    </div>
                    <button @click="closeModal()" class="bulma-modal-close bulma-is-large" aria-label="close"></button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import {mapActions, mapState} from 'pinia';
import {useFormsStore} from '@/stores/forms';

export default {
    name: 'modalDepositForms',

    data() {
        return {
            form: '',
            schema: {}
        }
    },

    computed: {
        ...mapState(useFormsStore, ['alertHandler', 'containerId', 'showFormsModal'])
    },

    methods: {
        ...mapActions(useFormsStore, ['setAlertHandler', 'setShowFormsModal']),

        async getSchema() {
            if (this.form !== '') {
                await fetch(`/static/deposit-forms/${this.form}.json`)
                    .then(response => response.json())
                    .then(data => {
                        this.schema = data;
                    });
            }
        },

        closeModal() {
            this.setShowFormsModal(false);
            this.form = '';
        },

        /**
         * See https://vueform.com/docs/handling-form-data#submit-via-function
         * @param FormData
         * @param form$
         * @returns {Promise<axios.AxiosResponse<any>>}
         */
        async submitForm(FormData, form$) {
            const request_data = this.formatSubmission(form$.requestData);
            // Setting cancel token
            form$.cancelToken = form$.$vueform.services.axios.CancelToken.source();

            return await form$.$vueform.services.axios.post('/forms/api/deposits',
                form$.requestData /* | data | requestData */, { cancelToken: form$.cancelToken.token }
            );
        },

        // axios response
        handleResponse(response, form$) {
            console.log(response)
        },

        formatSubmission(data) {
            let submission_package = {
                depositorEmail: data.depositorEmail,
                destination: this.containerId,
                form: "generic",
                sendEmailReceipt: /lib.unc.edu/.test(window.location)
            }
            delete data.depositorEmail;
            data.supplemental.forEach((f) => {
               if (f['supplemental-file'] == null) {
                delete f['supplemental-file'];
               }
            });
            submission_package.values = data;
            submission_package.values.file = data.file.data.id;

            return submission_package;
        }
    }
}
</script>

<style lang="scss">
$forms-warning-color: #ef4444;
$forms-warning-size: 1.5em;

#dcr-forms-app {
    h1 {
        font-size: 2rem;
        font-weight: bold;
    }
    .vf-static-tag-h2 h2 {
        color: #005B90;
    }
    h2 {
        color: #005B90;
    }

    select {
        margin: auto;
    }
    .required-note {
        color: $forms-warning-color;
        font-size: $forms-warning-size;
    }
    .required {
        color: inherit;

        label {
            &:after {
                color: $forms-warning-color;
                content: " *";
                font-size: $forms-warning-size;
            }
        }
    }
    label span {
        font-weight: bold;
    }

    .bulma-modal {
        z-index: 99;
    }

    .bulma-modal-content {
        background: white;
        padding: 25px;
        min-height: 300px;
        width: 90%;
    }
}
</style>