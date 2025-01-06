package com.serenitysystems.livable.ui.userprofil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.serenitysystems.livable.R

class ProfilansichtFragment : Fragment() {

    private val viewModel: ProfilansichtViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profilansicht, container, false)

        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val emailText: TextView = view.findViewById(R.id.emailText)
        val birthdateText: TextView = view.findViewById(R.id.birthdateText)
        val genderText: TextView = view.findViewById(R.id.genderText)
        val roleText: TextView = view.findViewById(R.id.roleText)

        viewModel.profileImage.observe(viewLifecycleOwner, Observer { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.pp)
                .error(R.drawable.pp)
                .into(profileImage)
        })

        viewModel.username.observe(viewLifecycleOwner, Observer { username ->
            usernameText.text = username ?: "N/A"
        })

        viewModel.email.observe(viewLifecycleOwner, Observer { email ->
            emailText.text = email ?: "N/A"
        })

        viewModel.birthdate.observe(viewLifecycleOwner, Observer { birthdate ->
            birthdateText.text = birthdate ?: "N/A"
        })

        viewModel.gender.observe(viewLifecycleOwner, Observer { gender ->
            genderText.text = gender ?: "N/A"
        })

        viewModel.role.observe(viewLifecycleOwner, Observer { role ->
            roleText.text = role ?: "N/A"
        })

        return view
    }
}
