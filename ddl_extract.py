import subprocess
import os

services = ['producto-api', 'inventario-api', 'usuario-api', 'pedido-api']

for svc in services:
    print(f"--- {svc} ---")
    props = f"{svc}/src/main/resources/application.properties"
    # Append postgres configs temporarily to dump DDL
    with open(props, "a") as f:
        f.write("\nspring.jpa.properties.javax.persistence.schema-generation.scripts.action=create\n")
        f.write(f"spring.jpa.properties.javax.persistence.schema-generation.scripts.create-target=../schema-{svc}.sql\n")
        f.write("spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect\n")
    
    # Run test or just load context (a test does it)
    subprocess.run(["./mvnw", "test", "-Dtest=*ApplicationTests"], cwd=svc, stdout=subprocess.DEVNULL)
    
    # Revert props
    os.system(f"git checkout {props}")
    
    if os.path.exists(f"schema-{svc}.sql"):
        with open(f"schema-{svc}.sql") as f:
            print(f.read())
