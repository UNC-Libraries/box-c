class tika(
  $user,
  $port,
) {
  
  package { "nc":
  }
  
  package { "cdr-tika":
    ensure => "1.4-2",
    require => [
      Package["nc"],
      User["irods"],
      Yumrepo["cdr-server"]
    ],
  }
  
  file { "/etc/init.d/tika":
    content => template("tika/tika.sh.erb"),
    mode => "a+x",
    require => [
      Package["cdr-tika"],
      User["irods"],
    ],
    notify => Service["tika"],
  }
  
  service { "tika":
    ensure => "running",
    enable => true,
  }
  
}
