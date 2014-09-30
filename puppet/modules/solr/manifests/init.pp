class solr(
  $remote_addr_valve_allow,
) {

  package { "cdr-solr":
    ensure => "4.3.0-2",
    require => [
      Class["tomcat"],
      Yumrepo["cdr-server"]
    ]
  }
  
  file { "/opt/repository/solr":
    source => "puppet:///modules/solr/home",
    recurse => remote,
    purge => false,
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/solr.xml":
    content => template("solr/tomcat/conf/Catalina/localhost/solr.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }
  
}
