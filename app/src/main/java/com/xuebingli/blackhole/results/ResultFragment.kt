package com.xuebingli.blackhole.results

import androidx.core.util.Pair
import androidx.fragment.app.Fragment

open class ResultFragment : Fragment()

class ResultFragmentPair(first: String, second: () -> ResultFragment) :
    Pair<String, () -> ResultFragment>(first, second) {

    fun createInstance(): ResultFragment {
        return second!!.invoke()
    }
}
