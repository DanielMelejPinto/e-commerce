import org.springframework.data.domain.Sort;
import org.springframework.data.domain.TypedSort;
import io.github.danielmelejpinto.productoapi.model.Producto;

public class TestSort {
    public static void main(String[] args) {
        Sort sort = Sort.sort(Producto.class).by(Producto::getId).ascending();
        System.out.println(sort);
    }
}
