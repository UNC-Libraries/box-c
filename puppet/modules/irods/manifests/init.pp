class irods(
  $tika_host,
  $tika_port,
  $zone_name,
  $replication_email_address,
  $resource_name,
  $hosts,
  $port,
  $admin_name,
  $admin_password,
  $icat_host,
  $resource_dir,
  $database_home,
  $database_lib,
  $database_host,
  $database_port,
  $database_user,
  $database_password,
  $database_name,
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
