server "default", :roles => [:web]

set :service_path, "/sbin/service"

set :ssh_options, lambda {

  # Get the configuration from Vagrant

  if ENV["VAGRANT_CWD"].nil? && File.exist?("Vagrantfile")
    ENV["VAGRANT_CWD"] = "."
  end

  if ENV["VAGRANT_CWD"].nil?
    raise "The VAGRANT_CWD environment variable must be set"
  end

  unless File.exist?(File.join(ENV["VAGRANT_CWD"], "Vagrantfile"))
    raise "The directory specified by the VAGRANT_CWD environment variable must contain a Vagrantfile"
  end

  config = `vagrant ssh-config --host default`

  if config == ""
    raise "vagrant ssh-config returned an invalid configuration -- is VAGRANT_CWD set correctly?"
  end

  # Replace User and IdentityFile configuration options

  config = config.gsub!(/^(\s*User) vagrant$/, "\\1 deploy")
  config = config.gsub!(/^(\s*IdentityFile) .*$/, "\\1 config/deploy/local_insecure_private_key")

  # Write the configuration a file

  File.open("./ssh_config", "w+") do |f|
    f.write(config)
  end

  # That's our config!

  { :config => "./ssh_config" }

}
