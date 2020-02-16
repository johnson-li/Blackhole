package com.xuebingli.blackhole.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.xuebingli.blackhole.R

class RawResultFragment : ResultFragment() {
    companion object {
        val name = "Raw Result"
    }
    lateinit var rawResultText: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_raw_result, container, false)
        rawResultText = view.findViewById(R.id.raw_result_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rawResultText.setText(R.string.ping_tab_result_raw)
    }
}