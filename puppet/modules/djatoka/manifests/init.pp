class djatoka {
  
  package { "cdr-djatoka":
    ensure => "1.1-3",
    require => [
      Class["tomcat"],
      Yumrepo["cdr-server"]
    ],
  }
  
}
