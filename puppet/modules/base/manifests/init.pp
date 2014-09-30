class base(
  $ssl_cert,
  $yumrepo_baseurl,
  $yumrepo_gpgkey
) {

  # Packages

  yumrepo { "cdr-server":
    baseurl => $yumrepo_baseurl,
    descr => "Carolina Digital Repository Packages - Server",
    enabled => 1,
    gpgcheck => 1,
    gpgkey => $yumrepo_gpgkey,
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

}
