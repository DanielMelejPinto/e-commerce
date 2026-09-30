import os

services = ['producto-api', 'inventario-api', 'usuario-api', 'pedido-api']

for s in services:
    path = os.path.join(s, 'pom.xml')
    with open(path, 'r') as f:
        content = f.read()
    
    if '<artifactId>flyway-core</artifactId>' not in content:
        # insert flyway dependencies
        dep = """
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-database-postgresql</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-flyway</artifactId>
		</dependency>
"""
        content = content.replace('<dependencies>', '<dependencies>\n' + dep)
        
    with open(path, 'w') as f:
        f.write(content)
