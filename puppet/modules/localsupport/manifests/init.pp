class localsupport(
  $apache_present,
  $apache_ssl_cert,
  $apache_ssl_key,
  $apache_servername,
  $apache_docroot,
  
  $fedora_user,
  $fedora_database_present,
  $fedora_database_name,
  $fedora_database_user,
  $fedora_database_password,
  
  $irods_user,
  $irods_database_present,
  $irods_database_name,
  $irods_database_user,
  $irods_database_password,
) {
  
  
  # PostgreSQL
  
  if $fedora_database_present or $irods_database_present {

    class { "postgresql::globals":
      manage_package_repo => true,
      version => "9.1",
    } ->
  
    class { "postgresql::server":
    }
    
  }
  
  
  # Fedora database
  
  if $fedora_database_present {
  
    postgresql::server::db { $fedora_database_name:
      user => $fedora_database_user,
      password => postgresql_password($fedora_database_user, $fedora_database_password),
    }

    postgresql::server::pg_hba_rule { "allow password authentication for $fedora_user on localhost":
      type => "host",
      database => $fedora_database_name,
      user => $fedora_user,
      address => "127.0.0.1/32",
      auth_method => "md5",
    }
    
  }
  
  
  # iRODS database
  
  if $irods_database_present {
  
    postgresql::server::db { $irods_database_name:
      user => $irods_database_user,
      password => postgresql_password($irods_database_user, $irods_database_password),
    }

    postgresql::server::pg_hba_rule { "allow password authentication for $irods_user on localhost":
      type => "host",
      database => $irods_database_name,
      user => $irods_user,
      address => "127.0.0.1/32",
      auth_method => "md5",
    }
    
  }


  # Apache
  
  if $apache_present {

    class { "apache":
      default_vhost => false,
    }

    apache::mod { "proxy_ajp": }

    apache::vhost { "${apache_servername}:80":
      servername => $apache_servername,
      docroot => $apache_docroot,
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
      docroot => $apache_docroot,
      ssl => true,
      ssl_cert => $ssl_cert,
      ssl_key => $ssl_key,
      proxy_pass => [
        { "path" => "/static", "url" => "!" },
        { "path" => "/", "url" => "ajp://localhost:8009/" },
      ],
    }
    
  }
  
}
