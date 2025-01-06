package com.serenitysystems.livable.ui.userprofil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.serenitysystems.livable.R

class ProfilansichtFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Layout für dieses Fragment aufblähen
        return inflater.inflate(R.layout.fragment_profilansicht, container, false)
    }
}
