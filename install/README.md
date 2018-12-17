# CM Install

Ansible playbooks for installing Pulse.

## csd-playbook.yml
Installs the CSD from the phData parcel repository

```bash 
$ ansible-playbook csd-playbook.yml -u <user> -i <host>, --extra-vars "version=<version>"  
```

## dev-playbook.yml
The dev playbook will install a Cloudera CSD and parcel from local artifacts
```bash 
$ ansible-playbook dev-playbook.yml -u <user> -i <host>, --extra-vars "version=$(sh ../version)"  
```
