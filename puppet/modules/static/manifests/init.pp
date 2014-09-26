class static(
  $apache_docroot,
) {
  
  file { "${apache_docroot}/static":
    ensure => "link",
    target => "/opt/deploy/static",
    force => true,
  }
  
}
