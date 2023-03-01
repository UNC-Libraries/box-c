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
                      <a v-if="getUsername" :href="logoutUrl" class="navbar-item"><i class="fas fa-user"></i>&nbsp;&nbsp;Log out</a>
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
                  <input name="queryType" type="hidden" value="${searchSettings.searchFieldParams['DEFAULT_INDEX']}">
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
import fullRecordUtils from "../../mixins/fullRecordUtils";
export default {
  name: "headerHome",
  mixins: [headerUtils, fullRecordUtils]
}
</script>

<style scoped lang="scss">
$unc-blue: #4B9CD3;
$unc-navy: #13294B;
$light-gray: #E1E1E1;
$dark-gray: #767676;
$black: #151515;

@mixin mobile-margin {
  @media only screen and (max-width: 1088px) {
    margin-left: 5px;
    margin-right: 16px;
  }
}

.logo-row {
  background-color: $unc-blue;
}

.logo-row-small {
  background-color: $unc-blue;
  padding: 5px 0;
}

.menu-row, .banner-row {
  background-color: $unc-navy;
}

.info-row {
  background-color: #F5F5F5;
}

.logo, .menu {
  padding: 12px 0; // 1rem = 16px
}

.logo, .logo-small {
  @include mobile-margin;
  a {
    display: inline-flex;
    align-items: center;
    max-width: 550px;
  }
  img {
    display: inline-block;
    max-width: 250px;
    width: 100%;
    margin: 8px 0;
  }
  h1 {
    display: inline-block;
    margin-left: 24px;
    font-family: 'Libre Baskerville', serif;
    font-size: 1.7rem;
    font-style: italic;
    color: white;
    line-height: 1.5;
  }

  .info-btns {
    float: right;
    padding-top: 13px;

    a {
      margin-left: 10px;
    }
  }
}

.logo-large {
  a {
    max-width: 650px;
  }
}

.logo-small {
  a, i {
    color: white;
    font-size: 19px;
    font-weight: 700;
  }

  img {
    max-width: 150px;
  }

  h1 {
    font-size: 1.5rem;
  }
}

.navbar {
  box-shadow: 2px 2px 2px 0 #071730;
  a {
    color: white;
  }

  .is-active {
    background-color: $unc-navy;
  }
}

.menu-row-small {
  background-color: $unc-navy;
}

.search {
  a {
    margin-left: 10px;
  }
}

.menu-row-small.navbar {
  box-shadow: none;

  button {
    border-radius: 0 5px 5px 0;
    height: 35px;
    margin-left: -1px;
    border: 1px solid #dbdbdb;
  }

  .is-active {
    .navbar-item {
      color: black;
      padding: .35rem;
    }
  }

  .navbar-display {
    display: none;
  }

  .navbar-menu.is-active {
    background-color: white;
    margin-top: 65px;
    position: absolute;
  }
}

.banner-row {
  background-image: url(/static/front/banner.jpg);
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  box-shadow: inset 0 0 10px black;

  .button.is-link {
    font-size: 1.5rem;
  }
}

.banner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around;
  min-height: 300px;
  h2 {
    max-width: 700px;
    padding: 24px 32px;
    font-size: 2rem;
    color: white;
    text-align: center;
    background-color: rgba(0,0,0,0.75);
    line-height: 1.5;
  }
  a.button {
    background-color: $unc-blue;
  }
}

header {
  .search {
    @include mobile-margin;
    display: flex;
    align-items: center;
    padding: 6px 0;

    label {
      position: absolute;
      width: 0;
      height: 0;
      margin: 0;
      padding: 0;
      border: 0;
      overflow: hidden;
    }
    input {
      border: 1px solid lightgray;
      border-bottom-left-radius: 5px;
      border-top-left-radius: 5px;
      font-size: 1rem;
      height: 35px;
      padding: 7px;
      width: 100%;
    }

    a {
      flex-shrink: 0;
      color: white;
      font-size: 16px;
      &:hover, &:active {
        color: $unc-navy;
      }
    }
  }

  .search-row-large {
    background-color: $unc-navy;
  }

  .search-large {
    input, button {
      height: 40px;
    }

    input {
      font-size: 1.2rem;
    }

    button {
      border-radius: 0 5px 5px 0;
      margin-left: -1px;
      border: 1px solid #dbdbdb;
    }

    a:hover, a:active {
      color: whitesmoke;
    }
  }
}

@media only screen and (min-width: 1088px) {
  .menu-row-small.navbar > .container {
    .navbar-menu {
      margin-right: 150px;
    }
  }

  .search-row {
    width: 100%;
  }

  header {
    .search {
      margin: auto;
    }
  }
}

@media only screen and (max-width: 1087px) {
  .menu-row-small.navbar > .container {
    display: inline-flex;
    flex-direction: row-reverse;
  }

  .menu-row-small.navbar {
    .navbar-menu.is-active {
      background-color: white;
      margin-top: 65px;
      position: absolute;
      z-index: 25;
    }
  }

  .search-row {
    width: 100%;
  }
}

@media only screen and (max-width: 767px) {
  .info-btns {
    display: none;
  }

  header {
    .search {
      display: block;

      a {
        margin-left: 5px;
      }

      form {
        display: inline-flex;
      }
    }
  }

  .menu-row-small {
    .search {
      button {
        margin-right: 0;
      }

      a {
        margin-bottom: 5px;
        margin-right: -5px;
      }

      a, button {
        float: right;
      }
    }
  }

  .menu-row-small.navbar {
    .navbar-menu.is-active {
      margin-top: 50px;
    }

    .navbar-display {
      display: block;
    }
  }

  p.footer-menu {
    line-height: 25px;
  }
}
</style>