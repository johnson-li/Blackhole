package com.xuebingli.blackhole.results

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.xuebingli.blackhole.PingActivity
import com.xuebingli.blackhole.R

class RawResultFragment : ResultFragment() {
    private lateinit var rawResultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity!!.getPreferences(Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == PingActivity.PING_RESULT_PREF_KEY) {
                    rawResultText.text = sharedPreferences.getString(key, "")
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_raw_result, container, false)
        rawResultText = view.findViewById(R.id.raw_result_text)
        rawResultText.movementMethod = ScrollingMovementMethod()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rawResultText.setText(R.string.ping_tab_result_raw)
    }
}