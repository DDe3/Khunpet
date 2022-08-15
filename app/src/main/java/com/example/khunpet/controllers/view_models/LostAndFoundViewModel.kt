package com.example.khunpet.controllers.view_models

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Video
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.khunpet.model.DeepImageSearchResponse
import com.example.khunpet.model.FlaskResponse
import com.example.khunpet.utils.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class LostAndFoundViewModel : ViewModel() {

    private val client = OkHttpClient()


    val imageUri : MutableLiveData<Uri> by lazy {
        MutableLiveData<Uri>()
    }

    val loading : MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val model : MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    init {
        imageUri.postValue(Uri.EMPTY)
        loading.postValue(false)
        model.postValue(1)
    }


    fun uploadImageToFirebaseStorage(context: Context) {
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val now = Date()
        val fileName = formatter.format(now)
        val storageReference = AppDatabase.getStorageReference()

        storageReference.getReference("temp/$fileName.jpg").putFile(imageUri.value!!)
            .addOnSuccessListener {
                imageUri.postValue(Uri.EMPTY)
                makeRequest(fileName)
            }
            .addOnFailureListener {
                imageUri.postValue(Uri.EMPTY)
            }

        loading.postValue(true)

    }

    private fun makeRequest(fileName: String) {
        var type = "custom";
        when(model.value) {
            1 -> {
               type = "custom"
            }
            2 -> {
                type = "vgg16"
            }
            3 -> {
                type = "deepimagesearch"
            }
            else -> type = "custom"
        }

        val request = Request.Builder()
            .url("http://192.168.100.143:5000/$type/$fileName.jpg")
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Response:",e.stackTraceToString())
                loading.postValue(false)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        Log.d("Response:","$name: $value")
                    }
                    val gson = Gson()
                    var lista : MutableList<FlaskResponse> = mutableListOf()
                    var listaDeepImageSearch = DeepImageSearchResponse()
                    if (type != "deepimagesearch") {
                        lista = gson.fromJson(response.body!!.string(), object : TypeToken<MutableList<FlaskResponse?>?>() {}.type)
                    } else {
                        listaDeepImageSearch = gson.fromJson(response.body!!.string(), DeepImageSearchResponse::class.java)
                    }

                    Log.d("Response",lista.toString())
                    Log.d("Response",listaDeepImageSearch.toString())
                    loading.postValue(false)
                }
            }
        })
    }





}