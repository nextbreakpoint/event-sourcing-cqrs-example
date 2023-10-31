Vagrant.configure(2) do |config|
  config.vm.define "blueprint" do |s|
    s.ssh.forward_agent = true
    s.vm.box = "ubuntu/focal64"
    s.vm.hostname = "blueprint"
    s.vm.network "private_network",
      ip: "192.168.10.30",
      netmask: "255.255.255.0",
      auto_config: true
    s.vm.provision :shell,
      path: "pipeline/setup.sh",
      privileged: false
    s.vm.provider "virtualbox" do |v|
      v.name = "blueprint"
      v.cpus = 2
      v.memory = 7168
      v.gui = false
    end
  end
end
