<template>
  <div id="app">
    <router-view/>
  </div>
</template>
<style lang="scss">
$cdr-blue: #1A698C;
.button.is-link {
    background-color: $cdr-blue;
    border-radius: 5px;
    font-size: .85rem;
    margin-bottom: 10px;
    margin-top: 10px;
    padding: 1.1em 0.8em 1.1em 1.2em;
}

a.button.is-link {
    width: initial;

    span {
        padding-right: 10px;
    }
}
</style>