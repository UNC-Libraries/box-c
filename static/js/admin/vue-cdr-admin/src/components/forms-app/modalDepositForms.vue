<template>
    <div id="dcr-forms-app" class="vf-boxc">
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
         * for different types of ways to submit data as multipart/form-data or application/json
         * @param FormData
         * @param form$
         * @returns {Promise<axios.AxiosResponse<any>>}
         */
        async submitForm(FormData, form$) {
            // Setting cancel token
            form$.cancelToken = form$.$vueform.services.axios.CancelToken.source();
            // Create a new FormData object for our custom submission
            const submissionData = new window.FormData();

            // Convert form data to a JSON string
            const jsonString = JSON.stringify(form$.requestData);
            const jsonFile = new File([jsonString], 'form-data.json', { type: 'application/json' });
            submissionData.append('file', jsonFile);
            submissionData.append('type', 'https://library.unc.edu/dcr/packaging/WorkFormJson1.0');

            return await form$.$vueform.services.axios.post(`/services/api/edit/ingest/${this.containerId}`,
                submissionData, {
                    cancelToken: form$.cancelToken.token,
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
            );
        },

        // axios response
        handleResponse(response, form$) {
            if (response.status === 200) {
                this.form = '';
                this.alertHandler.alertHandler('success', 'Form submitted successfully.');
            } else {
                this.alertHandler.alertHandler('error', 'There was an error submitting the form.');
                console.log(response)
            }
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