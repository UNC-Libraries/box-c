<template>
    <header>
        <div class="logo-row">
            <div class="logo logo-large container">
                <router-link to="/">
                    <img :src="'/static/front/university-libraries-logo.png'" alt="University Libraries Logo">
                    <h1>Digital Collections Repository</h1>
                </router-link>
            </div>
        </div>
        <nav class="menu-row navbar" role="navigation">
            <div class="container">
                <div class="navbar-brand">
                    <a @click="toggleMobileMenu()" role="button" id="navbar-burger" class="navbar-burger burger" aria-label="menu" :class="{open: mobileMenuOpen}" :aria-expanded="mobileMenuOpen" data-target="navbar">
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                    </a>
                </div>
                <div id="navbar" class="menu navbar-menu" :class="{'is-active': mobileMenuOpen}">
                    <div class="navbar-start">
                        <router-link to="/collections" class="navbar-item">Browse Collections</router-link>
                        <router-link to="/aboutRepository" class="navbar-item">What's Here?</router-link>
                        <a href="https://library.unc.edu/contact-us/" class="navbar-item">Contact Us</a>
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
                <router-link to="/collections" class="button is-link is-large">Begin your exploration</router-link>
            </div>
        </div>

        <div class="search-row search-row-large">
            <div class="columns container">
                <form method="get" action="api/basicSearch" class="column">
                    <input name="queryType" type="hidden" value="anywhere">
                    <label for="hsearch_text" class="is-sr-only">Search the Digital Collections Repository</label>
                    <div class="field has-addons">
                        <p class="control is-expanded">
                            <input name="query" type="text" placeholder="Search all collections" id="hsearch_text" class="input is-medium">
                        </p>
                        <p class="control">
                            <button type="submit" class="button is-medium">Search</button>
                        </p>
                    </div>
                </form>
                <div class="column is-narrow">
                    <router-link to="/advancedSearch" class="button is-text has-text-white is-medium">Advanced Search</router-link>
                </div>
            </div>
        </div>
    </header>
</template>

<script>
import headerUtils from "../../mixins/headerUtils";
import loginUrlUtils from "../../mixins/loginUrlUtils";

export default {
    name: "headerHome",

    mixins: [headerUtils, loginUrlUtils]
}
</script>