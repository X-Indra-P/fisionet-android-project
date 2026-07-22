package com.project.fisionettest.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.databinding.FragmentCashierMenuBinding

class CashierMenuFragment : Fragment() {

    private var _binding: FragmentCashierMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardAddTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_cashierMenu_to_addTransaction)
        }

        binding.cardHistory.setOnClickListener {
            findNavController().navigate(R.id.action_cashierMenu_to_history)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
