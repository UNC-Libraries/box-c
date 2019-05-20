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

<style scoped lang="scss">
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
