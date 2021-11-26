package sk.jojo.parkovanie_app.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class IdNumberViewModel: ViewModel() {

    private val TAG = "IdNumberViewModel"

    private var auth: FirebaseAuth
    private var currentUser: FirebaseUser
    private var db: FirebaseFirestore


    private var address: String = ""
    private var city: String = ""

    private var _minIdNumber = MutableLiveData<Int>()
    val minIdNumber: LiveData<Int>
        get() = _minIdNumber

    private var _maxIdNumber = MutableLiveData<Int>()
    val maxIdNumber: LiveData<Int>
        get() = _maxIdNumber


    fun setAddress(address: String){
        this.address = address
    }

    fun setCity(city: String){
        this.city = city
    }

    init{

        // Initialize Firebase Auth
        auth = Firebase.auth
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        currentUser = auth.currentUser
        db = FirebaseFirestore.getInstance()


    }

    fun readFromDB(){
        var i = 0
        db.collection(city).document(address)
            .collection("idNumber")
            .get()
            .addOnSuccessListener { documents ->
                for(document in documents){
                    Log.d(TAG, "${document.id} => ${document.data}")
                    if(i == 0){
                        _minIdNumber.value = document.id.toInt()
                    }
                    _maxIdNumber.value = document.id.toInt()
                    i++
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting history_of_reservation documents: ", exception)
            }

    }

}