package com.xuebingli.blackhole.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.restful.ProbingRecord
import com.xuebingli.blackhole.services.ProbingServiceListener
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Preferences
import com.xuebingli.blackhole.utils.getProbingDelayString

class ProbingActivity : BaseActivity(
    true, true, listOf(
        Pair(Preferences.PROBING_DELAY_KEY)
        { c -> getProbingDelayString(ConfigUtils(c).probingDelay) },
    )
) {
    private lateinit var probingButton: Button
    private lateinit var clientSent: TextView
    private lateinit var clientReceived: TextView
    private lateinit var serverSent: TextView
    private lateinit var serverReceived: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_probing)
        probingButton = findViewById(R.id.probing)
        clientSent = findViewById(R.id.client_sent)
        clientReceived = findViewById(R.id.client_received)
        serverSent = findViewById(R.id.server_sent)
        serverReceived = findViewById(R.id.server_received)
    }

    private fun startProbing() {
        probingService.startProbing(this)
    }

    private fun stopProbing() {
        probingService.stopProbing(this)
    }

    fun toggleProbing(view: View) {
        if (probingService.isProbing) {
            stopProbing()
        } else {
            startProbing()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_probing_activity, menu)
        return true
    }

    override fun onInitialized() {
        if (probingService.isProbing) {
            probingButton.setText(R.string.button_stop)
        }
        probingService.registerListener(object : ProbingServiceListener() {
            override fun onStarted() {
                probingButton.text = getString(R.string.button_stop)
            }

            override fun onStopped() {
                probingButton.text = getString(R.string.probing_button)
            }

            override fun onRecord(record: ProbingRecord) {
                val clientResult = probingService.result.clientResult
                clientSent.text = clientResult.sent.size.toString()
                if (clientResult.received.size > 0) {
                    val count = clientResult.received.size
                    val total = clientResult.received.last().sequence + 1
                    clientReceived.text =
                        "${count}/${total}, ${"%.1f".format(100.0 * count / total)}%"
                } else {
                    clientReceived.text =
                        "${clientResult.received.size.toString()}/0"
                }
            }
        })
    }
}