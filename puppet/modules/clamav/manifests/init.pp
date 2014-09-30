class clamav {

  package { ["clamav", "clamd"]:
  }

  service { "clamd":
    ensure => true,
    enable => true,
    hasrestart => true,
    hasstatus => true,
    require => Package["clamav", "clamd"],
  }
  
}
