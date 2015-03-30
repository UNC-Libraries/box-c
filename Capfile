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

WEBAPPS = {
  "access" => "access/target/ROOT.war",
  "admin" => "admin/target/admin.war",
  "services" => "services/target/services.war",
  "djatoka" => "djatoka-cdr/dist/djatoka.war"
}

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

desc "Remove temporary files"
task :clean do
  rm_f "static.tar.gz"
end

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
  
  # Define update tasks for each webapp
  
  namespace :webapps do
    
    WEBAPPS.each do |name, path|
      
      desc "Update #{name} webapp"
      task name => path do |t|
        on roles(:web) do
          execute :mkdir, "-p", "/var/deploy/webapps"

          t.prerequisites.each do |p|
            upload! p, "/var/deploy/webapps"
          end
        end
      end
      
    end
    
  end

  desc "Update webapps"
  task :webapps do |t|
    WEBAPPS.each do |name, _|
      invoke "update:webapps:#{name}"
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

end

desc "Update everything"
task :update do
  invoke "update:static"
  invoke "update:webapps"
  invoke "update:lib"
  invoke "update:solrlib"
  invoke "update:deposit"
end

# Clean out Tomcat directories

namespace :clean do
  
  namespace :webapps do
  
    desc "Clean access webapp"
    task :access do
      on roles(:web) do
        sudo :rm, "-rf", "/opt/repository/tomcat/webapps/ROOT"
      end
    end
  
    desc "Clean admin webapp"
    task :admin do
      on roles(:web) do
        sudo :rm, "-rf", "/opt/repository/tomcat/webapps/admin"
      end
    end
  
    desc "Clean services webapp"
    task :services do
      on roles(:web) do
        sudo :rm, "-rf", "/opt/repository/tomcat/webapps/services"
      end
    end
  
    desc "Clean djatoka webapp"
    task :djatoka do
      on roles(:web) do
        sudo :rm, "-rf", "/opt/repository/tomcat/webapps/djatoka"
      end
    end
  
  end

  desc "Clean all webapps"
  task :webapps do
    invoke "clean:webapps:access"
    invoke "clean:webapps:admin"
    invoke "clean:webapps:services"
    invoke "clean:webapps:djatoka"
  end
  
end

# Define individual service tasks (restart:tomcat, ...)

SERVICES = [:deposit, :tomcat]
ACTIONS = [:start, :stop, :restart, :status]

ACTIONS.each do |action|

  SERVICES.each do |service|
  
    namespace action do
      
      desc "Service script: service #{action} #{service}"
      task service do
        on roles(:web) do
          sudo :service, service, action
        end
      end
    
    end
  
  end
  
end

# Define tasks for watching logs

namespace :tail do
  
  desc "Watch catalina.out"
  task :tomcat do
    on roles(:web) do
      execute :tail, "-f", "/opt/data/logs/catalina.out"
    end
  end
  
  desc "Watch deposit.log"
  task :deposit do
    on roles(:web) do
      sudo :tail, "-f", "/opt/data/logs/deposit.log"
    end
  end
  
end
