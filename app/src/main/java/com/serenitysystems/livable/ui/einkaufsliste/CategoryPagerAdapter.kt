package com.serenitysystems.livable.ui.einkaufsliste

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(fragment: Fragment, private val categories: List<String>) : FragmentStateAdapter(fragment) {

    private val fragments = mutableMapOf<Int, CategoryFragment>()

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        val category = categories[position]
        val fragment = CategoryFragment.newInstance(category)
        fragments[position] = fragment
        return fragment
    }

    fun getFragment(position: Int): CategoryFragment {
        return fragments[position]!!
    }
}
