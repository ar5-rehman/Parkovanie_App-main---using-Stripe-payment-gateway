package sk.jojo.parkovanie_app.view_model


import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.text.Editable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import sk.jojo.parkovanie_app.MainPageActivity
import sk.jojo.parkovanie_app.database.DatabaseService
import sk.jojo.parkovanie_app.database.IDatabaseService
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*


class MainViewModel: ViewModel() {

    private val TAG = "MainViewModel"

    private var auth: FirebaseAuth
    private var currentUser: FirebaseUser
    private var db: FirebaseFirestore

    private val _timeLeft = MutableLiveData<String>()
    val timeLeft: LiveData<String>
        get() = _timeLeft

    private val _secondsLeft = MutableLiveData<Long>()
    val secondsLeft: LiveData<Long>
        get() = _secondsLeft

    private val _ticketText = MutableLiveData<String>()
    val ticketText: LiveData<String>
        get() = _ticketText

    private var _isTimerStarted = MutableLiveData<Boolean>()
    val isTimerStarted: LiveData<Boolean>
        get() = _isTimerStarted

    private var _hours = MutableLiveData<Long>()
    val hours: LiveData<Long>
        get() = _hours

    private var _minutes = MutableLiveData<Long>()
    val minutes: LiveData<Long>
        get() = _minutes

    private var _seconds = MutableLiveData<Long>()
    val seconds: LiveData<Long>
        get() = _seconds

    private var _isBuyNewButtonClicked = MutableLiveData<Boolean>()
    val isBuyNewButtonClicked: LiveData<Boolean>
        get() = _isBuyNewButtonClicked

    private var _address = MutableLiveData<String>()
    val address: LiveData<String>
        get() = _address

    private var _city = MutableLiveData<String>()
    val city: LiveData<String>
        get() = _city


    private lateinit var _cTimer: CountDownTimer
    private var numberOfParking: Int = 0

    val databaseService = DatabaseService()


    init{
        _secondsLeft.value = 0
        _timeLeft.value = ""
        _isTimerStarted.value = false
        _isBuyNewButtonClicked.value = false

        // Initialize Firebase Auth
        auth = Firebase.auth
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        currentUser = auth.currentUser
        db = FirebaseFirestore.getInstance()


    }

    fun setAddress(address: String){
        _address.value = address
    }

    fun setCity(city: String){
        _city.value = city
    }


    private fun initCountDownTimer(milisInFuture: Long) {
        _cTimer = object : CountDownTimer(milisInFuture, 1000){
            override fun onFinish() {
                _timeLeft.value = ""
                stopTimer()
            }

            override fun onTick(milisToFinish: Long) {
                _secondsLeft.value = (milisToFinish/1000)
                _hours.value = ((milisToFinish / 1000) / 60 / 60)
                _minutes.value = ((milisToFinish / 1000) / 60) - (_hours.value!!.times(60))
                _seconds.value = (milisToFinish / 1000) - (_minutes.value!!.times(60) + _hours.value!!.times(60).times(60))

                _timeLeft.value = String.format( "%02d:%02d:%02d", _hours.value,_minutes.value,_seconds.value)

            }
        }
    }


    private fun startTimer() {
        _isTimerStarted.value = true
        _cTimer.start()
        _ticketText.value = "Lístok platí ešte na:"

    }

    private fun stopTimer() {
        _isBuyNewButtonClicked.value = false
        _isTimerStarted.value = false
        _cTimer.cancel()
        _ticketText.value = "Nemáte zakúpený žiadny lístok"
    }


    fun buyButtonClicked(minute: Long, ) {

        if(_isBuyNewButtonClicked.value!!){
            stopTimer()
            _timeLeft.value = ""
        }
        if (!_isTimerStarted.value!!) {
            _secondsLeft.value = minute * 60 * 1000
            writingAllInfoToDatabse(minute)
            initCountDownTimer(_secondsLeft.value as Long)
            startTimer()
        }
        else{
            databaseService.updateActiveResevation(minute, _city.value.toString(), _address.value.toString())

            _secondsLeft.value = (_secondsLeft.value?.times(1000))?.plus(minute * 60 * 1000)
            stopTimer()
            initCountDownTimer(_secondsLeft.value as Long)
            startTimer()
        }
    }

    fun setNumberOfParking(numberOfParking: Editable){
        this.numberOfParking = numberOfParking.toString().toInt()
    }

    fun buyNewButtonClicked(value: Boolean){
        _isBuyNewButtonClicked.value = value
    }


    @SuppressLint("SimpleDateFormat")
    private fun writingAllInfoToDatabse(minute: Long){
        val sdf = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss")
        val currentDateandTime: String = sdf.format(Date())

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val date: Date = sdf.parse(currentDateandTime)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, minute.toInt())


        val reservation = hashMapOf<String, Any>(
            "start" to Timestamp(Date()),
            "end" to Timestamp(calendar.time),
            "city" to city.value.toString(),
            "address" to address.value.toString(),
            "idNumber" to this.numberOfParking
        )

        databaseService.writingToActiveReservation(reservation)
        databaseService.writingToHistoryOfReservation(reservation)
        databaseService.writingReservationToAddress(_city.value.toString(), _address.value.toString(), numberOfParking, calendar.time)
    }

    @SuppressLint("SimpleDateFormat")
    fun readTimeFromDB(){
        db.collection("users_reservation").document(currentUser.uid)
            .collection("active_reservation").document("reservation")
            .get()
            .addOnSuccessListener { result ->
                val resultdb = result.data
                val time = resultdb?.get("end") as Timestamp

                val sdf = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss")
                val currentDateandTime: String = sdf.format(Date())

                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                val date: Date = sdf.parse(currentDateandTime)
                val calendar = Calendar.getInstance()
                calendar.time = date

                /**
                 * ak je calendar.time mensi vrati 1
                 * ak je calendar.time rovny vrati 0
                 * ak je calendar.time vacsi vrati -1
                 */

                Log.i(TAG, calendar.time.toString())
                Log.i(TAG, time.compareTo(Timestamp(calendar.time)).toString())
                when(time.compareTo(Timestamp(calendar.time))){
                    1 -> {
                        val leftTime = (time.seconds * 1000 + time.nanoseconds / 1000000) - (Timestamp(calendar.time).seconds * 1000 + Timestamp(calendar.time).nanoseconds/1000000)
                        Log.i(TAG,"Zostavajuci cas v milisekundach " + leftTime.toString())
                        _hours.value = ((leftTime / 1000) / 60 / 60)
                        _minutes.value = ((leftTime / 1000) / 60) - (_hours.value!!.times(60))
                        _seconds.value = (leftTime / 1000) - (_minutes.value!!.times(60) + _hours.value!!.times(60).times(60))
                        _secondsLeft.value = leftTime
                        initCountDownTimer(_secondsLeft.value as Long)
                        startTimer()

                        _address.value = result.getString("address")!!
                        _city.value = result.getString("city")!!
                        Log.i(TAG, "Zostavajuci cas: Hodiny ${hours.value} minuty ${minutes.value} sekundy ${seconds.value}")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting active_reservation documents.", exception)
            }
    }

    fun readAddressFromActualReservation(){
        db.collection("users_reservation").document(currentUser.uid)
            .collection("active_reservation").document("reservation")
            .get()
            .addOnSuccessListener { result->
                _address.value = result.getString("address")!!
                _city.value = result.getString("city")!!
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting active_reservation documents.", exception)
            }
    }
}
