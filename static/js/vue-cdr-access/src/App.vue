<template>
  <div>
    <browse-display :browse-path="urlParams"
                    :records-per-page="records_per_page"></browse-display>
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
      records_per_page: 20
    }
  },

  computed: {
    urlParams() {
      let additional_params = location.search;
      let params = `?page=1&start=0&rows=${this.records_per_page}`;

      if (additional_params !== '') {
        params = additional_params
      }

      return `/listJson/${location.pathname.split('/')[2]}${params}`
    }
  }
}
</script>
