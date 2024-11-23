import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodkeeper_final.R

class FridgeAdapter(private val items: List<String>) :
    RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder>() {

    // ViewHolder - связывает данные с элементами разметки
    class FridgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.productName)
        val productImage: ImageView = view.findViewById(R.id.productImage)
    }

    // Создаем ViewHolder из разметки
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fridge, parent, false)
        return FridgeViewHolder(view)
    }

    // Привязываем данные к элементам
    override fun onBindViewHolder(holder: FridgeViewHolder, position: Int) {
        holder.productName.text = items[position]
        holder.productImage.setImageResource(android.R.drawable.ic_menu_gallery) // Пример изображения
    }

    override fun getItemCount(): Int = items.size
}
