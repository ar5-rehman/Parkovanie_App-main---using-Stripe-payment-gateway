package sk.jojo.parkovanie_app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import sk.jojo.parkovanie_app.R
import sk.jojo.parkovanie_app.credit.CreditActivity
import sk.jojo.parkovanie_app.databinding.FragmentSecondBuyingTicketBinding
import sk.jojo.parkovanie_app.view_model.IdNumberViewModel
import sk.jojo.parkovanie_app.view_model.MainViewModel
import sk.jojo.parkovanie_app.view_model.PriceViewModel


class SecondBuyingTicketFragment : Fragment() {

    private val TAG = "sk.secondBuyingTicket"
    private lateinit var binding: FragmentSecondBuyingTicketBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var idNumberViewModel: IdNumberViewModel
    private lateinit var priceViewModel: PriceViewModel
    private var min: Int = 0
    private var max: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_second_buying_ticket, container, false)

        idNumberViewModel = ViewModelProvider(requireActivity()).get(IdNumberViewModel::class.java)
        priceViewModel = ViewModelProvider(requireActivity()).get(PriceViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        binding.viewModel = mainViewModel

        val numberOfParking = binding.numberOfParking.text

        binding.depositeMoneyBtn.setOnClickListener {
            val intent = Intent(requireActivity(), CreditActivity::class.java)
            startActivity(intent)
        }

        idNumberViewModel.minIdNumber.observe(viewLifecycleOwner, Observer {
            min = it
        })
        idNumberViewModel.maxIdNumber.observe(viewLifecycleOwner, Observer {
            max = it
        })

        binding.buyingBtn30.setOnClickListener {
            conditionsBeforeNewBuy(numberOfParking,1L)
        }

        binding.buyingBtn45.setOnClickListener {
            conditionsBeforeNewBuy(numberOfParking,45L)
        }
        binding.buyingBtn60.setOnClickListener {
            conditionsBeforeNewBuy(numberOfParking,60L)
        }

//        mainViewModel.city.observe(viewLifecycleOwner, Observer {
//            priceViewModel.readPriceFromDB(it,mainViewModel.address.value.toString())
//        })

        priceViewModel.price30.observe(viewLifecycleOwner, Observer {
            binding.price30.text = getString(R.string.price30, it)
        })

        priceViewModel.price45.observe(viewLifecycleOwner, Observer {
            binding.price45.text = getString(R.string.price45, it)
        })

        priceViewModel.price60.observe(viewLifecycleOwner, Observer {
            binding.price60.text = getString(R.string.price60, it)
        })

        return binding.root
    }

    private fun comeBackToMainPage() {
        view?.findNavController()?.navigate(
            SecondBuyingTicketFragmentDirections.actionSecondBuyingTicketFragmentToMainPageFragment()
        )
    }

    private fun isEmptynumberOfParking(numberOfParking: Editable): Boolean{
        return numberOfParking.isEmpty() || numberOfParking.toString().toInt() <= 0
    }

    private fun isInRange(numberOfParking: Editable): Boolean{
        return (numberOfParking.toString().toInt() >= min) && (numberOfParking.toString().toInt() <= max)
    }

    private fun buyingTicket(minute: Long){
        mainViewModel.buyNewButtonClicked(true)
        mainViewModel.buyButtonClicked(minute)                //Nastavovanie casu
        comeBackToMainPage()
    }

    private fun conditionsBeforeNewBuy(numberOfParking: Editable, minute: Long){
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