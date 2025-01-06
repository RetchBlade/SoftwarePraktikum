// FullScreenImageDialogFragment.kt
package com.serenitysystems.livable.ui

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.serenitysystems.livable.R

class FullScreenImageDialogFragment : DialogFragment() {

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bild-URI vom Bundle erhalten und in Uri umwandeln
        imageUri = arguments?.getString("imageUri")?.let { Uri.parse(it) }
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        // Vollbildmodus ohne Titelzeile
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Layout für den Dialog laden
        val view = inflater.inflate(R.layout.dialog_fullscreen_image, container, false)

        // Vollbild-ImageView und Schließen-Button
        val imageView: ImageView = view.findViewById(R.id.fullscreenImageView)
        val closeButton: ImageView = view.findViewById(R.id.closeButton)

        // Bild anzeigen, falls ein URI vorhanden ist
        imageUri?.let {
            Glide.with(requireContext())
                .load(it)
                .into(imageView)
        }

        // Schließen-Button für den Dialog
        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    companion object {
        // Erstellt eine neue Instanz des Dialogs mit dem Bild-Uri
        fun newInstance(imageUri: String): FullScreenImageDialogFragment {
            val args = Bundle()
            args.putString("imageUri", imageUri)
            val fragment = FullScreenImageDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
