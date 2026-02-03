import yaml
import os

with open('infrastructure/local-podman/podman-compose.yml', 'r') as f:
    data = yaml.safe_load(f)

for service_name, config in data.get('services', {}).items():
    if 'build' in config and 'image' not in config:
        config['image'] = f"localhost/payu-{service_name.replace('_', '-')}"

with open('infrastructure/local-podman/podman-compose.yml', 'w') as f:
    yaml.dump(data, f, default_flow_style=False, sort_keys=False)
