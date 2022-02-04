export default {
    emits: ['show-modal', 'reset-changes-check'],

    data() {
        return {
            is_submitting: false,
            response_message: '',
            unsaved_changes: false
        }
    },

    watch: {
        changesCheck: {
            handler(check) {
                if (check) {
                    this.showModal();
                }
            },
            deep: true
        }
    },

    methods: {
        displayModal() {
            if (!this.is_submitting && this.unsaved_changes) {
                let message = 'There are unsaved permission updates. Are you sure you would like to exit?';
                if (window.confirm(message)) {
                    this.$emit('show-modal', false);
                }
            } else {
                this.$emit('show-modal', false);
            }

            // Reset changes check in parent component
            this.$emit('reset-changes-check', false);
        }
    }
}