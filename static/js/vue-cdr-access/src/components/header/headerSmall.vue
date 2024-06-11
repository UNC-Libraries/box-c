<template>
    <header>
        <div class="logo-row-small">
            <div class="logo-small container">
                <router-link to="/">
                    <img :src="'/static/front/university-libraries-logo.png'" alt="University Libraries Logo">
                    <h1>Digital Collections Repository</h1>
                </router-link>
                <span class="info-btns">
                    <a href="https://library.unc.edu/wilson/contact/">Contact Us</a>
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
                    </a>
                </div>
                <div id="navbar" class="menu navbar-menu" :class="{'is-active': mobileMenuOpen}" >
                    <router-link to="/collections" class="navbar-item">Browse Collections</router-link>
                    <router-link to="/aboutRepository" class="navbar-item">What's Here?</router-link>
                    <a v-if="adminAccess" :href="jumpToAdminUrl" class="navbar-item" target="_blank">Admin</a>
                    <a class="navbar-item navbar-display" href="https://library.unc.edu/wilson/contact/">Contact Us</a>
                </div>
                <div class="search-row">
                    <div class="search">
                        <form method="get" action="basicSearch" class="search">
                            <input name="queryType" type="hidden" value="anywhere">
                            <label for="hsearch_text">Search the Digital Collections Repository</label>
                            <input name="query" type="text" id="hsearch_text" placeholder="Search all collections">
                            <button type="submit" class="button">Search</button>
                        </form>
                        <router-link class="navbar-item" to="/advancedSearch">Advanced Search</router-link>
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