class deposit(
  $user,
  $enable_debug,
  $fedora_username,
  $fedora_password,
  $jms_host,
  $jms_port,
) {
  
  package { "redis":
    ensure => installed,
  }
  
  service { "redis":
    ensure => true,
    enable => true,
    hasrestart => true,
    hasstatus => true,
    require => Package["redis"],
  }
  
  package { "cdr-commons-daemon":
    ensure => "1.0.15-1",
    require => [
      User["tomcat"],
      Package["java-1.7.0-openjdk-devel"],
      Yumrepo["cdr-server"]
    ],
  }
  
  file { "/opt/deposit":
    source => "puppet:///modules/deposit/home",
    recurse => remote,
    purge => false,
    owner => $user,
    group => $user,
  }
  
  file { "/opt/deposit/deposit.jar":
    ensure => "link",
    target => "/opt/deploy/deposit/deposit.jar",
  }
  
  file { "/opt/deposit/deposit.properties":
    content => template("deposit/home/deposit.properties.erb"),
    owner => $user,
    group => $user,
    require => File["/opt/deposit"],
  }
  
  file { "/etc/init.d/deposit":
    content => template("deposit/deposit.sh.erb"),
    mode => "a+x",
  }
  
  exec { "/sbin/chkconfig --add deposit":
    unless => "/sbin/chkconfig deposit",
    require => File["/etc/init.d/deposit"],
  }
  
}
