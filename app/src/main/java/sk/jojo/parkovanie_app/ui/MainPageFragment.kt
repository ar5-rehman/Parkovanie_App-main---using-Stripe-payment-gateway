package sk.jojo.parkovanie_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import sk.jojo.parkovanie_app.LoginActivity
import sk.jojo.parkovanie_app.R
import sk.jojo.parkovanie_app.credit.CreditActivity
import sk.jojo.parkovanie_app.databinding.FragmentMainPageBinding
import sk.jojo.parkovanie_app.view_model.MainViewModel
import sk.jojo.parkovanie_app.view_model.PriceViewModel


class MainPageFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        val priceViewModel = ViewModelProvider(requireActivity()).get(PriceViewModel::class.java)

//        viewModel.readTimeFromDB()

        val binding: FragmentMainPageBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_main_page, container, false)

        binding.viewModel = viewModel


        binding.depositeMoneyBtn.setOnClickListener {
            val intent = Intent(requireActivity(), CreditActivity::class.java)
            startActivity(intent)
        }

        viewModel.timeLeft.observe(viewLifecycleOwner, Observer {
            binding.timeText.text = getString(R.string.time_left,it)
            if(!it.equals("")){
                binding.timeText.setVisibility(View.VISIBLE)
            }
            else
                binding.timeText.setVisibility(View.GONE)
        })


        viewModel.ticketText.observe(viewLifecycleOwner, Observer {
            binding.ticketText.text = it.toString()
        })

        binding.buyBtn.setOnClickListener { view: View ->
            if(binding.buyBtn.text.equals("Kúpiť")){
                view.findNavController().navigate(
                    MainPageFragmentDirections.actionMainPageFragmentToMapFragment()
                )
            }
            else{
                priceViewModel.readPriceForActualAddress()
                viewModel.readAddressFromActualReservation()
                view.findNavController().navigate(
                    MainPageFragmentDirections.actionMainPageFragmentToBuyingTicketFragment()
                )
            }
        }

        viewModel.isTimerStarted.observe(viewLifecycleOwner, Observer {
            if(it){
                binding.buyBtn.text = "Predĺžiť"
                binding.buyNewBtn.setVisibility(View.VISIBLE)
                binding.buyNewText.setVisibility(View.VISIBLE)
            }
            else {
                binding.buyBtn.text = "Kúpiť"
                binding.buyNewBtn.setVisibility(View.GONE)
                binding.buyNewText.setVisibility(View.GONE)
            }
        })

        binding.buyNewBtn.setOnClickListener { view: View ->
            view.findNavController().navigate(
                MainPageFragmentDirections.actionMainPageFragmentToMapFragment()
            )
//            viewModel.buyNewButtonClicked()
        }


////        binding.ticketText.text  = "Listok plati este na:\n\n 30 s"
//        binding.timeText.setVisibility(View.GONE)
//
//        binding.depositeMoneyBtn.setOnClickListener {
//            binding.ticketText.text  = "Listok plati este na:"
//            binding.timeText.setVisibility(View.VISIBLE)
//        }
//        binding.buyBtn.setOnClickListener { view: View ->
//            view.findNavController().navigate(
//                MainPageFragmentDirections.actionMainPageFragmentToBuyingTicketFragment()
//            )
//        }

        return binding.root
    }


}