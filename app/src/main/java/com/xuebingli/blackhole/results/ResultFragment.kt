package com.xuebingli.blackhole.results

import androidx.core.util.Pair
import androidx.fragment.app.Fragment

open class ResultFragment : Fragment() {
    open fun onDataInserted(index: Int) {}

    open fun onDataReset() {}

    open fun onFinished() {}
}

class ResultFragmentPair(first: String, second: () -> ResultFragment) :
    Pair<String, () -> ResultFragment>(first, second) {

    fun createInstance(): ResultFragment {
        return second!!.invoke()
    }
}
