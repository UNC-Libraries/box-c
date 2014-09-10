class mulgara(
  $user,
  $host,
  $port,
  $rmi_port,
) {
  
  package { "cdr-mulgara":
    ensure => "2.1.13-1",
    require => [
      User["tomcat"],
      Class["repository"],
      Yumrepo["cdr-packages"]
    ],
  }
  
  file { "/opt/repository/mulgara":
    source => "puppet:///modules/mulgara/home",
    recurse => remote,
    purge => false,
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-mulgara"],
  }
  
  file { "/opt/repository/mulgara/mulgara-config.xml":
    content => template("mulgara/home/mulgara-config.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository/mulgara"],
  }
  
  file { "/etc/init.d/mulgara":
    content => template("mulgara/mulgara.sh.erb"),
    mode => "a+x",
  }
  
  exec { "/sbin/chkconfig --add mulgara":
    unless => "/sbin/chkconfig mulgara",
    require => File["/etc/init.d/mulgara"],
  }
  
}
