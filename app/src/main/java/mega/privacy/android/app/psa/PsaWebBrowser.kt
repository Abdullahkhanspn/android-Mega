package mega.privacy.android.app.psa

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.databinding.FragmentPsaWebBrowserBinding
import mega.privacy.android.app.psa.PsaManager.dismissPsa
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.isURLSanitized
import timber.log.Timber

class PsaWebBrowser : Fragment() {
    private var binding: FragmentPsaWebBrowserBinding? = null

    private var psaId = Constants.INVALID_VALUE

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return FragmentPsaWebBrowserBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    @SuppressLint("SetJavaScriptEnabled", "HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.domStorageEnabled = true

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        binding.webView.addJavascriptInterface(this, JS_INTERFACE)
        loadPsa(
            binding.webView,
            requireArguments().getString(ARGS_URL_KEY).orEmpty(),
            requireArguments().getInt(ARGS_ID_KEY)
        )
    }

    override fun onDestroyView() {
        binding?.webView?.run {
            removeJavascriptInterface(JS_INTERFACE)
            destroy()
        }
        binding = null
        super.onDestroyView()
    }

    private fun loadPsa(webView: WebView, url: String, psaId: Int) {
        webView.visibility = View.INVISIBLE
        this.psaId = psaId

        try {
            if (!url.isURLSanitized()) {
                throw RuntimeException("PsaWebBrowser (Psa id: $psaId): Vulnerable/Malicious Url detected: $url")
            }
            val megaApi = MegaApplication.getInstance().megaApi
            val myUserHandle = megaApi.myUserHandle ?: return

            // This is the same way SDK getting device id:
            // https://github.com/meganz/sdk/blob/develop/src/posix/fs.cpp#L1575
            // and we find out the in param of `PosixFileSystemAccess::statsid` is an empty string
            // through debugging.
            val androidId =
                Settings.Secure.getString(requireContext().contentResolver, "android_id")
            val finalUrl = "$url/$myUserHandle?$androidId"
            webView.loadUrl(finalUrl)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * JS interface to show the PSA.
     */
    @JavascriptInterface
    fun showPSA() {
        // Due to the possible delay introduced by JS showPSA,
        // If the activity is no longer the activity sit on the top at the moment
        // then don't show psa on it. Show psa even if the app(activity task) is already on the background.
        if (!Util.isTopActivity(activity?.javaClass?.name, requireContext())) {
            hidePSA()
            return
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, onBackPressedCallback
        )
        uiHandler.post {
            binding?.webView?.visibility = View.VISIBLE
            onBackPressedCallback.isEnabled = true
            if (psaId != Constants.INVALID_VALUE) {
                dismissPsa(psaId)
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hidePSA()
        }
    }

    /**
     * JS interface to close the PSA.
     *
     * We need close this fragment here, so that when we get a new PSA, we can display it.
     */
    @JavascriptInterface
    fun hidePSA() {
        uiHandler.post {
            val currentActivity = activity
            if (currentActivity is BaseActivity && binding?.webView?.visibility == View.VISIBLE) {
                onBackPressedCallback.isEnabled = false
                onBackPressedCallback.remove()
                currentActivity.supportFragmentManager
                    .beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss()
            }
        }
    }

    @Deprecated(
        "All activities and fragments should handle their own " + "onBackPressedDispatcher callbacks independent from any other fragments or activities."
    )
    fun consumeBack() = onBackPressedCallback.isEnabled

    companion object {
        private const val ARGS_URL_KEY = "URL"
        private const val ARGS_ID_KEY = "ID"
        private const val JS_INTERFACE = "megaAndroid"

        fun newInstance(url: String, id: Int): PsaWebBrowser {
            return PsaWebBrowser().apply {
                arguments = bundleOf(
                    ARGS_URL_KEY to url,
                    ARGS_ID_KEY to id
                )
            }
        }
    }
}
