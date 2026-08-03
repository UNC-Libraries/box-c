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
                        <th scope="col">Date</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="event in fileEvents" :key="event.id">
                        <td>{{ event.description }}</td>
                        <td>{{ event.date }}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

</div>
</template>

<script>
export default {
    name: 'fileHistory',
    
    data() {
        return {
            showFilHistory: false,
            fileEvents: [
                { description: "New version added as file2.txt", date: Date.now() },
                { description: "Added to rare books collection", date: Date.now() - 50000 },
                { description: "Uploaded as file.txt", date: Date.now() - 100000 },
            ]
        }
    },

    computed: {
        toggleArrow() {
            return this.showFilHistory ? 'fa-angle-down' : 'fa-angle-right'
        }
    },
    
    methods: {
        getFileEvents() {

        },

        toggleFileHistory() {
            this.showFilHistory = !this.showFilHistory;
        }
    }
}
</script>

<style scoped>

</style>