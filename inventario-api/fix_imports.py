with open("src/test/java/io/github/danielmelejpinto/inventarioapi/controller/InventarioControllerTest.java", "r") as f:
    content = f.read()

imports = """
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
"""

if "import java.util.List;" not in content:
    content = content.replace("import java.util.concurrent.atomic.AtomicLong;", "import java.util.concurrent.atomic.AtomicLong;\n" + imports)
    with open("src/test/java/io/github/danielmelejpinto/inventarioapi/controller/InventarioControllerTest.java", "w") as f:
        f.write(content)
