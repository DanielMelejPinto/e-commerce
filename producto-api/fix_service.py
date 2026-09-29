with open("src/main/java/io/github/danielmelejpinto/productoapi/service/ProductoService.java", "r") as f:
    content = f.read()

content = content.replace("Sort sort = pageable.getSort().and(Sort.by(\"id\"));", "Sort sort = pageable.getSort().and(Sort.sort(Producto.class).by(Producto::getId).ascending());")

with open("src/main/java/io/github/danielmelejpinto/productoapi/service/ProductoService.java", "w") as f:
    f.write(content)
