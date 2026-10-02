import yaml

with open('docker-compose.yml', 'r') as f:
    data = yaml.safe_load(f)

data['services']['kafka']['healthcheck'] = {
    'test': ["CMD", "nc", "-z", "localhost", "9092"],
    'interval': '5s',
    'timeout': '5s',
    'retries': 5
}

for svc in ['inventario-api', 'pedido-api', 'producto-api']:
    if svc in data['services'] and 'depends_on' in data['services'][svc]:
        if 'kafka' in data['services'][svc]['depends_on']:
            data['services'][svc]['depends_on']['kafka'] = {'condition': 'service_healthy'}

with open('docker-compose.yml', 'w') as f:
    yaml.dump(data, f, sort_keys=False)
