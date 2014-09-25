class base(
  $ssl_cert,
  $ssl_key,
  $package_repo_baseurl,
  $package_gpgkey,
  $apache_servername
) {

  # Packages

  yumrepo { "cdr-server":
    baseurl => $package_repo_baseurl,
    descr => "Carolina Digital Repository Packages - Server",
    enabled => 1,
    gpgcheck => 1,
    gpgkey => $package_gpgkey,
  }


  # Java

  package { ["java-1.7.0-openjdk",
             "java-1.7.0-openjdk-devel"]:
    ensure => installed,
  }

  java_ks { "localhost:/usr/lib/jvm/java/jre/lib/security/cacerts":
    ensure => latest,
    certificate => $ssl_cert,
    password => "changeit",
    require => Package["java-1.7.0-openjdk-devel"],
  }
  
  package { "cdr-commons-daemon":
    ensure => "1.0.15-1",
    require => [
      User["tomcat"],
      Package["java-1.7.0-openjdk-devel"],
      Yumrepo["cdr-server"]
    ],
  }
  
  
  # Redis
  
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


  # PostgreSQL

  class { "postgresql::globals":
    manage_package_repo => true,
    version => "9.1",
  } ->
  
  class { "postgresql::server":
  }


  # Apache

  class { "apache":
    default_vhost => false,
  }

  apache::mod { "proxy_ajp": }

  apache::vhost { "${apache_servername}:80":
    servername => $apache_servername,
    docroot => "/var/www/html",
    port => "80",
    rewrites => [
      {
        rewrite_cond => [
          "%{SERVER_PORT} ^80$",
          "%{REMOTE_ADDR} !^127.0.0.1$",
        ],
        rewrite_rule => ["^/(.*) https://%{SERVER_NAME}/$1 [L,R,NC]"],
      }
    ],
    proxy_pass => [
      { "path" => "/services", "url" => "ajp://localhost:8009/services" },
      { "path" => "/solr", "url" => "ajp://localhost:8009/solr" },
    ],
  }

  apache::vhost { "${apache_servername}:443":
    servername => $apache_servername,
    port => "443",
    docroot => "/var/www/html",
    ssl => true,
    ssl_cert => $ssl_cert,
    ssl_key => $ssl_key,
    proxy_pass => [
      { "path" => "/static", "url" => "!" },
      { "path" => "/", "url" => "ajp://localhost:8009/" },
    ],
  }
  
  # Symlink to deploy
  
  file { "/var/www/html/static":
    ensure => "link",
    target => "/opt/deploy/static",
    force => true,
  }

}
