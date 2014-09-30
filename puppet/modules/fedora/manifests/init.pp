class fedora(
  $ssl_cert,
  $trust_store_password,
  $remote_broker_uri,
  $users,
  $admin_email_list,
  $postgresql_db_username,
  $postgresql_db_password,
  $postgresql_db_name,
  $postgresql_jdbc_url,
  $mulgara_host,
  $mulgara_port,
  $remote_addr_valve_allow,
) {
  
  package { "cdr-fedora":
    ensure => "3.6.2-3",
    require => [
      Class["tomcat"],
      Yumrepo["cdr-server"]
    ],
  }
  
  file { "/opt/repository/fedora":
    source => "puppet:///modules/fedora/home",
    recurse => true,
    purge => false,
    replace => true,
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-fedora"],
  }
  
  file { "/opt/repository/tomcat/webapps/fedora":
    source => "puppet:///modules/fedora/tomcat/webapps/fedora",
    recurse => remote,
    purge => false,
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-fedora"],
  }
  
  file { "/opt/repository/fedora/server/bin/env-server.sh":
    content => template("fedora/home/server/bin/env-server.sh.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository/fedora"],
  }
  
  file { "/opt/repository/fedora/server/config/activemq.xml":
    content => template("fedora/home/server/config/activemq.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository/fedora"],
  }
  
  file { "/opt/repository/fedora/server/config/fedora-users.xml":
    content => template("fedora/home/server/config/fedora-users.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository/fedora"],
  }
  
  file { "/opt/repository/fedora/server/config/fedora.fcfg":
    content => template("fedora/home/server/config/fedora.fcfg.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository/fedora"],
  }
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/fedora.xml":
    content => template("fedora/tomcat/conf/Catalina/localhost/fedora.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-fedora"],
  }
  
  java_ks { "localhost:/opt/repository/fedora/client/truststore":
    ensure => latest,
    certificate => $ssl_cert,
    password => "tomcat",
    require => File["/opt/repository/fedora"],
  }
  
  java_ks { "localhost:/opt/repository/fedora/server/truststore":
    ensure => latest,
    certificate => $ssl_cert,
    password => "tomcat",
    require => File["/opt/repository/fedora"],
  }
  
}
