import yaml

with open('infrastructure/local-podman/podman-compose.yml', 'r') as f:
    data = yaml.safe_load(f)

for service_name, config in data.get('services', {}).items():
    image = config.get('image')
    if image and not image.startswith('localhost/') and not image.startswith('quay.io/') and not image.startswith('docker.io/'):
        if '/' in image:
             config['image'] = f"docker.io/{image}"
        elif image.startswith('postgres:') or image.startswith('redis:'):
             config['image'] = f"docker.io/library/{image}"

with open('infrastructure/local-podman/podman-compose.yml', 'w') as f:
    yaml.dump(data, f, default_flow_style=False, sort_keys=False)
