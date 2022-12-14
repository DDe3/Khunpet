package com.example.khunpet.ui.fragments

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.khunpet.R
import com.example.khunpet.controllers.view_models.LostAndFoundViewModel
import com.example.khunpet.databinding.FragmentLostAndFoundBinding
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor

class LostAndFoundFragment : Fragment() {

    private val viewModel: LostAndFoundViewModel by viewModels()
    private var _binding: FragmentLostAndFoundBinding? = null
    private val binding get() = _binding!!

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val uri = it.data?.data!!
            var bitmap = uriToBitmap(uri, requireContext())
            var newBitmap = resizePhoto(bitmap)
            var newUri = bitmapToUri(newBitmap, requireContext())
            viewModel.imageUri.postValue(newUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLostAndFoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.customCheck.isChecked = true

        binding.uploadImageButton.setOnClickListener {
            ImagePicker.with(requireActivity())
                .provider(ImageProvider.BOTH)
                .crop(224f,224f)
                .createIntentFromDialog { launcher.launch(it) }
        }

        binding.requestButton.setOnClickListener {
            uploadImage()
        }

        viewModel.imageUri.observe(viewLifecycleOwner) {
            loadWithPicasso(it)
        }

        viewModel.loading.observe(viewLifecycleOwner) { bool ->
            if (bool) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        binding.customCheck.setOnClickListener {
            binding.vggCheck.isChecked = false
            binding.disCheck.isChecked = false
            uncheckCheckboxes(binding.customCheck.isChecked, binding.vggCheck.isChecked, binding.disCheck.isChecked)
        }
        binding.vggCheck.setOnClickListener {
            binding.customCheck.isChecked = false
            binding.disCheck.isChecked = false
            uncheckCheckboxes(binding.customCheck.isChecked, binding.vggCheck.isChecked, binding.disCheck.isChecked)
        }
        binding.disCheck.setOnClickListener {
            binding.vggCheck.isChecked = false
            binding.customCheck.isChecked = false
            uncheckCheckboxes(binding.customCheck.isChecked, binding.vggCheck.isChecked, binding.disCheck.isChecked)
        }

        viewModel.model.observe(viewLifecycleOwner) {
            Log.d("Model", it.toString())
        }
    }

    private fun uncheckCheckboxes(custom : Boolean, vgg : Boolean, deep : Boolean) {
        if (custom) {
            viewModel.model.postValue(1)
        }
        else if (vgg) {
            viewModel.model.postValue(2)
        }
        else if (deep) {
            viewModel.model.postValue(3)
        }
    }

    fun resizePhoto(bitmap: Bitmap): Bitmap {
        val w = 224
        val h = 224
        return Bitmap.createScaledBitmap(bitmap, w, h, false)
    }

    fun uriToBitmap(uri: Uri, context: Context): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor? = context.contentResolver
            .openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor?.fileDescriptor!!
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    private fun bitmapToUri(inImage: Bitmap, inContext: Context): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun uploadImage() {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Cargando imagen...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        if (viewModel.imageUri.value != Uri.EMPTY) {
            viewModel.uploadImageToFirebaseStorage(requireContext())
            Toast.makeText(requireContext(), "Imagen subida con exito", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Escoge una imagen primero", Toast.LENGTH_SHORT).show()
        }
        if (progressDialog.isShowing) progressDialog.dismiss()

    }


    private fun loadWithPicasso(uri: Uri) {
        if (uri != Uri.EMPTY) {
            Picasso.get()
                .load(uri)
                .fit().into(binding.uploadedImageView)
        } else  {
            Picasso.get()
                .load(R.drawable.place_holder)
                .fit().into(binding.uploadedImageView)
        }

    }

}