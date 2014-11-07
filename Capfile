require "capistrano/setup"

# Upload the tarball to a temporary directory, chdir to the specified directory, and expand
def upload_and_expand!(tarball, dir)
  temp_dir = capture(:mktemp, "-d")
  execute :chmod, "a+rx", temp_dir

  tarball_basename = File.basename(tarball)
  upload_path = File.join(temp_dir, tarball_basename)

  upload! tarball, upload_path
  execute :tar, "--warning=no-unknown-keyword", "-xzf", upload_path, "-C", dir
end

WEBAPPS = FileList[
  "access/target/ROOT.war",
  "admin/target/admin.war",
  "services/target/services.war"
]

LIB = FileList[
  "fcrepo-cdr-fesl/target/fcrepo-cdr-fesl-3.4-SNAPSHOT.jar",
  "fcrepo-clients/target/fcrepo-clients-3.4-SNAPSHOT.jar",
  "fcrepo-irods-storage/target/fcrepo-irods-storage-3.4-SNAPSHOT.jar",
  "metadata/target/cdr-metadata.jar",
  "security/target/security-3.4-SNAPSHOT.jar",
  "staging-areas/target/staging-areas-0.0.1-SNAPSHOT.jar"
]

SOLRLIB = FileList[
  "oai4solr/oai2-plugin/target/oai2-plugin-4.1.jar"
]

file "static.tar.gz" => FileList["access/src/main/external/static/**/*"] do |t|
  sh "export COPYFILE_DISABLE=1; tar -cvzf #{t.name} -C access/src/main/external/static ."
end

file "puppet.tar.gz" => FileList["puppet/**/*"] do |t|
  sh "export COPYFILE_DISABLE=1; tar -cvzf #{t.name} -C puppet ."
end

desc "Remove temporary files"
task :clean do
  rm_f "static.tar.gz"
  rm_f "puppet.tar.gz"
end

namespace :deploy do

  namespace :update do
  
    desc "Update static files"
    task :static => "static.tar.gz" do |t|
      tarball = t.prerequisites.first
      
      on roles(:web) do
        execute :rm, "-rf", "/var/deploy/static"
        execute :mkdir, "-p", "/var/deploy/static"
        
        upload_and_expand!(tarball, "/var/deploy/static")
      end
    end
  
    desc "Update webapps"
    task :webapps => WEBAPPS do |t|
      on roles(:web) do
        execute :mkdir, "-p", "/var/deploy/webapps"

        t.prerequisites.each do |p|
          upload! p, "/var/deploy/webapps"
        end
      end
    end
  
    desc "Update libraries"
    task :lib => LIB do |t|
      on roles(:web) do
        execute :rm, "-rf", "/var/deploy/lib"
        execute :mkdir, "-p", "/var/deploy/lib"
        
        t.prerequisites.each do |p|
          upload! p, "/var/deploy/lib"
        end
      end
    end
    
    desc "Update Solr libraries"
    task :solrlib => SOLRLIB do |t|
      on roles(:web) do
        execute :rm, "-rf", "/var/deploy/solrlib"
        execute :mkdir, "-p", "/var/deploy/solrlib"
        
        t.prerequisites.each do |p|
          upload! p, "/var/deploy/solrlib"
        end
      end
    end
    
    desc "Update deposit service"
    task :deposit => "deposit/target/deposit.jar" do |t|
      on roles(:web) do
        execute :rm, "-rf", "/var/deploy/deposit"
        execute :mkdir, "-p", "/var/deploy/deposit"
        
        upload! t.prerequisites.first, "/var/deploy/deposit"
      end
    end
    
    desc "Update the Puppet configuration"
    task :config => "puppet.tar.gz" do |t|
      tarball = t.prerequisites.first
      
      on roles(:all) do
        execute :rm, "-rf", "/var/deploy/puppet"
        execute :mkdir, "-p", "/var/deploy/puppet"
        
        upload_and_expand!(tarball, "/var/deploy/puppet")
      end
    end

  end
  
  task :update do
    invoke "deploy:update:config"
    invoke "deploy:update:static"
    invoke "deploy:update:webapps"
    invoke "deploy:update:lib"
    invoke "deploy:update:solrlib"
    invoke "deploy:update:deposit"
  end
  
end

desc "Update the configuration, apply the configuration, and then update everything else"
task :deploy do
  invoke "deploy:update"
end

namespace :service do
  
  namespace :tomcat do
    
    desc "Restart the tomcat service"
    task :restart do
      on roles(:web) do
        sudo :service, :tomcat, :restart
      end
    end
    
  end
  
  namespace :deposit do
    
    desc "Restart the deposit service"
    task :restart do
      on roles(:web) do
        sudo :service, :deposit, :restart
      end
    end
    
  end
  
end
