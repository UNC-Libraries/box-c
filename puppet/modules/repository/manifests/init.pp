class repository(
  $user,
  $staging,
  $collection_indexing,
  $activemq_connector_uri,
  $enable_debug,
  $web_prototcol,
  $web_host,
  $web_port,
  $web_context,
  $fedora_administrator_email,
  $fedora_protocol,
  $fedora_port,
  $fedora_port_number,
  $fedora_redirect_port_number,
  $fedora_shutdown_port_number,
  $fedora_host,
  $fedora_admin_username,
  $fedora_admin_password,
  $fedora_app_user_username,
  $fedora_app_user_password,
  $fedora_irods_host,
  $fedora_irods_port,
  $fedora_irods_zone,
  $fedora_irods_default_resc,
  $fedora_irods_username,
  $fedora_irods_password,
  $mulgara_protocol,
  $mulgara_host,
  $mulgara_port,
  $irods_host,
  $irods_port,
  $irods_zone,
  $irods_default_resc,
  $irods_services_username,
  $irods_services_password,
  $solr_protocol,
  $solr_host,
  $solr_port,
  $jms_host,
  $jms_port,
  $smtp_host,
  $smtp_port,
  $clamd_host,
  $clamd_port,
  $contact_email,
  $administrator_email,
  $from_email,
  $reply_email,
  $external_base_file_url,
  $forms_default_container_pid,
  $forms_sword_username,
  $forms_sword_password,
  $forms_administrator_email,
  $forms_from_email,
  $forms_site_name,
  $forms_site_url,
  $forms_maxuploadsize,
  $forms_external_dir,
  $forms_external_uri_base,
  $fixity_resource_names,
  $fixity_polling_interval_seconds,
  $fixity_stale_interval_seconds,
  $fixity_object_limit,
  $rest_allow_ip_regex,
  $sword_username,
  $sword_password,
  $recaptcha_private_key,
  $recaptcha_public_key,
  $google_tracking_id,
) {

  file { "/opt/repository":
    ensure => "directory",
    owner => "tomcat",
    group => "tomcat",
    require => [
      User["tomcat"],
      Package["cdr-tomcat"],
    ],
  }
  
  file { "/opt/repository/cdr-activemq.xml":
    content => template("repository/home/cdr-activemq.xml.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository"],
  }
  
  file { "/opt/repository/cdr-env.sh":
    content => template("repository/home/cdr-env.sh.erb"),
    owner => "tomcat",
    group => "tomcat",
    mode => "a+x",
    require => File["/opt/repository"],
  }
  
  file { "/opt/repository/collectionIndexing.properties":
    content => template("repository/home/collectionIndexing.properties.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository"],
  }
  
  file { "/opt/repository/server.properties":
    content => template("repository/home/server.properties.erb"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository"],
  }
  
  file { "/opt/repository/stagesConfig.json":
    content => inline_template("<%= @staging.to_json %>"),
    owner => "tomcat",
    group => "tomcat",
    require => File["/opt/repository"],
  }

  $data_directories = [
    "/opt/data",
    "/opt/data/enhance",
    "/opt/data/enhance/failed",
    "/opt/data/fedora-policy-db",
    "/opt/data/logs",
    "/opt/data/mulgara-tmp",
    "/opt/data/forms-stash",
    "/opt/data/staging",
    "/data",
    "/data/djatoka-temp",
  ]

  file { $data_directories:
    ensure => "directory",
    owner => "tomcat",
    group => "tomcat",
    require => User["tomcat"],
  }

}
