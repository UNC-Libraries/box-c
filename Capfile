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

file "puppet.tar.gz" do |t|
  sh "tar -cvzf #{t.name} -C puppet ."
end

namespace :deploy do

  namespace :update do

    task :static => "static.tar.gz" do |t|
      tarball = t.prerequisites.first
      
      on roles(:all) do
        execute :mkdir, "-p", "/tmp/deploy"
        upload! tarball, "/tmp/deploy"
        sudo :tar, "-xzf", File.join("/tmp/deploy", File.basename(tarball)), "-C /var/www/html/static/"
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
    
    task :config => "puppet.tar.gz" do |t|
      tarball = t.prerequisites.first
      
      on roles(:all) do
        execute :mkdir, "-p", "/tmp/deploy"
        upload! tarball, "/tmp/deploy"
    
        sudo :rm, "-rf", "/etc/puppet/environments/cdr"
        sudo :mkdir, "-p", "/etc/puppet/environments/cdr"
        sudo :tar, "-xzf", File.join("/tmp/deploy", File.basename(tarball)), "-C /etc/puppet/environments/cdr"
      end
    end

  end
  
  task :update do
    invoke "deploy:update:static"
    invoke "deploy:update:webapps"
    invoke "deploy:update:libs"
    invoke "deploy:update:config"
  end
  
  namespace :apply do
  
    task :noop do
      on roles(:all) do
        sudo :sudo, :puppet, :apply, "--execute \"hiera_include(\\\"classes\\\")\"", "--environment cdr", "--noop"
      end
    end
    
  end
  
  task :apply do
    on roles(:all) do
      sudo :puppet, :apply, "--execute \"hiera_include(\\\"classes\\\")\"", "--environment cdr"
    end
  end
  
end

task :deploy do
  invoke "deploy:update"
  invoke "deploy:apply"
end
