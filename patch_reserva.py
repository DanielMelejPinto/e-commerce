with open('inventario-api/src/main/java/io/github/danielmelejpinto/inventarioapi/model/Reserva.java', 'r') as f:
    content = f.read()

import re
content = re.sub(r'public Reserva\(Long pedidoId, Long productoId, Integer cantidad\) \{.*?\n        this\.estado = "ACTIVA";\n    \}', 
'''public Reserva(Long pedidoId, Long productoId, Integer cantidad) {
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.cantidad = cantidad.longValue();
        this.estado = "ACTIVA";
    }''', content, flags=re.DOTALL)

with open('inventario-api/src/main/java/io/github/danielmelejpinto/inventarioapi/model/Reserva.java', 'w') as f:
    f.write(content)
