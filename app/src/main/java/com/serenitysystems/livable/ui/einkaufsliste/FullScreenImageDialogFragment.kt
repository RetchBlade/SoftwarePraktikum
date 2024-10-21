package com.serenitysystems.livable.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.serenitysystems.livable.R

class FullScreenImageDialogFragment : DialogFragment() {

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bild-URI vom Bundle erhalten und in Uri umwandeln
        imageUri = arguments?.getString("imageUri")?.let { Uri.parse(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_fullscreen_image, container, false)

        // Vollbild-ImageView und Schließen-Button
        val imageView: ImageView = view.findViewById(R.id.fullscreenImageView)
        val closeButton: ImageView = view.findViewById(R.id.closeButton)

        // Bild anzeigen, falls ein URI vorhanden ist
        if (imageUri != null) {
            imageView.setImageURI(imageUri)
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
