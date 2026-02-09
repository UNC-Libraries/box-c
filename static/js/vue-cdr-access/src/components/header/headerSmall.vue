<template>
    <header>
        <div class="logo-row-small">
            <div class="logo-small container">
                <router-link to="/">
                    <h1>University Library <span>Digital Collections Repository</span></h1>
                </router-link>
                <span class="info-btns">
                    <a href="https://library.unc.edu/contact-us/">Contact Us</a>
                    <a v-if="isLoggedIn" :href="logoutUrl"><i class="fas fa-user"></i>&nbsp;&nbsp;Log out</a>
                    <a v-else :href="loginUrl"><i class="fas fa-user"></i>&nbsp;&nbsp;Login</a>
                </span>
            </div>
        </div>
        <nav class="menu-row-small navbar" role="navigation">
            <div class="container">
                <div class="navbar-brand">
                    <a @click="toggleMobileMenu()" role="button" id="navbar-burger" class="navbar-burger burger" aria-label="menu" :class="{open: mobileMenuOpen}"  :aria-expanded="mobileMenuOpen" data-target="navbar">
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                        <span aria-hidden="true"></span>
                    </a>
                </div>
                <div id="navbar" class="menu navbar-menu" :class="{'is-active': mobileMenuOpen}" >
                    <router-link to="/collections" class="navbar-item">Browse Collections</router-link>
                    <router-link to="/aboutRepository" class="navbar-item">What's Here?</router-link>
                    <a v-if="adminAccess" :href="jumpToAdminUrl" class="navbar-item" target="_blank">Admin</a>
                    <a class="navbar-item is-hidden-desktop" href="https://library.unc.edu/contact-us/">Contact Us</a>
                    <router-link to="/advancedSearch" class="navbar-item is-hidden-desktop">Advanced Search</router-link>
                    <a v-if="isLoggedIn" class="navbar-item is-hidden-desktop" :href="logoutUrl">Log out</a>
                    <a v-else :href="loginUrl" class="navbar-item is-hidden-desktop">Login</a>
                </div>

                <div class="search-row columns container">
                    <form method="get" action="/api/basicSearch" class="column">
                        <input name="queryType" type="hidden" value="anywhere">
                        <label for="hsearch_text" class="is-sr-only">Search the Digital Collections Repository</label>
                        <div class="field has-addons">
                            <p class="control is-expanded">
                                <input name="query" type="text" placeholder="Search all collections" id="hsearch_text" class="input">
                            </p>
                            <p class="control">
                                <button type="submit" class="button">Search</button>
                            </p>
                        </div>
                    </form>
                    <div class="column is-narrow is-hidden-touch">
                        <router-link to="/advancedSearch" class="button is-text has-text-white">Advanced Search</router-link>
                    </div>
                </div>
            </div>
        </nav>
    </header>
</template>

<script>
import headerUtils from "../../mixins/headerUtils";
import loginUrlUtils from "../../mixins/loginUrlUtils";

export default {
    name: "headerSmall",

    mixins: [headerUtils, loginUrlUtils],
}
</script>