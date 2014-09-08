require "capistrano/setup"

def upload_as!(local, remote, user, options = {})
  temp_dir = capture(:mktemp, "-d")
  execute :chmod, "a+rx", temp_dir

  local_basename = File.basename(local)
  upload_path = File.join(temp_dir, local_basename)

  upload! local, upload_path, options

  as user do
    if options[:recursive]
      execute :cp, "-R", upload_path, remote
    else
      execute :cp, upload_path, remote
    end
  end
end

WEBAPPS = FileList[
  "access/target/ROOT.war",
  "admin/target/admin.war",
  "services/target/services.war"
]

TOMCAT_LIBS = FileList[
  "metadata/target/cdr-metadata.jar",
  "security/target/security-3.4-SNAPSHOT.jar"
]

FEDORA_LIBS = FileList[
  "fcrepo-irods-storage/target/fcrepo-irods-storage-3.4-SNAPSHOT.jar",
  "metadata/target/cdr-metadata.jar",
  "security/target/security-3.4-SNAPSHOT.jar",
  "fcrepo-cdr-fesl/target/fcrepo-cdr-fesl-3.4-SNAPSHOT.jar",
  "fcrepo-clients/target/fcrepo-clients-3.4-SNAPSHOT.jar",
  "staging-areas/target/staging-areas-0.0.1-SNAPSHOT.jar"
]

file "static.tar.gz" do |t|
  sh "tar -cvzf #{t.name} -C access/src/main/external/static ."
end

namespace :update do

  task :static => "static.tar.gz" do |t|
    tarball = t.prerequisites.first
    on roles(:all) do
      execute :mkdir, "-p", "/tmp/deploy"
      upload! tarball, "/tmp/deploy"
      as :tomcat do
        execute :tar, "-xzf", "/tmp/deploy/static.tar.gz", "-C /var/www/html/static/"
      end
    end
  end
  
  task :webapps => WEBAPPS do |t|
    on roles(:all) do
      t.prerequisites.each do |p|
        upload_as! p, "/opt/repository/tomcat/webapps/", :tomcat
      end
    end
  end
  
  task :tomcat_libs => TOMCAT_LIBS do |t|
    on roles(:all) do
      t.prerequisites.each do |p|
        upload_as! p, "/opt/repository/tomcat/lib/", :tomcat
      end
    end
  end
  
  task :fedora_libs => FEDORA_LIBS do |t|
    on roles(:all) do
      t.prerequisites.each do |p|
        upload_as! p, "/opt/repository/tomcat/webapps/fedora/WEB-INF/lib/", :tomcat
      end
    end
  end
  
  task :libs => [:tomcat_libs, :fedora_libs]

end

task :update do
  invoke "update:static"
  invoke "update:webapps"
  invoke "update:libs"
end

task :restart do
  on roles(:all) do
    as :root do
      execute :service, :tomcat, :restart
    end
  end
end
