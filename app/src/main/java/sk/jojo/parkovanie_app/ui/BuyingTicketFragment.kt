package sk.jojo.parkovanie_app.ui

//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import sk.jojo.parkovanie_app.R
import sk.jojo.parkovanie_app.credit.CreditActivity
import sk.jojo.parkovanie_app.databinding.FragmentBuyingTicketBinding
import sk.jojo.parkovanie_app.view_model.IdNumberViewModel
import sk.jojo.parkovanie_app.view_model.MainViewModel
import sk.jojo.parkovanie_app.view_model.PriceViewModel


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class BuyingTicketFragment : Fragment() {


    private val TAG = "BuyingTicketFragment"
    private lateinit var binding: FragmentBuyingTicketBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var idNumberViewModel: IdNumberViewModel
    private lateinit var priceViewModel: PriceViewModel
    private var isTimeStarted: Boolean = false
    private var min: Int = 0
    private var max: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_buying_ticket, container, false)

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        idNumberViewModel = ViewModelProvider(requireActivity()).get(IdNumberViewModel::class.java)
        priceViewModel = ViewModelProvider(requireActivity()).get(PriceViewModel::class.java)

        binding.viewModel = mainViewModel

        val numberOfParking = binding.numberOfParking.text

        binding.depositeMoneyBtn.setOnClickListener {
            val intent = Intent(requireActivity(), CreditActivity::class.java)
            startActivity(intent)
        }

//        mainViewModel.city.observe(viewLifecycleOwner, Observer {
//            priceViewModel.readPriceFromDB(it,mainViewModel.address.value.toString())
//        })

        idNumberViewModel.minIdNumber.observe(viewLifecycleOwner, Observer {
            min = it
        })
        idNumberViewModel.maxIdNumber.observe(viewLifecycleOwner, Observer {
            max = it
        })

        mainViewModel.isTimerStarted.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            isTimeStarted = it
        })

        mainViewModel.isTimerStarted.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(it){
                binding.numberOfParking.setVisibility(View.GONE)
                binding.numberOfParkingHeadLine.text = "Máte zakúpený lístok"
            }
            else{
                binding.numberOfParking.setVisibility(View.VISIBLE)
                binding.numberOfParkingHeadLine.text = "Zadaj číslo parkovacieho miesta"
            }
        })

        priceViewModel.price30.observe(viewLifecycleOwner, Observer {
            binding.price30.text = getString(R.string.price30, it)
        })

        priceViewModel.price45.observe(viewLifecycleOwner, Observer {
            binding.price45.text = getString(R.string.price45, it)
        })

        priceViewModel.price60.observe(viewLifecycleOwner, Observer {
            binding.price60.text = getString(R.string.price60, it)
        })

        binding.buyingBtn30.setOnClickListener {
            if(isTimeStarted){
                buyingTicket(1L)
            }
            else{
                conditionsBeforeFirstBuy(numberOfParking, 1L)
            }
        }

        binding.buyingBtn45.setOnClickListener {
            if(isTimeStarted){
                buyingTicket(45L)
            }
            else{
                conditionsBeforeFirstBuy(numberOfParking, 45L)
            }
        }
        binding.buyingBtn60.setOnClickListener {
            if(isTimeStarted){
                buyingTicket(60L)
            }
            else{
                conditionsBeforeFirstBuy(numberOfParking, 60L)
            }
        }
        return binding.root
    }

    private fun comeBackToMainPage() {
        view?.findNavController()?.navigate(
                BuyingTicketFragmentDirections.actionBuyingTicketFragmentToMainPageFragment()
        )
    }

    private fun isEmptynumberOfParking(numberOfParking: Editable): Boolean{
        return numberOfParking.isEmpty() || numberOfParking.toString().toInt() <= 0
    }

    private fun isInRange(numberOfParking: Editable): Boolean{
        return (numberOfParking.toString().toInt() >= min) && (numberOfParking.toString().toInt() <= max)
    }

    private fun buyingTicket(minute: Long){
            mainViewModel.buyNewButtonClicked(false)
            mainViewModel.buyButtonClicked(minute)                //Nastavovanie casu
            comeBackToMainPage()
    }

    private fun conditionsBeforeFirstBuy(numberOfParking: Editable, minute: Long){
        if (isEmptynumberOfParking(numberOfParking)) {
            Toast.makeText(context, "Please insert all data", Toast.LENGTH_LONG).show()
        }
        else if(!isInRange(numberOfParking)){
            Toast.makeText(context, "Zadal si nesprávne číslo parkovacieho miesta", Toast.LENGTH_LONG).show()
        }
        else {
            mainViewModel.setNumberOfParking(numberOfParking)
            buyingTicket(minute)
        }
    }

}