import yaml
import os

with open('infrastructure/local-podman/podman-compose.yml', 'r') as f:
    data = yaml.safe_load(f)

services = data.get('services', {})

# Common environment variables
spring_common = {
    'SPRING_PROFILES_ACTIVE': 'container',
    'REDIS_HOST': 'redis',
    'REDIS_PORT': '6379',
    'KAFKA_BROKERS': 'kafka:29092',
    'DB_HOST': 'postgres',
    'DB_PORT': '5432',
    'DB_USERNAME': 'payu',
    'DB_PASSWORD': '${POSTGRES_PASSWORD:-payu_secret}',
    'OIDC_ISSUER': 'http://keycloak:8080/realms/payu',
    'OIDC_JWK_SET_URI': 'http://keycloak:8080/realms/payu/protocol/openid-connect/certs',
    'OTEL_ENDPOINT': 'http://jaeger:4317'
}

quarkus_common = {
    'QUARKUS_REDIS_HOSTS': 'redis:6379',
    'KAFKA_BOOTSTRAP_SERVERS': 'kafka:29092',
    'OTEL_ENDPOINT': 'http://jaeger:4317'
}

# DB name mapping for services that don't follow the 1:1 rule
db_overrides = {
    'bi-fast-simulator': 'payu_bifast',
    'dukcapil-simulator': 'payu_dukcapil',
    'qris-simulator': 'payu_qris',
}

for service_name, config in services.items():
    if 'build' not in config:
        continue
        
    env = config.get('environment', {})
    context = config['build']['context']
    abs_context = os.path.normpath(os.path.join('infrastructure/local-podman', context))
    
    # Get port from compose definition
    port = None
    if 'ports' in config:
        p = config['ports'][0]
        if isinstance(p, str):
            port = p.split(':')[1]
        elif isinstance(p, int):
            port = str(p) # unlikely but handle
            
    # Determine service root based on Dockerfile location relative to context
    dockerfile = config['build'].get('dockerfile', 'Dockerfile')
    service_root = os.path.dirname(os.path.join(abs_context, dockerfile))
    
    # Determine tech stack
    is_spring = os.path.exists(os.path.join(service_root, 'pom.xml')) and \
                (os.path.exists(os.path.join(service_root, 'src/main/resources/application.yml')) or \
                 os.path.exists(os.path.join(service_root, 'src/main/resources/application.yaml')))
    
    is_quarkus = os.path.exists(os.path.join(service_root, 'pom.xml')) and not is_spring
    
    is_python = os.path.exists(os.path.join(service_root, 'requirements.txt')) or \
                os.path.exists(os.path.join(service_root, 'pyproject.toml'))
                
    is_node = os.path.exists(os.path.join(service_root, 'package.json')) and not is_spring and not is_quarkus

    # Cleanup and set
    if is_spring:
        for k in list(env.keys()):
            if k.startswith('QUARKUS_'): del env[k]
        for k, v in spring_common.items(): env[k] = v
        db_name = db_overrides.get(service_name, f"payu_{service_name.replace('-service', '').replace('-', '_')}")
        env['DB_URL'] = f"jdbc:postgresql://postgres:5432/{db_name}"
        env['DATABASE_URL'] = env['DB_URL']
        env['SPRING_DATASOURCE_URL'] = env['DB_URL']
        env['SPRING_DATASOURCE_USERNAME'] = 'payu'
        env['SPRING_DATASOURCE_PASSWORD'] = '${POSTGRES_PASSWORD:-payu_secret}'
        if port:
            env['SERVER_PORT'] = port
            env['PORT'] = port
        if service_name == 'auth-service':
            env['KEYCLOAK_URL'] = 'http://keycloak:8080'
            env['KEYCLOAK_REALM'] = 'payu'
            env['KEYCLOAK_CLIENT_ID'] = 'payu-backend'
            env['KEYCLOAK_CLIENT_SECRET'] = '${KEYCLOAK_CLIENT_SECRET:-secret}'
            env['KEYCLOAK_ADMIN_USERNAME'] = '${KEYCLOAK_ADMIN:-admin}'
            env['KEYCLOAK_ADMIN_PASSWORD'] = '${KEYCLOAK_ADMIN_PASSWORD:-admin}'

    elif is_quarkus:
        for k in list(env.keys()):
            if k.startswith('SPRING_') or k in spring_common: 
                 if k not in ['OTEL_ENDPOINT']: del env[k]
        for k, v in quarkus_common.items(): env[k] = v
        db_name = db_overrides.get(service_name, f"payu_{service_name.replace('-service', '').replace('-', '')}")
        db_name = db_name.replace('bi-fast', 'bifast').replace('dukcapil-simulator', 'dukcapil').replace('qris-simulator', 'qris')
        
        has_db = db_name.replace('payu_', '') in ['account', 'auth', 'transaction', 'wallet', 'notification', 'billing', 'kyc', 'analytics', 'compliance', 'bifast', 'dukcapil', 'qris', 'investment', 'lending', 'backoffice', 'partner', 'promotion', 'support', 'statement']
        if has_db:
            env['QUARKUS_DATASOURCE_JDBC_URL'] = f"jdbc:postgresql://postgres:5432/{db_name}"
            env['QUARKUS_DATASOURCE_USERNAME'] = 'payu'
            env['QUARKUS_DATASOURCE_PASSWORD'] = '${POSTGRES_PASSWORD:-payu_secret}'
        if port:
            env['QUARKUS_HTTP_PORT'] = port

    elif is_python:
        for k in list(env.keys()):
            if k.startswith('QUARKUS_') or k.startswith('SPRING_'): del env[k]
        env['OTEL_ENDPOINT'] = 'http://jaeger:4317'
        env['KAFKA_BOOTSTRAP_SERVERS'] = 'kafka:29092'
        db_name = f"payu_{service_name.replace('-service', '').replace('-', '_')}"
        env['DATABASE_URL'] = f"postgresql+asyncpg://payu:${{POSTGRES_PASSWORD:-payu_secret}}@postgres:5432/{db_name}"
        if port:
            env['PORT'] = port

    elif is_node:
        for k in list(env.keys()):
            if k.startswith('QUARKUS_') or k.startswith('SPRING_'): del env[k]
        if service_name == 'web-app':
            env['NEXT_PUBLIC_API_URL'] = 'http://localhost:8080/api/v1'

    config['environment'] = env

with open('infrastructure/local-podman/podman-compose.yml', 'w') as f:
    yaml.dump(data, f, default_flow_style=False, sort_keys=False)
