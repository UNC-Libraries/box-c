class fits {
  
  package { ["java-1.6.0-openjdk", "perl"]:
    ensure => installed,
  }

  package { "cdr-fits":
    ensure => "0.6.2-3",
    require => [
      User["irods"],
      Yumrepo["cdr-packages"],
    ],
  }

}
