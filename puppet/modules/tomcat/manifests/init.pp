class tomcat(
  $user,
  $roles,
  $users,
  $enable_spoof,
  $jmx_remote_users,
  $manager_remote_addr_valve_allow,
) {

  $packages = [
    "apr-devel",
    "apr-util-devel",
  ]
  
  package { $packages:
    ensure => "installed",
  }
  
  user { $user:
    ensure => present,
    comment => "Tomcat",
    expiry => "absent",
    gid => "tomcat",
    home => "/opt/repository",
    shell => "/bin/bash",
  }

  group { "tomcat":
    ensure => present,
  }
  
  package { "cdr-tomcat":
    ensure => "7.0.22-2",
    require => [
      User["tomcat"],
      Package[$packages],
      Yumrepo["cdr-packages"]
    ],
  }
  
  file { [
    "/opt/repository/tomcat/conf",
    "/opt/repository/tomcat/conf/Catalina",
    "/opt/repository/tomcat/conf/Catalina/localhost",
    "/opt/repository/tomcat/shared",
    "/opt/repository/tomcat/webapps",
  ]:
    ensure => "directory",
    owner => "tomcat",
    group => "tomcat",
    require => [
      User["tomcat"],
      Package["cdr-tomcat"],
    ],
  }
  
  # Symlinks to deploy
  
  file { "/opt/repository/tomcat/shared/lib":
    ensure => "link",
    target => "/opt/deploy/lib",
    force => true,
  }
  
  # Contexts for webapps
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/ROOT.xml":
    source => "puppet:///modules/tomcat/home/conf/Catalina/localhost/ROOT.xml",
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/admin.xml":
    source => "puppet:///modules/tomcat/home/conf/Catalina/localhost/admin.xml",
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/services.xml":
    source => "puppet:///modules/tomcat/home/conf/Catalina/localhost/services.xml",
    owner => "tomcat",
    group => "tomcat",
  }
  
  # The following implicitly require /opt/repository/tomcat/conf
  
  file { "/opt/repository/tomcat/conf/catalina.properties":
    source => "puppet:///modules/tomcat/home/conf/catalina.properties",
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/context.xml":
    source => "puppet:///modules/tomcat/home/conf/context.xml",
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/logging.properties":
    source => "puppet:///modules/tomcat/home/conf/logging.properties",
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/manager.xml":
    content => template("tomcat/home/conf/Catalina/localhost/manager.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/jmxremote.access":
    content => template("tomcat/home/conf/jmxremote.access.erb"),
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/jmxremote.password":
    content => template("tomcat/home/conf/jmxremote.password.erb"),
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/server.xml":
    content => template("tomcat/home/conf/server.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/opt/repository/tomcat/conf/tomcat-users.xml":
    content => template("tomcat/home/conf/tomcat-users.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
  }
  
  file { "/etc/init.d/tomcat":
    content => template("tomcat/tomcat.sh.erb"),
    mode => "a+x",
  }
  
  exec { "/sbin/chkconfig --add tomcat":
    unless => "/sbin/chkconfig tomcat",
    require => File["/etc/init.d/tomcat"],
  }
  
}
