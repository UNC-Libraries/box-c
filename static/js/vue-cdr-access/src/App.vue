<template>
  <div>
    <browse-display :browse-path="record_id"
                    :records-per-page="20"></browse-display>
  </div>
</template>

<script>
import browseDisplay from './components/browseDisplay.vue'

export default {
  name: 'app',

  components: {
    browseDisplay
  },

  data() {
    return {
      record_id: `/listJson/${location.pathname.split('/')[2]}`
    }
  }
}
</script>
