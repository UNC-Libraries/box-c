<template>
<div id="file-history">
    <div class="card">
        <header class="card-header">
            <p class="card-header-title">View File History</p>
            <button class="card-header-icon" aria-label="display file events" @click="toggleFileHistory">
              <span class="icon">
                <i class="fas" :class="toggleArrow" aria-hidden="true"></i>
              </span>
            </button>
        </header>
        <div class="card-content" :class="{'is-hidden': !showFilHistory}">
            <div class="content">
                <table class="table is-bordered is-striped is-fullwidth">
                    <thead>
                    <tr>
                        <th scope="col">File Activity</th>
                        <th scope="col">Updated By</th>
                        <th scope="col">Date</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="event in fileEvents" :key="event.id">
                        <td class="note-width">{{ event.note }}</td>
                        <td>{{ event.username }}</td>
                        <td>{{ this.formatDate(event.timestamp) }}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
</template>

<script>
import fetchUtils from '@/mixins/fetchUtils';

export default {
    name: 'fileHistory',

    mixins: [fetchUtils],

    props: {
        uuid: String,
    },

    data() {
        return {
            showFilHistory: false,
            fileEvents: []
        }
    },

    computed: {
        toggleArrow() {
            return this.showFilHistory ? 'fa-angle-down' : 'fa-angle-right'
        }
    },
    
    methods: {
        async getFileEvents() {
            try {
                this.fileEvents = await this.fetchWrapper(`/services/api/premisEvents/${this.uuid}`);
            } catch (error) {
                console.log(error);
            }
        },

        toggleFileHistory() {
            this.showFilHistory = !this.showFilHistory;
        },

        formatDate(date_string) {
            const date = new Date(date_string);
            return date.toLocaleDateString('en-US', {
                timeZone: 'America/New_York',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: true
            });
        }
    },

    beforeMount() {
        this.getFileEvents();
    }
}
</script>

<style scoped>
    .note-width {
        width: 65%;
        word-break: break-word;
    }
</style>