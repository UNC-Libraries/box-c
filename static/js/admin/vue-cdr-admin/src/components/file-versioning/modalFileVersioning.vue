<template>
    <div class="vue-dcr-admin-wrapper vue-dcr-modal">
        <div id="file-versioning" class="modal" :class="{'is-active': showModal}">
            <div class="modal-background"></div>
            <div class="modal-card">
                <header class="modal-card-head modal-head">
                    <p class="modal-card-title">File versioning for {{ files[0].name }}</p>
                    <button class="delete" aria-label="close" @click="closeModal"><span class="is-sr-only">Close</span>
                    </button>
                </header>
                <section class="modal-card-body">
                    <h3 class="title is-4">Upload a new version</h3>
                    <div>
                        <form method="post" enctype="multipart/form-data">
                            <div class="file is-medium has-name">
                                <label class="file-label">
                                    <input ref="fileInput" class="file-input" type="file" name="version" @change="uploadedFileDisplay($event)" />
                                    <span class="file-cta">
                                        <span class="file-icon">
                                            <i class="fas fa-upload"></i>
                                        </span>
                                        <span class="file-label">Select file… </span>
                                    </span>
                                    <span class="file-name">{{ selected_file_name }}</span>
                                </label>
                            </div>
                            <button type="button" class="button is-primary" :disabled="!selected_file" @click="uploadNewVersion()">Upload</button>
                        </form>
                        <hr>
                        <h3 class="title is-4">Versions <span class="versions">(<span class="current">*</span> Current version)</span></h3>
                        <table class="table is-bordered is-fullwidth is-hoverable">
                            <thead>
                            <tr>
                                <th>File name</th>
                                <th>MIME type</th>
                                <th>Date uploaded</th>
                                <th><span class="is-sr-only">Download</span></th>
                                <th><span class="is-sr-only">Restore</span></th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="file in sortedByCurrentVersion" :class="{'is-light': file.current_version}">
                                <td><span v-if="file.current_version" class="current">*</span> {{ file.name }}</td>
                                <td>{{ file.mimetype }}</td>
                                <td>{{ displayDate(file.uploaded) }}</td>
                                <td><a class="button is-primary" href="#">Download</a></td>
                                <td><button @click="setAsCurrentVersion(file.name)" class="button is-danger" :disabled="file.current_version">Restore</button></td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
                <footer class="modal-card-foot">
                    <div class="buttons">
                        <button @click="closeModal" class="button is-danger">Close</button>
                    </div>
                </footer>
            </div>
        </div>
    </div>
</template>

<script>
import {mapActions, mapState} from "pinia";
import {useFileVersioningStore} from "@/stores/file-versioning";
import fetchUtils from "@/mixins/fetchUtils";

export default {
    name: 'modalFileVersioning',

    mixins: [fetchUtils],

    data() {
        return {
            current_version: '',
            selected_file: null,
            selected_file_name: 'No file selected.',
            files: [
                { name: 'image.jpg', mimetype: 'image/jpeg', uploaded: 1785161550842, current_version: false },
                { name: 'image2.jpg', mimetype: 'image/jpeg', uploaded: 1785161591280, current_version: false },
                { name: 'image3.jpg', mimetype: 'image/jpeg', uploaded: 1785161611920, current_version: true },
            ],
        }
    },

    computed: {
        ...mapState(useFileVersioningStore, {
            actionHandler: state => state.actionHandler,
            alertHandler: state => state.alertHandler,
            checkForUnsavedChanges: state => state.checkForUnsavedChanges,
            resultObject: state => state.resultObject,
            showModal: state => state.showFileVersioningModal
        }),

        sortedByCurrentVersion() {
            return this.files.slice().sort((a, b) => {
                // Sort by current_version descending (true first)
                if (a.current_version !== b.current_version) {
                    return b.current_version - a.current_version;
                }
                // Then sort by uploaded date descending (most recent first)
                return new Date(b.uploaded) - new Date(a.uploaded);
            });
        }
    },

    methods: {
        ...mapActions(useFileVersioningStore, ['setCheckForUnsavedChanges', 'setShowFileVersioningModal']),


        /**
         * Demo version that doesn't actually upload anything
         */
        uploadNewVersion() {
            if (!this.selected_file) return;
            try {
                this.files.push({
                    name: this.selected_file_name,
                    mimetype: this.selected_file.type,
                    uploaded: Date.now(),
                    current_version: true
                });
                this.setAsCurrentVersion(this.selected_file_name);
                this.alertHandler.alertHandler('success', 'File uploaded successfully.');
            } catch (error) {
                this.alertHandler.alertHandler('error', 'Unable to upload the selected file');
                console.log(error);
            } finally {
                this.clearFileUpload();
            }
        },

        /* async uploadNewVersion() {
             if (!this.selected_file) return;
             try {
                 const formData = new FormData();
                 formData.append('file', this.selected_file);
                 await this.fetchWrapper('/services/api/edit/version', false, {
                     method: 'POST',
                     body: formData
                 });
                 this.alertHandler.alertHandler('success', 'File uploaded successfully.');
             } catch (error) {
                 this.alertHandler.alertHandler('error', 'Unable to upload the selected file');
                 console.log(error);
             } finally {
                 this.clearFileUpload();
             }
         },*/

        async setAsCurrentVersion(filename) {
            try {
                /* const data = await this.fetchWrapper('', true, {method: 'POST', headers: {'Content-Type': 'application/json'}});
                if (!isEmpty(data)) {
                    this.files = data;
                } */
                const timestamp = this.files.find((file) => file.name === filename)?.uploaded;
                this.files.forEach((file) => file.current_version = (file.uploaded === timestamp));
                this.alertHandler.alertHandler('success', 'File version updated successfully.');
            } catch (error) {
                const response_msg = `Unable to update file version: ${this.title}`;
                this.alertHandler.alertHandler('error', response_msg);
                console.log(error);
            }
        },

        displayDate(timestamp) {
            const date = new Date(timestamp);
            const options = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false };
            return new Intl.DateTimeFormat('en-US', options).format(date)
        },

        uploadedFileDisplay(event) {
            const file = event?.target?.files?.[0];
            this.selected_file = file ?? null;
            this.selected_file_name = file ? file.name : 'No file selected.';
        },

        clearFileUpload() {
            this.selected_file = null;
            this.selected_file_name = 'No file selected.';
            this.$refs.fileInput.value = '';
        },

        closeModalCheck() {
            this.setCheckForUnsavedChanges(true);
        },

        resetChangesCheck(check_changes) {
            this.setCheckForUnsavedChanges(check_changes);
        },

        closeModal() {
            this.clearFileUpload();
            this.setShowFileVersioningModal(false);
        }
    }
}
</script>

<style>
#file-versioning.modal .table {
    --bulma-table-cell-border-color: dark-gray;
    tr.is-light {
        --bulma-table-cell-border-color: dark-gray;
    }
}

#file-versioning.modal {
    .versions {
        font-size: .9rem;
    }
    .current {
        color: firebrick;
    }
}
</style>
