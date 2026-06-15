package com.budgetmate.app.ui.transactions

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.budgetmate.app.databinding.FragmentPhotoViewerBinding

/**
 * Full-screen photo viewer.
 * Receives the photo URI via navigation arguments (key: "photoUri").
 */
class PhotoViewerFragment : Fragment() {

    private var _binding: FragmentPhotoViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhotoViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uriString = arguments?.getString("photoUri") ?: return
        Glide.with(this).load(Uri.parse(uriString)).into(binding.ivFullPhoto)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
