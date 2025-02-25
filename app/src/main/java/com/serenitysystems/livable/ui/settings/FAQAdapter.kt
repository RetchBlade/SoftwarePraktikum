import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R

class FAQAdapter(private val faqList: List<Pair<String, String>>) :
    RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    private var expandedPosition = -1
    private var previousExpandedPosition = -1

    inner class FAQViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val question: TextView = itemView.findViewById(R.id.faqQuestion) // ✅ Stelle sicher, dass die ID existiert
        val answer: TextView = itemView.findViewById(R.id.faqAnswer) // ✅ ID muss in faq_item.xml existieren
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.faq_item, parent, false) // ✅ Stelle sicher, dass faq_item.xml verwendet wird!
        return FAQViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val (questionText, answerText) = faqList[position]
        holder.question.text = questionText
        holder.answer.text = answerText

        val isExpanded = position == expandedPosition
        val wasPreviouslyExpanded = position == previousExpandedPosition

        if (isExpanded) {
            holder.answer.visibility = View.VISIBLE
            val fadeIn = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.text_fade_in)
            fadeIn.duration = 300
            holder.answer.startAnimation(fadeIn)
        } else if (wasPreviouslyExpanded) {
            val fadeOut = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.text_fade_out)
            fadeOut.duration = 300
            holder.answer.startAnimation(fadeOut)
            holder.answer.visibility = View.GONE
        } else {
            holder.answer.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (expandedPosition != adapterPosition) {
                    previousExpandedPosition = expandedPosition
                    expandedPosition = adapterPosition
                } else {
                    expandedPosition = -1
                }
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = faqList.size
}
