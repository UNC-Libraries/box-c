<template>
    <teleport to="#vue-cdr-admin-add-work">
        <div id="dcr-forms-app" class="columns is-centered">
            <div class="column is-12">
                <button class="button" @click="is_open = true">Open!</button>
                <div class="modal" :class="{'is-active': is_open}">
                    <div @click="closeModal()" class="modal-background"></div>
                    <div class="modal-content">
                        <div v-if="form === ''">
                            <h1 class="has-text-centered">Add a work to the current collection</h1>
                            <div class="column has-text-centered">
                                <div class="select">
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
                    <button @click="closeModal()" class="modal-close is-large" aria-label="close"></button>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script>
export default {
    name: 'formContainer',

    data() {
        return {
            is_open: false,
            form: '',
            schema: {}
        }
    },

    methods: {
        async getSchema() {
            if (this.form !== '') {
                await fetch(`./forms/${this.form}.json`)
                    .then(response => response.json())
                    .then(data => {
                        this.schema = data;
                    });
            }
        },
        closeModal() {
            this.is_open = false;
            this.form = '';
        },
        /**
         * See https://vueform.com/docs/handling-form-data#submit-via-function
         * @param FormData
         * @param form$
         * @returns {Promise<axios.AxiosResponse<any>>}
         */
        async submitForm(FormData, form$) {
            // Using FormData will EXCLUDE conditional elements and it
            // will submit the form as "Content-Type: multipart/form-data".
            const formData = FormData;

            // Using form$.data will INCLUDE conditional elements and it
            // will submit the form as "Content-Type: application/json".
            const data = form$.data;

            // Using form$.requestData will EXCLUDE conditional elements and it
            // will submit the form as "Content-Type: application/json".
            const requestData = form$.requestData;

            // Setting cancel token
            form$.cancelToken = form$.$vueform.services.axios.CancelToken.source();

            return await form$.$vueform.services.axios.post('/my/endpoint',
                formData /* | data | requestData */,
                {
                    cancelToken: form$.cancelToken.token,
                }
            );
        },
        handleResponse(response, form$) {
            console.log(response) // axios response
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
    .modal-content {
        background: white;
        padding: 25px;
        min-height: 300px;
        width: 90%;
    }
    select {
        margin: auto;
    }
    .required-note {
        color: $forms-warning-color;
        font-size: $forms-warning-size;
    }
    .required {
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
}
</style>