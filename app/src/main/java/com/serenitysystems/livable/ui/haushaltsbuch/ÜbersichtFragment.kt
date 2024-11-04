package com.serenitysystems.livable.ui.haushaltsbuch.view

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.serenitysystems.livable.databinding.FragmentUebersichtBinding
import com.serenitysystems.livable.databinding.ItemCategoryDetailBinding
import com.serenitysystems.livable.ui.haushaltsbuch.viewmodel.HaushaltsbuchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ÜbersichtFragment : Fragment() {

    private var _binding: FragmentUebersichtBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUebersichtBinding.inflate(inflater, container, false)

        // Kontostand'u gözlemle
        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner) { kontostand ->
            binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
        }

        // Seçilmiş tarih için harcamaları gözlemle
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) {
            updateDiagramAndPanel()
        }

        // Tarih değişiklik butonlarını yapılandır
        binding.leftArrow.setOnClickListener {
            haushaltsbuchViewModel.changeDateByDays(-1)
            updateDateDisplay()
        }
        binding.rightArrow.setOnClickListener {
            haushaltsbuchViewModel.changeDateByDays(1)
            updateDateDisplay()
        }

        updateDateDisplay()

        return binding.root
    }

    private fun updateDiagramAndPanel() {
        // Arka planda çalışan bir coroutine başlat
        CoroutineScope(Dispatchers.Default).launch {
            val categories = haushaltsbuchViewModel.categories

            // CategoryData listesi oluştur
            val categoryDataList = categories.mapNotNull { category ->
                val amount = haushaltsbuchViewModel.getCategoryAmount(category)
                if (amount > 0f) {
                    val percentage = haushaltsbuchViewModel.getCategoryPercentage(category)
                    val color = haushaltsbuchViewModel.getCategoryColor(category)
                    CategoryData(
                        category = category,
                        amount = amount.toDouble(),
                        percentage = percentage,
                        color = color
                    )
                } else {
                    null
                }
            }

            Log.d("ÜbersichtFragment", "Anzahl der Kategorien: ${categoryDataList.size}")
            categoryDataList.forEach { data ->
                Log.d("ÜbersichtFragment", "Kategorie: ${data.category}, Betrag: ${data.amount}, Prozent: ${data.percentage}")
            }

            val categoriesForChart = categoryDataList.map { it.category }
            val valuesForChart = categoryDataList.map { it.amount }
            val colors = categoryDataList.map { it.color }

            withContext(Dispatchers.Main) {
                // PieChartView'u güncelle
                binding.pieChartView.setData(categoriesForChart, valuesForChart, colors)

                // categoriesListPanel içindeki mevcut öğeleri temizle
                binding.categoriesListPanel.removeAllViews()

                // Yeni öğeleri ekle
                val inflater = LayoutInflater.from(requireContext())
                categoryDataList.forEach { data ->
                    val itemBinding = ItemCategoryDetailBinding.inflate(inflater, binding.categoriesListPanel, false)

                    itemBinding.textViewCategoryLetter.text = data.category.first().toString().uppercase()
                    itemBinding.textViewCategoryName.text = data.category
                    itemBinding.textViewCategoryAmount.text = "${"%.2f".format(data.amount)} EUR"
                    itemBinding.textViewCategoryPercentage.text = "${"%.0f".format(data.percentage)}%"

                    // Arka plan rengini ayarla
                    val drawable = itemBinding.textViewCategoryLetter.background as? GradientDrawable
                    drawable?.setColor(data.color)

                    // Kutu arka planını ayarla (Transparan yapıldı)
                    val categoryItemBackground = itemBinding.root.background as? GradientDrawable
                    categoryItemBackground?.setColor(Color.TRANSPARENT) // Arka planı transparan yapın

                    // Öğeyi panel'e ekle
                    binding.categoriesListPanel.addView(itemBinding.root)
                }

                // Panelin görünürlüğünü kontrol et
                if (categoryDataList.isNotEmpty()) {
                    binding.categoriesScrollView.visibility = View.VISIBLE
                } else {
                    binding.categoriesScrollView.visibility = View.GONE
                }
            }
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedDateStr = dateFormat.format(haushaltsbuchViewModel.selectedDate.time)
        binding.dateTextView.text = selectedDateStr
        haushaltsbuchViewModel.loadExpensesForDate(selectedDateStr)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // CategoryData veri sınıfı
    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
