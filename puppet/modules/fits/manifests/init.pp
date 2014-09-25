class fits {
  
  package { ["perl"]:
    ensure => installed,
  }

  package { "cdr-fits":
    ensure => "0.6.2-3",
    require => [
      User["irods"],
      Yumrepo["cdr-server"],
      Package["java-1.7.0-openjdk"],
    ],
  }

}
