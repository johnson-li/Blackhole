package com.xuebingli.blackhole.activities

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xuebingli.blackhole.MyApplication
import com.xuebingli.blackhole.R
import com.xuebingli.blackhole.dialog.ClearDataDialog
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
    private val bindService: Boolean = false
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