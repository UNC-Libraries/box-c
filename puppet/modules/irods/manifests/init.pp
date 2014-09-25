class irods(
  $user,
  $tika_host,
  $tika_port,
  $zone_name,
  $replication_email_address,
  $resource_name,
  $hosts,
  $database_home,
  $database_lib,
  $database_host,
  $database_port,
  $database_admin_name,
  $database_admin_password,
  $port,
  $admin_name,
  $admin_password,
  $icat_host,
  $database_name,
  $resource_dir,
  $database_key,
) {
  
  $packages = [
    "ImageMagick",
    "ImageMagick-perl",
    "jasper-libs",
    "unixODBC",
    "unixODBC-devel",
  ]
  
  package { $packages:
    ensure => "installed"
  }
  
  package { "postgresql91-odbc":
    require => Class["postgresql::server"],
  }
  
  # iRODS 3.2 expects to see libodbcpsql.so
  file { "/usr/pgsql-9.1/lib/libodbcpsql.so":
    ensure => "link",
    target => "/usr/pgsql-9.1/lib/psqlodbc.so",
    require => Package["postgresql91-odbc"]
  }
  
  user { "irods":
    ensure => present,
    comment => "iRODS",
    expiry => "absent",
    gid => "irods",
    home => "/opt/iRODS",
    shell => "/bin/bash",
  }

  group { "irods":
    ensure => present,
  }
  
  package { "cdr-irods":
    ensure => "3.2-3",
    require => [
      User["irods"],
      Package[$packages],
      Yumrepo["cdr-server"]
    ],
  }
  
  postgresql::server::db { $database_name:
    user => $database_admin_name,
    password => postgresql_password($database_admin_name, $database_admin_password),
  }

  postgresql::server::pg_hba_rule { "allow password authentication for irods user on localhost":
    type => "host",
    database => $database_name,
    user => $user,
    address => "127.0.0.1/32",
    auth_method => "md5",
  }
  
  file { $resource_dir:
    ensure => "directory",
    owner => "irods",
    group => "irods",
  }
  
  file { "/opt/iRODS":
    source => "puppet:///modules/irods/home",
    recurse => remote,
    purge => false,
    owner => "irods",
    group => "irods",
    require => Package["cdr-irods"],
  }
  
  file { "/opt/iRODS/config/irods.config":
    content => template("irods/home/config/irods.config.erb"),
    owner => "irods",
    group => "irods",
    require => File["/opt/iRODS"],
  }
  
  file { "/opt/iRODS/server/config/reConfigs/core.re":
    content => template("irods/home/server/config/reConfigs/core.re.erb"),
    owner => "irods",
    group => "irods",
    require => File["/opt/iRODS"],
  }
  
  file { "/opt/iRODS/server/config/reConfigs/config.re":
    content => template("irods/home/server/config/reConfigs/config.re.erb"),
    owner => "irods",
    group => "irods",
    require => File["/opt/iRODS"],
  }
  
  file { "/opt/iRODS/server/config/irodsHost":
    content => template("irods/home/server/config/irodsHost.erb"),
    owner => "irods",
    group => "irods",
    require => File["/opt/iRODS"],
  }
  
  file { "/etc/init.d/irods":
    content => template("irods/irods.sh.erb"),
    mode => "a+x",
  }
  
  exec { "/sbin/chkconfig --add irods":
    unless => "/sbin/chkconfig irods",
    require => File["/etc/init.d/irods"],
  }

}
