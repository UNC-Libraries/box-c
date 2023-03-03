<template>
    <header>
        <div class="logo-row">
            <div class="logo logo-large container">
                <a href="/">
                    <img :src="'static/front/university-libraries-logo.png'" alt="University Libraries Logo">
                    <h1>Digital Collections Repository</h1>
                </a>
            </div>
        </div>
        <nav class="menu-row navbar" role="navigation">
            <div class="container">
                <div class="navbar-brand">
                    <a role="button" id="navbar-burger" class="navbar-burger burger" aria-label="menu" aria-expanded="false" data-target="navbar">
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                    </a>
                </div>
                <div id="navbar" class="menu navbar-menu">
                    <div class="navbar-start">
                        <a href="collections" class="navbar-item">Browse Collections</a>
                        <a href="aboutRepository" class="navbar-item">What's Here?</a>
                        <a href="https://library.unc.edu/wilson/contact/" class="navbar-item">Contact Us</a>
                        <a v-if="adminAccess" :href="adminUrl" class="navbar-item" target="_blank">Admin</a>
                    </div>
                    <div class="navbar-end">
                        <a v-if="isLoggedIn" :href="logoutUrl" class="navbar-item"><i class="fas fa-user"></i>&nbsp;&nbsp;Log out</a>
                        <a v-else :href="loginUrl" class="navbar-item"><i class="fas fa-user"></i>&nbsp;&nbsp;Login</a>
                    </div>
                </div>
            </div>
        </nav>

        <div class="banner-row">
            <div class="banner container">
                <h2>Explore materials from Wilson Special Collections Library</h2>
                <a href="collections" class="button is-link is-large">Begin your exploration</a>
            </div>
        </div>

        <div class="search-row search-row-large">
            <div class="search search-large container">
                <form method="get" action="basicSearch" class="search">
                    <input name="queryType" type="hidden" value="anywhere">
                    <label for="hsearch_text">Search the Digital Collections Repository</label>
                    <input name="query" type="text" id="hsearch_text" placeholder="Search all collections">
                    <button type="submit" class="button">Search</button>
                </form>
                <a href="advancedSearch">Advanced Search</a>
            </div>
        </div>
    </header>
</template>

<script>
import headerUtils from "../../mixins/headerUtils";
export default {
    name: "headerHome",

    mixins: [headerUtils]
}
</script>

<style scoped lang="scss">
</style>