import re
with open('docker-compose.yml', 'r') as f:
    lines = f.readlines()
with open('docker-compose.yml', 'w') as f:
    for line in lines:
        if 'SPRING_FLYWAY' in line and 'POSTGRES_USER' not in line:
            if 'postgres:' in ''.join(lines):
                # just skip the flyway env vars from postgres block somehow
                pass
