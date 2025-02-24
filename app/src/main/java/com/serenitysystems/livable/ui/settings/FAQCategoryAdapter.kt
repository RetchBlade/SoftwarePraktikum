import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.serenitysystems.livable.R

class FAQCategoryAdapter(private val categorizedFaqList: List<Pair<String, List<Pair<String, String>>>>) :
    RecyclerView.Adapter<FAQCategoryAdapter.FAQCategoryViewHolder>() {

    private val expandedCategories = mutableSetOf<Int>()

    inner class FAQCategoryViewHolder(itemView: View) : ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.faqCategory)
        val questionsRecyclerView: RecyclerView = itemView.findViewById(R.id.faqQuestionsRecyclerView)
        val expandArrow: ImageView = itemView.findViewById(R.id.expandArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQCategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.faq_category_item, parent, false)
        return FAQCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQCategoryViewHolder, position: Int) {
        val (category, questions) = categorizedFaqList[position]

        holder.categoryTitle.text = category

        val faqAdapter = FAQAdapter(questions)
        holder.questionsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.questionsRecyclerView.adapter = faqAdapter

        val isExpanded = expandedCategories.contains(position)
        holder.questionsRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.expandArrow.rotation = if (isExpanded) 180f else 0f // 🔽 Pfeil nach unten oder oben

        // Click Listener für Kategorie-Text UND Pfeil
        val toggleCategory: (View) -> Unit = {
            if (isExpanded) {
                expandedCategories.remove(position)
                val fadeOut = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.text_fade_out)
                holder.questionsRecyclerView.startAnimation(fadeOut)
            } else {
                expandedCategories.add(position)
                val fadeIn = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.text_fade_in)
                holder.questionsRecyclerView.startAnimation(fadeIn)
            }

            // Pfeil-Animation
            holder.expandArrow.animate().rotation(if (isExpanded) 0f else 180f).setDuration(200).start()

            notifyItemChanged(position)
        }

        // Pfeil und Kategorie-Text klickbar machen
        holder.categoryTitle.setOnClickListener(toggleCategory)
        holder.expandArrow.setOnClickListener(toggleCategory)
    }

    override fun getItemCount(): Int = categorizedFaqList.size
}
