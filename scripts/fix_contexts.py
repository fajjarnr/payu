import yaml
import os

with open('infrastructure/local-podman/podman-compose.yml', 'r') as f:
    data = yaml.safe_load(f)

services = data.get('services', {})

for service_name, config in services.items():
    if 'build' in config:
        context = config['build']['context']
        # Only move context to parent for backend services
        # that actually have a pom.xml in the parent directory
        if context.startswith('../../backend/'):
            service_path = context.replace('../../backend/', '')
            config['build']['context'] = '../../backend'
            config['build']['dockerfile'] = f"{service_path}/Containerfile"
        # For others, if they were already changed, we might want to revert or skip
        # Actually, let's just be explicit about which ones we want to change.
        
with open('infrastructure/local-podman/podman-compose.yml', 'w') as f:
    yaml.dump(data, f, default_flow_style=False, sort_keys=False)
