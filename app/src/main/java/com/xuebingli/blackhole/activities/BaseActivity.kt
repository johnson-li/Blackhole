package com.xuebingli.blackhole.activities

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.xuebingli.blackhole.MyApplication
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.dialog.*
import com.xuebingli.blackhole.restful.Response
import com.xuebingli.blackhole.restful.ServerApi
import com.xuebingli.blackhole.restful.Status
import com.xuebingli.blackhole.services.ForegroundService
import com.xuebingli.blackhole.utils.ConfigUtils
import com.xuebingli.blackhole.utils.Preferences
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.ConnectException
import java.net.SocketTimeoutException


open class BaseActivity(
    private val displayHomeAsUp: Boolean = true,
    private val bindService: Boolean = false,
    private val parameters: List<Pair<String, (SharedPreferences) -> String>> = listOf(),
) : AppCompatActivity() {
    lateinit var sharedPreferences: SharedPreferences
    var foregroundService: ForegroundService? = null
    val disposables = CompositeDisposable()
    val serverApi: ServerApi
        get() = (application as MyApplication).serverApi

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as ForegroundService.ForegroundBinder
            foregroundService = binder.getService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            foregroundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = applicationContext.getSharedPreferences(
            Preferences.PREFERENCE_NAME,
            Context.MODE_PRIVATE
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUp)
        if (bindService) {
            Intent(this, ForegroundService::class.java).also {
                this.bindService(it, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear_data -> {
                ClearDataDialog {
                    ConfigUtils(this).getDataDir().listFiles()?.forEach {
                        if (it.isFile) {
                            it.delete()
                        }
                    }
                }.show(supportFragmentManager, "Clear data dialog")
                true
            }
            R.id.set_interface -> {
                InterfacePicker()
                    .show(supportFragmentManager, "interface picker")
                true
            }
            R.id.set_pour_mode -> {
                PourModePicker {
                    sharedPreferences.edit(true) {
                        putString(Preferences.POUR_MODE_KEY, it.name)
                    }
                }.show(supportFragmentManager, "Pour mode picker")
                true
            }
            R.id.set_sink_mode -> {
                SinkModePicker {
                    sharedPreferences.edit(true) {
                        putString(Preferences.SINK_MODE_KEY, it.name)
                    }
                }.show(supportFragmentManager, "Sink mode picker")
                true
            }
            R.id.set_bitrate -> {
                BitratePicker {
                    sharedPreferences.edit(true) {
                        putInt(Preferences.POUR_BITRATE_KEY, it)
                    }
                }.show(supportFragmentManager, "Bitrate picker")
                true
            }
            R.id.set_bitrate_sink -> {
                BitratePicker {
                    sharedPreferences.edit(true) {
                        putInt(Preferences.SINK_BITRATE_KEY, it)
                    }
                }.show(supportFragmentManager, "Bitrate picker")
                true
            }
            R.id.set_duration -> {
                DurationPicker {
                    sharedPreferences.edit(true) {
                        putInt(Preferences.DURATION_KEY, it)
                    }
                }.show(supportFragmentManager, "Duration picker")
                true
            }
            R.id.show_parameters -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.parameters)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(parameters.joinToString("\n") {
                        "${it.first}: ${it.second(sharedPreferences)}"
                    })
                    .create().show()
                true
            }
            R.id.show_data_folder -> {
//                val intent = Intent(Intent.ACTION_VIEW)
//                val dir = FileProvider.getUriForFile(
//                    this,
//                    "$packageName.provider",
//                    ConfigUtils(this).getDataDir()
//                )
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                intent.setDataAndType(dir, "application/*")
//                startActivity(intent)
                true
            }
            R.id.set_packet_size -> {
                PacketSizePicker().show(supportFragmentManager, "Packet size picker")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun <T> subscribe(single: Single<T>, onSuccess: (T) -> Unit): Disposable {
        return subscribe(single, onSuccess, {})
    }

    fun <T> subscribe(single: Single<T>, onSuccess: (T) -> Unit, onFail: () -> Unit): Disposable {
        return subscribe0(single, {
            if (it is Response) {
                if (it.status == Status.SUCCESS) {
                    onSuccess(it)
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                onSuccess(it)
            }
        }, {
            when (it) {
                is ConnectException -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
                is SocketTimeoutException -> {
                    Log.w("johnson", it.message ?: "timeout")
                }
                else -> {
                    Log.e("johnson", it.message, it)
                }
            }
            onFail()
        })
    }

    private fun <T> subscribe0(
        single: Single<T>,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit
    ): Disposable {
        return single.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
            .subscribe(
                onSuccess, onError
            ).also { disposables.add(it) }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }
}