package com.serenitysystems.livable.ui.haushaltsbuch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentUebersichtBinding

class ÜbersichtFragment : Fragment() {

    private var _binding: FragmentUebersichtBinding? = null
    private val binding get() = _binding!!
    private lateinit var haushaltsbuchViewModel: HaushaltsbuchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        haushaltsbuchViewModel =
            ViewModelProvider(requireActivity()).get(HaushaltsbuchViewModel::class.java)

        _binding = FragmentUebersichtBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Kontostand güncellemesi
        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner, Observer { kontostand ->
            binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
        })

        // Harcamalar ve Gelirler üzerinden Kategori Yüzdelerini Hesaplama ve PieChart Güncelleme
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner, Observer { expenses ->
            val categories = haushaltsbuchViewModel.getCategories()
            val colors = categories.map { category ->
                android.graphics.Color.parseColor(haushaltsbuchViewModel.getCategoryColor(category))
            }
            val percentages = categories.map { category ->
                haushaltsbuchViewModel.getCategoryPercentage(category)
            }
            binding.pieChartView.setData(categories, percentages.map { it / 100 }, colors)

            // Kategori yüzdeleri için liste güncelleme
            binding.categoriesList.removeAllViews()
            for (i in categories.indices) {
                if (percentages[i] > 0) {
                    val containerView = LayoutInflater.from(context).inflate(R.layout.item_category_percentage, binding.categoriesList, false)
                    val categoryTextView = containerView.findViewById<TextView>(R.id.textViewCategory)
                    categoryTextView.text = "${categories[i]}: ${"%.2f".format(percentages[i])}%"
                    binding.categoriesList.addView(containerView)
                }
            }
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
