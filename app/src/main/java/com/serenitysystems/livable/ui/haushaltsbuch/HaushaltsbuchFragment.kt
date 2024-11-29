package com.serenitysystems.livable.ui.haushaltsbuch

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.serenitysystems.livable.databinding.FragmentHaushaltsbuchBinding

class HaushaltsbuchFragment : Fragment() {

    private var _binding: FragmentHaushaltsbuchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHaushaltsbuchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val sectionsPagerAdapter = SectionsPagerAdapter(childFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SectionsPagerAdapter(fm: androidx.fragment.app.FragmentManager) :
        androidx.fragment.app.FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> EinnahmenFragment()
                1 -> EinnahmenÜbersichtFragment()
                2 -> AusgabenFragment()
                3 -> AusgabenÜbersichtFragment()
                else -> Fragment()
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> "Einnahmen"
                1 -> "Übersicht Einnahmen"
                2 -> "Ausgaben"
                3 -> "Übersicht Ausgaben"
                else -> ""
            }
        }

        override fun getCount(): Int {
            return 4
        }
    }
}
