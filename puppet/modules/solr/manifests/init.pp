class solr(
  $remote_addr_valve_allow,
  $oai_web_protocol,
  $oai_web_host,
  $oai_etd_set_query
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
  
  file { "/opt/repository/solr/lib":
    ensure => "link",
    target => "/opt/deploy/solrlib",
    force => true,
  }
  
  file { "/opt/repository/tomcat/conf/Catalina/localhost/solr.xml":
    content => template("solr/tomcat/conf/Catalina/localhost/solr.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }
  
  file { "/opt/repository/solr/access/conf/solrconfig.xml":
    content => template("solr/home/access/conf/solrconfig.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }

  file { "/opt/repository/solr/access/oai/Identify.xml":
    content => template("solr/home/access/oai/Identify.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }

  file { "/opt/repository/solr/access/oai/oai_dc.xsl":
    content => template("solr/home/access/oai/oai_dc.xsl.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }

  file { "/opt/repository/solr/access/oai/solr.xsl":
    content => template("solr/home/access/oai/solr.xsl.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => Package["cdr-solr"],
  }
  
}
